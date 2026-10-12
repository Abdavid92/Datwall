# Data Model: Actualizar dependencias y compatibilidad con Android 14–16

Esta fase **no cambia el modelo de datos**. El objetivo (FR-011, SC-006) es que los datos existentes
sobrevivan intactos a la actualización.

## Datos que deben conservarse

| Dato | Dónde vive | Riesgo en esta fase |
|---|---|---|
| Reglas del cortafuegos (apps permitidas/bloqueadas) | Base de datos Room | Cambio de Room 2.7 → 2.8 y de kapt → KSP |
| Registro de consumo (por app y por periodo) | Base de datos Room | Igual que el anterior |
| Paquetes comprados, SIMs, datos de usuario | Base de datos Room | Igual que el anterior |
| Preferencias y configuración | DataStore | Cambio de DataStore 1.0 → 1.2 |

## Reglas

- La versión de la base de datos Room y su esquema **no cambian**. Si al migrar a KSP el esquema
  generado difiriera, la tarea se detiene: no se añaden migraciones en esta fase.
- Las claves de DataStore no se renombran ni se eliminan.
- La eliminación de código para API < 23 (`NetworkUsageManagerLegacy`, `compactTrafficsLollipop`)
  no puede borrar tablas ni columnas, aunque solo las usara ese código.

## Lógica nueva testeable

- **Decisión de permiso de notificaciones**: entrada (versión de Android, permiso concedido o no),
  salida (hay que pedirlo o no). Requerido solo en API ≥ 33 y si no está concedido.
