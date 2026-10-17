# Research: convertir las 4 pantallas restantes

Decisiones del diseño del 2026-10-16. Rutas relativas a `app/src/main/java/com/smartsolutions/paquetes/`.

## R1. Cómo devuelve su resultado una hoja de permisos

**Decisión**: la API de resultados de fragmentos (`FragmentManager.setFragmentResult` y
`setFragmentResultListener`), con una clave fija por tipo de hoja.

- La hoja se abre en el `childFragmentManager` de la pantalla de origen; al terminar, escribe el resultado en su
  `parentFragmentManager`, que es ese mismo gestor.
- La pantalla de origen registra el oyente una vez, al crearse (`onCreate`), y reenvía el resultado a su ViewModel con la
  acción que ya tenía (`onPermissionGranted`, `onPermissionDenied`…).

**Por qué**:
- sobrevive al giro de pantalla y a la muerte del proceso: el gestor de fragmentos guarda el resultado hasta que haya
  un oyente en estado iniciado, y el oyente se vuelve a registrar al recrearse la pantalla de origen;
- elimina el objeto de respuesta que guarda hoy `SinglePermissionViewModel.callback` y los de Resumen, Controles y la
  configuración de líneas, que apuntan a una pantalla que puede haberse destruido;
- es parte de `androidx.fragment`, que el proyecto ya usa.

**Alternativas descartadas**:
- *Compartir un ViewModel entre la hoja y su origen*: obliga a que ambas pantallas compartan alcance y a distinguir qué
  petición es cada resultado.
- *Un canal global de eventos de permisos*: capa nueva sin necesidad (KISS) y sin garantía de entrega al recrearse.
- *Mantener el objeto de respuesta*: es lo que se quiere eliminar (FR-001).

## R2. Qué sabe la lógica de la hoja y qué sabe la pantalla

**Decisión**: la lógica (`SinglePermissionViewModel`) decide qué botón ofrecer, cuándo pedir el permiso al sistema, cuándo
llevar a los ajustes y cuándo terminar. La pantalla solo hace lo que depende de Android: lanzar el diálogo del sistema,
abrir los ajustes y dibujar.

- **Dato en lugar de referencia para "puede volver a pedirse"**: la decisión de qué acción ofrecer necesita saber si el
  sistema todavía muestra el diálogo (`shouldShowRequestPermissionRationale`), que exige una actividad. La pantalla lo
  calcula y se lo pasa a la lógica como un booleano (`canAskAgain`), nunca como función ni actividad.
- **La acción del permiso se decide en un solo sitio**: hoy la regla se repite en `actionFor` (hoja, lista y
  `PermissionNeeded`). Queda `PermissionRequestPolicy.decide` como única regla y `actionFor` como ayuda de UI que
  calcula el dato y llama a la regla.
- **Ya no se entrega "denegado" al girar**: hoy `onDestroy` avisa de "denegado" si no hubo resultado, y se ejecuta también
  al girar la pantalla. El aviso pasa a `onDismiss`, solo si la actividad no se está recreando
  (`isChangingConfigurations`) y solo si no se entregó otro resultado. **Corrección tras probarlo**: se creyó que
  `DialogFragment` no llama a `onDismiss` al recrearse; sí lo hace desde `onDestroyView`, así que sin esa comprobación el
  giro contaba como respuesta y la hoja perdía el resultado real (ver R8).

## R3. Permiso de llamadas y configuración de líneas pendiente

Hoy `notifyResult` comprueba, tras conceder el permiso de llamadas, si hay pasos de configuración sin hacer y reinicia
el flujo de arranque. Esa comprobación es de negocio y vive en una función de la pantalla.

**Decisión**: la lógica de la hoja consulta `IConfigurationManager.getUncompletedConfigurations()` y, si hay pasos,
emite el evento `SimSetupPending`. La pantalla lo atiende reiniciando el arranque con `DatwallKernel.restart()` y
abriendo la pantalla inicial. La lista de Permisos hace lo mismo.

## R4. Reiniciar el arranque

**Decisión**: añadir `DatwallKernel.restart()`, que limpia el destino de inicio y vuelve a ejecutar `main()`.
Sustituye a `AndroidViewModel.next()`, a `DatwallApplication.main()` y al reinicio de `restartIfSimSetupPending`.

**Por qué**: tres sitios repiten la misma secuencia, y la lógica de pantalla no debe necesitar `Application` para
reiniciar el arranque. El kernel ya es un `@Singleton` inyectable.

## R5. Setup: pasos, orden y recuperación

**Decisión**: `SetupViewModel` expone `SetupUiState(steps, index)` con el paso actual derivado y recibe
`onStepCompleted()` y `onSetupSkipped()`. Al agotar los pasos o al omitir, llama a `kernel.restart()`. El índice vive
en el ViewModel (sobrevive al giro) y **no** se guarda en `SavedStateHandle`: los pasos pendientes se recalculan al
recrearse el proceso y los ya completados no salen, así que un índice guardado se desfasaría.

