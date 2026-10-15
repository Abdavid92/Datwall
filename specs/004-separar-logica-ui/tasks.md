# Tasks: Separar la lógica de negocio de la UI

**Input**: Design documents from `/specs/004-separar-logica-ui/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md),
[contracts/domain-api.md](contracts/domain-api.md), [contracts/ui-state.md](contracts/ui-state.md),
[quickstart.md](quickstart.md)

**Tests**: obligatorias (constitución III y FR-007, FR-009a, FR-012). Las pruebas de reglas se escriben antes de
mover la regla y fijan el comportamiento actual. Las de errores fallan primero. Usan `kotlinx-coroutines-test`, MockK
y Turbine (R11).

**Rutas**: `B/` = `app/src/main/java/com/smartsolutions/paquetes/`, `R/` = `app/src/main/res/`,
`T/` = `app/src/test/java/com/smartsolutions/paquetes/`.

**Commits**: no se hace commit por tarea. Toda la implementación va en un único commit al final, cuando lo pida el
usuario.

**Regla de cada tarea**: al terminarla, la app compila y `testDebugUnitTest` pasa. Lo visible no cambia (FR-001), salvo
los errores confirmados de US4.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: historia del spec (US1–US5)

---

## Phase 1: Setup

- [ ] T001 Comprobar que el árbol compila y pasa las pruebas antes de empezar: `./gradlew testDebugUnitTest
  assembleDebug` con `JAVA_HOME=$HOME/.jdks/jbr-17.0.14`
- [ ] T002 En app/build.gradle:
  - añadir `implementation "androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version"`;
  - añadir `testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0'`,
    `testImplementation 'io.mockk:mockk:1.14.11'` y `testImplementation 'app.cash.turbine:turbine:1.2.1'`;
  - crear T/MainDispatcherRule.kt (regla JUnit que hace `Dispatchers.setMain(StandardTestDispatcher())` y lo
    restablece al terminar).

---

## Phase 2: Foundational (bloquea US2 y US3)

- [ ] T003 [P] Escribir primero T/helpers/DataFormatterTest.kt con el formato actual:
  - bytes a valor y unidad con las mismas reglas que `DataUnitBytes`;
  - porcentaje entero;
  - días restantes;
  - fecha con el patrón que usan hoy Resumen e Historial.

  Debe fallar.
- [ ] T004 Crear B/helpers/DataFormatter.kt, puro y sin Android, según
  [contracts/domain-api.md](contracts/domain-api.md). T003 pasa.
- [ ] T005 [P] Escribir primero T/repositories/PreferencesRepositoryTest.kt con un DataStore de preferencias en un
  archivo temporal (`PreferenceDataStoreFactory`). Comprueba:
  - lee las mismas claves que hoy (`PreferencesKeys`) con sus valores por defecto;
  - las escrituras actualizan la copia en memoria.

  Debe fallar.
- [ ] T006 Crear `IPreferencesRepository` y su implementación:
  - B/repositories/contracts/IPreferencesRepository.kt y B/repositories/PreferencesRepository.kt, con el modelo
    `AppPreferences`;
  - leen y escriben las claves actuales de los DataStore `settings`, `internal` y `ui`, sin cambiar nombres ni
    valores (FR-014);
  - mantienen la copia en memoria con `stateIn(…, Eagerly, …)` (R8);
  - enlazar en B/repositories/modules/RepositoriesModule.kt;
  - en B/DatwallApplication.kt, la lectura inicial del tema pasa por el repositorio.

  T005 pasa.

---

## Phase 3: User Story 5 - Sin código muerto (Priority: P3)

Se hace antes que el resto (R13 y plan): reduce lo que hay que tocar. Cada borrado se comprueba antes con búsqueda en
el código y en los XML.

**Independent Test**: [quickstart.md](quickstart.md) sección 1 (lint sin recursos nuevos sin uso) y el Dashboard igual
que antes.

- [ ] T007 [P] [US5] Borrar los layouts sin referencia: R/layout/activity_ask.xml, fragment_default_sims_dialog.xml,
  fragment_notifications.xml, item_packages.xml, item_plan.xml, item_sim_default.xml y vertical_notification.xml (y
  sus variantes `layout-v*`)
