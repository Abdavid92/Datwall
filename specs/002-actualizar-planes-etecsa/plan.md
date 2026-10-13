# Implementation Plan: Actualizar los planes de ETECSA

**Branch**: `002-actualizar-planes-etecsa` | **Date**: 2026-10-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/002-actualizar-planes-etecsa/spec.md`

## Summary

Sustituir el catálogo de paquetes por los cinco planes USSD vigentes, sin cambiar el esquema de la base
de datos: el Plan Diario reutiliza `DailyBag`, los planes de 35 días son valores nuevos al final del enum y
los obsoletos se marcan `deprecated`. El catálogo pasa a escribirse con *upsert* para no borrar el
historial en cascada. La configuración por SIM se reduce a "puede comprar planes sí/no". USSD (compra y
consulta) pasa a usar la SIM elegida. La lectura del saldo se adapta al formato nuevo (`Datos:`) en una
clase pura con pruebas, y la detección por SMS acepta el remitente "ETECSA" y compara texto normalizado.
Detalle en [research.md](research.md).

## Technical Context

**Language/Version**: Kotlin 2.4.21, Java 17

**Primary Dependencies**: Room 2.8.5 (`@Upsert`), Hilt 2.60.1, Android Telephony/Telecom APIs

**Storage**: Room, versión 1, sin cambios de esquema ([data-model.md](data-model.md))

**Testing**: JUnit 4 en `app/src/test` para catálogo, códigos USSD, lectura de saldo y SMS

**Target Platform**: Android 6.0 (API 23) a Android 16 (API 36)

**Project Type**: mobile-app

**Performance Goals**: sin requisitos nuevos

**Constraints**: no cambiar ordinales de `PackageId` ni nombres de `DataType`; no añadir migraciones de
Room; no comprar planes reales durante las pruebas

**Scale/Scope**: 5 planes activos, 8 obsoletos; ~15 archivos afectados

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Estado | Cómo se cumple |
|---|---|---|
| I. KISS | ✅ | Sin cambios de esquema; se reutilizan `Sim.network` y `DailyBag`; se elimina el modo de configuración automático en lugar de adaptarlo. |
| II. DRY | ✅ | Oferta y códigos USSD en un único lugar (`DataPackages.kt`); lectura de saldo y SMS en funciones únicas. |
| III. Pruebas unitarias | ✅ | Catálogo, `buildUssd`, `UssdBalanceParser`, comparación de SMS y el arreglo de `DataBytes` con pruebas en `src/test`, usando los ejemplos reales. |
| IV. Privacidad | ✅ | Solo se leen SMS de remitentes de ETECSA; nada sale del dispositivo. |
| V. Android actual y Play | ✅ | Desaparece el modo automático que dependía del servicio de accesibilidad para configurar. |

**Re-evaluación tras el diseño**: sin cambios.

## Project Structure

### Documentation (this feature)

```text
specs/002-actualizar-planes-etecsa/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── ussd-sms.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
app/src/main/java/com/smartsolutions/paquetes/
├── data/DataPackages.kt                       # catálogo v4, constantes USSD
├── repositories/
│   ├── contracts/IDataPackageDao.kt           # @Upsert
│   ├── SimRepository.kt                       # filtrar deprecated
│   └── models/DataBytes.kt                    # arreglo getDataTypeOfInternationalNetworks
├── managers/
│   ├── DataPackageManager.kt                  # buildUssd, configuración sí/no, migración de SIM
│   ├── SynchronizationManager.kt              # usa UssdBalanceParser y la sim recibida
│   ├── UssdBalanceParser.kt                   # nuevo, puro
│   ├── UserDataBytesManager.kt                # 35 días
│   └── PurchasedPackagesManager.kt            # comparación normalizada
├── helpers/USSDHelper.kt                      # parámetro Sim
├── receivers/SmsReceiver.kt                   # remitente ETECSA
└── ui/
    ├── packages/                              # una sección "Planes", sin gate de SIM por defecto
    └── settings/PackagesConfigurationFragment.kt  # sí/no
app/src/main/res/                              # strings y layouts de compra y configuración
app/src/test/java/com/smartsolutions/paquetes/ # pruebas nuevas
```

**Structure Decision**: estructura existente; un único archivo nuevo de lógica (`UssdBalanceParser.kt`).

## Orden de implementación

1. Catálogo v4, constantes USSD, `@Upsert` y migración de SIM (con pruebas).
2. `buildUssd` y USSD por SIM (compra y consulta).
3. Lectura de saldo (`UssdBalanceParser`) y arreglo de `DataBytes` (con pruebas de los ejemplos reales).
4. Detección por SMS (remitente y comparación normalizada, con pruebas).
5. Configuración sí/no y pantalla de compra.
6. Verificación con [quickstart.md](quickstart.md).

## Complexity Tracking

Sin violaciones de la constitución.
