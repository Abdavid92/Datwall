# Quickstart: validar la fase 1

## Requisitos

- Android Studio con su JBR (Java 25) o JDK 17+.
- Android SDK con las plataformas 36 y 37.
- Emuladores o dispositivos con Android 14, 15 y 16 (al menos uno con navegación por gestos).
- Para las historias de ETECSA: un dispositivo con SIM de Cubacel.
- Para la prueba de actualización: un APK de la versión anterior (4.4.0).

## 1. Compilación y pruebas

```bash
./gradlew clean testDebugUnitTest assembleDebug assembleRelease
```

Esperado: BUILD SUCCESSFUL, sin desactivar checks de lint (FR-012, FR-013). Las pruebas
instrumentadas (`connectedDebugAndroidTest`) se ejecutan en un emulador con Android 16.

## 2. Actualización desde la versión anterior (SC-006)

1. Instalar la 4.4.0, configurar el cortafuegos con 2 apps bloqueadas y usar datos un rato.
2. Instalar la nueva versión encima (`adb install -r`).
3. Comprobar: las mismas apps siguen bloqueadas y el historial de consumo se conserva.

## 3. Cortafuegos (historia 1, SC-001) — en Android 14, 15 y 16

1. Activar el cortafuegos y bloquear una app. Abrirla: no tiene datos. Otra permitida: sí.
2. Quitar Datwall de recientes: la notificación del cortafuegos sigue y el bloqueo también.
3. Reiniciar el teléfono: el cortafuegos se reactiva solo.
4. `adb shell dumpsys activity services com.smartsolutions.paquetes` muestra los servicios en
   primer plano con tipo `specialUse`.

## 4. Medición (historia 1, SC-002)

Tras un día de uso, comparar el consumo total y por app con Ajustes → Uso de datos: diferencia ≤ 5 %.

## 5. Notificaciones y burbuja (historia 2) — instalación nueva en Android 14+

1. Aceptar el permiso de notificaciones al activar la función: la notificación persistente aparece.
2. Reinstalar y rechazarlo: la app no se cierra y avisa de que no habrá notificación.
3. Activar la burbuja con el permiso de superposición: se muestra y se actualiza sobre otras apps.

## 6. Compra y consulta de paquetes (historia 3) — con SIM de Cubacel

1. Consultar saldo desde la app: la respuesta USSD se lee y se muestra (sin cierre en Android 14+).
2. Comprar un paquete: se ejecuta y queda registrado.

## 7. Pantalla completa y botón atrás (historia 4, SC-003, SC-004) — Android 15 y 16

1. Recorrer todas las pantallas con navegación por gestos y por botones: nada queda tapado por las
   barras del sistema.
2. En Android 16, usar el gesto atrás en `AppControlActivity` y `ExceptionsActivity`: guardan y
   cierran igual que antes.
3. Repetir el recorrido con permisos concedidos y rechazados: 0 cierres inesperados.