- [ ] T008 [US5] Borrar el Dashboard antiguo:
  - B/ui/dashboard/DashboardFragment.kt y R/layout/fragment_dashboard.xml;
  - B/ui/dashboard/DashboardControlActivity.kt y su entrada en app/src/main/AndroidManifest.xml;
  - BubbleControlsFragment.kt y FirewallControlsFragment.kt;
  - R/layout/bubble_controls.xml, firewall_controls.xml y activity_dashboard_control.xml.

  Antes, comprobar que ningún widget, atajo ni `PendingIntent` abre `DashboardControlActivity`.
- [ ] T009 [P] [US5] Borrar clases y métodos sin llamadas:
  - B/ui/SplashViewModel.kt (y su uso vacío en SplashActivity);
  - `AppsListAdapter.search()` y la extensión `where` duplicada;
  - `EventsViewModel.filter` y `EventsRecyclerAdapter.search()`;
  - `HistoryRecyclerAdapter.filter()` y `historyShow`.

  Para `HistoryRecyclerAdapter`, anotar en este archivo el resultado del posible error 3 (R12).
- [ ] T010 [P] [US5] Quitar los imports sin uso de B/modules/DependenciesModule.kt y de
  B/ui/settings/SettingsActivity.kt, y las constantes sin uso de B/ui/MainActivity.kt (`ACTION_OPEN_FRAGMENT`,
  `EXTRA_FRAGMENT`, `FRAGMENT_UPDATE_DIALOG`), si nada las usa (incluido `NotificationHelper`).
- [ ] T011 [US5] Quitar las bibliotecas sin uso, tras comprobar que ningún layout ni clase las usa:
  `com.getkeepsafe.taptargetview` (app/build.gradle) y app/libs/alertbottomdialog-1.1.aar.
- [ ] T012 [US5] Ejecutar `lintRelease` y borrar los strings y drawables que hayan quedado sin uso por T007–T011. No
  tocar los que ya estaban sin uso antes de esta fase.

**Checkpoint**: compila; el Dashboard se ve y funciona igual.

---

## Phase 4: User Story 1 - El dominio no depende de la interfaz (Priority: P1) 🎯 MVP

**Goal**: FR-002 a FR-006.

**Independent Test**: [quickstart.md](quickstart.md) secciones 2 y 4.

- [ ] T013 [US1] Pasos de configuración como datos (R4):
  - B/managers/models/ConfigurationStep.kt con `enum ConfigurationStep { SIMS, PACKAGES }`;
  - B/managers/models/Configuration.kt: `fragment` → `step: ConfigurationStep`;
  - B/managers/ConfigurationManager.kt deja de importar fragmentos;
  - B/ui/setup/SetupActivity.kt y SetupViewModel.kt traducen `step` a fragmento.
- [ ] T014 [US1] Permisos como datos (R5):
  - B/managers/models/PermissionKind.kt con `RUNTIME`, `USAGE_ACCESS`, `OVERLAY` y `VPN`;
  - B/managers/models/Permission.kt: − `name`, `description`, `requestPermissionActivity` y
    `requestPermissionFragment`; + `kind`;
  - B/managers/PermissionsManager.kt sin `getString` ni `startActivityForResult`;
  - B/exceptions/MissingPermissionException.kt sin `requestPermission(Activity/Fragment)`.
- [ ] T015 [US1] UI de permisos:
  - crear B/ui/permissions/PermissionTexts.kt, que traduce `requestCode` a los strings actuales de nombre y
    descripción;
  - crear B/ui/permissions/PermissionLauncher.kt, que pide cada `kind`: permiso del sistema, ajuste de acceso de uso,
    de superposición o diálogo de VPN, con las mismas intents que hoy `PermissionsManager`;
  - usarlos en SinglePermissionFragment.kt, PermissionsActivity.kt, PermissionsAdapter.kt, PermissionNeeded.kt y
    PermissionRequests.kt;
  - adaptar B/helpers/FirewallHelper.kt, BubbleServiceHelper.kt y B/services/FirewallService.kt si usaban las lambdas
    de petición.