- Los pasos (`SimsConfigurationFragment`, `PackagesConfigurationFragment`) avisan con un resultado de fragmento
  (`setup_step_result`) en lugar de la interfaz `OnCompletedListener`. `SetupActivity` y `FragmentContainerActivity`
  escuchan ese resultado: la primera reenvía al ViewModel, la segunda cierra con `RESULT_OK`.
- `OnCompletedListener` desaparece; `AbstractSettingsFragment.complete()` y `skipSetup()` escriben el resultado.

**Error real (confirmado)**: al girar, `SetupActivity` volvía a observar el `LiveData`, que entregaba otra vez su valor
y llamaba a `nextOrComplete()`: avanzaba al paso siguiente o terminaba el asistente (R8). La muerte del proceso, en
cambio, no es un error: se vuelven a calcular los pasos pendientes.

## R6. Lista de Permisos

**Decisión**: `PermissionsViewModel` expone `PermissionsUiState(items, firstUse)` y acciones
`onShown(rationaleKeys)`, `onPermissionAction(item)` y `onFinishFirstUse()`.

- `items` son pares permiso + acción (`PermissionItem` ya existe); se recalculan en cada `onShown`.
- La pantalla pasa como dato el conjunto de claves de permisos para las que el sistema aún muestra el diálogo.
- `onFinishFirstUse()` guarda `DATA_DISCLOSURE_ANSWERED` con `IPreferencesRepository` (ya tiene el campo) y reinicia el
  arranque con el kernel, en vez de escribir el DataStore a través de `Application`.

## R7. Historial y Eventos

- **Historial**: `HistoryViewModel` combina la lista de líneas con el historial de compras y expone un único estado con
  las líneas y, por línea, las filas (cabecera mensual + compras) que ya calcula `HistorySummary`. No necesita una
  revisión como Resumen: `PurchasedPackage` es una clase de datos con todos sus campos en `equals`.
- **Eventos**: el gestor de registros se inyecta (proveedor en un módulo de Hilt) para poder sustituirlo en las pruebas; `DropLogsWorker` no es un worker de Hilt y no cambia. El
  estado lleva la lista ordenada del más reciente al más antiguo y el filtro por tipo, que hoy aplica el adaptador.
  Enviar por correo es una acción: si no hay app de correo, el evento `NoEmailApp` avisa.

## R8. Posibles errores a comprobar

Se comprueban con una prueba antes de cambiar nada; si se confirman, se corrigen y se anotan.

| Posible error | Dónde | Cómo se comprueba |
|---|---|---|
| Al girar la hoja de permiso se avisa de "denegado" aunque el usuario no respondió | `SinglePermissionFragment.onDestroy` | prueba de `SinglePermissionViewModel`: un descarte tras recrear no entrega resultado; en el teléfono, girar con la hoja abierta |
| El resultado de la hoja se pierde si la pantalla de origen se destruye durante el diálogo del sistema | objeto de respuesta guardado en la lógica | con R1 el resultado queda en el gestor de fragmentos; probado en el emulador girando con el diálogo del sistema visible (ver tasks.md T012) |
| Setup avanza o termina al girar el teléfono | `SetupActivity` observa un `LiveData` que se vuelve a entregar | **Confirmado** (ver tasks.md T017); la muerte del proceso no es un error. Corregido con estado y un último paso mostrado guardado en la actividad |

## R9. Pendientes menores

- **Permiso de sobreponerse**: la descripción dice "...mostrar la Burbuja Flotante o el Cortafuegos sobre otras apps".
  Hoy el cortafuegos no se dibuja sobre otras apps (solo la burbuja necesita el permiso). Se corrige nombre y
  descripción para decir lo que hace; se comprueba en el código qué funciones lo usan antes de escribirlo.
- **Textos de LTE y mensajería**: opción A de la clarificación: reescribir los textos que prometen planes que ya no se
  venden y conservar las categorías mientras haya datos. Se localizan en `strings.xml` (notificaciones y estadísticas)
  y en `DisplayTexts`.
- **Lint**: se mide la línea base con `lintRelease` antes de empezar y se corrigen los avisos de `UnusedResources` y
  `SetTextI18n` hasta llegar a 0 en la versión de lanzamiento. Un recurso que solo usa código eliminado se borra; uno
  que se usa por reflexión o desde el manifiesto se conserva con `tools:keep`.

## R10. Pruebas

Mismo esquema del paso 1: `MainDispatcherRule`, MockK para los gestores y Turbine para `state` y `events`. Las funciones
de `PermissionResult` que usan `FragmentManager` se validan en el teléfono; la regla que decide la acción del permiso ya
tiene su prueba en JVM (`PermissionRequestPolicy`).
