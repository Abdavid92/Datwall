# Quickstart: validar el primer paso de la fase 4

Dispositivos: el teléfono (Android 16, dos SIM; SIM 2 para planes, SIM 1 empresarial) y el emulador Android 7
(`Medium_Phone`, arrancado con `-no-snapshot-save`; reinstalar la versión actual después de arrancarlo). No comprar
planes. No ejecutar `connectedDebugAndroidTest`.

## 1. Pruebas y compilación

```bash
./gradlew lintRelease testDebugUnitTest assembleDebug assembleRelease
```

- Pasan las pruebas de todas las reglas puras de [research.md](research.md) R9.
- Pasan las pruebas de los 6 ViewModels: estado inicial y una acción principal (FR-009a).
- Pasan las pruebas que reproducen los errores confirmados (R12).
- Lint no informa de recursos nuevos sin uso.

## 2. Revisión del dominio (SC-001)

```bash
grep -rn "import com.smartsolutions.paquetes.ui\.\|import androidx.fragment\|import android.view.View\b" \
  app/src/main/java/com/smartsolutions/paquetes --include=*.kt | grep -v "/ui/"
```

Solo aparecen las excepciones admitidas: notificaciones, burbuja y la implementación de `IAppNavigator` en `ui/`.

## 3. Sin esperas bloqueantes (SC-004)

```bash
grep -rn "runBlocking" app/src/main/java --include=*.kt
```

Solo queda la lectura inicial de `DatwallApplication.onCreate` (R10).

## 4. Arranque y navegación (R2): emulador, sin datos

1. Instalación limpia, sin acceso de uso: primer uso → configuración de líneas → app principal.
2. "Ahora no" en el permiso de teléfono → app; al reiniciar, sin bucle.
3. Conceder el permiso de teléfono desde Sincronizar → se abre la configuración de líneas → app.
4. Una notificación de la app (consumo, cortafuegos, SIM) abre la pantalla que abría antes.

## 5. Revisión pantalla por pantalla (SC-005): teléfono y emulador

En cada pantalla, comparar con la versión de `development`, girar el dispositivo y volver a comprobar.

| Pantalla | Qué comprobar |
|---|---|
| Dashboard | activar y desactivar el cortafuegos y la burbuja; tamaño y transparencia; cortafuegos dinámico; accesos USSD (sin marcar códigos de pago) |
| Consumo | periodo y filtro; gráfica y lista iguales; "Otras"; sin acceso de uso, la explicación |
| Resumen | totales, porcentajes y medias iguales; sincronizar la SIM 2 y la SIM 1 (consultas gratuitas autorizadas); añadir y editar datos manualmente con valores válidos y no válidos |
| Aplicaciones | filtro, búsqueda, cambiar acceso (aviso), aplicar cambios |
| Ajustes | cambiar tema, estilo de notificación (vista previa), sincronización, borrar historial (las compras sin confirmar se conservan) |
| Paquetes | SIM 2 muestra los 5 planes; SIM 1 "no puede comprar planes"; pulsar Comprar solo en el emulador |
| Historial, Permisos, Eventos, Setup | sin cambios visibles (no se convierten en este paso) |

## 6. Actualización

Instalar encima de la versión de `development` en el teléfono. Se conservan el tema, los filtros, la burbuja, la
sincronización y el historial.