- [ ] T016 [P] [US1] Errores como códigos (R6):
  - quitar `errorMessages` de B/helpers/USSDHelper.kt;
  - crear B/exceptions/PackageNotConfiguredException.kt y lanzarla en B/managers/DataPackageManager.kt (línea del
    `pkg_not_configured`);
  - crear B/ui/ErrorTexts.kt, que traduce cada código de `USSDHelper` y la nueva excepción a los mismos strings;
  - usarlo donde la UI mostraba `e.message` (Resumen, Paquetes, Dashboard).
- [ ] T017 [P] [US1] Textos de presentación fuera de los modelos:
  - crear B/helpers/DisplayTexts.kt con `dataTypeName(type, context)` (movido de `getDataTypeName` en
    B/helpers/Utils.kt) y `simName(sim, context)`, que sustituye a `Sim.name()` con un string "Sim %d";
  - quitar `UserDataBytes.getName(context)` y `Sim.name()`;
  - actualizar UserDataBytesRecyclerAdapter.kt, EditAddUserDataBytesFragment.kt, PackagesConfigurationFragment.kt,
    `setTabLayoutMediatorSims` y la notificación de SIM de B/services/DatwallService.kt:446-447.
- [ ] T018 [P] [US1] B/repositories/AbstractAppRepository.kt:75 guarda los grupos nuevos con nombre vacío; la lista de
  apps (B/ui/applications) muestra `generic_group_name` cuando el nombre está vacío
- [ ] T019 [US1] Navegación de servicios y notificaciones (R3):
  - crear B/managers/contracts/IAppNavigator.kt (`main`, `splash`, `simsConfiguration`, `updates`, `exceptions`, que
    devuelven `Intent`);
  - crear B/ui/AppNavigator.kt con las mismas actividades y extras que hoy, y enlazarlo en
    B/modules/BindingManagerModule.kt;
  - usarlo en B/services/NotificationBuilder.kt (`getSplashActivityPendingIntent`), LinearNotificationBuilder.kt,
    DatwallService.kt (notificación de SIM), FirewallService.kt, B/helpers/NotificationHelper.kt, B/DatwallKernel.kt
    y B/exceptions/ExceptionsController.kt;
  - mover la constante `MAIN_IS_OPEN` a B/services/BubleFloatingService.kt o a una constante de dominio.
- [ ] T020 [US1] Arranque como dato (R2):
  - crear B/managers/models/StartDestination.kt;
  - en B/DatwallKernel.kt, `startDestination: StateFlow<StartDestination?>` sustituye a `openActivity` y sus
    suscriptores; `main()` pone `null` al empezar;
  - crear B/ui/StartNavigator.kt, que traduce el destino a actividad;
  - SplashActivity.kt, SetupActivity.kt y PermissionsActivity.kt recogen el destino;
  - borrar `addOpenActivityListener` y `removeOpenActivityListener` (B/DatwallApplication.kt), B/ui/
    AndroidViewModelExtensions.kt y `SetupActivity.EXTRA_INITIAL_FRAGMENT`;
  - adaptar `restartIfSimSetupPending` y `PermissionsViewModel.finishFirstUse`.
- [ ] T021 [P] [US1] Utilidades de UI fuera de `ui/` (R7):
  - mover `findView` y `setTabLayoutMediatorSims` de B/helpers/Utils.kt a B/ui/UiExtensions.kt;
  - mover `UIHelper.setVpnAccessCheckBoxListener` y su diálogo a B/ui/applications;
  - B/helpers/LocalFileHelper.kt devuelve el resultado en lugar de mostrar `Toast`, y la UI muestra el mensaje.
- [ ] T022 [US1] Verificar SC-001 con la búsqueda de [quickstart.md](quickstart.md) sección 2, y los cuatro escenarios
  de arranque de la sección 4 en el emulador.

**Checkpoint**: el dominio no importa clases de UI; el arranque y las notificaciones funcionan igual.

---

## Phase 5: User Story 2 - Las reglas de negocio viven en el dominio (Priority: P1)

**Goal**: FR-007 y FR-008. Por cada regla: prueba del comportamiento actual, mover la regla y usarla desde la UI.

**Independent Test**: las pruebas de T/managers pasan sin dispositivo; las pantallas muestran los mismos valores.

- [ ] T023 [P] [US2] Escribir primero T/managers/AppListRulesTest.kt con el orden, la agrupación y las cabeceras
  actuales de B/ui/applications/ApplicationsViewModel.kt:147-228 y la búsqueda de :117-145. Debe fallar.
