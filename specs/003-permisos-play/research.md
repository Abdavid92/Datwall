# Research: Permisos conformes con Google Play

Revisión del código y de las dependencias del 2026-10-14. Rutas relativas a
`app/src/main/java/com/smartsolutions/paquetes/` salvo que se indique otra cosa.

## R1. Nada bloquea el uso de la app

- **Decisión**: se elimina la categoría `Required` de `Permission` y la comprobación `missingSomePermission()` de
  `DatwallKernel.main()`.
  - El kernel abre el primer uso solo si no se ha aceptado ni rechazado la explicación de datos (marca en DataStore)
    **y** el acceso de uso no está concedido.
  - Los usuarios que actualizan ya tienen el acceso de uso, así que no ven el primer uso (caso límite del spec).
  - La configuración de SIM y planes (`SetupActivity`) solo se exige si el permiso de teléfono está concedido; sin él
    se entra a la app y las pantallas que dependen de la SIM muestran el botón de configurar que ya existe.
- **Por qué**: FR-001. Hoy la app vuelve a `PermissionsActivity` en cada arranque mientras falte un permiso
  `Required`.
- **Alternativas**: mantener `Required` solo para el acceso de uso. Se descarta porque Play exige que la app siga
  funcionando si se rechaza, y las pantallas que no son de estadísticas (compra, cortafuegos estático, ajustes) no lo
  necesitan.

## R2. Una sola pantalla de permisos, para el primer uso y para Ajustes

- **Decisión**: `PermissionsActivity` deja el stepper y muestra una lista simple (RecyclerView) con:
  - una cabecera con la explicación de datos (FR-002);
  - una fila por permiso: nombre, para qué se usa, estado y botón "Conceder" (o "Abrir ajustes", ver R3).
  - En el primer uso: solo los permisos marcados `atStartup` (acceso de uso y, en Android 13+, notificaciones), con
    los botones "Continuar" y "Ahora no".
  - Desde Ajustes → "Permisos y privacidad": todos los permisos y la recomendación de batería (FR-011a).
- **Por qué**: DRY (una pantalla para los dos usos) y KISS. Quitar el stepper elimina la dependencia
  `com.github.fython:MaterialStepperView`, que solo se usa ahí. El rediseño visual queda para la fase 4.
- **Alternativas**: mantener el stepper para el primer uso y crear otra lista en Ajustes; dos pantallas para lo mismo.

## R3. Permiso rechazado de forma permanente

