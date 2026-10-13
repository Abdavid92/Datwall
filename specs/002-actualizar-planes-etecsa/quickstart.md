# Quickstart: validar la fase 2

## 1. Pruebas unitarias

```bash
./gradlew testDebugUnitTest assembleDebug assembleRelease
```

Deben pasar las pruebas de catálogo (5 planes activos y sus códigos), `buildUssd`, `UssdBalanceParser`
(con los ejemplos reales de [contracts/ussd-sms.md](contracts/ussd-sms.md)), la comparación de SMS y
`getDataTypeOfInternationalNetworks`.

## 2. Actualización sin perder historial (emulador Android 7)

1. Con la versión de `development`, registrar una compra de un paquete antiguo (Agregar paquete manual).
2. Instalar la nueva versión encima.
3. El historial sigue mostrando la compra; la pantalla de compra solo muestra los 5 planes vigentes.

## 3. Pantalla de compra (teléfono, SIM 2)

1. Abrir Más → Paquetes: aparecen Plan Diario, 4.5 GB, 120, 240 y 360 con sus recursos; ningún plan LTE.
2. **No pulsar "Comprar"**: el código se marca directamente y compraría el plan. La correspondencia
   plan ↔ código se verifica con las pruebas unitarias.

## 4. Configuración por SIM

1. En la configuración inicial, marcar la SIM 1 como "no puede comprar planes" y la SIM 2 como "sí".
2. La SIM 1 no muestra planes para comprar; la SIM 2 sí.

## 5. Consulta por la SIM elegida (teléfono, dos SIM, llamadas en "preguntar siempre")

1. En Resumen, elegir la pestaña SIM 2 y Sincronizar.
2. En el log de radio, el USSD sale por `PHONE1` (SIM 2) y no aparece el selector de SIM del sistema.
3. Sin plan activo: la app muestra 0 datos sin error.
