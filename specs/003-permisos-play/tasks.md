# Tasks: Permisos conformes con Google Play

**Input**: Design documents from `/specs/003-permisos-play/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md),
[contracts/permisos.md](contracts/permisos.md), [quickstart.md](quickstart.md)

**Tests**: obligatorias por la constitución (principio III) para la lógica nueva: decisión de petición de permisos (R3)
y selección de la compra a confirmar (R5). Se escriben antes de la implementación y deben fallar primero.

**Rutas**: `B/` = `app/src/main/java/com/smartsolutions/paquetes/`, `R/` = `app/src/main/res/`,
`T/` = `app/src/test/java/com/smartsolutions/paquetes/`.

**Commits**: no se hace commit por tarea. Toda la implementación va en un único commit al final, cuando lo pida el
usuario.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: historia del spec (US1–US5)

---

## Phase 1: Setup

- [ ] T001 Comprobar que el árbol compila antes de empezar: `./gradlew testDebugUnitTest assembleDebug` con
  `JAVA_HOME=$HOME/.jdks/jbr-17.0.14`

---

## Phase 2: Foundational (bloquea US1 y US2)

**Purpose**: modelo de permisos sin categoría obligatoria y decisión de petición común.

- [ ] T002 [P] Escribir primero T/managers/PermissionRequestPolicyTest.kt: concedido → `Nothing`; no concedido y el
  sistema deja preguntar → `Request`; no concedido y nunca pedido → `Request`; no concedido, ya pedido y el sistema no
  deja preguntar → `OpenAppSettings` (tabla de [data-model.md](data-model.md), "Estado de una petición"). Debe fallar
- [ ] T003 Crear B/managers/PermissionRequestPolicy.kt: función pura
  `decide(granted: Boolean, requestedBefore: Boolean, canAskAgain: Boolean): PermissionAction` con
  `enum PermissionAction { Nothing, Request, OpenAppSettings }`. T002 pasa
- [ ] T004 En B/managers/models/Permission.kt sustituir `category: Category` (y el enum `Category`) por
  `atStartup: Boolean = false`; actualizar `equals`/`hashCode`
- [ ] T005 En B/managers/PermissionsManager.kt: quitar el permiso de SMS y el de optimización de batería; marcar
  `atStartup = true` solo en acceso de uso y notificaciones; adaptar las demás entradas al nuevo modelo. En
  B/managers/contracts/IPermissionsManager.kt quitar `SMS_CODE`, `BATTERY_OPTIMIZATION_CODE` y los casos de SMS de
  `getRequestCode`; sustituir `getDeniedPermissions(onlyRequired)` por `getDeniedPermissions(onlyAtStartup: Boolean)`
- [ ] T006 En B/PreferencesKeys.kt añadir `DATA_DISCLOSURE_ANSWERED` (Boolean), `REQUESTED_PERMISSIONS`
  (Set<String>) y `ACCESSIBILITY_DISCLOSURE_ACCEPTED` (Boolean); quitar `LICENSE` y `WAITING_PUCHASE` (sin uso)
- [ ] T007 Guardar el código del permiso en `REQUESTED_PERMISSIONS` cada vez que se lanza una petición del sistema
  (en el punto común de petición de B/ui/permissions/SinglePermissionFragment.kt y de la lista de US1) y exponer en
  `IPermissionsManager` una forma de consultar si un permiso se pidió antes

**Checkpoint**: compila con el modelo nuevo; T002 pasa.

---

## Phase 3: User Story 1 - Primer uso sin bloqueos (Priority: P1) 🎯 MVP

**Goal**: FR-001, FR-002, FR-011a. Explicación de datos y, como mucho, acceso de uso y notificaciones; nada bloquea.

**Independent Test**: [quickstart.md](quickstart.md) secciones 3 y 4.

- [ ] T008 [US1] Rehacer B/ui/permissions/PermissionsActivity.kt y R/layout/activity_permissions.xml como lista
  (RecyclerView) en lugar del stepper (R2): cabecera con la explicación de datos ([contracts/permisos.md](contracts/permisos.md),
  "Primer uso") y una fila por permiso con nombre, `description`, estado y botón "Conceder"/"Abrir ajustes" según
  `PermissionRequestPolicy`; reutilizar R/layout/item_permission.xml o adaptarlo. Borrar
  B/ui/permissions/StepperAdapter.kt
- [ ] T009 [US1] Modo primer uso de `PermissionsActivity` (extra o argumento): solo permisos `atStartup` no
  concedidos, botones "Continuar" y "Ahora no"; ambos guardan `DATA_DISCLOSURE_ANSWERED = true` y abren la app
  (`next()` del kernel). "Ahora no" no lanza ninguna petición
- [ ] T010 [US1] Modo Ajustes de `PermissionsActivity`: todos los permisos (concedidos marcados como tales) y la fila
  de recomendación de batería que abre `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` (R7); el estado se
  refresca en `onResume`. Simplificar B/ui/permissions/PermissionsViewModel.kt
- [ ] T011 [US1] Añadir la entrada "Permisos y privacidad" en R/xml/header_preferences.xml y
  R/xml-v26/header_preferences.xml que abre `PermissionsActivity` en modo Ajustes, con su manejo en
  B/ui/settings/SettingsActivity.kt
- [ ] T012 [US1] En B/DatwallKernel.kt: sustituir `missingSomePermission()` por "no se ha respondido la explicación
  (`DATA_DISCLOSURE_ANSWERED` falso) y el acceso de uso no está concedido" → abrir `PermissionsActivity` en modo
  primer uso; quitar la notificación `missing_permmissions_*`; exigir `missingSomeConfiguration()` solo si el
  permiso de llamadas/teléfono está concedido (R1)
- [ ] T013 [US1] Revisar en el emulador qué falla sin acceso de uso (B/managers/NetworkUsageManagerDefault.kt,
  B/managers/AbstractNetworkUsage.kt, B/watcher/WatcherUtils.kt, B/watcher/RxWatcher.kt, B/services/FirewallService.kt
  modo dinámico, B/services/BubleFloatingService.kt) y proteger cada punto para que no lance excepciones (R8)
- [ ] T014 [US1] Pantallas de estadísticas (B/ui/usage/UsageHolderFragment.kt y B/ui/resume/UsageGeneralFragment.kt):
  sin acceso de uso, mostrar la explicación del permiso y un botón que lo pida con `SinglePermissionFragment`
  (`USAGE_ACCESS_CODE`); al volver con el permiso concedido, cargar los datos sin reiniciar
- [ ] T015 [US1] Textos de la explicación de datos y de "Permisos y privacidad" en R/values/strings.xml y
  R/values-es/strings.xml; quitar `required_permission_denied`, `missing_permmissions_title_notification` y
  `missing_permmissions_description_notification` si quedan sin uso

**Checkpoint**: instalación limpia → explicación → app usable sin permisos.

---

## Phase 4: User Story 2 - Cada permiso al usar su función (Priority: P1)

**Goal**: FR-003–FR-005, FR-011. Peticiones en contexto con rechazo permanente resuelto.

**Independent Test**: [quickstart.md](quickstart.md) sección 5.

- [ ] T016 [US2] En B/ui/permissions/SinglePermissionFragment.kt: mostrar siempre "Ahora no" (ya no hay
  obligatorios); al pulsar "Conceder" usar `PermissionRequestPolicy` con `shouldShowRequestPermissionRationale` y
  `REQUESTED_PERMISSIONS`; si la acción es `OpenAppSettings`, cambiar el texto del botón a "Abrir ajustes" y abrir
  `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`; al volver, comprobar el permiso y notificar el resultado
- [ ] T017 [P] [US2] En B/workers/SynchronizationWorker.kt comprobar el permiso `CALL_PHONE` antes de sincronizar y
  omitir la ejecución sin notificar; revisar que B/exceptions/ExceptionsController.kt no notifique de forma repetida
  `MissingPermissionException` desde tareas en segundo plano
- [ ] T018 [P] [US2] Comprobar que las peticiones en contexto existentes usan `SinglePermissionFragment` y continúan
  la acción al conceder: compra (B/ui/packages/PackagesHolderFragment.kt), sincronización (B/ui/resume/ResumeFragment.kt),
  USSD rápidos, burbuja y cortafuegos (B/ui/dashboard/DashboardViewModel.kt), configuración de SIM
  (B/ui/settings/SimsDelegate.kt)

**Checkpoint**: con todo denegado, cada función explica, pide y no bloquea.

---

## Phase 5: User Story 3 - Confirmar la compra sin permisos de SMS (Priority: P2)

**Goal**: FR-006–FR-008a. SMS User Consent tras comprar; compras sin confirmar visibles.

**Independent Test**: [quickstart.md](quickstart.md) sección 7.

- [ ] T019 [P] [US3] Escribir primero T/managers/PendingPurchaseSelectorTest.kt: entre compras pendientes elegir la
  más reciente del plan y la SIM dados con antigüedad < 24 h; ninguna si son más antiguas, de otro plan o de otra SIM
  ([data-model.md](data-model.md), "Selección de la compra a confirmar"). Debe fallar
- [ ] T020 [US3] Crear la función pura de selección (B/managers/PendingPurchaseSelector.kt) y usarla en
  `confirmPurchased` de B/managers/PurchasedPackagesManager.kt: quitar el borrado de pendientes de más de un día y la
  creación de una compra nueva con `ConnectionMode.Unknown`. T019 pasa
- [ ] T021 [US3] Eliminar los SMS: B/receivers/SmsReceiver.kt, B/helpers/SmsInboxReaderHelper.kt,
  `seedOldPurchasedPackages` (B/managers/PurchasedPackagesManager.kt, su contrato y la llamada en
  B/ui/history/HistoryFragment.kt y B/ui/history/HistoryViewModel.kt), el receptor y `READ_SMS`/`RECEIVE_SMS` de
  `AndroidManifest.xml`, `IS_SEED_OLD_PURCHASED_PACKAGES` si queda sin uso, y los textos `sms_permission*`
- [ ] T022 [US3] En B/managers/DataPackageManager.kt sustituir `registerDataPackage(smsBody, simIndex)` por la
  confirmación de una compra a partir de un SMS compartido y la SIM de la compra: `DataPackages.findPurchasedPackage`
  → `confirmPurchased`; sin sumar datos (R5) y sin el tratamiento de bonos de promoción ni Bolsa Mensajería; ajustar
  B/managers/contracts/IDataPackageManager.kt
- [ ] T023 [US3] Añadir `implementation 'com.google.android.gms:play-services-auth-api-phone:18.3.0'` en
  app/build.gradle
- [ ] T024 [US3] En B/ui/packages/PackagesHolderFragment.kt (o su ViewModel): tras una compra iniciada con éxito,
  llamar a `SmsRetriever.getClient(context).startSmsUserConsent(null)`; registrar mientras la vista vive un receptor
  de `SmsRetriever.SMS_RETRIEVED_ACTION` (con `SmsRetriever.SEND_PERMISSION`, exportado) que lance el intent de
  consentimiento con un `ActivityResultLauncher`; con resultado OK leer `SmsRetriever.EXTRA_SMS_MESSAGE` y confirmar
  la compra (T022) en la SIM de la compra. Si la tarea falla o no hay Google Play Services, no hacer nada
- [ ] T025 [US3] Historial: en B/ui/history/HistoryViewModel.kt dejar de filtrar `!it.pending`; en
  B/ui/history/HistoryRecyclerAdapter.kt y R/layout/item_history.xml mostrar "Sin confirmar" cuando `pending`; texto
  en R/values/strings.xml y R/values-es/strings.xml

**Checkpoint**: sin permisos de SMS; la compra aparece "Sin confirmar" y se confirma al compartir el SMS.

---

## Phase 6: User Story 4 - Accesibilidad solo cuando hace falta y con consentimiento (Priority: P2)

**Goal**: FR-009, FR-010.

**Independent Test**: [quickstart.md](quickstart.md) sección 6.

- [ ] T026 [P] [US4] En B/ui/permissions/StartAccessibilityServiceFragment.kt y
  R/layout/fragment_start_accessibility_service.xml: texto de divulgación (qué lee: solo el texto de la respuesta
  USSD en la app de teléfono; para qué; no se guarda ni se envía) con "Acepto" y "No, gracias"; "Acepto" guarda
  `ACCESSIBILITY_DISCLOSURE_ACCEPTED` y abre el ajuste de accesibilidad; "No, gracias" notifica `onDenied` y no abre
  nada
- [ ] T027 [P] [US4] En R/xml/accessibility_service_config.xml quitar `android:settingsActivity`; reescribir
  `accessibility_service_description` en R/values/strings.xml y R/values-es/strings.xml con el uso real (sin
  licencia)
- [ ] T028 [US4] Comprobar que la divulgación solo se muestra desde la sincronización por el camino legacy
  (B/ui/resume/ResumeViewModel.kt `onAccessibilityServiceDisabled`, que solo ocurre con
  `ACCESSIBILITY_SERVICE_UNAVAILABLE`) y que "No, gracias" cancela la sincronización con un mensaje en
  B/ui/resume/ResumeFragment.kt

**Checkpoint**: en Android 7 la divulgación aparece antes de cualquier ajuste; en Android 8+ moderno no aparece.

---

## Phase 7: User Story 5 - Sin permisos ni restos innecesarios (Priority: P3)

**Goal**: FR-012–FR-015. Puede hacerse en cualquier momento; no depende de otras historias.

**Independent Test**: [quickstart.md](quickstart.md) sección 2.

- [ ] T029 [P] [US5] En app/src/main/AndroidManifest.xml quitar `READ_PRIVILEGED_PHONE_STATE`,
  `REQUEST_INSTALL_PACKAGES`, `CHANGE_WIFI_STATE`, `ACCESS_WIFI_STATE` y `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- [ ] T030 [P] [US5] Quitar el escáner QR de recarga: opción `action_scan_qr` de R/menu/packages_menu.xml y su
  manejo y el lanzador del escáner en B/ui/packages/PackagesFragment.kt, `sendUssdCode` de
  B/ui/packages/PackagesViewModel.kt, la `CaptureActivity` y `<uses-sdk tools:overrideLibrary=...>` de
  AndroidManifest.xml, las dependencias `zxing-android-embedded` y `zxing:core` de app/build.gradle y los textos
  `recharge_credit` y `fail_scan_qr`