- [ ] T024 [US2] Crear las piezas de apps:
  - B/managers/AppListRules.kt (puro, con secciones como `AppListSection`, sin textos);
  - B/managers/contracts/IAppsManager.kt y B/managers/AppsManager.kt (`flowApps`, `sections`, `search`, `setAccess`,
    `commit`), enlazados en BindingManagerModule;
  - ApplicationsViewModel.kt y AppsListAdapter.kt los usan en lugar de `IAppRepository` y de sus reglas.

  T023 pasa.
- [ ] T025 [P] [US2] Escribir primero T/managers/UserDataSummaryTest.kt con las reglas actuales:
  - totales, restante y porcentaje sin nacional ni mensajería (B/ui/resume/ResumeViewModel.kt:97-111);
  - "existe y no vencido" y el orden (:159-175);
  - días restantes y porcentaje de cada dato (UserDataBytesRecyclerAdapter.kt:57-96).

  Debe fallar.
- [ ] T026 [US2] Crear las piezas de datos del usuario:
  - B/managers/UserDataSummary.kt;
  - B/managers/contracts/IUserDataManager.kt y B/managers/UserDataManager.kt (`flowUserData(simId)`, `summary`,
    `save`), enlazados en BindingManagerModule;
  - ResumeViewModel.kt y UserDataBytesRecyclerAdapter.kt los usan en lugar de `IUserDataBytesRepository` y de sus
    cálculos.

  T025 pasa.
- [ ] T027 [P] [US2] Escribir primero T/managers/UserDataBytesEditorTest.kt con las reglas de
  B/ui/resume/EditAddUserDataBytesFragment.kt:
  - conversión de unidades (:139-163);
  - `initialBytes` nunca menor que `bytes` (:124);
  - fecha de vencimiento;
  - tipos que se pueden añadir, "faltan o vencidos" (:84);
  - valores no válidos.

  Debe fallar.
- [ ] T028 [US2] Crear B/managers/UserDataBytesEditor.kt. EditAddUserDataBytesFragment.kt y
  EditUserDataBytesViewModel.kt validan y guardan con él y con `IUserDataManager.save` (sin `UserDataBytesRepository`
  concreto). T027 pasa.
- [ ] T029 [P] [US2] Escribir primero T/managers/UsageAggregatorTest.kt con el comportamiento actual de
  B/ui/usage/UsageViewModel.kt:143-214:
  - unir el tráfico con las apps;
  - quitar duplicados;
  - ordenar;
  - umbral del 1 % para "otras".

  Incluir el caso de apps repetidas (posible error 2) tal como funciona hoy. Debe fallar.
- [ ] T030 [US2] Crear B/managers/UsageAggregator.kt. UsageViewModel.kt lo usa y obtiene las apps de `IAppsManager`
  en lugar de `IAppRepository`. Los colores aleatorios y la etiqueta "Otras" se quedan en la UI. T029 pasa.
- [ ] T031 [P] [US2] Escribir primero T/managers/UsageChartGroupingTest.kt con el comportamiento actual:
  - periodo a intervalo y unidad (B/ui/resume/UsageGeneralViewModel.kt:44-85 y
    B/ui/usage/UsageAppDetailsViewModel.kt:46-54);
  - agrupación por horas o días (:87-126).

  Debe fallar.
- [ ] T032 [US2] Crear B/managers/UsageChartGrouping.kt:
  - UsageGeneralViewModel.kt y UsageAppDetailsViewModel.kt lo usan;
  - UsageGeneralViewModel.kt deja de usar `IUsageGeneralRepository` e `IUserDataBytesRepository` directamente: añadir
    a `IUserDataManager` (o a `IStatisticsManager`) el flujo de consumo general que necesita.

  T031 pasa.
- [ ] T033 [P] [US2] Escribir primero T/managers/HistorySummaryTest.kt (filtro por SIM, agrupación por mes y suma de
  precios de B/ui/history/HistoryViewModel.kt:35-85); crear B/managers/HistorySummary.kt y usarlo en
  HistoryViewModel.kt. La pantalla no se convierte.
