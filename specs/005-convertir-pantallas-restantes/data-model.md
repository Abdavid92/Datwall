# Data Model: estados, eventos y resultados

No hay cambios en Room ni en DataStore. Lo nuevo es el contenido de estado y eventos de las 6 lógicas de pantalla, más
los resultados que viajan entre pantallas. Los campos son orientativos y se ajustan al implementar sin cambiar lo que se
ve.

## Resultados entre pantallas

| Entidad | Campos | Quién la produce / consume |
|---|---|---|
| **PermissionResult** | `requestCode: Int`, `granted: Boolean` | la hoja de permiso y la de accesibilidad la producen; Resumen, Controles, Paquetes y la configuración de líneas la consumen |
| **SetupStepResult** | `skipped: Boolean` | los pasos de configuración la producen; `SetupActivity` y `FragmentContainerActivity` la consumen |

Ambos viajan como paquete de la API de resultados de fragmentos, con una clave fija por tipo (ver
[contracts/permission-result.md](contracts/permission-result.md)).

## Hoja de permiso (`SinglePermissionViewModel`)

- **Estado** `SinglePermissionUiState`:
  - `requestCode`, para elegir los textos;
  - `keys: List<String>`: permisos de Android de ese permiso, para calcular `canAskAgain`;
  - `action: PermissionAction?` (`Request`, `OpenAppSettings`, `Nothing`), nulo hasta calcularlo.
- **Acciones**:
  - `onShown(canAskAgain)`: calcula la acción;
  - `onGrantClicked()` y `onSkipClicked()`;
  - `onSystemResult(granted, canAskAgain)`: resultado del diálogo del sistema o de la pantalla de configuración;
  - `onReturnedFromAppSettings()`;
  - `onDismissed()`.
- **Eventos**:
  - `RequestFromSystem`: lanzar el diálogo del sistema;
  - `OpenAppSettings`;
  - `Finish(granted)`;
  - `SimSetupPending`: permiso de llamadas concedido con líneas por configurar.
- **Estado interno guardado** (`SavedStateHandle`): si ya se entregó un resultado, para no repetirlo ni entregar un
  "denegado" al descartar tras recrear.

## Hoja de accesibilidad (`StartAccessibilityServiceViewModel`)

- **Estado**: `waitingForSettings: Boolean` (se fue a los ajustes y falta volver).
- **Acciones**: `onOpenSettingsClicked()`, `onReturned()` (comprueba si el servicio quedó activo), `onSkipped()`.
- **Eventos**: `OpenAccessibilitySettings`, `Finish(granted)`.

## Lista de Permisos (`PermissionsViewModel`)

- **Estado** `PermissionsUiState`: `items: List<PermissionItem>` (permiso + acción), `firstUse: Boolean`.
- **Acciones**:
  - `onShown(rationaleKeys: Set<String>)`;
  - `onPermissionClicked(item)`;
  - `onFinishFirstUse()`;
  - `onSystemResult(requestCode, granted)`.
- **Eventos**: `RequestFromSystem(permission)`, `OpenAppSettings`, `SimSetupPending`, `Finish`.

## Setup (`SetupViewModel`)

- **Estado** `SetupUiState`: `steps: List<ConfigurationStep>?` (nulo mientras se leen), `index: Int`, y el paso actual
  derivado (`current`). El índice vive en el ViewModel; los pasos pendientes se recalculan si muere el proceso.
- **Acciones**: `onStepCompleted()`, `onSetupSkipped()`.
- **Eventos**: ninguno; al agotar los pasos o al omitir llama al kernel (`restart()`), y las pantallas que siguen el
  destino de inicio abren la que toque.

## Historial (`HistoryViewModel`)

- **Estado** `HistoryUiState`:
  - `loading: Boolean`;
  - `sims: List<Sim>`;
  - `rows: Map<String, List<IPurchasedPackage>>`: filas por línea (cabecera mensual + compras), a partir de
    `HistorySummary.months`.
- **Acciones**: ninguna (solo lectura).
- **Eventos**: ninguno.

## Eventos (`EventsViewModel`)

- **Estado** `EventsUiState`: `loading`, `events` (más reciente primero), `filter: EventType?`.
- **Acciones**: `onFilterChanged(type)`, `onSendByEmail(event)`.
- **Eventos**: `ShowMessage(NO_EMAIL_APP)`.

## Relación con el dominio

| Pieza | Cambio |
|---|---|
| `DatwallKernel` | + `restart()` |
| `modules/` | + proveedor de `LogManager` |
| `IPreferencesRepository` | sin cambios (ya tiene `setDataDisclosureAnswered`) |
| `PermissionRequestPolicy` | sin cambios (ya es la regla única de la acción) |
