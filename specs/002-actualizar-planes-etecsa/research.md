# Research: Actualizar los planes de ETECSA

Fecha: 2026-10-13. Basado en el inventario del código de la rama `002-actualizar-planes-etecsa`.

## Restricciones del código actual

- `PackageId` se guarda en Room por **ordinal** (`PackageIdConverter`): quitar o reordenar valores del
  enum rompe las filas existentes de `data_packages` y `purchased_packages`.
- `DataBytes.DataType` se guarda **por nombre**: quitar un valor rompe la lectura de `users_data_bytes`.
- La base de datos está en la versión 1, **sin migraciones** y sin `fallbackToDestructiveMigration`.
- El catálogo se escribe con `@Insert(REPLACE)`; `purchased_packages` tiene FK a `data_packages` con
  `onDelete = CASCADE`. Un `REPLACE` borra y reinserta la fila: puede **borrar en cascada el historial**.
- Ningún camino USSD recibe la SIM: `sendUSSDRequestOreo` usa el `TelephonyManager` por defecto y
  `sendUSSDRequestLegacy` lanza `ACTION_CALL` sin cuenta telefónica.

## R1. Catálogo sin cambiar el esquema

- **Decisión**: los cinco planes vigentes se definen en `DataPackages.kt`. El Plan Diario **reutiliza**
  `PackageId.DailyBag` (mismo concepto, ahora para todas las redes). Los cuatro planes de 35 días son
  valores **nuevos al final** del enum (`Plan_4_5GB`, `Plan_2GB`, `Plan_4GB`, `Plan_6GB`). Los valores
  obsoletos se quedan en el enum y en el catálogo con `deprecated = true`. `PACKAGES_VERSION` pasa a 4 y
  `GENERAL_DURATION` a 35.
- **Por qué**: conserva los ordinales y las filas existentes sin migración de base de datos.
- **Alternativas**: renombrar o borrar valores obsoletos (rompe ordinales y el historial).

## R2. Escribir el catálogo sin borrar el historial

- **Decisión**: sustituir el `REPLACE` del catálogo por un *upsert* (`@Upsert` de Room: actualiza si
  existe, inserta si no), que no borra la fila y por tanto no dispara el `CASCADE`.
- **Por qué**: FR-004 (conservar el historial).

## R3. Códigos de compra en un único lugar

- **Decisión**: `DataPackages` define `DAILY_PLAN_USSD = "*133*1*3#"` y
  `PLANS_USSD_PREFIX = "*133*1*4*"`; `buildUssd` usa solo esas constantes y el `index` de cada plan.
  Los planes de 35 días usan `index` 1–4 según el orden del menú `*133*1*4#`.
- **Por qué**: FR-002 y FR-008. Si ETECSA cambia el menú, se edita solo `DataPackages.kt`.

## R4. Configuración por SIM: "puede comprar planes sí/no"

- **Decisión**: se reutiliza `Sim.network` sin cambiar el esquema: `NETWORK_3G_4G` = puede comprar planes,
  `NETWORK_NONE` = no puede. Los cinco planes nuevos usan `NETWORK_3G_4G`. "Configurada" pasa a
  significar `setupDate > 0` (hoy es `network != NONE`, que dejaría bloqueadas las líneas que no pueden
  comprar). La configuración es solo manual (sí/no); se elimina el modo automático, que marcaba `*133#`
  con el servicio de accesibilidad y escribía siempre en la SIM de voz por defecto.
- **Migración de datos**: al aplicar la versión 4 del catálogo, las SIM con `network != NONE` pasan a
  `NETWORK_3G_4G`.
- **Por qué**: FR-006 (respuesta Q1: B) y KISS.
- **Alternativas**: columna nueva en `sims` (requiere migración de Room).

## R5. USSD por la SIM elegida

- **Decisión**: `USSDHelper` recibe la `Sim` en todos sus caminos.
  - Modo moderno: `telephonyManager.createForSubscriptionId(sim.id.toInt())` antes de `sendUssdRequest`.
  - Modo legacy (`ACTION_CALL`): se añade `TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE` con la cuenta de la
    SIM. En API 30+ se busca con `TelephonyManager.getSubscriptionId(handle)`; en versiones anteriores se
    compara `handle.id` con el `subscriptionId` o el `iccId`. Si no se encuentra, se lanza sin cuenta y el
    sistema muestra su selector (comportamiento actual).
  - La sincronización guarda los datos en la `sim` recibida, no en la SIM de voz por defecto.
  - Se elimina la comprobación "¿es esta SIM la predeterminada para llamadas?" en la compra y la
    sincronización, que ya no hace falta.
- **Por qué**: FR-005. Los ids de `Sim` ya son el `subscriptionId`.

## R6. Lectura del saldo con el formato nuevo

- **Decisión**: extraer la lectura a una clase pura `UssdBalanceParser` (probada con JUnit, sin Android).
  - `*222*328#`: los datos para todas las redes van tras `Datos:` → `DataType.International`, con
    vencimiento = ahora + "validos N dias". Sin `Datos:` (p. ej. "Tarifa: No activa...") → cero, sin error.
    Se mantiene `Diaria:` por si el Plan Diario usa ese formato (no verificado).
  - `*222*266#`: sin cambios (`Datos.cu ... vence dd-MM-yy` ya se lee bien); se cubre con prueba.
  - Ejemplos reales de [spec.md](spec.md) como casos de prueba.
- **Bug relacionado**: `DataBytes.getDataTypeOfInternationalNetworks()` usa `==` con `&&` y devuelve
  siempre una lista vacía; el consumo de datos "todas las redes" no se descuenta. Con la oferta nueva
  todo es "todas las redes", así que se corrige.
- **Duración**: los 30 días fijos de `UserDataBytesManager` pasan a `GENERAL_DURATION` (35).

## R7. Detección de compras por SMS

- **Decisión**: extraer la comparación a una función pura que normaliza el texto (sin espacios, sin
  mayúsculas) antes de buscar el `smsKey`. Claves:

| Plan | `smsKey` | Estado |
|---|---|---|
| Plan 120 | `Plan 2GB+15MIN+20SMS` | ✅ verificada |
| Plan 240 | `Plan 4GB+35MIN+40SMS` | ✅ verificada |
| Plan 360 | `Plan 6GB+60MIN+70SMS` | ✅ verificada |
| Plan 4.5 GB | `Plan 4.5GB` | ✅ verificada |
| Plan Diario | `bolsa Diaria de 200MB` (clave antigua) | por confirmar |

- **Bug relacionado**: `SmsReceiver` solo acepta el remitente "cubacel"; los SMS de ETECSA del teléfono de
  pruebas llegan del remitente **"ETECSA"**. Se aceptan ambos.

## R8. Pantalla de compra

- **Decisión**: cambios mínimos (el rediseño es la fase 4): una sola sección "Planes" con los planes no
  obsoletos; `item_plan2` oculta los campos a cero (LTE, minutos, SMS); se quita la sección "otros"
  (Bolsa Mensajería) y el caso especial de `P_4GB_12GB_LTE`. `SimRepository` filtra `deprecated` en
  todas sus consultas.

## Fuera de alcance

- Quitar los textos "LTE" de las notificaciones y estadísticas (fase 4, UI).
- Planes Adicional, Extra, Sectorial, SMS y voz (decisión del usuario).
- La exactitud de las claves "por confirmar" hasta tener los SMS reales.
