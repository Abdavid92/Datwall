# Implementation Plan: Permisos conformes con Google Play

**Branch**: `003-permisos-play` | **Date**: 2026-10-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/003-permisos-play/spec.md`

## Summary

Quitar el bloqueo inicial: ningún permiso es obligatorio y el primer uso muestra una explicación de datos, pide el
acceso de uso y, en Android 13+, las notificaciones, y después configura las líneas pidiendo el permiso de teléfono. El resto se pide en contexto, como ya ocurre en la
mayoría de funciones, añadiendo el caso de rechazo permanente (abrir los ajustes). Una sola pantalla de
permisos sirve para el primer uso y para Ajustes → "Permisos y privacidad". Los SMS se sustituyen por la API SMS
User Consent tras comprar desde la app; las compras sin SMS quedan en el historial como "sin confirmar". La
accesibilidad se ofrece solo en el camino USSD legacy y tras una divulgación con consentimiento. Se quitan permisos,
dominios sin cifrar, dependencias y textos sin uso, y el escáner QR de
recarga (las tarjetas de ETECSA ya no existen). Detalle en [research.md](research.md).

## Technical Context

**Language/Version**: Kotlin 2.4.21, Java 17

**Primary Dependencies**: Hilt 2.60.1, DataStore, `play-services-auth-api-phone` 18.3.0 (nueva); se quitan
MaterialStepperView, Picasso y zxing (escáner QR)

**Storage**: Room versión 1 sin cambios de esquema; tres claves nuevas de DataStore ([data-model.md](data-model.md))

**Testing**: JUnit 4 en `app/src/test` (decisión de petición de permisos, selección de la compra a confirmar)

**Target Platform**: Android 6.0 (API 23) a Android 16 (API 36)

**Project Type**: mobile-app

**Performance Goals**: sin requisitos nuevos

**Constraints**: la app debe funcionar sin Google Play Services; no comprar planes reales en las pruebas sin
autorización; no cambiar el esquema de Room

**Scale/Scope**: 5 permisos en tiempo de ejecución, 8 declaraciones eliminadas; ~25 archivos afectados

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Estado | Cómo se cumple |
|---|---|---|
| I. KISS | ✅ con excepción | Una pantalla de permisos en lugar de dos; se elimina la categoría `Required`, el stepper y el código de SMS. Excepción: biblioteca de Google para SMS User Consent (ver Complexity Tracking). |
| II. DRY | ✅ | Explicación de cada permiso en un único sitio (`Permission.description`); una función decide pedir o abrir ajustes para todas las peticiones. |
| III. Pruebas unitarias | ✅ | Funciones puras con pruebas: decisión de petición (R3) y selección de la compra a confirmar (R5). |
| IV. Privacidad | ✅ | Desaparece la lectura de toda la bandeja de SMS; el SMS solo se lee si el usuario lo comparte; se prohíbe el tráfico sin cifrar. |
| V. Android actual y Play | ✅ | Es el objetivo de la fase: peticiones en contexto, app usable sin permisos, sin permisos restringidos innecesarios. |

**Re-evaluación tras el diseño**: sin cambios.

## Project Structure

### Documentation (this feature)

```text
specs/003-permisos-play/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── permisos.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
app/
├── build.gradle                                   # + auth-api-phone; − stepper, Picasso, zxing
└── src/main/
    ├── AndroidManifest.xml                        # − 8 permisos, − SmsReceiver, − CaptureActivity
    ├── res/xml/network_security_config.xml        # sin dominios en claro
    ├── res/xml/accessibility_service_config.xml   # sin settingsActivity, descripción real
    ├── res/xml/header_preferences.xml (y -v26)    # entrada "Permisos y privacidad"
    ├── res/layout/                                # lista de permisos, divulgación de accesibilidad
    ├── res/values*/strings.xml                    # textos de divulgación; − licencia
    └── java/com/smartsolutions/paquetes/
        ├── DatwallKernel.kt                       # sin bloqueo; primer uso; setup solo con teléfono
        ├── PreferencesKeys.kt                     # claves nuevas; − LICENSE, WAITING_PUCHASE
        ├── managers/
        │   ├── PermissionsManager.kt              # − SMS, − batería, atStartup
        │   ├── PermissionRequestPolicy.kt         # nuevo, puro (R3)
        │   ├── PurchasedPackagesManager.kt        # sin borrado; selección pura (R5); − seed SMS
        │   ├── DataPackageManager.kt              # confirmar compra desde el SMS compartido
        │   └── models/Permission.kt               # atStartup en lugar de category
        ├── helpers/SmsInboxReaderHelper.kt        # se elimina
        ├── receivers/SmsReceiver.kt               # se elimina
        ├── workers/SynchronizationWorker.kt       # omitir sin permiso de llamadas
        └── ui/
            ├── permissions/                       # lista única; divulgación de accesibilidad; abrir ajustes
            ├── packages/                          # SMS User Consent tras comprar; − escáner QR
            ├── history/                           # mostrar "Sin confirmar"
            ├── resume/ y usage/                   # explicación sin acceso de uso
            └── settings/SettingsActivity.kt       # "Permisos y privacidad"
app/src/test/java/com/smartsolutions/paquetes/managers/   # pruebas nuevas
```

**Structure Decision**: estructura existente; dos archivos nuevos de lógica pura y se eliminan dos clases de SMS.

## Orden de implementación

1. Limpieza sin riesgo: manifiesto, `network_security_config`, dependencias, textos y claves sin uso, y el escáner
   QR de recarga (US5).
2. Modelo `Permission` sin `Required`, `PermissionRequestPolicy` con pruebas y "Abrir ajustes" en
   `SinglePermissionFragment` (US2).
3. Pantalla única de permisos, primer uso y "Permisos y privacidad"; kernel sin bloqueo (US1).
4. Revisar el comportamiento sin acceso de uso y sin permiso de llamadas (worker, estadísticas) (US1/US2).
5. Accesibilidad con divulgación (US4).
6. SMS User Consent, compras sin confirmar e historial (US3).
7. Verificación con [quickstart.md](quickstart.md).

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Biblioteca de terceros (`play-services-auth-api-phone`) frente a "APIs de Android antes que terceros" (principio I) | Es la única forma de leer el SMS de confirmación sin permisos de SMS (decisión del usuario) | Android no tiene una API equivalente; SMS Retriever exige un hash en el SMS que ETECSA no envía; mantener los permisos de SMS no cumple Play |
