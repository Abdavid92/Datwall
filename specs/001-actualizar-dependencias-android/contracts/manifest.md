# Contrato: componentes del manifest de `app`

La app no expone APIs a otros sistemas. Su contrato con Android es el manifest. Este es el estado
que debe tener al terminar la fase.

## SDK

| Campo | Antes | Después |
|---|---|---|
| `minSdk` | 21 | 23 |
| `targetSdk` | 32 | 36 |
| `compileSdk` | 35 | 37 |

## Servicios

| Servicio | `foregroundServiceType` | `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` |
|---|---|---|
| `.services.FirewallService` | `specialUse` | Cortafuegos local basado en VPN que bloquea por app el tráfico de datos móviles; debe seguir activo mientras el usuario lo tenga encendido. |
| `.services.DatwallService` | `specialUse` | Medición continua del consumo de datos móviles y notificación persistente con la velocidad y el saldo del paquete. |
| `.services.BubbleFloatingService` | `specialUse` | Burbuja flotante, activada por el usuario, que muestra el consumo de datos en tiempo real sobre otras apps. |
| `.services.UIScannerService` | — (accesibilidad) | — |

## Permisos que se añaden

| Permiso | Motivo |
|---|---|
| `FOREGROUND_SERVICE_SPECIAL_USE` | Obligatorio para el tipo `specialUse` (Android 14+) |
| `POST_NOTIFICATIONS` | Mostrar notificaciones en Android 13+ |

Ningún otro permiso cambia en esta fase (la revisión completa es la fase 3).

## Receivers

- Receivers del manifest: sin cambios.
- Receiver USSD en tiempo de ejecución: `RECEIVER_NOT_EXPORTED`; su emisor envía con `setPackage`.

## Application

- `android:enableOnBackInvokedCallback="true"`.