- [ ] T034 [P] [US2] Escribir primero T/data/PackagesRulesTest.kt:
  - línea configurada sin planes = "no puede comprar" (B/ui/packages/PackagesHolderFragment.kt:98);
  - planes visibles (B/ui/packages/PackagesViewModel.kt:47-61).

  Crear B/data/PackagesRules.kt y usarlo en los dos sitios.
- [ ] T035 [US2] `IPurchasedPackagesManager.clearHistory()` conserva las compras sin confirmar, como hace hoy
  B/ui/settings/SettingsViewModel.kt:20-31 (con prueba en T/managers/PurchasedPackagesManagerTest.kt usando MockK).
  SettingsViewModel lo usa en lugar de `IPurchasedPackageRepository`.
- [ ] T036 [US2] Usar `DataFormatter` en los adaptadores y gráficas de Resumen, Consumo e Historial en lugar del
  formato repetido:
  - UserDataBytesRecyclerAdapter.kt, UsageRecyclerAdapter.kt, UsageAppDetailsRecyclerAdapter.kt y
    HistoryRecyclerAdapter.kt;
  - los formateadores de ejes de UsageGeneralFragment.kt y HistoryHolderFragment.kt;
  - los textos fijos ("Rest:", "Gast:", "Desconocido", "Otras", "Comprar …") pasan a R/values*/strings.xml con el
    mismo texto.
- [ ] T037 [US2] PackagesViewModel.kt usa `ISimManager` en lugar de `ISimRepository.get(simID, true)`, conservando la
  línea con sus planes (relaciones).

**Checkpoint**: ninguna regla de negocio en pantallas, listas ni ViewModels; las 6 pantallas convertibles ya usan
gestores.

---

## Phase 6: User Story 4 - Errores confirmados corregidos (Priority: P2)

**Goal**: FR-012.

- [ ] T038 [US4] Posible error 1 (R12):
  - en T/managers/UsageChartGroupingTest.kt, añadir una prueba con consumo en varias horas que exija todos los grupos,
    incluido el último;
  - si falla, corregir B/managers/UsageChartGrouping.kt;
  - si pasa, anotarlo como descartado en este archivo.
- [ ] T039 [US4] Posible error 2: en T/managers/UsageAggregatorTest.kt, añadir una prueba que exija que al quitar
  duplicados no se pierda ni repita ninguna app. Corregir B/managers/UsageAggregator.kt o anotarlo como descartado.
- [ ] T040 [US4] Posible error 3: anotar en este archivo el resultado de T009 (si se eliminó el filtro, el error
  desaparece con él).

---

## Phase 7: User Story 3 - Cada pantalla depende solo de su estado (Priority: P2)

**Goal**: FR-009, FR-009a, FR-010 y FR-011, según [contracts/ui-state.md](contracts/ui-state.md). Cada pantalla, con
su prueba en T/ui (estado inicial y una acción principal, MockK y Turbine).

**Independent Test**: [quickstart.md](quickstart.md) sección 5, girando el dispositivo en cada pantalla.

- [ ] T041 [US3] Dashboard:
  - `DashboardUiState` y `DashboardEvent`;
  - B/ui/dashboard/DashboardViewModel.kt sin `SwitchCompat`, `RadioButton`, `IndicatorSeekBar`, `FragmentManager`,
    vistas guardadas, `Toast` ni hojas;
  - preferencias por `IPreferencesRepository`; apps por `IAppsManager`;
  - códigos USSD del panel como constantes de dominio;
  - DashboardFragment2.kt dibuja el estado y responde a los eventos;
  - prueba T/ui/dashboard/DashboardViewModelTest.kt.
- [ ] T042 [US3] Consumo:
  - `UsageUiState` y `UsageEvent`;
  - el periodo y el filtro se comparten entre UsageFragment.kt y UsageHolderFragment.kt con el ViewModel del
    fragmento padre (no con DataStore), y se recuerdan con `IPreferencesRepository`;
  - el ViewModel entrega valores y etiquetas, y la pantalla construye el `PieData`;
  - prueba T/ui/usage/UsageViewModelTest.kt.
- [ ] T043 [US3] Resumen:
  - `ResumeUiState` y `ResumeEvent`;
  - sincronizar con resultado como estado o evento, sin `SynchronizationResult`;
  - filtro compartido sin DataStore y recordado con `IPreferencesRepository`;
  - ResumeFragment.kt y ResumeHolderFragment.kt dibujan el estado;
  - el `onGranted` de la fase 3 (reintentar al conceder) pasa a una acción del ViewModel;
  - prueba T/ui/resume/ResumeViewModelTest.kt.
