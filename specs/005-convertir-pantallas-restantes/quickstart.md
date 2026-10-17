# Quickstart: validar el paso 2 de la fase 4

Dispositivos: el teléfono (Android 16, dos SIM; SIM 2 para planes, SIM 1 empresarial) y el emulador Android 7
(`Medium_Phone`, arrancado con `-no-snapshot-save`; reinstalar la versión actual después de arrancarlo). No comprar
planes. No ejecutar `connectedDebugAndroidTest`. En el teléfono, los toques por `adb input` funcionan; los de `mobile-mcp`
los rechaza el sistema.

## 1. Pruebas y compilación

```bash
./gradlew lintRelease testDebugUnitTest assembleDebug assembleRelease
```

- Pasan las pruebas de las 6 lógicas de pantalla de [contracts/ui-state.md](contracts/ui-state.md).
- Pasan las pruebas de los posibles errores de [research.md](research.md) R8.
- Lint no informa de `UnusedResources` ni de `SetTextI18n` (SC-006).

## 2. Búsquedas de código (SC-001 y SC-002)

```bash
# Sin objetos de respuesta ni referencias a pantallas en las 4 pantallas:
grep -rn "SinglePermissionCallback\|OnCompletedListener\|var callback\|var listener" \
  app/src/main/java --include=*.kt

# Sin Application, DataStore ni LiveData en sus lógicas de pantalla:
grep -n "AndroidViewModel\|settingsDataStore\|LiveData" \
  app/src/main/java/com/smartsolutions/paquetes/ui/{permissions,history,events,setup}/*ViewModel.kt

# Sin esperas bloqueantes en pantallas:
grep -rn "runBlocking" app/src/main/java/com/smartsolutions/paquetes/ui --include=*.kt
```

No debe aparecer nada. En todo el código solo quedan los `runBlocking` de servicios y constructores de notificaciones
y la lectura inicial de `DatwallApplication`.

## 3. Primer uso y Setup: emulador Android 7, instalación limpia

1. Bienvenida → Permisos y privacidad → "Continuar" → configuración de líneas → planes → pantalla principal.
2. Lo mismo con "Ahora no" en cada paso; al reiniciar la app no vuelve a pedir nada.
3. Girar el emulador en cada paso: no se reinicia ni se salta ninguno.
4. Con la app abierta en el paso de planes, matar el proceso y reabrir: continúa en el paso o al arranque normal, sin
   quedarse en blanco.

## 4. Hojas de permisos: teléfono Android 16

Desde cada pantalla que las abre, con el permiso sin conceder (Ajustes → Aplicaciones → Permisos):

- **Resumen** → Sincronizar (llamadas): conceder → la sincronización continúa una vez. Rechazar → no continúa.
- **Controles** → Burbuja (sobreponerse) y Cortafuegos (VPN): conceder / rechazar en cada una.
- **Paquetes**: la hoja solo se abre al intentar una compra sin el permiso de llamadas, así que **no se prueba en el
  teléfono**. Se prueba en el emulador, donde no hay red móvil ni USSD, pulsando "Comprar" sin ese permiso.
- **Configuración de líneas**: en el paso de líneas del primer uso (emulador, instalación limpia) y desde la
  notificación de configuración pendiente, con el permiso de llamadas sin conceder.
- Rechazar dos veces el permiso de llamadas → la hoja ofrece "Abrir ajustes"; conceder desde ahí y volver.
- Girar el teléfono con la hoja abierta y mientras el diálogo del sistema está visible: el resultado llega igualmente y
  no se avisa de "denegado" sin responder.
- Descartar la hoja con el gesto de volver: "denegado" una sola vez.
- Servicio de accesibilidad: ir a los ajustes y volver con y sin activarlo.

## 5. Lista de Permisos, Historial y Eventos

- Ajustes → Permisos y privacidad: cada fila refleja el estado y cambia al conceder; el permiso de sobreponerse muestra
  su nombre y descripción corregidos.
- Historial: sin compras (mensaje), con compras (gráfica y lista por mes).
- Eventos: lista del más reciente al más antiguo; filtrar por tipo; abrir un evento; "Enviar informe" con y sin app de
  correo.

## 6. Textos y lint

- Notificaciones y Estadísticas: ningún texto ofrece planes LTE ni Bolsa de Mensajería; las categorías aparecen solo con
  datos de ese tipo.
- Cambio de idioma a inglés: los textos corregidos están traducidos.
