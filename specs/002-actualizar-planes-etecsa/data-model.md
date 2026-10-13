# Data Model: Actualizar los planes de ETECSA

**Sin cambios de esquema** (base de datos versión 1, sin migraciones; ver [research.md](research.md)).
Los cambios son de datos.

## DataPackage (`data_packages`) — catálogo, versión 4

| `PackageId` | Nombre | Precio | `bytes` | `nationalBytes` | min / sms | `network` | `index` | `duration` | `deprecated` |
|---|---|---|---|---|---|---|---|---|---|
| `DailyBag` (reutilizado) | Plan Diario | 25 | 200 MB | 0 | 0 / 0 | 3G_4G | — | 1 | no |
| `Plan_4_5GB` (nuevo) | Plan 4.5 GB | 240 | 4.5 GB | 300 MB | 0 / 0 | 3G_4G | 1 | 35 | no |
| `Plan_2GB` (nuevo) | Plan 120 | 120 | 2 GB | 300 MB | 15 / 20 | 3G_4G | 2 | 35 | no |
| `Plan_4GB` (nuevo) | Plan 240 | 240 | 4 GB | 300 MB | 35 / 40 | 3G_4G | 3 | 35 | no |
| `Plan_6GB` (nuevo) | Plan 360 | 360 | 6 GB | 300 MB | 60 / 70 | 3G_4G | 4 | 35 | no |
| `Combined_*`, `P_*_LTE`, `P_4GB_12GB_LTE`, `MessagingBag` | (sin cambios) | | | | | | | | **sí** |

Reglas:

- Los valores nuevos del enum se añaden **al final**; ningún valor existente cambia de posición ni se borra.
- `bytesLte = 0` en todos los planes nuevos.
- El catálogo se escribe con *upsert* (no `REPLACE`) para no borrar `purchased_packages` en cascada.

## Sim (`sims`)

- `network`: `3G_4G` = puede comprar planes; `NONE` = no puede.
- `setupDate > 0` = línea configurada.
- Migración de datos al aplicar la versión 4: `network != NONE` → `3G_4G`.

## PurchasedPackage (`purchased_packages`)

Sin cambios; las compras de paquetes obsoletos siguen apuntando a su fila, que se conserva.

## UserDataBytes / DataBytes.DataType

Sin cambios en el enum. La oferta nueva solo produce `International` (todas las redes), `National` (.cu),
`PromoBonus` y `DailyBag`; los tipos LTE y `MessagingBag` quedan a cero.
