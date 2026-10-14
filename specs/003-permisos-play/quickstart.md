# Quickstart: validar la fase 3

Dispositivos: el teléfono (Android 16, dos SIM; SIM 2 para planes) y el emulador Android 7 (`Medium_Phone`). No
pulsar "Comprar" en el teléfono salvo que el usuario lo autorice expresamente; no ejecutar
`connectedDebugAndroidTest`.

## 1. Pruebas y compilación

```bash
./gradlew lintRelease testDebugUnitTest assembleDebug assembleRelease
```

Pasan las pruebas nuevas: decisión de petición (R3), selección de la compra a confirmar (R5) y las de fases
anteriores.

## 2. Permisos declarados

```bash
adb shell dumpsys package com.smartsolutions.paquetes | grep "permission"
```

No aparecen SMS, `READ_PRIVILEGED_PHONE_STATE`, `REQUEST_INSTALL_PACKAGES`, `CHANGE_WIFI_STATE`,
`ACCESS_WIFI_STATE`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` ni `CAMERA` (SC-003). El menú de compra ya no tiene
"Recargar saldo".

## 3. Primer uso (emulador; instalación limpia de una copia de prueba)

1. Abrir la app: aparece la explicación de datos antes de cualquier diálogo del sistema.
2. "Ahora no" → se llega a la app sin ningún diálogo de permisos (SC-001).
3. Reinstalar, "Continuar" → acceso de uso (y notificaciones en 13+) → configuración de las líneas, que pide el
   permiso de teléfono. Rechazarlo → se llega a la app sin bucles; concederlo después → se abre la configuración de
   las líneas.
4. Las estadísticas muestran por qué necesitan el acceso de uso y el botón para concederlo; nada se cierra (SC-002).

## 4. Actualización (teléfono)

Instalar encima de la versión de `development`: no aparece el primer uso; la app sigue funcionando y desaparecen los
permisos de SMS.

## 5. Peticiones en contexto (emulador, con todo denegado)

| Acción | Esperado |
|---|---|
| Sincronizar | Explicación → permiso de llamadas; al rechazarlo dos veces, "Abrir ajustes" |
| Activar la burbuja | Explicación → ajuste de superposición |
| Activar el cortafuegos | Explicación → diálogo de VPN |
| Ajustes → "Permisos y privacidad" | Lista con estado de cada permiso y recomendación de batería |

## 6. Accesibilidad (emulador Android 7)

Sincronizar con el servicio apagado: aparece la divulgación con "Acepto" / "No, gracias"; "No, gracias" cancela sin
abrir ajustes. En el teléfono (Android 16, modo moderno) no se pide nunca.

## 7. Compra sin confirmar (solo con autorización del usuario para comprar)

- Teléfono con Google Play Services: comprar el Plan Diario por la SIM 2 → al llegar el SMS aparece el diálogo del
  sistema; aceptar → la compra pasa a confirmada en el historial.
- Emulador sin Google Play Services (o rechazando el diálogo): la compra aparece "Sin confirmar" sin errores.

Si no se autoriza una compra real, se valida con las pruebas unitarias y comprobando que el diálogo de la compra
registra la entrada "Sin confirmar" (marcando el código en el emulador, que no tiene línea).
