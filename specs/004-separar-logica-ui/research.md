# Research: Separar la lógica de negocio de la UI

Basado en el diagnóstico del 2026-10-14 (dos revisiones del código: de la UI hacia el dominio y del dominio hacia la
UI). Rutas relativas a `app/src/main/java/com/smartsolutions/paquetes/` (`B/`) salvo que se indique otra cosa.

## R1. Estado de pantalla y eventos

- **Decisión**:
  - Cada ViewModel convertido (Dashboard, Consumo, Resumen, Aplicaciones, Ajustes, Paquetes) expone:
    - `val state: StateFlow<XUiState>`, una `data class` inmutable con todo lo que la pantalla dibuja;
    - `val events: Flow<XEvent>`, eventos de una sola vez (mensaje, pedir permiso, navegar), implementado con
      `Channel(BUFFERED).receiveAsFlow()`;
    - funciones públicas para las acciones del usuario (`onSyncClicked()`, `onFilterChanged(f)`…).
  - Los fragmentos recogen `state` y `events` con `repeatOnLifecycle(STARTED)` y solo dibujan.
  - Se eliminan las interfaces de callback (`SynchronizationResult`, `PurchaseResult`, `filterChangeListener`,
    `onComplete`) de estas pantallas: el resultado llega como estado o evento, y sobrevive a la recreación porque vive
    en el ViewModel.
  - Las funciones `getX()` que lanzaban un colector nuevo y devolvían un `MutableLiveData` compartido se sustituyen por
    flujos con `stateIn(viewModelScope, WhileSubscribed(5_000), …)`.
  - Se añade `androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version` (2.11.0) para `repeatOnLifecycle`.
- **Por qué**: FR-009. Un solo estado es lo que dibujará cualquier UI del segundo paso (Compose o XML), y los eventos
  por canal no se repiten al recrear la pantalla.
- **Alternativas**:
  - LiveData + `SingleLiveEvent`: más código propio y peor encaje con Compose.
  - `SharedFlow` para eventos: pierde eventos si no hay suscriptor en ese momento (pantalla recreándose).

## R2. Navegación del arranque sin clases de actividad

- **Decisión**:
  - `DatwallKernel` deja de guardar `openActivity: Pair<Class<out Activity>, Bundle?>` y de avisar a suscriptores.
  - Expone `val startDestination: StateFlow<StartDestination?>`, con
    `sealed interface StartDestination { FirstUse; Setup(step: ConfigurationStep); Main }`.
  - `main()` sigue decidiendo igual. Al empezar, pone el estado a `null`, que equivale al "olvida la decisión anterior"
    de la fase 3.
  - En la UI, `ui/StartNavigator.kt` traduce cada destino a su actividad: `PermissionsActivity`, `SetupActivity` o
    `SplashActivity` seguida de `MainActivity`.
  - `SplashActivity`, `SetupActivity` y `PermissionsActivity` recogen `startDestination` en lugar de registrar oyentes.
  - Se eliminan `addOpenActivityListener`, `removeOpenActivityListener`, `AndroidViewModelExtensions.kt` y el
    `EXTRA_INITIAL_FRAGMENT`.
- **Por qué**: FR-005. Es la misma decisión, pero expresada como dato.
- **Riesgo**: es el flujo que se tocó en la fase 3 (primer uso, configuración de líneas, reinicio tras conceder el
  permiso de teléfono). Se valida con los escenarios de [quickstart.md](quickstart.md) sección 4.

## R3. Un único punto de navegación para servicios y notificaciones

- **Decisión**:
  - Interfaz de dominio `managers/contracts/IAppNavigator`, con funciones que devuelven `Intent`:
    - `main()`
    - `simsConfiguration()`
    - `updates()`
    - `exceptions()`
    - `splash()`
  - La implementa `ui/AppNavigator.kt` y se enlaza con Hilt.
  - La usan:
    - `services/NotificationBuilder.kt:164` (`getSplashActivityPendingIntent`)
    - `services/LinearNotificationBuilder.kt:23`
    - `services/DatwallService.kt:467-475` (notificación de SIM)
    - `services/FirewallService.kt:451`
    - `helpers/NotificationHelper.kt:63`
    - `DatwallKernel.kt:431`
    - `exceptions/ExceptionsController.kt:68-82`
  - La difusión `MainActivity.MAIN_IS_OPEN` (`services/BubleFloatingService.kt:556`) pasa a una constante de dominio.
- **Por qué**: FR-006. `Intent` es de `android.content`, no una clase de UI. La decisión de qué pantalla abrir queda
  en la UI.
