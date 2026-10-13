---

description: "Lista de tareas: actualizar los planes de ETECSA"
---

# Tasks: Actualizar los planes de ETECSA

**Input**: Design documents from `/specs/002-actualizar-planes-etecsa/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/ussd-sms.md](contracts/ussd-sms.md), [quickstart.md](quickstart.md)

**Tests**: FR-011 y la constitución (principio III) exigen pruebas unitarias para la lógica nueva o
modificada. Se escriben antes de la implementación y deben fallar primero.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede hacer en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: historia de usuario (US1–US4)

## Path Conventions

- `B` = `app/src/main/java/com/smartsolutions/paquetes`
- `T` = `app/src/test/java/com/smartsolutions/paquetes`
- `R` = `app/src/main/res`
- **Nunca** pulsar "Comprar" en un dispositivo real: la app marca el código y compraría el plan.

---

## Phase 1: Setup

- [X] T001 Comprobar que la rama `002-actualizar-planes-etecsa` compila y pasa las pruebas antes de cambiar nada: `./gradlew testDebugUnitTest assembleDebug`

---

## Phase 2: Foundational (catálogo v4 sin cambiar el esquema)

**Purpose**: research R1, R2 y R4 (migración de SIM). **Bloquea todas las historias.**

- [X] T002 Escribir primero T/data/DataPackagesTest.kt: los planes con `deprecated = false` son exactamente `DailyBag`, `Plan_4_5GB`, `Plan_2GB`, `Plan_4GB`, `Plan_6GB` con los valores de data-model.md (precio, `bytes`, `nationalBytes` = 300 MB salvo el Diario = 0, `minutes`, `sms`, `network` = `NETWORK_3G_4G`, `index` 1–4 para los de 35 días, `duration` 35 y 1 para el Diario, `bytesLte` = 0); todos los demás valores del enum tienen `deprecated = true`; los ordinales de los 8 valores antiguos no cambian (`DailyBag` = 0 … `MessagingBag` = 7). Debe fallar
- [X] T003 En B/data/DataPackages.kt: añadir **al final** de `PackageId` los valores `Plan_4_5GB`, `Plan_2GB`, `Plan_4GB`, `Plan_6GB` (sin mover ni borrar los existentes); redefinir `DailyBag` como Plan Diario (25 CUP, 200 MB todas las redes, `NETWORK_3G_4G`, 24 horas, `smsKey` = `Plan Diario 200MB`); añadir los 4 planes de 35 días con los `smsKey` verificados de research.md R7 (`Plan 4.5GB`, `Plan 2GB+15MIN+20SMS`, `Plan 4GB+35MIN+40SMS`, `Plan 6GB+60MIN+70SMS`); marcar `deprecated = true` los 7 restantes; `PACKAGES_VERSION = 4`; `GENERAL_DURATION = 35`; añadir `DAILY_PLAN_USSD = "*133*1*3#"` y `PLANS_USSD_PREFIX = "*133*1*4*"`. Descripciones con la vigencia de 35 días y la suma de recursos (FR-009). T002 pasa
- [X] T004 En B/data/IDataPackageDao.kt cambiar el `@Insert(onConflict = REPLACE)` usado por `createOrUpdate` por `@Upsert`, para que actualizar el catálogo no borre en cascada `purchased_packages` (research R2)
- [X] T005 En B/managers/DataPackageManager.kt (`createOrUpdateDataPackages`, ≈ líneas 68–81): al pasar de una versión < 4 a la 4, poner `network = NETWORK_3G_4G` en las SIM con `network != NETWORK_NONE` (research R4, migración de datos)
- [X] T006 [P] En B/repositories/SimRepository.kt (`transform`, ≈ líneas 87–117) y B/repositories/DataPackageRepository.kt (`transform`, ≈ líneas 81–111) excluir los paquetes con `deprecated = true` en todas las consultas
- [X] T007 Compilar y pasar las pruebas; commit: "Catálogo de planes de ETECSA v4"

**Checkpoint**: catálogo nuevo en la base de datos sin perder filas antiguas.

---

## Phase 3: User Story 1 - Ver y comprar los planes actuales (Priority: P1) 🎯 MVP

**Goal**: la pantalla de compra muestra los 5 planes y cada uno marca su código (FR-001–FR-003, FR-006, FR-008).

**Independent Test**: [quickstart.md](quickstart.md) secciones 1, 3 y 4.

- [X] T008 [P] [US1] Escribir primero T/managers/BuildUssdTest.kt: `DailyBag` → `*133*1*3#`; `Plan_4_5GB` → `*133*1*4*1#`; `Plan_2GB` → `*133*1*4*2#`; `Plan_4GB` → `*133*1*4*3#`; `Plan_6GB` → `*133*1*4*4#`. Para poder probarlo sin Android, extraer `buildUssd` como función pura `buildPurchaseUssd(dataPackage)` en B/data/DataPackages.kt. Debe fallar
- [X] T009 [US1] Implementar `buildPurchaseUssd` en B/data/DataPackages.kt con `DAILY_PLAN_USSD` y `PLANS_USSD_PREFIX + index + "#"`, y hacer que `buildUssd` de B/managers/DataPackageManager.kt (≈ líneas 222–240) la use. T008 pasa
- [X] T010 [US1] Configuración "puede comprar planes sí/no" (FR-006): en B/ui/settings/PackagesConfigurationFragment.kt y B/ui/settings/PackagesConfigurationViewModel.kt sustituir el modo automático/manual y el diálogo de `R.array.sim_networks` por una elección sí/no que llame a `setDataPackagesManualConfiguration` con `NETWORK_3G_4G` (sí) o `NETWORK_NONE` (no); `complete()` exige que la SIM de voz esté configurada, no que pueda comprar
- [X] T011 [US1] En R/layout/fragment_packages_configuration.xml quitar el modo automático (`radio_group_mode`, `automatic_mode`, `manual_mode`, `btn_start_configuration`) y dejar la elección sí/no; actualizar R/values/array.xml (`sim_networks`) y los textos `all_packages_plan_available`, `packages_avaible`, `plan_avalaible`, `select_packages_available` en R/values/strings.xml y R/values-es/strings.xml
- [X] T012 [US1] En B/managers/DataPackageManager.kt: `isConfiguredDataPackages` pasa a `setupDate > 0` de la SIM de voz (research R4); eliminar `configureDataPackages` (modo automático) y su uso en el ViewModel y en B/managers/contracts/IDataPackageManager.kt
- [X] T013 [US1] Pantalla de compra: en B/ui/packages/PackagesViewModel.kt (`prepareListPackages`/`getNetworksPackages`, ≈ líneas 66–127) una sola sección "Planes" con los planes no obsoletos, sin la sección "otros" de `MessagingBag`; en B/ui/packages/PackagesRecyclerAdapter.kt quitar los casos especiales de `P_4GB_12GB_LTE` y `MessagingBag` (≈ líneas 95–109) y en el `PlanHolder` ocultar los campos con valor 0 (LTE, minutos, SMS, .cu); ajustar R/layout/item_plan2.xml si hace falta; `plan_title` = "Planes" en R/values/strings.xml y R/values-es/strings.xml
- [X] T014 [US1] En B/managers/DataPackageManager.kt (`buyDataPackage`, ≈ líneas 149–169) quitar la excepción de `MessagingBag`
- [X] T015 [US1] Compilar, pasar pruebas y validar quickstart.md secciones 3 y 4 en el teléfono (sin pulsar "Comprar"); commit: "Pantalla de compra y configuración con los planes vigentes"

