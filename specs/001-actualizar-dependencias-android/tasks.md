---

description: "Lista de tareas: actualizar dependencias y compatibilidad con Android 14–16"
---

# Tasks: Actualizar dependencias y compatibilidad con Android 14–16

**Input**: Design documents from `/specs/001-actualizar-dependencias-android/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/manifest.md](contracts/manifest.md),
[quickstart.md](quickstart.md)

**Tests**: la constitución (principio III) exige pruebas unitarias para la lógica nueva y una
prueba que reproduzca cada error corregido. Se incluyen donde la lógica se puede probar; lo que
depende del sistema (servicios, insets, botón atrás) se verifica con [quickstart.md](quickstart.md).

**Organization**: tareas agrupadas por historia de usuario de la spec.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede hacer en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: historia de usuario (US1–US4)

## Path Conventions

- `B` = `app/src/main/java/com/smartsolutions/paquetes`
- `T` = `app/src/test/java/com/smartsolutions/paquetes`
- `R` = `app/src/main/res`
- Para compilar se usa JDK 17+ (el JBR de Android Studio sirve a partir de la tarea T001).

---

## Phase 1: Setup (herramientas de build, sin cambios de comportamiento)

**Purpose**: AGP 9, Gradle 9, Kotlin integrado y KSP; eliminar kapt y data binding (research R1, R2).
Al terminar la fase la app compila con `targetSdk 32` y se comporta igual.

- [X] T001 Cambiar `distributionUrl` a `gradle-9.8.1-bin.zip` en gradle/wrapper/gradle-wrapper.properties
- [X] T002 En build.gradle (raíz): `com.android.tools.build:gradle:9.4.1`, `kotlin_version = "2.4.21"`, `hilt_version = '2.60.1'`, `room_version = "2.8.5"`, añadir classpath `com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.12`; eliminar las variables `ext` sin uso (`dagger_version`, `retrofit_version` si no se referencian)
- [X] T003 En app/build.gradle: quitar los plugins `kotlin-android` y `kotlin-kapt` (Kotlin queda integrado en AGP 9); cambiar `kotlin-parcelize` por `org.jetbrains.kotlin.plugin.parcelize`; añadir `com.google.devtools.ksp`; eliminar el bloque `kapt { ... }`; sustituir `kotlinOptions { jvmTarget = '11' }` y `compileOptions` por `JavaVersion.VERSION_17`; cambiar `kapt` → `ksp` en `hilt-android-compiler`, `androidx.hilt:hilt-compiler` y `room-compiler`, y `kaptAndroidTest` → `kspAndroidTest`
- [X] T004 [P] Pasar R/layout/item_app.xml de data binding a view binding (quitar `<layout>`/`<data>` y las expresiones `@{}`) y asignar esos valores desde código donde hoy se hace `binding.app = ...` en B/ui/applications/AppsListAdapter.kt (≈ línea 266)
- [X] T005 [P] Igual con R/layout/item_theme.xml: quitar la variable `checked` y asignar el estado desde B/ui/settings/ThemesAdapter.kt (≈ línea 53)
- [X] T006 [P] Quitar `<layout>`/`<data>` de R/layout/fragment_packages_configuration.xml; la expresión `network` (≈ línea 252) nunca se asigna, sustituirla por el valor por defecto que muestra hoy; comprobar que B/ui/settings/PackagesConfigurationFragment.kt (≈ línea 52) sigue inflando el binding
- [X] T007 [P] Quitar `<layout>`/`<data>` de R/layout/activity_ask.xml y la expresión `@{app.name}` (su binding no se usa en ningún sitio; dejar el texto vacío)
- [X] T008 Quitar `buildFeatures.dataBinding = true` de app/build.gradle (mantener `viewBinding true`); en vpncore/build.gradle pasar `compileOptions` a `JavaVersion.VERSION_17`
- [X] T009 Compilar con `./gradlew clean assembleDebug assembleRelease testDebugUnitTest`; corregir los errores derivados del cambio de herramientas. Comprobar que la `version` de cada `@Database` de Room no ha cambiado (data-model.md: "La versión de la base de datos Room y su esquema **no cambian**")
- [X] T010 Commit: "Migrar a AGP 9.4, Gradle 9.8, Kotlin integrado y KSP"

**Checkpoint**: compila igual que antes, sin kapt.

---

## Phase 2: Foundational (bibliotecas, SDKs y limpieza)

**Purpose**: versiones de research R10, `minSdk 23`, eliminar código para API < 23 (R8) y subir a
`targetSdk 36`. **Bloquea todas las historias.**

- [X] T011 En app/build.gradle: `compileSdk 37`, `minSdkVersion 23`; en vpncore/build.gradle: `compileSdk 37`, `minSdkVersion 23` y quitar `targetSdkVersion` (obsoleto en bibliotecas)
- [X] T012 Actualizar en app/build.gradle las versiones de la tabla R10 de research.md: core-ktx 1.19.1, appcompat 1.8.0, material 1.14.0, constraintlayout 2.2.2, navigation 2.10.2, lifecycle 2.11.0, preference-ktx 1.2.1, datastore-preferences 1.2.1, work-runtime-ktx 2.12.0, recyclerview 1.4.0, hilt-work y hilt-compiler 1.4.0, Retrofit/converter-gson/adapter-rxjava2 3.0.0, Gson 2.14.0, RxJava 2.2.21, taptargetview 1.15.0, zxing-android-embedded 4.3.0 (mantener `transitive = false` y core 3.3.0), androidx.test.ext:junit 1.3.0, espresso-core 3.7.0; añadir `androidx.activity:activity-ktx:1.13.0`, `androidx.fragment:fragment-ktx:1.9.1` y `androidx.localbroadcastmanager:localbroadcastmanager:1.1.0`; eliminar `legacy-support-v4` y `kotlin-stdlib` explícito
- [X] T013 [P] En vpncore/build.gradle: `androidx.annotation:annotation:1.11.0`, `junit:junit:4.13.2`, androidx.test.ext:junit 1.3.0, espresso-core 3.7.0
- [X] T014 Sustituir `commons-lang:commons-lang:2.4` por `org.apache.commons:commons-lang3:3.21.0` en app/build.gradle y cambiar `import org.apache.commons.lang.time.DateUtils` por `import org.apache.commons.lang3.time.DateUtils` en los 11 archivos de B que lo importan (localizarlos con `grep -rl "org.apache.commons.lang.time" app/src/main`)
- [X] T015 Compilar y corregir los errores de las nuevas versiones (APIs que cambiaron de nulabilidad o de firma); si una biblioteca falta tras quitar `legacy-support-v4`, añadir la dependencia AndroidX concreta. `./gradlew assembleDebug testDebugUnitTest` debe pasar
- [X] T016 Commit: "Actualizar bibliotecas y subir minSdk a 23"
- [X] T017 [P] Eliminar el camino de `ConnectivityManager.CONNECTIVITY_ACTION` para API ≤ 22: el registro en B/DatwallKernel.kt (≈ líneas 286–292), B/receivers/ChangeNetworkReceiver.kt y B/receivers/AbstractBroadcastReceiver.kt si queda sin uso
- [X] T018 [P] Hacer que B/modules/ManagersModule.kt (≈ línea 21) proporcione siempre `NetworkUsageManagerDefault` y eliminar B/managers/NetworkUsageManagerLegacy.kt
- [X] T019 [P] Eliminar `compactTrafficsLollipop` y su rama en B/workers/TrafficDbOptimizer.kt (≈ línea 44) y las ramas para API < 23 de B/watcher/TrafficRegistration.kt (≈ líneas 88, 193–217, 229, 251). No borrar tablas ni columnas de Room (data-model.md)
- [X] T020 Eliminar el resto de comprobaciones `Build.VERSION.SDK_INT` contra `KITKAT`, `LOLLIPOP`, `LOLLIPOP_MR1` o `M` (dejar solo la rama que se ejecuta con API ≥ 23) y las anotaciones `@RequiresApi` de esos niveles en app y vpncore; localizarlas con `grep -rnE "VERSION_CODES\.(KITKAT|LOLLIPOP|LOLLIPOP_MR1|M)\b" app/src/main vpncore/src/main`. Incluye las entradas con `minSdk` 21/23 de B/managers/PermissionsManager.kt y las ramas `else` de los `PendingIntent`
- [X] T021 Compilar y pasar las pruebas; commit: "Eliminar código para Android 5"
- [X] T022 Subir `targetSdkVersion` a 36 en app/build.gradle y eliminar el bloque `lint { disable 'ExpiredTargetSdkVersion' }`
- [X] T023 Commit: "Subir targetSdk a 36" (a partir de aquí las historias corrigen el comportamiento)

**Checkpoint**: bibliotecas al día, `targetSdk 36`; las historias pueden empezar.

---

## Phase 3: User Story 1 - Cortafuegos y medición en Android 14–16 (Priority: P1) 🎯 MVP

**Goal**: el cortafuegos bloquea, sigue activo en segundo plano y tras reiniciar; la medición sigue
registrando (FR-002, FR-003, FR-004).

**Independent Test**: [quickstart.md](quickstart.md) secciones 2, 3 y 4 en Android 14, 15 y 16.

- [X] T024 [US1] En app/src/main/AndroidManifest.xml: añadir `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"/>`; en `.services.FirewallService` y `.services.DatwallService` añadir `android:foregroundServiceType="specialUse"` y el `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="..."/>` con el texto exacto de contracts/manifest.md
- [X] T025 [P] [US1] En B/services/FirewallService.kt: cambiar `startForeground` (≈ línea 378) por `ServiceCompat.startForeground(this, id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)` (el tipo solo en API ≥ 34, `ServiceCompat` lo gestiona); `stopForeground(true)` (≈ línea 370) por `ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)`; los `getParcelableExtra<App>` (≈ líneas 95, 110, 124) por `IntentCompat.getParcelableExtra(intent, key, App::class.java)`
- [X] T026 [P] [US1] En B/services/DatwallService.kt: el mismo cambio de `startForeground` (≈ línea 130) y `stopForeground` (≈ línea 117)
- [ ] T027 [US1] Compilar, instalar en emuladores Android 14, 15 y 16 y validar con quickstart.md secciones 2–4; commit: "Declarar tipos de servicio en primer plano del cortafuegos y la medición"

