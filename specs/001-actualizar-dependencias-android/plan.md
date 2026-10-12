# Implementation Plan: Actualizar dependencias y compatibilidad con Android 14–16

**Branch**: `001-actualizar-dependencias-android` | **Date**: 2026-10-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-actualizar-dependencias-android/spec.md`

## Summary

Llevar la app de `targetSdk 32` a `36` (`compileSdk 37`, `minSdk 23`) y actualizar todas las
bibliotecas, sin cambiar el comportamiento del cortafuegos, la medición, la burbuja, la notificación
ni la compra de paquetes. El enfoque, por orden: subir las herramientas de build (AGP 9.4, Gradle 9.8,
Kotlin integrado, KSP) y quitar kapt y data binding; actualizar las bibliotecas; eliminar el código
para API < 23; y atender los cambios de Android 13–16 (tipos de servicio en primer plano, receiver
USSD, permiso de notificaciones, edge-to-edge y botón atrás predictivo). Detalle en
[research.md](research.md).

## Technical Context

**Language/Version**: Kotlin 2.4.21 (integrado en AGP 9.4.1), Java 17; `vpncore` en Java

**Primary Dependencies**: AndroidX (core 1.19, appcompat 1.8, lifecycle 2.11, navigation 2.10,
work 2.12), Material 1.14, Hilt 2.60.1, Room 2.8.5, Retrofit 3.0, RxJava 2.2.21 (ver
[research.md](research.md) R10)

**Storage**: Room (SQLite) y DataStore; sin cambios de esquema ([data-model.md](data-model.md))

**Testing**: JUnit 4 en `src/test` (unitarias); pruebas instrumentadas con Hilt en `src/androidTest`

**Target Platform**: Android 6.0 (API 23) a Android 16 (API 36); compilado contra API 37

**Project Type**: mobile-app (módulos `app` y `vpncore`)

**Performance Goals**: sin regresión respecto a la 4.4.0 en batería y fluidez en gama baja

**Constraints**: servicios persistentes que deben sobrevivir a segundo plano y reinicio; app
instalada desde Apklis (fuera de Play)

**Scale/Scope**: ~11 activities, 4 servicios, 5 workers, 1 widget; ~39 comprobaciones de `SDK_INT`
que quedan obsoletas

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Estado | Cómo se cumple |
|---|---|---|
| I. KISS | ✅ | Cambios mínimos: no se migra RxJava, `LocalBroadcastManager` se declara explícito en vez de reescribirse, un solo tipo de servicio (`specialUse`) para los tres servicios. |
| II. DRY | ✅ | Un único helper de insets para todas las pantallas; los datos de ETECSA no se tocan. |
| III. Pruebas unitarias | ✅ | La lógica nueva (decisión del permiso de notificaciones) tiene pruebas en `src/test`; las pruebas existentes deben pasar. Lo que depende del sistema (servicios, insets, back) se verifica con [quickstart.md](quickstart.md). |
| IV. Privacidad | ✅ | Ninguna conexión ni SDK nuevo. |
| V. Android actual y Play | ✅ | `targetSdk 36`; solo se añaden los permisos que exige Android (`POST_NOTIFICATIONS`, `FOREGROUND_SERVICE_SPECIAL_USE`); se reactiva el check de lint `ExpiredTargetSdkVersion`. |
| Estándares técnicos | ✅ | Solo repos oficiales; Hilt y Room se mantienen. |

**Re-evaluación tras el diseño**: sin cambios. Las tres excepciones de versión (RxJava 2, Picasso 2,
zxing core 3.3.0) están documentadas en R10 y no violan principios.

## Project Structure

### Documentation (this feature)

```text
specs/001-actualizar-dependencias-android/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── manifest.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
build.gradle                       # AGP, Kotlin, KSP, Hilt
gradle/wrapper/                    # Gradle 9.8.1
gradle.properties
app/
├── build.gradle                   # SDKs, dependencias, KSP, sin dataBinding
├── proguard-rules.pro
└── src/
    ├── main/
    │   ├── AndroidManifest.xml    # ver contracts/manifest.md
    │   ├── java/com/smartsolutions/paquetes/
    │   │   ├── services/          # FirewallService, DatwallService, BubleFloatingService, UIScannerService
    │   │   ├── helpers/           # USSDHelper, NotificationHelper, BubbleServiceHelper
    │   │   ├── managers/          # PermissionsManager (+ POST_NOTIFICATIONS)
    │   │   ├── ui/                # AbstractActivity (edge-to-edge), AppControlActivity, ExceptionsActivity
    │   │   └── ...                # código para API < 23 a eliminar (R8)
    │   └── res/                   # 4 layouts de data binding → view binding; temas sin statusBarColor
    └── test/                      # pruebas unitarias nuevas
vpncore/
└── build.gradle                   # compileSdk 37, minSdk 23, Java 17
```

**Structure Decision**: se mantiene la estructura actual de dos módulos (`app`, `vpncore`); no se
crean módulos ni paquetes nuevos.

## Orden de implementación

1. **Build**: AGP 9.4.1, Gradle 9.8.1, Kotlin integrado, KSP para Room y Hilt, data binding →
   view binding. Compila con `targetSdk 32` todavía.
2. **Bibliotecas**: versiones de R10 y `minSdk 23`. Compila y pasan las pruebas.
3. **Limpieza API < 23** (R8).
4. **`targetSdk 36`** con los cambios de comportamiento: tipos de servicio (R3), receiver USSD (R4),
   permiso de notificaciones (R5), edge-to-edge (R6), botón atrás (R7), APIs obsoletas tocadas (R9).
5. **Verificación** con [quickstart.md](quickstart.md) en Android 14, 15 y 16.

Los pasos 1–3 no cambian el comportamiento. El paso 4 va en commits propios para aislar regresiones.

## Complexity Tracking

Sin violaciones de la constitución.