**Checkpoint**: MVP — la app ofrece la oferta correcta con sus códigos.

---

## Phase 4: User Story 2 - La compra y la consulta usan la SIM elegida (Priority: P1)

**Goal**: USSD por la SIM elegida, también sin SIM de voz predeterminada (FR-005).

**Independent Test**: [quickstart.md](quickstart.md) sección 5.

- [X] T016 [US2] En B/helpers/USSDHelper.kt añadir un parámetro `sim: Sim?` a `sendUSSDRequest`, `sendUSSDRequestOreo` y `sendUSSDRequestLegacy`; en el modo moderno usar `telephonyManager.createForSubscriptionId(sim.id.toInt())` cuando `sim` no sea null y su id sea numérico (research R5)
- [X] T017 [US2] En `sendUSSDRequestLegacy` de B/helpers/USSDHelper.kt añadir `TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE` al `ACTION_CALL` con la cuenta de la SIM: en API ≥ 30 la cuenta de `TelecomManager.getCallCapablePhoneAccounts()` cuyo `TelephonyManager.getSubscriptionId(handle)` sea el de la SIM; en API < 30 la que tenga `handle.id` igual al `subscriptionId` o al `iccId`; si no se encuentra, sin extra (comportamiento actual)
- [X] T018 [US2] Pasar la SIM: B/managers/DataPackageManager.kt `buyDataPackageForUSSD` (≈ línea 213) usa `sim`; B/managers/SynchronizationManager.kt `synchronizeUserDataBytes(sim)` (≈ líneas 85–127 y 343–349) la pasa a USSD y guarda los datos en `sim`, no en `getDefaultSimBoth(VOICE)`
- [X] T019 [US2] Quitar la comprobación "¿es la SIM predeterminada para llamadas?" (`SimsHelper.invokeOnDefault(VOICE)`) en la compra de B/ui/packages/PackagesHolderFragment.kt (≈ líneas 108–127) y en la sincronización de B/ui/resume/ResumeViewModel.kt (≈ líneas 138–142), que debe sincronizar la SIM de la pestaña seleccionada
- [X] T020 [US2] Validar quickstart.md sección 5 en el teléfono (SIM 2, sin plan: 0 datos sin error; en el log de radio el USSD sale por `PHONE1`); commit: "USSD por la SIM elegida"