**Checkpoint**: MVP — la función principal funciona en Android 14–16.

---

## Phase 4: User Story 2 - Notificación persistente y burbuja (Priority: P2)

**Goal**: pedir el permiso de notificaciones en Android 13+ y que la burbuja siga funcionando
(FR-005, FR-006).

**Independent Test**: [quickstart.md](quickstart.md) sección 5.

- [X] T028 [P] [US2] Escribir primero T/managers/NotificationPermissionPolicyTest.kt (JUnit 4) para `isNotificationPermissionRequired(sdkInt: Int, granted: Boolean)`: API 32 sin conceder → `false`; API 33 sin conceder → `true`; API 33 concedido → `false`; API 36 sin conceder → `true`; API 36 concedido → `false`. Debe fallar antes de T029
- [X] T029 [US2] Implementar `isNotificationPermissionRequired` como función pura en B/managers/NotificationPermissionPolicy.kt (requerido solo si `sdkInt >= 33` y no está concedido); `./gradlew testDebugUnitTest` pasa
- [X] T030 [US2] Declarar `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>` en app/src/main/AndroidManifest.xml y añadirlo como permiso opcional (la app sigue funcionando si se rechaza) en la lista de B/managers/PermissionsManager.kt, usando `isNotificationPermissionRequired` para decidir si se muestra; título y descripción en los recursos de strings existentes (es y en), indicando que sin él no se verá la notificación persistente
- [X] T031 [US2] En app/src/main/AndroidManifest.xml: `.services.BubbleFloatingService` con `android:foregroundServiceType="specialUse"` y su `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` de contracts/manifest.md; en B/services/BubleFloatingService.kt cambiar `startForeground` (≈ línea 128) por `ServiceCompat.startForeground(..., FOREGROUND_SERVICE_TYPE_SPECIAL_USE)`
- [ ] T032 [US2] Validar con quickstart.md sección 5 (permiso concedido y rechazado, burbuja); commit: "Pedir permiso de notificaciones y declarar el tipo de servicio de la burbuja"

