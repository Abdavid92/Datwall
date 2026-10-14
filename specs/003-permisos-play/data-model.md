# Data Model: Permisos conformes con Google Play

Sin cambios en el esquema de Room (sigue en la versión 1, sin migraciones).

## Permission (modelo en memoria, `managers/models/Permission.kt`)

| Campo | Cambio | Notas |
|---|---|---|
| `name`, `keys`, `description`, `requestCode`, `minSdk` | sin cambios | `description` explica para qué se usa (texto de la divulgación en contexto) |
| `category` (`Required` / `Optional`) | **se elimina** | Ningún permiso bloquea la app (R1) |
| `atStartup: Boolean` | **nuevo** | `true` solo para el acceso de uso y las notificaciones; los pide el primer uso |
| `checkPermission`, `requestPermissionActivity`, `requestPermissionFragment` | sin cambios | |

Permisos de `PermissionsManager` tras el cambio:

| Código | Permiso | `atStartup` | minSdk |
|---|---|---|---|
| `USAGE_ACCESS_CODE` | Acceso de uso | sí | 21 |
| `NOTIFICATIONS_CODE` | Notificaciones | sí | 33 |
| `CALL_CODE` | Llamadas y estado del teléfono | no | 23 |
| `DRAW_OVERLAYS_CODE` | Superposición | no | 23 |
| `VPN_CODE` | VPN | no | 21 |
| ~~`SMS_CODE`~~ | se elimina | | |
| ~~`BATTERY_OPTIMIZATION_CODE`~~ | deja de ser un permiso: es una recomendación en "Permisos y privacidad" | | |

## Estado de una petición (función pura, R3)

```text
concedido                                   → Nada
no concedido, el sistema deja preguntar     → Pedir
no concedido, nunca se pidió                → Pedir
no concedido, ya se pidió y no deja pedir   → AbrirAjustesDeLaApp
```

## Preferencias (DataStore `settingsDataStore`)

| Clave | Tipo | Uso |
|---|---|---|
| `DATA_DISCLOSURE_ANSWERED` (nueva) | Boolean | El usuario aceptó o rechazó la explicación de datos del primer uso; si es `true` no se vuelve a mostrar |
| `REQUESTED_PERMISSIONS` (nueva) | Set<String> | Códigos de permiso pedidos alguna vez (R3) |
| `ACCESSIBILITY_DISCLOSURE_ACCEPTED` (nueva) | Boolean | Aceptó la divulgación de accesibilidad; si es `true`, la siguiente vez se abre directamente el ajuste |
| `LICENSE`, `WAITING_PUCHASE` | se eliminan | Sin uso |

## PurchasedPackage (tabla `purchased_packages`, sin cambios de esquema)

| Campo | Significado nuevo |
|---|---|
| `pending = true` | Compra **sin confirmar**: se muestra en el historial con esa marca |
| `pending = false` | Compra confirmada por el SMS compartido (o anterior a esta versión) |

Transiciones:

```text
Comprar en la app ─► sin confirmar ──(SMS compartido con ese plan, misma SIM, < 24 h)──► confirmada
                          │
                          └── (sin SMS) se queda sin confirmar; ya no se borra
```

Selección de la compra a confirmar (función pura con pruebas): entre las pendientes del plan del SMS y de la SIM, la
más reciente de las últimas 24 horas; si no hay ninguna, no se crea un registro nuevo.
