# Data Model: Separar la lógica de negocio de la UI

Sin cambios en el esquema de Room (versión 1) ni en las claves de DataStore.

## Tipos de dominio nuevos

| Tipo | Forma | Uso |
|---|---|---|
| `StartDestination` | `sealed interface`: `FirstUse`, `Setup(step: ConfigurationStep)`, `Main` | Destino que decide `DatwallKernel.main()` (R2) |
| `ConfigurationStep` | `enum`: `SIMS`, `PACKAGES` | Sustituye a `Configuration.fragment` (R4) |
| `PermissionKind` | `enum`: `RUNTIME`, `USAGE_ACCESS`, `OVERLAY`, `VPN` | Cómo se pide un permiso; la UI decide la pantalla (R5) |
| `PackageNotConfiguredException` | excepción sin texto | Sustituye al `IllegalStateException(getString(…))` de la compra (R6) |
| `UserDataSummary` | `data class(totalBytes, restBytes, usedBytes, percent)` | Totales de una línea (R9) |
| `UsageSlice` / `UsageBreakdown` | app y bytes; lista ordenada más el resto agrupado en "otras" | Consumo por app (R9) |
| `ChartPoint` | instante y bytes | Puntos de las gráficas de consumo (R9) |
| `AppListSection` | `enum` de cabecera (permitidas, bloqueadas, sistema…) más la lista de apps | Agrupación de apps sin textos (R9) |
| `FormattedBytes` | valor y unidad | Formato común (R9) |

## Cambios en modelos existentes

| Modelo | Cambio |
|---|---|
| `managers/models/Configuration` | `fragment: Class<out AbstractSettingsFragment>` → `step: ConfigurationStep` |
| `managers/models/Permission` | − `name`, `description`, `requestPermissionActivity`, `requestPermissionFragment`; + `kind: PermissionKind`; conserva `keys`, `requestCode`, `minSdk`, `atStartup` y `checkPermission` |
| `exceptions/MissingPermissionException` | − `requestPermission(Activity/Fragment)` |
| `exceptions/USSDRequestException` | el mensaje deja de ser texto visible; la UI usa `errorCode` |
| `repositories/models/UserDataBytes` | − `getName(context)` |
| `repositories/models/Sim` | − `name()` (la UI compone "Sim N" con `slotIndex`). `icon` y `Parcelable` se quedan hasta el segundo paso |
| `AppGroup` (nuevos grupos) | se guardan con nombre vacío; la UI muestra el nombre genérico |

## Estado de las preferencias en memoria

`IPreferencesRepository` (R8) mantiene una copia de las preferencias que usa la UI, cargada al arrancar el proceso.
Las escrituras siguen yendo a los mismos DataStore y claves.

Transiciones de `startDestination` en el kernel:

```text
null ──main()──► FirstUse                 (explicación sin responder y sin acceso de uso)
     ──main()──► Setup(SIMS | PACKAGES)   (configuración pendiente y permiso de teléfono concedido o no rechazado)
     ──main()──► Main
main() siempre empieza poniendo null (olvida la decisión anterior).
```