---

## Phase 5: User Story 3 - Compra y consulta de paquetes (Priority: P2)

**Goal**: la consulta de saldo y la compra siguen funcionando; se elimina el cierre en Android 14+
(FR-006).

**Independent Test**: [quickstart.md](quickstart.md) sección 6 (requiere SIM de Cubacel).

- [X] T033 [P] [US3] Prueba que reproduce el fallo: app/src/androidTest/java/com/smartsolutions/paquetes/helpers/USSDHelperReceiverTest.kt registra el receiver de respuesta USSD igual que B/helpers/USSDHelper.kt y comprueba que no lanza `SecurityException` y que recibe un broadcast enviado con `setPackage`. Debe fallar en un emulador Android 14+ antes de T034
- [X] T034 [US3] En B/helpers/USSDHelper.kt (≈ línea 201) registrar con `ContextCompat.registerReceiver(context, receiver, IntentFilter(ACTION_SEND_USSD_REQUEST), ContextCompat.RECEIVER_NOT_EXPORTED)`
- [X] T035 [US3] En B/services/UIScannerService.kt (≈ líneas 51–56) añadir `setPackage(packageName)` al `Intent` del `sendBroadcast`
- [ ] T036 [US3] Pasar T033 en un emulador Android 14+ y validar con quickstart.md sección 6; commit: "Corregir el receiver de respuestas USSD en Android 14+"

