# Implementation Plan: Separar la lógica de negocio de la UI

**Branch**: `004-separar-logica-ui` | **Date**: 2026-10-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/004-separar-logica-ui/spec.md`

## Summary

Primer paso de la fase 4: dejar la lógica de negocio fuera de las pantallas sin cambiar lo que ve el usuario.

- **Dominio sin UI**: el dominio deja de depender de la UI.
  - Los pasos de configuración y los permisos pasan a ser datos (`ConfigurationStep`, `PermissionKind`).
  - Los errores pasan a ser códigos.
  - El kernel publica un `StartDestination` en lugar de abrir actividades.
  - Servicios y notificaciones abren la app con un `IAppNavigator` implementado en la UI.
- **Reglas al dominio**: las reglas de negocio de las pantallas pasan a objetos puros con pruebas JVM, y se crean las
  piezas que faltan (`IAppsManager`, `IUserDataManager`, `IPreferencesRepository`, `DataFormatter`).
- **Pantallas con estado**: las 6 pantallas más acopladas pasan a `StateFlow` de estado más canal de eventos, con
  pruebas de ViewModel.
- **Errores**: se comprueban los tres posibles errores, y los confirmados se corrigen con una prueba.
- **Limpieza**: se elimina el código muerto.

Detalle en [research.md](research.md).

## Technical Context

**Language/Version**: Kotlin 2.4.21, Java 17

**Primary Dependencies**:
- Hilt 2.60.1, Room 2.8.5, DataStore 1.2.1, lifecycle 2.11.0;
- se añaden `lifecycle-runtime-ktx` y, solo para pruebas, `kotlinx-coroutines-test` 1.11.0, MockK 1.14.11 y
  Turbine 1.2.1;
- se quitan taptargetview y alertbottomdialog si se confirma que no se usan.

**Storage**: Room versión 1 y DataStore, sin cambios de esquema ni de claves

**Testing**: JUnit 4, `kotlinx-coroutines-test`, MockK y Turbine en `app/src/test`

**Target Platform**: Android 6.0 (API 23) a Android 16 (API 36)

**Project Type**: mobile-app

**Performance Goals**: ninguna pantalla bloquea el hilo de la interfaz (SC-004)

**Constraints**:
- sin cambios visibles salvo los errores corregidos (FR-001);
- las notificaciones y la burbuja conservan su forma;
- `Parcelable` y `Sim.icon` se quedan hasta el segundo paso.

**Scale/Scope**:
- unas 9.400 líneas de UI;
- 6 ViewModels convertidos;
- unas 10 piezas nuevas de dominio;
- unos 15 archivos de dominio tocados;
- unos 20 archivos eliminados.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Estado | Cómo se cumple |
|---|---|---|
| I. KISS | ✅ | Reglas como funciones puras en objetos pequeños, y una interfaz solo donde hay más de un uso o hace falta un doble de prueba (`IAppsManager`, `IUserDataManager`, `IPreferencesRepository`, `IAppNavigator`). Sin capas de "use cases" genéricas. MockK y Turbine se añaden solo para pruebas: Android y Jetpack no ofrecen mocks ni utilidades para probar `Flow`. |
| II. DRY | ✅ | Se unifican la búsqueda de apps (duplicada), el formato de bytes y fechas, y el acceso a preferencias (unos 10 sitios). |
| III. Pruebas unitarias | ✅ | Todas las reglas movidas y las piezas nuevas con pruebas JVM; los 6 ViewModels con estado inicial y acción principal; los errores confirmados, con una prueba que primero falla. |
| IV. Privacidad | ✅ | Sin cambios en datos ni conexiones. |
| V. Android actual y Play | ✅ | `repeatOnLifecycle` y estados que sobreviven a la recreación; sin cambios de permisos. |

**Re-evaluación tras el diseño**: sin cambios.

## Project Structure

### Documentation (this feature)

```text
specs/004-separar-logica-ui/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── domain-api.md
│   └── ui-state.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
app/
├── build.gradle                              # + lifecycle-runtime-ktx; pruebas: coroutines-test, mockk, turbine; − taptargetview, alertbottomdialog
└── src/
    ├── main/
    │   ├── AndroidManifest.xml               # − DashboardControlActivity
    │   ├── res/                              # − layouts y recursos muertos
    │   └── java/com/smartsolutions/paquetes/
    │       ├── DatwallKernel.kt              # startDestination: StateFlow; usa IAppNavigator
    │       ├── DatwallApplication.kt         # − addOpenActivityListener
    │       ├── exceptions/                   # MissingPermission sin UI; PackageNotConfiguredException; ExceptionsController con IAppNavigator
    │       ├── helpers/                      # − findView/setTabLayoutMediatorSims; DataFormatter; USSDHelper sin textos; LocalFileHelper sin Toast
    │       ├── managers/
    │       │   ├── contracts/                # IAppNavigator, IAppsManager, IUserDataManager
    │       │   ├── models/                   # Configuration(step), Permission(kind), StartDestination, ConfigurationStep, PermissionKind
    │       │   ├── AppsManager.kt, UserDataManager.kt
    │       │   └── AppListRules.kt, UserDataSummary.kt, UserDataBytesEditor.kt, UsageAggregator.kt,
    │       │       UsageChartGrouping.kt, HistorySummary.kt, PackagesRules.kt   # puros
    │       ├── repositories/
    │       │   ├── contracts/IPreferencesRepository.kt
    │       │   └── PreferencesRepository.kt
    │       ├── services/                     # intents vía IAppNavigator
    │       └── ui/
    │           ├── AppNavigator.kt, StartNavigator.kt, UiExtensions.kt, ErrorTexts.kt, DataTypeTexts.kt
    │           ├── permissions/              # PermissionLauncher, PermissionTexts
    │           ├── dashboard/ usage/ resume/ applications/ settings/ packages/   # estado + eventos
    │           └── (history, permissions, events, setup: solo lo imprescindible para compilar)
    └── test/java/com/smartsolutions/paquetes/
        ├── managers/                         # pruebas de reglas puras y piezas nuevas
        └── ui/                               # pruebas de los 6 ViewModels
```

**Structure Decision**: estructura existente. Las piezas nuevas van en `managers/` y `repositories/`, y la UI solo
cambia dentro de `ui/`.

## Orden de implementación

1. **Base**: dependencias de prueba (`kotlinx-coroutines-test`, MockK, Turbine) y `DataFormatter`, con pruebas.
2. **Código muerto (US5)**: reduce lo que hay que tocar en el resto de pasos.
3. **Dominio sin UI (US1)**: `ConfigurationStep`, `PermissionKind`, errores como códigos, `IAppNavigator`, la
   navegación del arranque (`StartDestination`) y las utilidades de UI movidas.
4. **Reglas al dominio (US2) y errores (US4)**, de una en una. Para cada regla: prueba del comportamiento actual (y de
   su posible error), mover la regla y usarla desde la UI.
5. **Preferencias**: `IPreferencesRepository` (copia en memoria) y fuera los `runBlocking`, empezando por la base de
   las actividades.
6. **Pantallas a estado más eventos (US3)**, de la más a la menos acoplada: Dashboard, Consumo, Resumen, Aplicaciones,
   Ajustes y Paquetes. Cada una con la prueba de su ViewModel.
7. **Verificación** con [quickstart.md](quickstart.md).

Cada paso deja la app compilando y con las pruebas en verde. La validación visible se hace en el paso 7.

## Complexity Tracking

Sin violaciones de la constitución.
