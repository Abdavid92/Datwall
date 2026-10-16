# Hoja de ruta de revisión: fase 4, paso 1

161 archivos (89 modificados, 52 nuevos, 20 borrados), unas 4 700 líneas añadidas y 4 700 quitadas.
Gran parte de lo borrado es código muerto y de lo nuevo son pruebas, así que lo que de verdad hay que leer con
atención es bastante menos.

**Cómo usarla**: ve en orden, porque cada etapa se apoya en la anterior. Marca la casilla al terminar. Cada etapa
dice qué mirar y cómo comprobarla sin esperar al final. Para ver solo los archivos de una etapa:

```bash
git diff HEAD -- <ruta> <ruta>
```

Los archivos nuevos solo salen en ese comando si ya están marcados con `git add -N .`, que ya está hecho.

Leyenda de rutas: `B/` = `app/src/main/java/com/smartsolutions/paquetes/`, `T/` = `app/src/test/java/com/smartsolutions/paquetes/`.

---

## Etapa 0: contexto (10 min)

- [ ] Leer [spec.md](spec.md) (objetivo y alcance) y [tasks.md](tasks.md), sobre todo las notas de resultado de
  T038–T040, T047, T049 y T050.
- [ ] Decidir qué hacer con `.idea/deploymentTargetSelector.xml` (cambio del IDE, probablemente no debe entrar).
- [ ] Mirar `app/build.gradle`: dependencias añadidas (MockK, Turbine, coroutines-test, lifecycle) y las quitadas
  (taptargetview, alertbottomdialog).

## Etapa 1: piezas de base (30 min)

Pequeñas y sin dependencias. Todo lo demás las usa.

- [ ] `B/repositories/PreferencesRepository.kt`, `contracts/IPreferencesRepository.kt`, `models/AppPreferences.kt`,
  `modules/PreferencesModule.kt` (+ `T/repositories/PreferencesRepositoryTest.kt`)
- [ ] `B/helpers/DataFormatter.kt`, `DisplayTexts.kt`, `B/ui/ErrorTexts.kt` (+ `DataFormatterTest`)
- [ ] `B/exceptions/*` (códigos de error en lugar de mensajes)

**Qué mirar**: que las claves y los DataStore sean los mismos que usaba cada pantalla (no se pierden ajustes de
usuarios que ya tienen la app) y que los textos sean los mismos que había.
**Cómo comprobar**: `./gradlew :app:testDebugUnitTest --tests '*PreferencesRepositoryTest' --tests '*DataFormatterTest'`.

## Etapa 2: navegación y arranque (45 min) — de las más delicadas

- [ ] `B/managers/contracts/IAppNavigator.kt`, `B/ui/AppNavigator.kt`, `B/ui/UiBindingsModule.kt`
- [ ] `B/managers/models/StartDestination.kt`, `Configuration.kt`, `ConfigurationStep.kt`
- [ ] `B/DatwallKernel.kt`, `B/DatwallApplication.kt`
- [ ] `B/ui/MainActivity.kt` (nuevo `followDisplayedScreen`: resaltado de la barra al volver con "Atrás")
- [ ] `B/ui/StartNavigator.kt`, `SplashActivity.kt`, `SplashViewModel.kt` (borrado), `B/ui/setup/SetupActivity.kt`
- [ ] Quién usa el navegador: `ExceptionsController`, `NotificationHelper`, `DatwallService`, `FirewallService`,
  `NotificationBuilder`

**Qué mirar**: que el dominio ya no importe clases de pantallas; que `StartNavigator` termine la actividad en el
momento correcto (sin bucles ni pantallas fantasma); que `DatwallApplication` conserve la única espera inicial.
**Cómo comprobar en el teléfono**: instalación limpia en el emulador (primer uso → configuración → pantalla
principal), abrir la app desde una notificación y reiniciar la app (sin bucle de configuración).

## Etapa 3: permisos como datos (30 min)

- [ ] `B/managers/models/Permission.kt`, `PermissionKind.kt`, `B/managers/PermissionsManager.kt`,
  `contracts/IPermissionsManager.kt` (nuevo `isGranted`)
- [ ] `B/ui/permissions/PermissionLauncher.kt`, `PermissionTexts.kt` y los 4 archivos de pantalla modificados
- [ ] `B/exceptions/MissingPermissionException.kt`

**Qué mirar**: el modelo `Permission` ya no tiene textos ni lanza pantallas; todo eso vive en la UI.
**Cómo comprobar**: pantalla de permisos del teléfono y una consulta sin permiso de llamadas (debe pedirlo una vez).

## Etapa 4: reglas de negocio nuevas, con sus pruebas (1 h) — la parte más fácil de revisar

Lee cada regla junto con su prueba: la prueba dice lo que se espera.

- [ ] `B/managers/AppListRules.kt` + `AppListRulesTest`
- [ ] `B/managers/UserDataSummary.kt` + `UserDataSummaryTest`, `UserDataBytesEditor.kt` + `UserDataBytesEditorTest`
- [ ] `B/managers/UsageAggregator.kt` + `UsageAggregatorTest`, `UsageChartGrouping.kt` + `UsageChartGroupingTest`
- [ ] `B/managers/HistorySummary.kt` + `HistorySummaryTest`, `B/data/PackagesRules.kt` + `PackagesRulesTest`
- [ ] Gestores que las exponen: `AppsManager`, `UserDataManager` y sus contratos (`IAppsManager`, `IUserDataManager`,
  y el `ISimManager.getSim` nuevo)