---

## Phase 5: User Story 3 - Conservar el historial de paquetes antiguos (Priority: P2)

**Goal**: FR-004.

**Independent Test**: [quickstart.md](quickstart.md) sección 2.

- [X] T021 [US3] Comprobar que B/ui/history (HistoryViewModel, HistoryRecyclerAdapter) muestra las compras de paquetes con `deprecated = true` (nombre y precio desde su fila) y corregir si los filtra
- [X] T022 [US3] Validar quickstart.md sección 2 en el emulador de Android 7 (registrar una compra antigua con la versión de `development`, actualizar, el historial la conserva); commit si hubo cambios

---

## Phase 6: User Story 4 - Saldo y compras detectados con la oferta nueva (Priority: P3)

**Goal**: FR-007 y FR-010 con los ejemplos reales de [contracts/ussd-sms.md](contracts/ussd-sms.md).

**Independent Test**: pruebas unitarias con los ejemplos reales; quickstart.md sección 5.

- [X] T023 [P] [US4] Escribir primero T/managers/UssdBalanceParserTest.kt con los textos exactos del contrato: "Tarifa: No activa. Ud debe adquirir una oferta. Para una nueva compra marque *133#" → International 0 sin error; "Tarifa: No activa. Datos: 130.08 MB validos 24 dias." → International 130.08 MB que vence en 24 días; "Datos.cu 273 MB vence 06-11-26." → National 273 MB que vence 06-11-2026 23:59:59. Debe fallar
- [X] T024 [US4] Crear B/managers/UssdBalanceParser.kt (sin dependencias de Android) moviendo desde B/managers/SynchronizationManager.kt `obtainDataBytesPackages`, `obtainDataByteBonus`, `getInternationals`, `getExpireDatePackages`, `getExpireDateBonus` y sus constantes; `*222*328#` lee los datos tras `Datos:` como `International` (se mantiene `Diaria:`); SynchronizationManager usa el parser. T023 pasa
- [X] T025 [P] [US4] Escribir primero T/repositories/DataBytesTest.kt: `DataBytes.getDataTypeOfInternationalNetworks()` devuelve `International` y `PromoBonus`. Debe fallar; corregir el `&&`/`==` en B/repositories/models/DataBytes.kt (≈ líneas 39–46)
- [X] T026 [P] [US4] En B/managers/UserDataBytesManager.kt sustituir los 30 días fijos (≈ líneas 91, 106, 188, 276) por `DataPackages.GENERAL_DURATION`
- [X] T027 [P] [US4] Escribir primero T/managers/PurchaseSmsMatcherTest.kt con los 5 SMS reales de spec.md: cada uno devuelve su `PackageId` (`DailyBag`, `Plan_4_5GB`, `Plan_2GB`, `Plan_4GB`, `Plan_6GB`); un SMS de promoción no devuelve ningún plan; el SMS de 240 no se confunde con el de 4.5 GB. Debe fallar
- [X] T028 [US4] Crear la función pura `findPurchasedPackage(smsBody)` en B/data/DataPackages.kt que normaliza (sin espacios, minúsculas) el texto y cada `smsKey` y devuelve el plan cuya clave completa aparece; usarla en `registerDataPackage` de B/receivers/SmsReceiver.kt (≈ líneas 171–209) y en `seedOldPurchasedPackages` de B/managers/PurchasedPackagesManager.kt (≈ líneas 107–158). T027 pasa
- [X] T029 [US4] En B/receivers/SmsReceiver.kt (≈ líneas 33–60) aceptar los remitentes `cubacel` y `ETECSA` sin distinguir mayúsculas (research R7)
- [X] T030 [US4] Compilar y pasar pruebas; validar la sincronización en el teléfono (SIM 2); commit: "Lectura del saldo y de los SMS con el formato nuevo"

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T031 Ejecutar `./gradlew lintRelease testDebugUnitTest assembleDebug assembleRelease` y corregir lo nuevo
- [X] T032 [P] Revisar textos que mencionen "LTE", "Bolsa" o "Mensajería" en la pantalla de compra y la configuración (R/values/strings.xml, R/values-es/strings.xml); las notificaciones y estadísticas quedan para la fase 4
- [X] T033 Anotar en este archivo los resultados de la verificación en dispositivo y lo que queda pendiente

