# Contrato: estado y eventos de las pantallas convertidas en este paso

Mismo patrón que [el del paso 1](../../004-separar-logica-ui/contracts/ui-state.md): `state: StateFlow<XUiState>`,
`events: Flow<XEvent>` (canal con búfer) y funciones de acción. Las pruebas usan MockK para los gestores y Turbine para
`state` y `events`. Los fragmentos y actividades solo dibujan el estado y reaccionan a los eventos. Los campos de cada
estado están en [data-model.md](../data-model.md).

## Reglas comunes

- La lógica de pantalla no recibe ni guarda vistas, fragmentos, actividades, funciones que los capturen ni objetos de
  respuesta. Lo que depende de Android llega como dato (por ejemplo `canAskAgain: Boolean`, `rationaleKeys: Set<String>`).
- No usa `Application` ni accede al almacén de datos: usa `IPreferencesRepository` y `@ApplicationContext` si necesita
  un contexto para comprobar permisos.
- Las pantallas no leen gestores ni repositorios a través de la lógica de pantalla.
- Los resultados se entregan como evento y se consumen una vez; el estado que debe sobrevivir a un giro o a la muerte del
  proceso se guarda en `SavedStateHandle`.

## Pruebas por lógica de pantalla

| ViewModel | Estado inicial | Acciones y eventos a cubrir |
|---|---|---|
| `SinglePermissionViewModel` | acción según haya sido pedido antes y pueda volver a pedirse | conceder, rechazar, rechazar de forma permanente → ajustes, permiso de llamadas con líneas pendientes → `SimSetupPending`, descartar sin responder entrega "denegado" una sola vez, no entrega nada al recrearse |
| `StartAccessibilityServiceViewModel` | sin esperar | abrir ajustes, volver con el servicio activo y sin activar |
| `PermissionsViewModel` | solo permisos de inicio en el primer uso; todos desde Ajustes | continuar y "Ahora no" guardan la preferencia y reinician el arranque; la acción de cada fila cambia al conceder |
| `SetupViewModel` | pasos pendientes y primer paso | completar avanza, omitir termina, sin pasos pendientes termina enseguida, leer el estado de nuevo (giro) no cambia de paso |
| `HistoryViewModel` | cargando, luego líneas y filas | una compra nueva aparece sin reabrir; línea sin compras |
| `EventsViewModel` | lista ordenada del más reciente al más antiguo | filtrar por tipo, enviar por correo sin app de correo → `NoEmailApp` |
