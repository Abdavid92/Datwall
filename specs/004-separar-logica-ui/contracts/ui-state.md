# Contrato: estado y eventos de las pantallas convertidas

Patrón común (R1): `state: StateFlow<XUiState>`, `events: Flow<XEvent>` y funciones de acción. Las pruebas de cada
ViewModel usan MockK para los gestores y Turbine para recoger `state` y `events` (R11). Los fragmentos solo
dibujan el estado y reaccionan a los eventos. Los campos son orientativos; se ajustan al implementar sin cambiar lo
que se ve.

Eventos comunes a varias pantallas:

| Evento | Qué hace la pantalla |
|---|---|
| `ShowMessage(code)` | muestra el texto del código (`ErrorTexts`) |
| `RequestPermission(requestCode)` | abre `SinglePermissionFragment`; el resultado vuelve al ViewModel con una acción |
| `RequestAccessibility` | abre la divulgación de accesibilidad |

## Dashboard (`DashboardViewModel`)

- **Estado**:
  - cortafuegos activo y dinámico;
  - número de apps permitidas y bloqueadas;
  - burbuja: activa, tamaño, transparencia y siempre visible;
  - si falta algún permiso para cada control.
- **Acciones**:
  - `onFirewallToggled(on)`, `onDynamicFirewallToggled(on)`;
  - `onBubbleToggled(on)`, `onBubbleSizeChanged(size)`, `onBubbleTransparencyChanged(value)`,
    `onBubbleAlwaysShowChanged(on)`;
  - `onUssdShortcut(code)`; los códigos `*222#`, `*222*266#` y `*222*328#` pasan a constantes de dominio.
- **Eventos**: `RequestPermission(VPN | OVERLAY | CALL)`, `ShowMessage(FIREWALL_NEEDS_BLOCKED_APP)`.
- **Ya no**: recibe `SwitchCompat`, `RadioButton`, `IndicatorSeekBar` ni `FragmentManager`, ni guarda la vista de la
  burbuja.

## Consumo (`UsageViewModel`, compartido por `UsageFragment` y sus `UsageHolderFragment`)

- **Estado**: periodo, filtro, cargando, falta acceso de uso, `UsageBreakdown` (apps ordenadas y "otras") y datos para
  la gráfica circular (valores y etiquetas, sin `PieData`).
- **Acciones**: `onPeriodChanged(p)`, `onFilterChanged(f)`, `onUsageAccessGranted()`.
- **Ya no**: construye ni modifica el `PieChart`. La pantalla convierte los valores en `PieData`.

## Resumen (`ResumeViewModel`)

- **Estado**:
  - líneas instaladas;
  - para la línea elegida: lista de datos vigentes ordenada, `UserDataSummary`, medias (consumo diario y restante);
  - sincronizando, filtro.
- **Acciones**: `onSimSelected(simId)`, `onSyncClicked()`, `onFilterChanged(f)`, `onPermissionGranted(code)`.
- **Eventos**: `RequestPermission(CALL)`, `RequestAccessibility`, `ShowMessage(USSD_*)`, `SyncFinished`.
- **Ya no**: la pantalla implementa `SynchronizationResult`.
- La edición manual de datos (`EditAddUserDataBytesFragment`) usa `IUserDataManager` y `UserDataBytesEditor`, aunque
  su pantalla no se convierte entera.

## Aplicaciones (`ApplicationsViewModel`)

- **Estado**: filtro, consulta de búsqueda, secciones (`AppListSection`) para cada pestaña y cambios pendientes de
  aplicar.
- **Acciones**: `onFilterChanged(f)`, `onQueryChanged(q)`, `onAccessChanged(app, foreground, access)`,
  `onCommitChanges()`.
- **Eventos**: `ConfirmAccessChange(app)`, que sustituye al diálogo que hoy abre `UIHelper`.
- **Ya no**: recibe el adaptador para calcular el `DiffUtil` ni usa `filterChangeListener`. El `DiffUtil` lo hace el
  adaptador con `ListAdapter`.

## Ajustes (`SettingsViewModel` y fragmentos de `SettingsActivity`)

- **Estado**: la copia de preferencias de `IPreferencesRepository`.
- **Acciones**: una por preferencia (tema, notificaciones, sincronización…), y `onClearHistory()`.
- **Ya no**: `runBlocking`. El puente `PreferenceDataStore` lee de la copia en memoria.
- La configuración de líneas y planes (`SimsConfigurationViewModel`, `PackagesConfigurationViewModel`) deja de
  recibir el fragmento:
  - el permiso se pide con un evento `RequestPermission(CALL)`;
  - "omitir la configuración" pasa a ser una acción del ViewModel.

## Paquetes (`PackagesViewModel`)

- **Estado** por línea: puede comprar planes, configurada, lista de planes visibles (`PackagesRules`) y compra en
  curso.
- **Acciones**: `onPurchase(packageId)`, `onSmsShared(body)`, `onPermissionGranted(code)`.
- **Eventos**: `RequestPermission(CALL)`, `PurchaseStarted` (la pantalla arranca `PurchaseSmsConsent`),
  `ShowMessage(PURCHASE_FAILED | PACKAGE_NOT_CONFIGURED)`.
- **Ya no**: la pantalla implementa `PurchaseResult`. El ViewModel guarda la línea de la compra en `SavedStateHandle`,
  así que sobrevive a la muerte del proceso.