- **Decisión**: una función pura decide la acción a partir de tres datos:
  - el permiso está concedido → no hacer nada;
  - el sistema permite volver a preguntar (`shouldShowRequestPermissionRationale`) o nunca se pidió → pedir;
  - se pidió antes y el sistema ya no deja preguntar → abrir los ajustes de la app
    (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`).
  - "Se pidió antes" se guarda en DataStore (conjunto de códigos de permiso pedidos), porque
    `shouldShowRequestPermissionRationale` devuelve `false` tanto antes de la primera petición como tras un rechazo
    permanente.
  - La usan `SinglePermissionFragment` y la lista de R2.
- **Por qué**: FR-004; sin esto, al pulsar "Conceder" sobre un permiso rechazado de forma permanente no pasa nada.
- **Pruebas**: la función pura se prueba en `src/test` con las combinaciones.

## R4. SMS: API SMS User Consent

- **Decisión**:
  - Se eliminan `READ_SMS` y `RECEIVE_SMS`, `SmsReceiver`, `SmsInboxReaderHelper` y `seedOldPurchasedPackages`.
  - Se añade `com.google.android.gms:play-services-auth-api-phone:18.3.0` (Google Maven, publicada el 2025-09-29; la
    18.3.1 es de 2026-07-07 y se evita por reciente).
  - Al pulsar "Comprar" en `PackagesHolderFragment`:
    1. Se registra la compra "sin confirmar" (ya ocurre: `newPurchased` con `pending = true`).
    2. Se llama a `SmsRetriever.getClient(context).startSmsUserConsent(null)` (cualquier remitente que no sea un
       contacto; ETECSA es alfanumérico). El sistema escucha 5 minutos.
    3. Un receptor registrado mientras la pantalla está viva recibe `SmsRetriever.SMS_RETRIEVED_ACTION` con el intent
       de consentimiento y lo lanza.
    4. Si el usuario acepta, se lee el texto, se busca el plan con `DataPackages.findPurchasedPackage` y se confirma
       la compra pendiente de ese plan en la SIM de la compra.
  - Si Google Play Services no está o la tarea falla, no se hace nada (la compra queda "sin confirmar").
  - Ya no se tratan los SMS de bonos de promoción ni de Bolsa Mensajería: los bonos se leen con `*222*266#` al
    sincronizar.
- **Por qué**: decisión del usuario (FR-006–FR-008). Play permite SMS User Consent sin permisos de SMS.
- **Excepción a la constitución (principio I, "APIs de Android antes que terceros")**: Android no ofrece una
  alternativa sin permisos; la biblioteca es oficial de Google y del repositorio `google()`. Ver Complexity Tracking.
- **Alternativas**: SMS Retriever API (exige que el SMS lleve un hash de la app; ETECSA no lo envía); mantener los
  permisos como opcionales (no cumple Play).

## R5. Compras sin confirmar en el historial

- **Decisión**: `HistoryViewModel` deja de filtrar `!pending`; la fila muestra la marca "Sin confirmar" cuando
  `pending` es verdadero. No cambia el esquema (`pending` ya existe en `purchased_packages`).
- **Confirmación**: `confirmPurchased(dataPackageId, simId)` marca como confirmada una compra pendiente de ese plan y
  esa SIM. Hoy además **borra las compras pendientes de más de un día**, lo que haría desaparecer del historial las
  compras sin confirmar: ese borrado se elimina. Se confirma la compra pendiente más reciente de las últimas 24 horas
  (la ventana de espera es de 5 minutos, así que una más antigua no puede ser la del SMS).
- Al confirmar no se suman datos (`addDataBytes`): el saldo real llega por la sincronización, y sumarlos duplicaría
  los datos si la sincronización ya los registró.
- La lógica de elegir qué compra confirmar se extrae a una función pura con pruebas.
- **Por qué**: FR-008a.

## R6. Accesibilidad con divulgación

- **Decisión**:
  - `StartAccessibilityServiceFragment` muestra un texto de divulgación (qué lee: solo el texto de la respuesta USSD
    en la app de teléfono; para qué; que no se guarda ni se envía) con "Acepto" y "No, gracias".
  - Solo se muestra desde la sincronización y únicamente si la lectura va por el camino legacy (API < 26 o modo
    moderno desactivado), que es el único que usa el servicio.
  - `accessibility_service_config.xml`: se quita `settingsActivity` (apunta a una clase inexistente) y la descripción
    explica el uso real.
- **Por qué**: FR-009 y FR-010; política de Play sobre el uso de la API de accesibilidad por apps que no son de
  accesibilidad (divulgación destacada y consentimiento).

## R7. Batería

- **Decisión**: se elimina `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` del manifiesto. La recomendación abre
  `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, que no necesita permiso. Aparece solo en "Permisos y
  privacidad".
- **Por qué**: FR-011; Play restringe el diálogo directo de exención.

## R8. Peticiones en contexto que faltan

- **Ya existen** (con `SinglePermissionFragment`): llamadas al comprar, sincronizar, USSD rápidos del panel y
  configurar SIM; superposición al activar la burbuja; VPN al activar el cortafuegos.
- **Faltan**:
  - la sincronización en segundo plano (`SynchronizationWorker`) debe comprobar el permiso de llamadas antes de
    marcar y omitir la ejecución sin notificar;
  - `DatwallKernel` deja de notificar "faltan permisos".
- **Acceso de uso denegado**: hay que revisar en el dispositivo qué pantallas y servicios lanzan excepciones sin el
  acceso de uso (estadísticas, cortafuegos dinámico, burbuja) y protegerlos; las pantallas de estadísticas muestran
  la explicación y el botón de conceder.

## R9. Limpieza de permisos y red

- **Manifiesto**: se quitan `READ_PRIVILEGED_PHONE_STATE`, `REQUEST_INSTALL_PACKAGES`, `CHANGE_WIFI_STATE`,
  `ACCESS_WIFI_STATE`, `READ_SMS`, `RECEIVE_SMS` y `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
- **Escáner QR de recarga**: las tarjetas de recarga de ETECSA ya no existen (indicación del usuario). Se elimina la
  opción "Recargar saldo" del menú de compra (`packages_menu.xml`, `PackagesFragment`: escáner y `*662*código#`),
  `PackagesViewModel.sendUssdCode`, la `CaptureActivity` y el `overrideLibrary` del manifiesto, las dependencias
  `zxing-android-embedded` y `zxing:core` y los textos `recharge_credit` y `fail_scan_qr`. Con ello desaparece el
  permiso `CAMERA`, que añadía la biblioteca.
- **`network_security_config.xml`**: se eliminan los cuatro dominios sin cifrar; queda
  `cleartextTrafficPermitted="false"` como base. La comprobación de Apklis usa `https://api.apklis.cu/` (verificado en
  la biblioteca).
- **Dependencias**:
  - Se quitan Picasso, MaterialStepperView (sin uso tras R2) y zxing (escáner QR).
  - Retrofit, converter-gson y RxJava **se mantienen**: no los usa el código de la app, pero los necesita la
    biblioteca de actualizaciones de Apklis (`apklisupdate` v1.4); la declaración explícita fija versiones actuales.
  - Gson se usa en el código (`AbstractAppRepository`, `DataPackage`).
- **Textos**: se eliminan los strings del licenciamiento con Transfermóvil y `PreferencesKeys.LICENSE` y
  `WAITING_PUCHASE`, que no se usan.

## R10. Justificación de permisos restringidos (FR-015)

| Permiso | Justificación |
|---|---|
| `QUERY_ALL_PACKAGES` | El cortafuegos por app y las estadísticas por app necesitan ver todas las apps con acceso a Internet, incluidas las que no tienen icono en el lanzador. Es la función principal. |
| `PACKAGE_USAGE_STATS` | Consumo de datos por app (`NetworkStatsManager`) y app en primer plano para el cortafuegos dinámico. |
| Accesibilidad | Leer la respuesta USSD del marcador en Android 6–7 o en modo legacy, donde no existe `sendUssdRequest`. Solo actúa sobre `com.android.phone` mientras espera una respuesta. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Medición continua del consumo, burbuja flotante y cortafuegos VPN local; ningún tipo estándar (`dataSync`, `connectedDevice`…) describe estas tareas continuas iniciadas por el usuario. |
| `BIND_VPN_SERVICE` | Cortafuegos local; el tráfico nunca sale del dispositivo (principio IV). |
| `CALL_PHONE` | Marcar códigos USSD de ETECSA (saldo y compra). |

El texto se muestra resumido en la explicación de datos (R2) y completo en este archivo.
