# Research: Actualizar dependencias y compatibilidad con Android 14–16

Fecha: 2026-10-12. Versiones consultadas en Google Maven y Maven Central ese día.

## R1. Herramientas de build

- **Decisión**: AGP 9.4.1, Gradle 9.8.1, Kotlin 2.4.21 integrado en AGP (sin el plugin
  `kotlin-android`), KSP 2.3.12, Java 17. `compileSdk 37`, `targetSdk 36`, `minSdk 23`.
- **Por qué**: `androidx.core` 1.19 exige `compileSdk 37` y AGP ≥ 9.1; AGP 9.4 exige Gradle ≥ 9.6.
  `targetSdk 36` es el que exige Google Play desde agosto de 2026. Gradle 9 corre sobre el JBR 25 de
  Android Studio, lo que elimina el problema actual del JDK.
- **Alternativas**: quedarse en AGP 8.13 (máximo `core` 1.17 y más de un año de retraso, incumple
  SC-005); usar la opción `android.builtInKotlin=false` (desaparece en AGP 10).

## R2. Procesadores de anotaciones: kapt → KSP y data binding

- **Decisión**: Room y Hilt pasan a KSP. Se desactiva data binding y sus 4 layouts pasan a view
  binding asignando los valores en código. Con eso desaparece kapt del proyecto.
- **Por qué**: kapt es incompatible con el Kotlin integrado de AGP 9 (solo funciona con el plugin
  de transición `com.android.legacy-kapt`). Data binding se usa de forma mínima: `item_app.xml` e
  `item_theme.xml` tienen una variable cada uno; en `fragment_packages_configuration.xml` la
  variable nunca se asigna y el binding de `activity_ask.xml` no se usa.
- **Alternativas**: `com.android.legacy-kapt` (mantiene una herramienta en desuso, contra KISS).

## R3. Tipos de servicio en primer plano (Android 14+)

| Servicio | Función | Tipo elegido |
|---|---|---|
| `FirewallService` (VpnService) | Cortafuegos | `specialUse` |
| `DatwallService` | Medición y notificación persistente | `specialUse` |
| `BubbleFloatingService` | Burbuja flotante | `specialUse` |

- **Decisión**: `specialUse` en los tres, con su `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` explicando el
  uso, más el permiso `FOREGROUND_SERVICE_SPECIAL_USE`.
- **Por qué**: no tiene requisitos en tiempo de ejecución, se puede iniciar tras el arranque del
  teléfono y no tiene límite de tiempo. `DatwallService` arranca en cada inicio del proceso,
  incluido `BOOT_COMPLETED`.
- **Alternativas**: `systemExempted` para la VPN (solo vale si la VPN está configurada en Ajustes
  del sistema; si no, lanza `ForegroundServiceTypeNotAllowedException`); `dataSync` para la
  medición (límite de 6 h/24 h en Android 15 y prohibido desde `BOOT_COMPLETED`).
- `UIScannerService` es un servicio de accesibilidad y no necesita tipo.

## R4. Receivers registrados en tiempo de ejecución (Android 14+)

- **Decisión**: el receiver de respuesta USSD (`USSDHelper.kt:201`) se registra con
  `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)` y el envío desde
  `UIScannerService.kt:51` lleva `setPackage(packageName)`.
- **Por qué**: sin flag lanza `SecurityException` en Android 14+ y el camino está en uso (consulta
  de saldo y sincronización). Es el cambio mínimo.
- `AbstractBroadcastReceiver` solo se usa en un camino para API ≤ 22, que se elimina (R8).

## R5. Permiso de notificaciones (Android 13+)

- **Decisión**: declarar `POST_NOTIFICATIONS` y añadirlo como un permiso más del sistema de
  permisos actual (`PermissionsManager`), requerido solo en API 33+. La decisión de si hay que
  pedirlo se aísla en una función pura con pruebas unitarias.
- **Por qué**: hoy la app no lo declara ni lo pide; en Android 13+ ninguna notificación se ve. El
  rediseño del sistema de permisos es la fase 3; aquí solo se añade lo imprescindible.

## R6. Edge-to-edge (Android 15+, obligatorio en 16)

- **Decisión**: `enableEdgeToEdge()` en `AbstractActivity` (base de todas las activities) y un único
  helper que aplica los insets de las barras del sistema como padding a la vista raíz. Las pantallas
  con barras inferiores o listas que deban dibujarse bajo la barra se ajustan una a una. Se quitan
  `statusBarColor` de los temas porque dejan de tener efecto.
- **Por qué**: hoy no se gestiona ningún inset. Un solo helper evita repetir código (DRY).
- **Alternativas**: `windowOptOutEdgeToEdgeEnforcement` (ignorado con `targetSdk 36`).

## R7. Botón atrás predictivo (Android 16)

