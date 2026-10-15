# Contrato: API de dominio que usa la UI

Firmas orientativas: los nombres definitivos se ajustan al implementar. Ninguna incluye tipos de UI (`View`,
`Fragment`, `Activity`, `FragmentManager`) ni textos visibles.

## Navegación (R2, R3)

```kotlin
// DatwallKernel
val startDestination: StateFlow<StartDestination?>
fun main()                                   // decide y publica el destino

// managers/contracts/IAppNavigator (implementado en ui/)
fun main(): Intent
fun splash(): Intent
fun simsConfiguration(): Intent
fun updates(): Intent
fun exceptions(): Intent
```

## Configuración y permisos (R4, R5)

```kotlin
// IConfigurationManager (sin cambios de firma salvo el tipo Configuration)
suspend fun getUncompletedConfigurations(onlyRequires: Boolean = true): Array<Configuration> // Configuration.step

// IPermissionsManager (sin cambios de firma)
val permissions: List<Permission>            // Permission.kind en lugar de lambdas de petición
```

## Preferencias (R8)

```kotlin
interface IPreferencesRepository {
    val preferences: StateFlow<AppPreferences>   // copia en memoria: tema, filtros, periodo, burbuja, sincronización…
    suspend fun setThemeMode(mode: Int)
    suspend fun setUsagePeriod(period: Int)
    suspend fun setUsageFilter(filter: UsageFilter)
    suspend fun setResumeFilter(filter: ResumeFilter)
    suspend fun setAppsFilter(filter: AppsFilter)
    suspend fun setBubble(enabled: Boolean? = null, size: Int? = null, transparency: Float? = null, alwaysShow: Boolean? = null)
    suspend fun setDynamicFirewall(enabled: Boolean)
    // … una función por cada preferencia que escriben las pantallas convertidas
}
```

## Apps (R9)

```kotlin
interface IAppsManager {
    fun flowApps(): Flow<List<IApp>>
    fun sections(apps: List<IApp>, filter: AppsFilter): List<AppListSection>   // reglas en AppListRules
    fun search(apps: List<IApp>, query: String): List<IApp>
    suspend fun setAccess(app: IApp, foreground: Boolean, access: Boolean)
    suspend fun commit(changes: List<IApp>)
}
```

## Datos del usuario (R9)

```kotlin
interface IUserDataManager {
    fun flowUserData(simId: String): Flow<List<UserDataBytes>>
    fun summary(data: List<UserDataBytes>): UserDataSummary                    // UserDataSummary
    suspend fun save(edit: UserDataBytesEdit): Result<Unit>                    // validación en UserDataBytesEditor
}
```

## Reglas puras (sin Android, con pruebas)

| Objeto | Funciones |
|---|---|
| `AppListRules` | ordenar, agrupar y buscar apps |
| `UserDataSummary` / `UserDataBytesEditor` | totales, porcentaje, días restantes, vigentes; conversión de unidades y validación de una edición |
| `UsageAggregator` | unir tráfico con apps, quitar duplicados, ordenar, agrupar < 1 % |
| `UsageChartGrouping` | periodo a intervalo y unidad; agrupar puntos por hora o día |
| `HistorySummary` | filtrar por SIM, agrupar por mes, sumar precios |
| `PackagesRules` | línea que no puede comprar planes; planes visibles |
| `DataFormatter` | bytes a valor y unidad, porcentaje, días restantes y fecha |