- **Compatibilidad**: los `PendingIntent` de notificaciones antiguas apuntan a clases que siguen existiendo
  (`SplashActivity`), así que siguen abriendo la app (caso límite del spec).

## R4. Pasos de configuración como datos

- **Decisión**:
  - `managers/models/Configuration.kt`: `val fragment: Class<out AbstractSettingsFragment>` pasa a
    `val step: ConfigurationStep`, con `enum ConfigurationStep { SIMS, PACKAGES }`.
  - `ConfigurationManager` deja de importar fragmentos.
  - `SetupActivity` traduce `SIMS` a `SimsConfigurationFragment` y `PACKAGES` a `PackagesConfigurationFragment`.
- **Por qué**: FR-003, y es el punto que más acopla el contrato `IConfigurationManager` a la UI.

## R5. Permisos como datos

- **Decisión**:
  - `Permission` conserva `keys`, `requestCode`, `minSdk`, `atStartup` y `checkPermission(context)`.
  - Pierde `name`, `description`, `requestPermissionActivity` y `requestPermissionFragment`.
  - Gana `kind: PermissionKind` (`RUNTIME`, `USAGE_ACCESS`, `OVERLAY`, `VPN`) para saber qué pantalla del sistema
    abrir.
  - En la UI:
    - `ui/permissions/PermissionLauncher.kt` concentra cómo se pide cada `kind`: pedir el permiso del sistema o abrir
      el ajuste correspondiente. Lo usan `SinglePermissionFragment`, `PermissionsActivity` y `PermissionNeeded`.
    - `ui/permissions/PermissionTexts.kt` traduce `requestCode` a sus textos, que siguen siendo los strings actuales.
  - `MissingPermissionException` pierde `requestPermission(Activity/Fragment)`.
- **Por qué**: FR-002 y FR-003. Los textos dejan de construirse en el dominio con `getString`
  (`PermissionsManager.kt:34-127`).

## R6. Errores como códigos y textos en la UI

- **Decisión**:
  - **USSD**: `USSDRequestException` conserva `errorCode`. `USSDHelper.errorMessages` desaparece del dominio
    (`helpers/USSDHelper.kt:58-62`). `ui/ErrorTexts.kt` traduce el código a los mismos strings.
  - **Plan no configurado**: `DataPackageManager.kt:124` lanza `PackageNotConfiguredException` en lugar de un
    `IllegalStateException` con texto.
  - **Nombre del tipo de datos**: `getDataTypeName` (`helpers/Utils.kt:126-136`) y `UserDataBytes.getName(context)`
    pasan a la UI (`ui/DataTypeTexts.kt`).
  - **Nombre de la línea**: `Sim.name()` (texto fijo "Sim N") pasa a la UI.
  - **Nombre del grupo de apps**: `AbstractAppRepository.kt:75` guarda en la base de datos el nombre genérico del
    grupo con `getString`. Desde ahora guarda un nombre vacío y la UI muestra el texto genérico cuando está vacío. Los
    grupos ya guardados conservan su nombre.
- **Cambio durante la implementación (T018)**: el nombre genérico del grupo de apps se queda en el repositorio. Se
  usa para ordenar y buscar, y se muestra en varias pantallas; dejarlo vacío cambiaría el orden visible.
- **Excepciones admitidas** (FR-002):
  - textos de notificaciones (`NotificationHelper`, `ExceptionsController`, `SynchronizationWorker`, `FirewallHelper`,
    `BubbleServiceHelper`, `DatwallKernel` y los `NotificationBuilder`);
  - el catálogo de planes (`data/DataPackages.kt`), que es un dato de negocio de ETECSA;
  - el nombre genérico de los grupos de apps (`AbstractAppRepository`), que forma parte de los datos de la app.
- **Por qué**: FR-004.

## R7. Utilidades de UI fuera de `ui/`

- **Decisión**:
  - `Activity.findView`, `Fragment.findView` y `setTabLayoutMediatorSims` (`helpers/Utils.kt:83-121`) se mueven a
    `ui/UiExtensions.kt`.
  - `UIHelper.setVpnAccessCheckBoxListener`, con su diálogo (`helpers/UIHelper.kt:286-330`), se mueve a
    `ui/applications`. El resto de `UIHelper` (tema, colores e iconos) se queda: lo usan las notificaciones y la burbuja,
    que conservan su forma.
  - `LocalFileHelper` deja de mostrar `Toast`: devuelve el resultado y la UI decide el mensaje.
  - `DatwallApplication` y `DatwallService` siguen aplicando el modo de tema; se revisará con el tema nuevo del segundo
    paso.