- **Decisión**: migrar los dos `onBackPressed()` sobrescritos (`ExceptionsActivity`,
  `AppControlActivity`) a `OnBackPressedCallback`, cambiar las llamadas manuales a
  `onBackPressedDispatcher.onBackPressed()` y activar `enableOnBackInvokedCallback`.
- **Por qué**: con `targetSdk 36` el sistema deja de llamar a `onBackPressed()`.

## R8. Código muerto por `minSdk 23`

- **Decisión**: eliminar las ramas para API < 23 (≈39 comprobaciones de `SDK_INT`), incluidos
  `NetworkUsageManagerLegacy`, `ChangeNetworkReceiver` y `compactTrafficsLollipop`.
- **Por qué**: KISS; código que ya no puede ejecutarse.

## R9. Otras APIs obsoletas que se tocan

- `getParcelableExtra` sin clase → `IntentCompat.getParcelableExtra`.
- `stopForeground(Boolean)` → `ServiceCompat.stopForeground(..., STOP_FOREGROUND_REMOVE)`.
- `LocalBroadcastManager` llega hoy de forma transitiva por `legacy-support-v4`, que se elimina: se
  declara `androidx.localbroadcastmanager` explícitamente (sustituirlo queda fuera de alcance).
- Fuera de alcance (fase 3): `startActivityForResult` y `onRequestPermissionsResult` del sistema de
  permisos, que siguen funcionando.

## R10. Versiones de bibliotecas

| Biblioteca | Actual | Nueva | Nota |
|---|---|---|---|
| core-ktx | 1.8.0 | 1.19.1 | |
| appcompat | 1.5.0 | 1.8.0 | |
| material | 1.6.1 | 1.14.0 | |
| constraintlayout | 2.1.4 | 2.2.2 | |
| navigation | 2.5.1 | 2.9.8 | **excepción**: 2.10 exige API 24 (FR-010 fija API 23) |
| lifecycle | 2.5.1 | 2.11.0 | |
| activity-ktx / fragment-ktx | transitivas | 1.13.0 / 1.9.1 | explícitas para `enableEdgeToEdge` y back |
| preference-ktx | 1.2.0 | 1.2.1 | |
| datastore-preferences | 1.0.0 | 1.2.1 | |
| work-runtime-ktx | 2.7.1 | 2.11.2 | **excepción**: 2.12 exige API 24 (FR-010 fija API 23) |
| recyclerview | 1.2.1 | 1.4.0 | |
| legacy-support-v4 | 1.0.0 | se elimina | sustituida por dependencias explícitas (swiperefreshlayout 1.2.0, localbroadcastmanager 1.1.0) |
| Room | 2.7.2 | 2.8.5 | KSP |
| Hilt | 2.56.2 | 2.60.1 | KSP |
| androidx.hilt (work/compiler) | 1.0.0 | 1.4.0 | KSP |
| Retrofit + converter-gson + adapter-rxjava2 | 2.9.0 / 2.8.1 | 3.0.0 | |
| Gson | 2.8.6 | 2.14.0 | |
| RxJava 2 / RxKotlin / RxAndroid | 2.2.19 / 2.4.0 / 2.1.1 | 2.2.21 / 2.4.0 / 2.1.1 | **excepción**: RxJava 2 está sin mantenimiento; migrar es un cambio de modelo, fuera de alcance (KISS) |
| commons-lang 2.4 | 2.4 | commons-lang3 3.21.0 | misma API `DateUtils`, cambia el paquete |
| taptargetview | 1.13.3 | 1.15.0 | |
| Picasso | 2.71828 | 2.71828 | **excepción**: última estable; la 3 sigue en alfa |
| zxing-android-embedded / core | 4.1.0 / 3.3.0 | 4.3.0 / 3.3.0 | **excepción**: core ≥ 3.4 exige API 24 |
| MPAndroidChart, MaterialStepperView, apklisupdate | 3.1.0 / 0.2.5 / v1.4 | sin cambios | últimas publicadas; sin mantenimiento |
| AARs de `app/libs` | | sin cambios | FR-009, se sustituyen en la fase 4 |
| junit / androidx.test | 4.13.2 / 1.1.3, 3.4.0 | 4.13.2 / 1.3.0, 3.7.0 | |

## R11. Riesgos detectados

- **Ajustes restringidos (Android 13+)**: el sistema bloquea activar un servicio de accesibilidad en
  apps instaladas fuera de una tienda reconocida; el usuario debe permitirlo a mano. Afecta a la
  lectura de respuestas USSD. Se documenta y se aborda en la fase 3 (permisos).
- **Restricciones de batería de fabricantes**: se verifican en el quickstart, sin cambios de diseño.
- `app/src/test/.../ClaimsGenerator.kt` contiene claves en texto plano; queda fuera de esta fase.