---

## Dependencies & Execution Order

- **Setup (Phase 1)** → **Foundational (Phase 2)**: bloquea todo.
- **US1 (P1)**: depende de Foundational.
- **US2 (P1)**: depende de Foundational; T018 toca DataPackageManager igual que US1 (T009, T012, T014): hacerlas en secuencia.
- **US3 (P2)**: depende de Foundational (T004 es la protección del historial).
- **US4 (P3)**: depende de Foundational (los `smsKey` del catálogo); T024 toca SynchronizationManager igual que T018: en secuencia.
- **Polish**: al final.

### Parallel Opportunities

- Foundational: T006 en paralelo con T004/T005.
- US1: T008 en paralelo con el resto de US1 hasta T009.
- US4: T023, T025, T026 y T027 entre sí.

---

## Parallel Example: User Story 4

```text
Task: "T023 [US4] UssdBalanceParserTest con los ejemplos reales"
Task: "T025 [US4] DataBytesTest y arreglo de getDataTypeOfInternationalNetworks"
Task: "T026 [US4] 35 días en UserDataBytesManager"
Task: "T027 [US4] PurchaseSmsMatcherTest con los 5 SMS reales"
```

---

## Implementation Strategy

### MVP First

1. Setup + Foundational → catálogo v4 sin perder historial.
2. US1 → la app ofrece la oferta correcta con sus códigos.
3. **Parar y validar** en el teléfono (sin comprar).

### Incremental Delivery

1. US1 (oferta y códigos) → US2 (SIM elegida) → US4 (saldo y SMS) → US3 (verificación del historial).
2. Cada historia termina con su commit; al final, merge `--no-ff` en `development`.

---

## Verificación (2026-10-13)

**Pruebas unitarias**: 27 pasan (`DataPackagesTest`, `BuildPurchaseUssdTest`, `UssdBalanceParserTest`,
`PurchaseSmsMatcherTest`, `DataBytesTest` y las previas). `lintRelease`, `assembleDebug` y
`assembleRelease` sin errores.

**Teléfono** (REDMI 15C, Android 16, dos SIM Cubacel; llamadas en "preguntar siempre"):

- Configuración: SIM 1 "no puede comprar planes", SIM 2 "sí". La pantalla de compra de la SIM 2
  muestra los 5 planes vigentes; la de la SIM 1 muestra "Esta línea no puede comprar planes" sin el
  botón de configurar. No se pulsó "Comprar".
- Sincronización por la SIM de la pestaña, sin preguntar la SIM por defecto:
  - SIM 2 (`*222*328#`, `*222*266#`): Plan Diario 200 MB sin vencimiento ("no activos"); sin bonos.
  - SIM 1 empresarial (`*111#`): 16 700 MB sin vencimiento.

**Emulador** (Android 7): versión de `development` con tres compras antiguas (Plan Básico, Paquete 16 GB,
Bolsa Mensajería) → actualización a esta versión: catálogo v4, paquetes antiguos obsoletos, SIM
configurada y el historial conserva las tres compras con nombre y precio.

**Pendiente / no verificado**:

- La detección del SMS de compra se probó con los textos reales en pruebas unitarias, no con un SMS
  recibido en el teléfono.
- No se probó la compra real (por diseño: la app marca el código y compraría).
- En una línea empresarial, `*111#` también informa minutos y SMS; la app solo registra los datos.
- La sincronización programada (`SynchronizationWorker`) sigue usando la SIM de voz por defecto.
- Para la fase 4: textos LTE/Mensajería en notificaciones y estadísticas, layouts sin uso
  (`fragment_default_sims_dialog`, `item_sim_default`, `activity_ask`) y avisos de lint previos
  (`UnusedResources`, `SetTextI18n`).
