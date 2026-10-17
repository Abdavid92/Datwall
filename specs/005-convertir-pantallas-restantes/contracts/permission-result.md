# Contrato: resultado de las hojas de permisos y de los pasos de Setup

Sustituye a `SinglePermissionCallback` y a `OnCompletedListener`. Ninguna de las dos pantallas guarda una referencia a
la otra.

## Hojas de permisos

**Claves** (constantes en `ui/permissions/PermissionResult.kt`):

| Clave | Contenido del resultado | La produce |
|---|---|---|
| `permission_result` | `request_code: Int`, `granted: Boolean` | `SinglePermissionFragment` |
| `accessibility_result` | `granted: Boolean` | `StartAccessibilityServiceFragment` |

**Quien abre la hoja** (pantalla de origen, un fragmento):

1. En `onCreate`, registra un oyente de la clave en su `childFragmentManager` con su propio ciclo de vida, y reenvía el
   resultado a su ViewModel con la acción que ya tenía (`onPermissionGranted`, `onPermissionDenied`…). Así el oyente
   existe también al recrear la pantalla.
2. Para pedir el permiso, muestra la hoja en su `childFragmentManager` con el código del permiso. No pasa funciones ni
   objetos.

```kotlin
// Funciones de ayuda previstas (nombres orientativos)
fun Fragment.onPermissionResult(onResult: (requestCode: Int, granted: Boolean) -> Unit)
fun Fragment.showPermissionSheet(requestCode: Int)
fun Fragment.onAccessibilityResult(onResult: (granted: Boolean) -> Unit)
fun Fragment.showAccessibilitySheet()
```

**La hoja**:

- Escribe el resultado en `parentFragmentManager` una sola vez: al conceder, al rechazar o al descartarla sin responder
  (en `onDismiss`, pero no si la actividad se está recreando: `DialogFragment` también llama a `onDismiss` al girar).
- Con el permiso de llamadas concedido y líneas por configurar, no entrega "concedido": avisa de `SimSetupPending` y la
  pantalla reinicia el arranque (comportamiento actual).

**Garantías**:

- el resultado llega a lo más una vez a cada pantalla de origen;
- si la pantalla de origen se recrea antes de recibirlo, se entrega al volver a estar iniciada;
- si la pantalla de origen desaparece, el resultado se descarta sin errores.

## Pasos de Setup

| Clave | Contenido | La produce | La consumen |
|---|---|---|---|
| `setup_step_result` | `skipped: Boolean` | `AbstractSettingsFragment.complete()` y `skipSetup()` | `SetupActivity` (→ `SetupViewModel`), `FragmentContainerActivity` (cierra con `RESULT_OK`) |

Las actividades registran el oyente en `supportFragmentManager` en `onCreate`.
