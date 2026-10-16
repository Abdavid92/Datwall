# Tasks: Convertir las 4 pantallas restantes a estado y eventos

**Input**: Design documents from `/specs/005-convertir-pantallas-restantes/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md),
[contracts/permission-result.md](contracts/permission-result.md), [contracts/ui-state.md](contracts/ui-state.md),
[quickstart.md](quickstart.md)

**Tests**: obligatorias (constitución III y FR-011). Las pruebas de cada lógica de pantalla se escriben antes que la
lógica; las de errores (R8) deben fallar primero si el error existe. Usan `kotlinx-coroutines-test`, MockK y Turbine,
con `MainDispatcherRule` (ya existen).

**Rutas**: `B/` = `app/src/main/java/com/smartsolutions/paquetes/`, `R/` = `app/src/main/res/`,
`T/` = `app/src/test/java/com/smartsolutions/paquetes/`.

**Commits**: la documentación en un commit tras la revisión del usuario; la implementación en varios commits por
capas, a criterio del asistente. Nada se comitea ni se publica sin que el usuario lo pida.

**Regla de cada tarea**: al terminarla, la app compila y `testDebugUnitTest` pasa. Lo visible no cambia (FR-012), salvo
los errores confirmados y los textos de US4.

**JDK**: `JAVA_HOME=$HOME/.jdks/jbr-17.0.14`. Si Gradle tarda de más, parar los demonios (`./gradlew --stop`) antes de
reintentar. En el teléfono, los toques por `adb input` funcionan; los de `mobile-mcp` los rechaza el sistema.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: historia del spec (US1–US4)

---

## Phase 1: Setup

- [ ] T001 Comprobar que el árbol compila y pasa las pruebas antes de empezar: `./gradlew testDebugUnitTest
  assembleDebug`.
- [ ] T002 Medir la línea base de lint con `./gradlew lintRelease` y anotar al final de este archivo cuántos avisos hay
  de `UnusedResources` y de `SetTextI18n` (R9). `app/build` se borró al liberar disco: la primera compilación es larga.

---

## Phase 2: Foundational

**Bloquea todas las historias**: US1 y US2 reinician el arranque desde la lógica de pantalla, y US3 necesita el gestor
de registros inyectable.

- [X] T003 [P] Crear T/DatwallKernelRestartTest.kt (con dobles de las dependencias del kernel): `restart()` limpia el
  destino de inicio, vuelve a ejecutar `main()` y, si ya hay una ejecución en curso, no lanza otra a la vez. Debe
  fallar mientras `restart()` no exista.
- [X] T004 Añadir `DatwallKernel.restart()` en B/DatwallKernel.kt:
  - traer de `DatwallApplication.main()` la guarda `kernelRunning`, la llamada a `clearStartDestination()` y el
    lanzamiento de `main()` en un ámbito propio del kernel (`SupervisorJob` + `Dispatchers.Default`);
  - B/DatwallApplication.kt llama a `kernel.restart()` en `onCreate` y deja de tener `main()`, `kernelRunning` y el
    ámbito de corrutinas si ya no los usa (conservar `synchronizeDatabase()`);
  - borrar B/ui/AndroidViewModelExtensions.kt (`next()`) y reemplazar `restartIfSimSetupPending` más adelante (T015,
    T010).
  T003 pasa.

  **Hecho a medias a propósito**: `DatwallKernel.restart()` ya existe y `DatwallApplication.main()` solo lo llama.
  `AndroidViewModel.next()` y `DatwallApplication.main()` se quedan como envoltorios de una línea hasta convertir
  Permisos (T015), que es quien aún los usa; allí se eliminan junto con `restartIfSimSetupPending`.
- [X] T005 [P] Crear B/modules/LogModule.kt (`@Module @InstallIn(SingletonComponent::class)`) con un `@Provides` de
  `LogManager` (`LogManager.newInstance()`). `DropLogsWorker` no cambia (no es un worker de Hilt).

**Checkpoint**: compila; las pruebas siguen en verde.

---

## Phase 3: User Story 1 - Pedir un permiso no ata la pantalla que lo pide (Priority: P1) 🎯 MVP

**Goal**: FR-001, FR-002, FR-003, FR-005, FR-011. Las hojas de permiso y de accesibilidad devuelven su resultado con la
API de resultados de fragmentos y no se guardan objetos de respuesta.

**Independent Test**: [quickstart.md](quickstart.md) sección 4: pedir cada permiso desde Resumen, Controles y la
configuración de líneas, con giro a mitad y con el diálogo del sistema visible.

- [X] T006 [P] [US1] Escribir primero T/ui/permissions/SinglePermissionViewModelTest.kt (data-model "Hoja de permiso"):
  - estado inicial con la acción según se haya pedido antes y pueda volver a pedirse;
  - conceder, rechazar y rechazar de forma permanente (→ `OpenAppSettings`);
  - permiso de llamadas concedido con líneas pendientes → `SimSetupPending` y `kernel.restart()`;
  - descartar sin responder entrega "denegado" una sola vez;
  - recrear el ViewModel con el mismo `SavedStateHandle` tras entregar un resultado no entrega otro (R8, primer
    error: debe fallar si el error existe).
- [X] T007 [P] [US1] Escribir primero T/ui/permissions/StartAccessibilityServiceViewModelTest.kt: sin esperar al
  inicio; abrir ajustes emite `OpenAccessibilitySettings`; volver con el servicio activo entrega "concedido" y sin
  activar entrega "denegado"; volver sin haber abierto los ajustes no entrega nada.
- [X] T008 [US1] Crear B/ui/permissions/PermissionResult.kt según
  [contracts/permission-result.md](contracts/permission-result.md): claves `permission_result` y
  `accessibility_result`, y las funciones `Fragment.onPermissionResult`, `Fragment.showPermissionSheet`,
  `Fragment.onAccessibilityResult` y `Fragment.showAccessibilitySheet`, más lo que necesiten las hojas para escribir su
  resultado.
- [X] T009 [US1] Reescribir B/ui/permissions/SinglePermissionViewModel.kt (`SavedStateHandle`, `PermissionsManager`,
  `ConfigurationManager`, `DatwallKernel`, `@ApplicationContext`; estado, acciones y eventos de data-model) y
  B/ui/permissions/SinglePermissionFragment.kt:
  - sin constructor privado ni `callback`; `newInstance(requestCode)`;
  - el aviso de "denegado" pasa de `onDestroy` a `onDismiss`, y solo si no se entregó otro resultado;
  - la pantalla calcula `canAskAgain` con la actividad y se lo pasa a la lógica como dato;
  - con `SimSetupPending` abre la pantalla inicial (sin lógica de decisión en la pantalla).
  T006 pasa.
- [X] T010 [US1] Reescribir B/ui/permissions/StartAccessibilityServiceViewModel.kt (sin `Application` ni recibir el
  fragmento) y StartAccessibilityServiceFragment.kt (sin `listener`; entrega el resultado con T008). T007 pasa.
- [X] T011 [US1] Actualizar los cuatro sitios que abren una hoja con un objeto de respuesta, para registrar el oyente en
  `onCreate` y abrir la hoja con T008, reenviando el resultado a su ViewModel con la acción que ya tienen:
  - B/ui/resume/ResumeFragment.kt (permiso y accesibilidad);
  - B/ui/dashboard/DashboardFragment2.kt;
  - B/ui/packages/PackagesHolderFragment.kt (sin acción tras el resultado);
  - B/ui/settings/SimsConfigurationFragment.kt.
  Eliminar `SinglePermissionCallback`.
- [X] T012 [US1] Comprobar los posibles errores de R8 (primero y segundo) en el teléfono: girar con la hoja abierta,
  y con "No conservar actividades" activado concediendo el permiso desde el diálogo del sistema. Anotar el resultado
  aquí.

  **Evidencia previa** (2026-10-16, al probar Setup en el emulador): girar con la hoja del permiso de llamadas abierta
  omitió todo el asistente y llevó a Resumen. Es el primer error de R8, confirmado en el código actual: la hoja avisa de
  "denegado" desde `onDestroy` al girar y eso omite la configuración. Debe desaparecer con T009.

  **Resultado** (emulador Android 7, 2026-10-16; el teléfono no estaba conectado):
  - girar con la hoja abierta ya no omite el asistente; la hoja sigue ahí;
  - girar con el diálogo del sistema visible y aceptar: el permiso queda concedido y el flujo sigue (antes, o
    sin la comprobación de abajo, la hoja se quedaba abierta); el asistente pasa al paso de planes;
  - descartar la hoja con "Atrás" cuenta como "denegado" una sola vez y omite el asistente, como antes;
  - Resumen → Sincronizar sin el permiso abre la hoja y esta sobrevive al giro.
  - **Error que apareció al probarlo**: `DialogFragment` llama a `onDismiss` desde `onDestroyView`, también al girar.
    Con eso la hoja daba por descartada la petición y marcaba el resultado como entregado, así que al aceptar el
    diálogo del sistema no pasaba nada. Se corrigió ignorando `onDismiss` mientras la actividad se recrea
    (`isChangingConfigurations`) en las dos hojas.
  - Pendiente de ver en el teléfono: "Abrir ajustes" tras rechazar dos veces y la hoja de accesibilidad.

**Checkpoint**: ninguna pantalla pasa un objeto de respuesta a una hoja.

---

## Phase 4: User Story 2 - La lista de permisos y Setup dependen solo de su estado (Priority: P1)

**Goal**: FR-001, FR-004–FR-008, FR-010, FR-011.

**Independent Test**: [quickstart.md](quickstart.md) secciones 3 y 5 (primer uso y Permisos y privacidad).

- [X] T013 [P] [US2] Escribir primero T/ui/permissions/PermissionsViewModelTest.kt: en el primer uso solo los permisos
  de inicio y desde Ajustes todos; `onShown(rationaleKeys)` calcula la acción de cada fila; "Continuar" y "Ahora no"
  guardan `DATA_DISCLOSURE_ANSWERED` con `IPreferencesRepository` y llaman a `kernel.restart()`; la fila cambia de
  acción tras un resultado; con el permiso de llamadas concedido y líneas pendientes se emite `SimSetupPending`.
- [X] T014 [P] [US2] Escribir primero T/ui/setup/SetupViewModelTest.kt: estado inicial con los pasos pendientes y el
  primero actual; completar avanza; omitir llama a `kernel.restart()`; sin pasos pendientes termina enseguida; con el
  estado de nuevo (lo que hace la pantalla al girar) no cambia de paso (R8, tercer error).
- [X] T015 [US2] Reescribir B/ui/permissions/PermissionsViewModel.kt (sin `Application` ni DataStore; estado y eventos de
  data-model) y adaptar PermissionsActivity.kt y PermissionsAdapter.kt:
  - la actividad calcula las claves con rationale y se las pasa a `onShown`;
  - `PermissionRequests.kt`: `actionFor` sigue como ayuda de UI que calcula `canAskAgain` y delega en
    `PermissionRequestPolicy.decide`; B/ui/permissions/PermissionNeeded.kt la usa igual;
  - `restartIfSimSetupPending` se sustituye por abrir la pantalla inicial tras el evento `SimSetupPending`, sin
    comprobar nada de negocio.
  T013 pasa.
  **Resultado T015**: `onShown(firstUse, rationaleKeys)` y `onSystemResult(requestCode, granted, rationaleKeys)` reciben las claves
  con rationale como dato; se eliminaron `AndroidViewModel.next()`, `DatwallApplication.main()` y
  `restartIfSimSetupPending`. Pendiente la validación en dispositivo (T018).
- [X] T016 [US2] Cambiar el aviso de paso completado: B/ui/settings/AbstractSettingsFragment.kt (`complete()` y
  `skipSetup()` escriben `setup_step_result` en su `parentFragmentManager`), B/ui/FragmentContainerActivity.kt (escucha
  el resultado y cierra con `RESULT_OK`) y eliminar B/ui/setup/OnCompletedListener.kt.
- [X] T017 [US2] Reescribir B/ui/setup/SetupViewModel.kt (`IConfigurationManager`, `DatwallKernel`;
  sin `Application`, `LiveData` ni `Array`) y B/ui/setup/SetupActivity.kt (dibuja `current`, escucha el resultado de
  paso y lo reenvía). T014 pasa.

  **Resultado**: error confirmado y corregido. En el código anterior, girar el teléfono durante el asistente lo
  avanzaba o lo cerraba, porque `SetupActivity` volvía a recibir el valor del `LiveData`. Ahora `SetupActivity` dibuja
  el paso de `state` y recuerda el último que mostró; en el emulador Android 7, girar en el paso de planes lo
  conserva y "Continuar" termina el asistente y abre Resumen. `OnCompletedListener` desaparece: los pasos avisan con
  `setup_step_result` (B/ui/setup/SetupStepResult.kt).
- [ ] T018 [US2] Validar el primer uso y Setup en el emulador Android 7 con instalación limpia
  ([quickstart.md](quickstart.md) sección 3) y la lista de Permisos en el teléfono. Anotar los resultados aquí.

**Checkpoint**: ni Permisos ni Setup usan `Application`, DataStore, `LiveData` ni interfaces de aviso.

---

## Phase 5: User Story 3 - Historial y Eventos con estado y eventos (Priority: P2)

**Goal**: FR-004, FR-009, FR-010, FR-011.

**Independent Test**: [quickstart.md](quickstart.md) sección 5 (Historial y Eventos).

- [X] T019 [P] [US3] Escribir primero T/ui/history/HistoryViewModelTest.kt: cargando y luego líneas con sus filas por
  mes; una compra nueva aparece sin reabrir; línea sin compras.
- [X] T020 [P] [US3] Escribir primero T/ui/events/EventsViewModelTest.kt con un `LogManager` doble: lista del más
  reciente al más antiguo; filtrar por tipo; enviar por correo sin app de correo emite `ShowMessage`.
- [X] T021 [US3] Reescribir B/ui/history/HistoryViewModel.kt (estado único: líneas y filas por línea) y adaptar
  HistoryFragment.kt, HistoryHolderFragment.kt y HistoryRecyclerAdapter.kt. T019 pasa.
- [X] T022 [US3] Reescribir B/ui/events/EventsViewModel.kt (`@HiltViewModel`, `LogManager` y `LocalFileHelper`
  inyectados; filtro y envío por correo como acciones) y adaptar EventsFragment.kt y EventsRecyclerAdapter.kt (el
  adaptador deja de aplicar el filtro). T020 pasa.

**Checkpoint**: las 10 pantallas de la app exponen estado y eventos con el mismo patrón.

---

## Phase 6: User Story 4 - Cierre del paso (Priority: P3)

**Goal**: FR-013, FR-014, FR-015.

**Independent Test**: [quickstart.md](quickstart.md) secciones 1 y 6.

- [X] T023 [P] [US4] Corregir el nombre y la descripción del permiso de sobreponerse en R/values/strings.xml y
  R/values-es/strings.xml (`draw_overlays_permission` y `draw_overlays_permission_description`): solo lo usa la burbuja
  flotante (comprobado en B/helpers/BubbleServiceHelper.kt y B/services/BubleFloatingService.kt).
  **Resultado**: `draw_overlays_permission_description` y `stoped_missing_overlay_permissions_description_notification`
  (esta última sin uso en el código; la borra o conserva T025) ya nombran solo la burbuja. El nombre del permiso no cambia:
  es el del sistema.
- [X] T024 [P] [US4] Reescribir los textos de LTE y mensajería que prometen planes que ya no se venden, en
  notificaciones y estadísticas (R/values*/strings.xml y B/helpers/DisplayTexts.kt), conservando las categorías mientras
  haya datos de ese tipo (opción A de la clarificación). Antes, listar en este archivo cada texto afectado.
  **Resultado**: revisados todos los textos de LTE y mensajería y no se cambió ninguno. `data_type_messaging_bag`
  ("Bolsa Mensajería") nombra una bolsa que ya no se vende, pero solo se muestra para datos antiguos que el usuario aún
  conserva; "Megas de mensajería" no tiene sentido porque los planes de mensajes de Etecsa son por cantidad de mensajes,
  no por megas. El resto (`data_type_international_lte`, `data_type_promo_bonus_lte`, `traffic_international_lte`,
  `mb_lte_header`, `international_lte_notification*`, `traffic_messaging`, `tab_messaging`, `messaging_apps`) nombra tipos
  de datos que el usuario aún puede tener, no ofrece planes.
- [ ] T025 [US4] Dejar a 0 los avisos de `UnusedResources` y `SetTextI18n` en `lintRelease` (línea base de T002): borrar
  los recursos que solo usaba código eliminado, conservar con `tools:keep` los que se usan por reflexión o desde el
  manifiesto, y mover a strings.xml los textos escritos directamente al asignar texto a una vista.

**Checkpoint**: `lintRelease` sin avisos de esas dos reglas.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T026 Ejecutar `./gradlew lintRelease testDebugUnitTest assembleDebug assembleRelease` y corregir lo nuevo.
- [ ] T027 Ejecutar las búsquedas de [quickstart.md](quickstart.md) sección 2 (sin `SinglePermissionCallback`,
  `OnCompletedListener`, `AndroidViewModel`, DataStore ni `LiveData` en las 4 pantallas; sin `runBlocking` en `ui/`).
- [ ] T028 Validar [quickstart.md](quickstart.md) en el teléfono y en el emulador Android 7 (todas las secciones);
  anotar los resultados y los errores nuevos al final de este archivo.
- [ ] T029 Actualizar la documentación si la implementación se apartó del plan, actualizar la hoja de ruta de revisión
  de la 004 si procede, y preparar los commits de implementación. Esperar a que el usuario los pida.

---

## Dependencies & Execution Order

- **Setup (T001–T002)** → **Foundational (T003–T005)**: T004 después de T003; T005 es independiente.
- **US1 (T006–T012)**: después de Foundational (T006 usa `kernel.restart()`). T006 y T007 en paralelo; T008 antes de
  T009–T011; T011 después de T009 y T010.
- **US2 (T013–T018)**: después de Foundational. T015 depende de T009 (el mismo mecanismo de `SimSetupPending`); T016
  antes de T017.
- **US3 (T019–T022)**: depende solo de T005; puede hacerse en paralelo con US1 y US2 (archivos distintos).
- **US4 (T023–T025)**: T023 y T024 independientes; T025 al final, cuando ya no cambian los recursos.
- **Polish**: al final.

### Parallel Opportunities

- T003 y T005.
- T006 y T007; T013 y T014; T019 y T020; T023 y T024.
- US3 completa en paralelo con US1 y US2.

## Implementation Strategy

1. Setup y Foundational: `restart()` del kernel y el proveedor del gestor de registros.
2. US1 (MVP): el mecanismo de resultados y las dos hojas, y los cuatro sitios que las abren. Es lo que más pantallas
   toca y donde más riesgo hay.
3. US2: la lista de Permisos y Setup, ya con el mecanismo y el reinicio del arranque.
4. US3 (en cualquier momento tras T005): Historial y Eventos.
5. US4 y verificación; los commits de implementación cuando el usuario lo pida.

## Notas de línea base (se rellenan al implementar)

- Lint base (T002): _pendiente_.