- **Por qué**: FR-002 con el menor movimiento posible (KISS).

## R8. Acceso común a las preferencias

- **Decisión**:
  - Interfaz `repositories/contracts/IPreferencesRepository`, implementada sobre los tres DataStore actuales
    (`settings`, `internal`, `ui`), sin cambiar claves ni valores (FR-014).
  - Expone flujos y funciones `suspend` tipadas para lo que usan las pantallas convertidas y la base de la UI:
    - tema y modo de tema;
    - periodo y filtro de Consumo;
    - filtro de Resumen;
    - filtro de Aplicaciones;
    - burbuja (activa, tamaño, transparencia, siempre visible);
    - cortafuegos dinámico;
    - sincronización (modo moderno, automática).
  - Mantiene en memoria la última copia con `stateIn(appScope, Eagerly, …)`. Así:
    - `AbstractActivity` lee el tema sin `runBlocking`;
    - el puente `PreferenceDataStore` de Ajustes lee sin bloquear.
  - La primera lectura sigue ocurriendo en `DatwallApplication.onCreate`, que ya lee el tema al arrancar el proceso.
  - Filtros entre pantallas:
    - el periodo y el filtro de Consumo, que hoy pasan de `UsageFragment` a cada `UsageHolderFragment` por DataStore,
      se comparten con un ViewModel del fragmento padre;
    - DataStore solo los recuerda entre sesiones (FR-011).
    - Lo mismo para el filtro de Resumen y el de Aplicaciones.
- **Por qué**: FR-008, FR-010 y FR-011. Hoy las preferencias se leen desde unos 10 sitios.
- **Fuera de alcance**: las preferencias que solo leen servicios y trabajos en segundo plano pueden seguir usando
  DataStore directamente; no son pantallas.

## R9. Piezas que faltan y reglas que pasan al dominio

Cada pieza nueva va sin tipos de Android en su API y con pruebas JVM.

| Pieza | Contenido | Sale de |
|---|---|---|
| `managers/contracts/IAppsManager` + `AppsManager` | flujo de apps; buscar; filtrar y agrupar (reglas de orden y cabeceras como datos, no `getString`); cambiar el acceso; aplicar cambios por lotes | `ui/applications/ApplicationsViewModel.kt:46-228`, `AppsListAdapter.kt:127-150, 223-235` |
| `managers/AppListRules.kt` (puro) | ordenar, agrupar y buscar | ídem |
| `managers/UserDataSummary.kt` (puro) | totales, porcentaje y restante (excluye nacional y mensajería), orden, "existe y no vencido", días restantes | `ui/resume/ResumeViewModel.kt:97-175`, `UserDataBytesRecyclerAdapter.kt:57-96` |
| `managers/UserDataBytesEditor.kt` (puro) | conversión de unidades, `initialBytes ≥ bytes`, fecha de vencimiento, tipos que se pueden añadir | `ui/resume/EditAddUserDataBytesFragment.kt:84, 124, 139-163` |
| `managers/contracts/IUserDataManager` | consulta de datos por línea (flujo) y guardado de una edición; sustituye el uso directo de `IUserDataBytesRepository` en la UI | `ResumeViewModel`, `EditUserDataBytesViewModel` |
| `managers/UsageAggregator.kt` (puro) | unir tráfico con apps, quitar duplicados, ordenar, umbral del 1 % para "Otras" | `ui/usage/UsageViewModel.kt:143-214` |
| `managers/UsageChartGrouping.kt` (puro) | periodo a intervalo y unidad, agrupación por horas o días | `ui/resume/UsageGeneralViewModel.kt:44-126`, `ui/usage/UsageAppDetailsViewModel.kt:46-54` |
| `managers/HistorySummary.kt` (puro) | filtro por SIM, agrupación por mes y suma de precios | `ui/history/HistoryViewModel.kt:35-85` (sin convertir la pantalla: se usa desde su ViewModel actual) |
| `helpers/DataFormatter.kt` (puro) | formato de bytes (valor + unidad), porcentaje, días restantes y fechas con un patrón común | adaptadores y gráficas de Resumen, Consumo e Historial |
| `PackagesRules` (puro, junto a `DataPackages`) | "línea que no puede comprar planes" y lista de planes visibles | `ui/packages/PackagesViewModel.kt:47-61`, `PackagesHolderFragment.kt:98` |

- Los textos de las cabeceras ("Otras", grupos de apps, "Planes") los pone la UI a partir de datos.
- Los ViewModels de las 6 pantallas usan gestores, no repositorios. Se elimina el acceso directo a `IAppRepository`,
  `IUserDataBytesRepository`, `ISimRepository`, `IUsageGeneralRepository` e `IPurchasedPackageRepository`.