---

## Phase 6: User Story 4 - Pantalla completa y botón atrás (Priority: P3)

**Goal**: ningún contenido queda tapado por las barras del sistema y el botón atrás funciona con el
sistema predictivo (FR-007).

**Independent Test**: [quickstart.md](quickstart.md) sección 7.

- [ ] T037 [US4] Crear B/ui/WindowInsetsExt.kt con una única extensión `View.applySystemBarsPadding()` que use `ViewCompat.setOnApplyWindowInsetsListener` para sumar los insets de `systemBars()` y `displayCutout()` al padding original de la vista (DRY: es el único sitio con esta lógica)
- [ ] T038 [US4] En B/ui/AbstractActivity.kt llamar a `enableEdgeToEdge()` y aplicar `applySystemBarsPadding()` a la vista raíz del contenido (`findViewById<ViewGroup>(android.R.id.content).getChildAt(0)` tras `setContentView`, por ejemplo en `onContentChanged`)
- [ ] T039 [P] [US4] Quitar `android:statusBarColor` de R/values/themes.xml (≈ líneas 14 y 35), R/values-v23/themes.xml (≈ líneas 14 y 32) y R/values-night/themes.xml (≈ líneas 13 y 27); mantener `windowLightStatusBar`
- [ ] T040 [US4] Revisar una a una las pantallas de AbstractActivity (MainActivity, SettingsActivity, SetupActivity, PermissionsActivity, SplashActivity, FragmentContainerActivity, ExceptionsActivity, y las transparentes DashboardControlActivity y AppControlActivity) en Android 16 con navegación por gestos y por botones; corregir con `applySystemBarsPadding()` en la vista concreta los casos de doble padding o contenido tapado (barras inferiores, FAB, listas)
- [ ] T041 [US4] B/ui/PresentationActivity.kt no hereda de AbstractActivity (usa `WelcomeActivity` del AAR): aplicarle `enableEdgeToEdge()` y `applySystemBarsPadding()` a su raíz si su contenido queda tapado
- [ ] T042 [P] [US4] En B/ui/applications/AppControlActivity.kt: sustituir el `override fun onBackPressed()` (≈ línea 178) por un `OnBackPressedCallback` registrado en `onBackPressedDispatcher` que llame a `saveResult()` y cierre; las llamadas manuales (≈ líneas 82 y 190) por `onBackPressedDispatcher.onBackPressed()`; en `onPause` (≈ línea 185) llamar directamente a `saveResult()` en lugar de `onBackPressed()`
- [ ] T043 [P] [US4] En B/ui/exceptions/ExceptionsActivity.kt sustituir el `override fun onBackPressed()` (≈ línea 68) por un `OnBackPressedCallback` que llame a `close()`
- [ ] T044 [P] [US4] En B/ui/dashboard/FirewallControlsFragment.kt (≈ línea 58) y B/ui/dashboard/BubbleControlsFragment.kt (≈ línea 64) cambiar `activity?.onBackPressed()` por `activity?.onBackPressedDispatcher?.onBackPressed()`
- [ ] T045 [US4] Añadir `android:enableOnBackInvokedCallback="true"` al `<application>` de app/src/main/AndroidManifest.xml
- [ ] T046 [US4] Validar con quickstart.md sección 7; commit: "Adaptar la UI a pantalla completa y al botón atrás predictivo"

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T047 Ejecutar `./gradlew lintRelease` y corregir los errores nuevos; no desactivar checks sin justificarlo en un comentario (FR-013)
- [ ] T048 [P] Actualizar README.md: requisitos (Android Studio con JBR 17+, Android 6.0 mínimo, Kotlin 2.4) y añadir `POST_NOTIFICATIONS` a la tabla de permisos
- [ ] T049 Ejecutar quickstart.md completo en Android 16 (incluida la sección 2, actualización desde 4.4.0) y la sección 3 en Android 14 y 15; anotar en el PR los pasos de verificación manual
- [ ] T050 Ejecutar `./gradlew clean testDebugUnitTest assembleDebug assembleRelease` y `connectedDebugAndroidTest` en un emulador Android 16; commit final si hay cambios

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende de Setup. Bloquea todas las historias.
- **US1–US4 (Phases 3–6)**: dependen de Foundational; entre ellas son independientes.
- **Polish (Phase 7)**: depende de las historias que se quieran entregar.