- [ ] `PurchasedPackagesManager.clearHistory` + `PurchasedPackagesManagerTest`

**Qué mirar**: dos errores reales corregidos aquí, que conviene confirmar leyendo las pruebas:
`UsageChartGroupingTest` (último grupo que se perdía) y `UsageAggregatorTest` (apps repetidas por uid).
**Cómo comprobar**: `./gradlew :app:testDebugUnitTest` (debe dar 108 pruebas en verde).

## Etapa 5: código muerto (20 min) — rápido, solo confirmar que de verdad no se usaba

- [ ] Layouts, pantallas y strings borrados (`R/layout`, `R/values*/strings.xml`, `R/drawable/ic_call_24.xml`,
  `DashboardFragment`, `DashboardControlActivity`, `BubbleControlsFragment`, `FirewallControlsFragment`,
  `SimsDelegate`)
- [ ] `app/libs/alertbottomdialog-1.1.aar` y la entrada de `AndroidManifest.xml`
- [ ] Quedan intactos a propósito: `Theme.Datwall.Transparent` y `app_control_transition` (los usa `AppControlActivity`)

**Cómo comprobar**: `./gradlew :app:lintRelease` no debe dar recursos faltantes.

## Etapa 6: las pantallas, una por una (2–3 h en total)

Todas siguen el mismo patrón: `ViewModel` con `state` y `events`, el fragmento solo dibuja. Revisa primero el
ViewModel y su prueba, luego el fragmento. Empieza por las más simples:

- [ ] **6a. Paquetes** — `ui/packages/*` + `PackagesViewModelTest`
- [ ] **6b. Ajustes** — `ui/settings/*` (incluye `PreferencesBridge`) + `SettingsViewModelTest`
- [ ] **6c. Controles** — `ui/dashboard/DashboardViewModel.kt`, `DashboardFragment2.kt` + `DashboardViewModelTest`
- [ ] **6d. Consumo** — `ui/usage/*` + `UsageViewModelTest`, `UsageAppDetailsViewModelTest` (detalle por app)
- [ ] **6e. Resumen** — `ui/resume/*` + `ResumeViewModelTest`
- [ ] **6f. Aplicaciones** — `ui/applications/*` + `ApplicationsViewModelTest`
- [ ] **6g. Cambios menores** — `ui/history/*`, `ui/events/*`, `ui/exceptions/*`, `B/ui/UiExtensions.kt`

**Puntos de mayor riesgo** (míralos con lupa):
1. **Resumen y Aplicaciones**: el estado lleva `revision` porque `UserDataBytes.equals` y `App.equals` ignoran los
   valores. Si alguien lo quita, vuelve el error de "no se actualiza hasta reabrir".
2. **Ajustes**: `PreferencesBridge` lee de la copia en memoria y escribe sin esperar.
3. **`SimsConfigurationViewModel`**: pide el permiso de llamadas desde `init`; si no se concede, se omite la
   configuración.
4. **Controles**: los interruptores se redibujan desde el estado con la bandera `rendering`; los permisos del
   cortafuegos y de la burbuja vuelven al ViewModel con `onPermissionGranted/Denied`.
5. **Aplicaciones**: `AppsListAdapter` ahora es un `ListAdapter`; la búsqueda se limpia al recrear la pantalla.

**Cómo comprobar en el teléfono**: Resumen (cambia megas y comprueba que se actualiza al instante), Aplicaciones
(filtro, búsqueda, girar), Controles (modo del cortafuegos), Consumo (periodo, porción de la gráfica), Ajustes (tema).

## Etapa 7: sin esperas bloqueantes (20 min)

- [ ] `B/ui/AbstractActivity.kt` (lee el tema de la copia en memoria)
- [ ] `B/ui/widgets/ControlsWidget.kt` (`goAsync` + corrutina)
- [ ] `B/helpers/DateCalendarUtils.kt` (`getTimePeriod` ahora es `suspend`) y sus llamadas en
  `BubleFloatingService`

**Qué mirar**: siguen existiendo `runBlocking` en servicios y constructores de notificaciones (ver nota de T047);
decide si quieres un paso posterior para quitarlos.
**Cómo comprobar**: `grep -rn "runBlocking" app/src/main/java --include=*.kt`

## Etapa 8: cierre (30 min)

- [ ] `./gradlew lintRelease testDebugUnitTest assembleDebug assembleRelease` en verde
- [ ] Recorrido final en teléfono y emulador con la lista de [quickstart.md](quickstart.md)
- [ ] Decidir cuándo hacer el commit único de implementación (en español, con el `Co-Authored-By` de siempre)

---

## Consejos para revisar poco a poco

- Una etapa por sesión es razonable. Las etapas 1, 4 y 5 se pueden hacer casi sin pensar; las 2 y 6 piden más
  atención.
- Si en una etapa encuentras algo que no te convence, anótalo aquí al lado de la casilla y sigue: corregimos todo
  junto antes del commit.
- Si prefieres revisar con el IDE, abre cada etapa en la vista de cambios locales y filtra por la ruta.
