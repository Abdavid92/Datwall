# Implementation Plan: Convertir las 4 pantallas restantes a estado y eventos

**Branch**: `005-convertir-pantallas-restantes` | **Date**: 2026-10-16 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/005-convertir-pantallas-restantes/spec.md`

## Summary

Terminar el paso 1 de la fase 4: que Permisos, Historial, Eventos y Setup sigan el mismo patrón que las otras 6
pantallas (un `state` observable y `events`, sin referencias a pantallas), sin cambiar lo que ve el usuario.

- **Resultado de permisos sin objetos de respuesta**: las hojas de permiso y de accesibilidad devuelven su resultado con
  la API de resultados de fragmentos (`setFragmentResult`), que ya está en el proyecto y sobrevive al giro de pantalla.
  Cuatro pantallas ya convertidas cambian la forma de abrir la hoja (Resumen, Controles, Paquetes y configuración de
  líneas).
- **Lógicas de pantalla puras**: la hoja de permiso, la de accesibilidad, la lista de Permisos, Setup, Historial y Eventos
  pasan a ViewModels con `StateFlow` y canal de eventos. Lo que depende de la actividad (el aviso de "puede volver a
  pedirse") llega como dato, no como referencia.
- **Setup sin interfaz de aviso**: los pasos avisan con un resultado de fragmento y el paso actual sobrevive al giro.
- **Cierre**: nombre del permiso de sobreponerse, textos de LTE y mensajería, y avisos de lint.

Detalle de las decisiones en [research.md](research.md).

## Technical Context

**Language/Version**: Kotlin 2.4.21, Java 17

**Primary Dependencies**: las del paso 1 (Hilt 2.60.1, Room 2.8.5, DataStore 1.2.1, lifecycle 2.11.0, fragment 1.9.1);
pruebas con JUnit 4, `kotlinx-coroutines-test` 1.11.0, MockK 1.14.11 y Turbine 1.2.1. No se añaden bibliotecas.

**Storage**: Room versión 1 y DataStore, sin cambios de esquema ni de claves

**Testing**: JUnit 4, `kotlinx-coroutines-test`, MockK y Turbine en `app/src/test`; validación visual en teléfono y
emulador (sin `connectedAndroidTest`)

**Target Platform**: Android 6.0 (API 23) a Android 16 (API 36)

**Project Type**: mobile-app

**Performance Goals**: ninguna de las 4 pantallas bloquea el hilo de la interfaz (FR-010)

**Constraints**:
- sin cambios visibles salvo los errores corregidos (FR-012);
- las hojas de permisos siguen siendo hojas inferiores con los mismos textos y botones;
- los objetos que se pasan a una lógica de pantalla son datos (números, textos, conjuntos), nunca vistas, fragmentos ni
  funciones que los capturen.

**Scale/Scope**:
- 6 lógicas de pantalla a convertir (lista de Permisos, hoja de permiso, hoja de accesibilidad, Setup, Historial y
  Eventos) y 4 pantallas ya convertidas que cambian su forma de abrir la hoja;
- unos 1.800 líneas de UI tocadas en total;
- 2 piezas nuevas de dominio (`DatwallKernel.restart()` y un proveedor del gestor de registros).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Estado | Cómo se cumple |
|---|---|---|
| I. KISS | ✅ | Se reutiliza la API de resultados de fragmentos (Jetpack) en lugar de un bus de eventos o una biblioteca. La pieza nueva `DatwallKernel.restart()` sustituye a tres sitios que reinician el arranque. El proveedor de `LogManager` tiene un solo uso (`EventsViewModel`) y se justifica por la constitución III: la lógica debe poder probarse sin dispositivo y hoy crea el gestor dentro de sí misma; `DropLogsWorker` no es un worker de Hilt y sigue creándolo con `newInstance()`. |
| II. DRY | ✅ | El reinicio del arranque y la decisión de la acción de un permiso (hoy repetida en la hoja, la lista y `PermissionNeeded`) quedan cada uno en un solo lugar. |
| III. Pruebas unitarias | ✅ | Cada una de las 6 lógicas con estado inicial y acciones principales; las hojas de permisos cubren conceder, rechazar y rechazar de forma permanente; los errores confirmados (R8) con una prueba que primero falla. |
| IV. Privacidad | ✅ | Sin cambios en datos ni conexiones. El registro de eventos sigue en el dispositivo. |
| V. Android actual y Play | ✅ | Las hojas conservan el flujo de permisos en contexto y el rechazo permanente lleva a los ajustes; el estado sobrevive a la recreación. |

**Re-evaluación tras el diseño**: sin cambios. No hay complejidad que justificar.

## Project Structure

### Documentation (this feature)

```text
specs/005-convertir-pantallas-restantes/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── permission-result.md
│   └── ui-state.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
app/src/
├── main/
│   ├── res/values*/strings.xml                      # descripción del permiso de sobreponerse y su aviso (solo la burbuja); sin cambios en LTE/mensajería
│   └── java/com/smartsolutions/paquetes/
│       ├── DatwallKernel.kt                         # + restart(): limpia el destino y vuelve a ejecutar main()
│       ├── DatwallApplication.kt                    # − main() (lo reemplaza kernel.restart())
│       ├── modules/                                 # + proveedor de LogManager
│       └── ui/
│           ├── AndroidViewModelExtensions.kt        # − next() (queda sin usos)
│           ├── FragmentContainerActivity.kt         # escucha el resultado de paso, no OnCompletedListener
│           ├── permissions/
│           │   ├── PermissionResult.kt              # clave, datos y funciones de apertura/escucha de las hojas
│           │   ├── PermissionRequests.kt            # actionFor sigue como ayuda de UI; openStartScreen (sin lógica de decisión)
│           │   ├── PermissionsActivity.kt + PermissionsViewModel.kt      # estado + eventos
│           │   ├── SinglePermissionFragment.kt + SinglePermissionViewModel.kt
│           │   └── StartAccessibilityServiceFragment.kt + StartAccessibilityServiceViewModel.kt
│           ├── setup/                               # SetupViewModel con estado; − OnCompletedListener
│           ├── settings/AbstractSettingsFragment.kt # complete()/skipSetup() devuelven resultado de fragmento
│           ├── history/ events/                     # estado + eventos
│           └── dashboard/ resume/ packages/ settings/   # abren la hoja con el mecanismo nuevo
└── test/java/com/smartsolutions/paquetes/ui/
    ├── permissions/   # 3 archivos de pruebas (hojas, accesibilidad y lista)
    ├── setup/         # 1 archivo
    ├── history/       # 1 archivo
    └── events/        # 1 archivo; DatwallKernelRestartTest va en la raíz de test