- `SettingsViewModel.clearHistory` usa `IPurchasedPackagesManager.clearHistory()`, que se ajusta para conservar las
  compras sin confirmar, que es lo que hace hoy la pantalla.
- **Por qué**: FR-007 y FR-008 (DRY: la búsqueda de apps está duplicada).

## R10. Sin esperas bloqueantes

- `AbstractActivity`: lee el tema de la copia en memoria (R8).
- `SettingsActivity`: el puente `PreferenceDataStore` lee de la copia en memoria y escribe con `launch`.
- `PackagesConfigurationViewModel.isDefaultSim`: pasa a `suspend`.
- `ControlsWidget`: usa `goAsync()` y una corrutina.
- `DatwallApplication.onCreate` mantiene su única lectura inicial: es el arranque del proceso, no una pantalla.

## R11. Pruebas

- **Dependencias de prueba** (solo `testImplementation`, no van en el APK; Maven Central):
  - `org.jetbrains.kotlinx:kotlinx-coroutines-test` 1.11.0 (la versión de corrutinas que ya resuelve el proyecto);
  - `io.mockk:mockk` 1.14.11, para los dobles de los gestores, el kernel y las preferencias sin escribir clases
    falsas;
  - `app.cash.turbine:turbine` 1.2.1, para comprobar los `Flow` de estado y eventos de los ViewModels.
- **Por qué**: decisión del usuario (2026-10-15). La constitución prefiere las APIs de Android y Jetpack, pero no hay
  equivalente para mocks ni para probar `Flow`, y estas dependencias no llegan al APK.
- **Pruebas de ViewModel**:
  - corren con `Dispatchers.setMain(StandardTestDispatcher())`;
  - comprueban el estado inicial y una acción principal (FR-009a), por ejemplo: Resumen sincroniza → estado con
    resultado; Aplicaciones busca → lista filtrada; Paquetes con línea sin planes → estado "no puede comprar".
- Las pruebas de reglas puras van una por pieza de R9, con casos normales, límite y de error.

## R12. Posibles errores del diagnóstico

Se comprueban con una prueba antes de mover cada regla:

| Posible error | Dónde | Cómo se comprueba |
|---|---|---|
| Se pierde el último grupo al agrupar por horas o días | `ui/resume/UsageGeneralViewModel.kt:87-126` | prueba de `UsageChartGrouping` con datos en varias horas |
| La eliminación de duplicados está desplazada en uno (`subList(1, size-1)`) | `ui/usage/UsageViewModel.kt:143-196` | prueba de `UsageAggregator` con apps repetidas |
| El adaptador cuenta con `historyShow` y dibuja con `history` | `ui/history/HistoryRecyclerAdapter.kt:64, 88, 92` | revisión: si `filter()` no tiene llamadas, se elimina `filter()` y `historyShow` (código muerto) y el error desaparece; si las tiene, se corrige |

Los descartados se anotan en `tasks.md`.

## R13. Código muerto

Antes de borrar cada elemento se comprueba con búsqueda y con lint (`UnusedResources`).

- **Layouts**: `activity_ask`, `fragment_default_sims_dialog`, `fragment_notifications`, `item_packages`,
  `item_plan`, `item_sim_default`, `vertical_notification`.
- **Dashboard antiguo**: `ui/dashboard/DashboardFragment.kt`, `fragment_dashboard.xml`, `DashboardControlActivity` (y
  su entrada en el manifiesto), `BubbleControlsFragment`, `FirewallControlsFragment`, `bubble_controls.xml`,
  `firewall_controls.xml` y `activity_dashboard_control.xml`. Antes se comprueba que ningún widget ni atajo abre
  `DashboardControlActivity`.
- **Clases y métodos**: `SplashViewModel` (vacío), `AppsListAdapter.search()`, `EventsViewModel.filter`,
  `EventsRecyclerAdapter.search()` y `HistoryRecyclerAdapter.filter()` (R12).
- **Imports y constantes**: imports sin uso en `modules/DependenciesModule.kt` y `SettingsActivity`; constantes sin
  uso de `MainActivity`.
- **Bibliotecas**: `com.getkeepsafe.taptargetview` y `app/libs/alertbottomdialog-1.1.aar`, si no las usa ningún layout
  ni clase. `SimsHelper`, que usaba `alertbottomdialog`, ya se eliminó en la fase 2.
- **Strings y drawables** que queden sin uso tras lo anterior.