- [ ] T031 [P] [US5] R/xml/network_security_config.xml: quitar los cuatro dominios en claro y dejar
  `<base-config cleartextTrafficPermitted="false" />`
- [ ] T032 [P] [US5] app/build.gradle: quitar Picasso y `com.github.fython:MaterialStepperView` (tras T008);
  mantener Retrofit, converter-gson y RxJava con un comentario que diga que los necesita `apklisupdate` (R9)
- [ ] T033 [P] [US5] Quitar los textos del licenciamiento con Transfermóvil de R/values/strings.xml y
  R/values-es/strings.xml (bloque `purchase_licence`, `open_transfermovil`, `banking_transfer`, `credit_transfer`,
  `transfer*`, `is_purchased*`, `is_trial_period*` y similares sin uso; comprobar con lint `UnusedResources`)
- [ ] T034 [US5] Revisar que la tabla de justificación de [research.md](research.md) (R10) coincide con el
  manifiesto final

**Checkpoint**: la app instalada no declara SMS, cámara ni los permisos sin uso.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [ ] T035 Ejecutar `./gradlew lintRelease testDebugUnitTest assembleDebug assembleRelease` y corregir lo nuevo
- [ ] T036 Validar [quickstart.md](quickstart.md) en el emulador Android 7 y en el teléfono (sin comprar planes sin
  autorización del usuario) y anotar los resultados al final de este archivo