- [ ] T044 [US3] Aplicaciones:
  - `ApplicationsUiState` y `ApplicationsEvent` (`ConfirmAccessChange`);
  - el ViewModel no recibe el adaptador; AppsListAdapter.kt pasa a `ListAdapter` con `DiffUtil.ItemCallback`;
  - sin `filterChangeListener`; las pestañas (PlaceHolderFragment.kt) observan el estado del ViewModel padre;
  - prueba T/ui/applications/ApplicationsViewModelTest.kt.
- [ ] T045 [US3] Ajustes:
  - los fragmentos de B/ui/settings/SettingsActivity.kt leen y escriben con `IPreferencesRepository`; el puente
    `PreferenceDataStore` lee de la copia en memoria y escribe con `launch` (sin `runBlocking`);
  - SimsConfigurationViewModel.kt y SimsDelegate.kt no reciben el fragmento: el permiso se pide con un evento y
    "omitir la configuración" es una acción;
  - `PackagesConfigurationViewModel.isDefaultSim` pasa a `suspend`;
  - prueba T/ui/settings/SettingsViewModelTest.kt.
- [ ] T046 [US3] Paquetes:
  - `PackagesUiState` y `PackagesEvent`;
  - comprar con resultado como evento, sin `PurchaseResult`;
  - la línea de la compra en `SavedStateHandle`; el SMS compartido se entrega con `onSmsShared`;
  - PackagesHolderFragment.kt arranca `PurchaseSmsConsent` con el evento `PurchaseStarted`;
  - prueba T/ui/packages/PackagesViewModelTest.kt.
- [ ] T047 [US3] Sin esperas bloqueantes (FR-010):
  - B/ui/AbstractActivity.kt lee el tema de la copia en memoria;
  - el widget de controles usa `goAsync()` y una corrutina;
  - comprobar con la búsqueda de [quickstart.md](quickstart.md) sección 3 que solo queda la lectura inicial de
    DatwallApplication.

**Checkpoint**: las 6 pantallas sobreviven a giros sin perder resultados; sin `runBlocking` en pantallas.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [ ] T048 Ejecutar `./gradlew lintRelease testDebugUnitTest assembleDebug assembleRelease` y corregir lo nuevo
- [ ] T049 Validar [quickstart.md](quickstart.md) en el teléfono y en el emulador Android 7: revisión pantalla por
  pantalla con giros, actualización encima de `development`; anotar los resultados al final de este archivo
- [ ] T050 Actualizar la documentación de la fase si la implementación se apartó del plan, preparar el commit único
  de implementación y esperar a que el usuario lo pida

---

## Dependencies & Execution Order

- **Setup (T001–T002)** → **Foundational (T003–T006)**.
- **US5 (T007–T012)** puede ir primero (no depende de nada) y reduce el trabajo siguiente.
- **US1 (T013–T022)**: después de Foundational. T014 → T015; T019 antes de T020 (el kernel usa `IAppNavigator`).
- **US2 (T023–T037)**: después de Foundational; puede solaparse con US1 en archivos distintos. Cada par prueba →
  implementación es secuencial.
- **US4 (T038–T040)**: después de T029–T032 y T009.
- **US3 (T041–T047)**: después de US2 (usa sus gestores) y de T006 (preferencias). T041–T046 son independientes entre
  sí salvo en strings.xml.
- **Polish**: al final.

### Parallel Opportunities

- T003 y T005.
- T007, T009 y T010.
- T016, T017, T018 y T021.
- Las pruebas de US2: T023, T025, T027, T029, T031, T033 y T034.
- Dentro de US3, cada pantalla es independiente.

## Implementation Strategy

1. Setup, Foundational y US5: base de pruebas y menos código.
2. US1 (MVP): el dominio deja de depender de la UI. Es lo que hace posible el segundo paso.
3. US2 y US4: las reglas al dominio con pruebas, y los errores aclarados.
4. US3: las 6 pantallas a estado y eventos.
5. Verificación y un único commit de implementación cuando lo pida el usuario.