```

**Structure Decision**: estructura existente; solo cambia el interior de `ui/` y dos puntos pequeños del dominio.

## Orden de implementación

1. **Base (US1/US2)**: `DatwallKernel.restart()` (sustituye a `next()` y al reinicio de `restartIfSimSetupPending`),
   proveedor de `LogManager`, y las pruebas de lo que se mueva.
2. **Hojas de permisos (US1)**: `PermissionResult` (mecanismo), `SinglePermissionViewModel` y su fragmento,
   `StartAccessibilityServiceViewModel` y su fragmento, y luego los 4 sitios que las abren (Resumen, Controles, Paquetes,
   configuración de líneas). Con sus pruebas.
3. **Lista de Permisos (US2)**: `PermissionsViewModel` y `PermissionsActivity`.
4. **Setup (US2)**: `SetupViewModel`, `SetupActivity`, resultado de paso en `AbstractSettingsFragment` y
   `FragmentContainerActivity`; se elimina `OnCompletedListener`.
5. **Historial y Eventos (US3)**.
6. **Cierre (US4)**: texto del permiso de sobreponerse, revisión de textos LTE/mensajería (sin cambios) y avisos de lint.
7. **Verificación** con [quickstart.md](quickstart.md).

Cada paso deja la app compilando y con las pruebas en verde. La validación visible se hace en el paso 7.

## Complexity Tracking

Sin violaciones de la constitución.