- [ ] T037 Preparar el commit único de implementación y esperar a que el usuario lo pida

---

## Dependencies & Execution Order

- **Setup (T001)** → **Foundational (T002–T007)** → US1 y US2.
- **US1 (P1)**: depende de Foundational. T008 → T009/T010 → T011; T012 tras T009; T013 → T014.
- **US2 (P1)**: depende de Foundational; T016 comparte `SinglePermissionFragment` con T007: hacerlo después.
- **US3 (P2)**: independiente de US1/US2. T019 → T020; T021 → T022 → T024; T023 antes de T024.
- **US4 (P2)**: independiente.
- **US5 (P3)**: independiente; T032 después de T008 (stepper). T029–T031 y T033 pueden ir primero.
- **Polish**: al final.

### Parallel Opportunities

- T002 con T004–T006 (archivos distintos).
- US3, US4 y US5 en paralelo entre sí, y con US1/US2 salvo los archivos compartidos (`AndroidManifest.xml`,
  `strings.xml`, `app/build.gradle`).
- Dentro de US5: T029, T030, T031 y T033.

## Implementation Strategy

1. Foundational + US1 (MVP): la app deja de bloquearse en el primer uso.
2. US2: peticiones en contexto completas.
3. US3: fuera los SMS; es el cambio con más impacto en Play.
4. US4 y US5.
5. Verificación y un único commit de implementación cuando lo pida el usuario.