### User Story Dependencies

- **US1 (P1)**: solo Foundational.
- **US2 (P2)**: solo Foundational. Toca app/src/main/AndroidManifest.xml igual que US1, US4: hacer
  esos cambios del manifest de forma secuencial.
- **US3 (P2)**: solo Foundational.
- **US4 (P3)**: solo Foundational.

### Within Each User Story

- Las pruebas (T028, T033) se escriben primero y deben fallar antes de implementar.
- Cada historia termina con su validación del quickstart y su commit.

### Parallel Opportunities

- Setup: T004–T007 (cuatro layouts distintos).
- Foundational: T013 en paralelo con T012; T017, T018 y T019 entre sí.
- US1: T025 y T026.
- US4: T039, T042, T043 y T044.
- Tras Foundational, US1, US2, US3 y US4 pueden avanzar en paralelo salvo las tareas del manifest.

---

## Parallel Example: User Story 4

```text
Task: "T042 [US4] OnBackPressedCallback en ui/applications/AppControlActivity.kt"
Task: "T043 [US4] OnBackPressedCallback en ui/exceptions/ExceptionsActivity.kt"
Task: "T044 [US4] onBackPressedDispatcher en FirewallControlsFragment.kt y BubbleControlsFragment.kt"
Task: "T039 [US4] Quitar statusBarColor de los temas"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (Setup) → Phase 2 (Foundational) → Phase 3 (US1).
2. **Parar y validar**: quickstart.md secciones 2–4 en Android 14, 15 y 16.
3. Con eso el cortafuegos y la medición ya funcionan en Android moderno.

### Incremental Delivery

1. Setup + Foundational → base al día.
2. US1 → cortafuegos y medición (MVP).
3. US3 → elimina el cierre al consultar el saldo (riesgo alto, cambio pequeño).
4. US2 → notificaciones y burbuja.
5. US4 → pantalla completa y botón atrás.
6. Polish → lint, README y verificación completa antes del PR.
