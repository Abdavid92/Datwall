# Feature Specification: Actualizar dependencias y compatibilidad con Android 14–16

**Feature Branch**: `001-actualizar-dependencias-android`

**Created**: 2026-10-12

**Status**: Draft

**Input**: User description: "Actualizar las dependencias del proyecto y agregar compatibilidad con las últimas versiones de Android. La aplicación se fue quedando atrás: targetSdk está en 32, muchas bibliotecas (AndroidX, Material, Navigation, Lifecycle, WorkManager, Retrofit, Gson, RxJava, Picasso, ZXing, etc.) tienen versiones de 2022 o anteriores, y hay bibliotecas abandonadas incluidas como AAR en app/libs (indicatorseekbar, welcome, html-textview). El objetivo es que la app funcione correctamente en Android 14, 15 y 16 manteniendo el cortafuegos VPN, las estadísticas de consumo, la burbuja flotante, la notificación persistente y la compra de paquetes funcionando como hasta ahora."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El cortafuegos y la medición funcionan en Android 14, 15 y 16 (Priority: P1)

Un usuario con un teléfono con Android 14, 15 o 16 instala la nueva versión de la app, activa el
cortafuegos y bloquea apps. Las apps bloqueadas se quedan sin conexión de datos móviles, el
servicio sigue activo aunque el usuario salga de la app o reinicie el teléfono, y el consumo de
datos se sigue registrando.

**Why this priority**: es la función por la que existe la app. Si el cortafuegos se detiene o
la medición falla en las versiones actuales de Android, el usuario pierde datos que paga.

**Independent Test**: en un dispositivo o emulador con cada versión (14, 15 y 16): activar el
cortafuegos, bloquear una app, comprobar que no navega, bloquear la pantalla 30 minutos, reiniciar
el teléfono y comprobar que el bloqueo y el registro de consumo continúan.

**Acceptance Scenarios**:

1. **Given** el cortafuegos activado con una app bloqueada, **When** el usuario abre la app
   bloqueada, **Then** esa app no tiene conexión de datos y las apps permitidas sí.
2. **Given** el cortafuegos activado, **When** el usuario cierra Datwall y la quita de recientes,
   **Then** el cortafuegos sigue activo y su notificación sigue visible.
3. **Given** el cortafuegos activado, **When** el teléfono se reinicia, **Then** el cortafuegos se
   vuelve a activar solo, como en la versión actual.
4. **Given** consumo de datos durante el día, **When** el usuario abre las estadísticas, **Then**
   ve el consumo total y por app del día, igual que en la versión actual.

---

### User Story 2 - Notificación persistente y burbuja flotante en Android moderno (Priority: P2)

El usuario quiere ver la velocidad y el consumo en tiempo real. En Android 13 y posteriores la app
le pide permiso para mostrar notificaciones en el momento adecuado; si lo concede, la notificación
persistente funciona como antes. La burbuja flotante se sigue mostrando sobre otras apps cuando el
usuario la activa.

**Why this priority**: son funciones visibles y muy usadas, pero el cortafuegos puede funcionar
sin ellas.

**Independent Test**: en Android 14–16 con la app recién instalada: aceptar el permiso de
notificaciones y comprobar la notificación persistente; activar la burbuja y comprobar que se
muestra y se actualiza; repetir rechazando el permiso de notificaciones y comprobar que la app no
falla.

**Acceptance Scenarios**:

1. **Given** una instalación nueva en Android 13+, **When** el usuario activa una función que
   necesita notificaciones, **Then** la app pide el permiso de notificaciones antes de usarla.
2. **Given** el permiso de notificaciones rechazado, **When** el usuario usa la app, **Then** la
   app no se cierra y explica que la notificación no estará visible.
3. **Given** la burbuja activada y el permiso de superposición concedido, **When** el usuario usa
   otra app, **Then** la burbuja muestra el consumo actualizado.

---

### User Story 3 - Compra y consulta de paquetes siguen funcionando (Priority: P2)

El usuario consulta su saldo y compra un paquete de ETECSA desde la app, igual que en la versión
actual (los paquetes concretos se actualizan en la fase 2 del roadmap, no aquí).

**Why this priority**: es la tercera función principal; la actualización no debe romperla.

**Independent Test**: con una SIM de Cubacel en Android 14–16: lanzar la consulta de saldo y la
compra de un paquete desde la app y comprobar que se ejecutan y que la app lee la respuesta.

**Acceptance Scenarios**:

1. **Given** una SIM de Cubacel, **When** el usuario consulta su saldo desde la app, **Then** la
   app muestra el resultado como en la versión actual.
2. **Given** una SIM de Cubacel con saldo, **When** el usuario compra un paquete desde la app,
   **Then** la compra se ejecuta y el paquete queda registrado.

---

### User Story 4 - Interfaz visible y usable con pantalla completa (Priority: P3)

En Android 15+ las apps se dibujan obligatoriamente a pantalla completa (por detrás de la barra de
estado y la de navegación). El usuario debe poder ver y pulsar todo el contenido de todas las
pantallas sin que quede tapado por las barras del sistema.

**Why this priority**: sin esto partes de la UI quedan ocultas o no se pueden pulsar, pero el
rediseño completo de la UI corresponde a la fase 4.

**Independent Test**: recorrer todas las pantallas en Android 15 y 16, con navegación por gestos y
por botones, y comprobar que ningún texto ni botón queda tapado.

**Acceptance Scenarios**:

1. **Given** Android 15 o 16, **When** el usuario abre cualquier pantalla, **Then** ningún
   contenido interactivo queda debajo de la barra de estado o de navegación.

---

### Edge Cases

- El usuario actualiza desde la versión anterior con el cortafuegos activo: tras actualizar, la
  configuración de apps bloqueadas y el historial de consumo se conservan.
- El sistema mata el servicio del cortafuegos por falta de memoria: se reinicia solo como en la
  versión actual.
- El usuario rechaza el permiso de notificaciones o de superposición: la app sigue funcionando y
  solo se desactiva la función afectada.
- El teléfono tiene Android 6–13: la app sigue funcionando como hasta ahora.
- El teléfono tiene Android 5.x: la nueva versión no se puede instalar; el usuario conserva la
  versión que ya tenga instalada.
- Restricciones de batería del fabricante (Xiaomi, Samsung, Huawei): el comportamiento no debe ser
  peor que el de la versión actual.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La app DEBE declarar como versión objetivo de Android la que exige Google Play a
  fecha de esta especificación (Android 16) y atender los cambios de comportamiento de cada
  versión intermedia (13, 14, 15 y 16).
- **FR-002**: El cortafuegos DEBE seguir bloqueando y permitiendo el tráfico por app, mantenerse
  activo en segundo plano y reactivarse tras reiniciar el teléfono en Android 14, 15 y 16.
- **FR-003**: La medición de consumo (total, por app y por hora) DEBE seguir registrándose y
  mostrando los mismos resultados que en la versión actual.
- **FR-004**: Cada servicio que se ejecute en primer plano DEBE declarar el tipo de uso que exige
  Android 14+ para poder seguir ejecutándose.
- **FR-005**: La app DEBE pedir el permiso de notificaciones en Android 13+ en el contexto de la
  función que lo necesita y seguir funcionando si se rechaza.
- **FR-006**: La burbuja flotante, la notificación persistente, el widget y la consulta y compra de
  paquetes DEBEN seguir funcionando como en la versión actual.
- **FR-007**: Todas las pantallas DEBEN mostrar su contenido sin que quede tapado por las barras
  del sistema en Android 15+.
- **FR-008**: Todas las bibliotecas DEBEN actualizarse a su última versión estable compatible con
  la versión mínima de Android que soporte la app.
- **FR-009**: Las bibliotecas abandonadas incluidas como archivos locales (selector de rango,
  pantalla de bienvenida y visor de HTML) se mantienen en esta fase y DEBEN seguir funcionando con
  la nueva versión objetivo. Su sustitución se hace en la fase 4 (UI), donde se rediseñan las
  pantallas que las usan.
- **FR-010**: La versión mínima de Android soportada DEBE ser Android 6.0 (API 23), mínimo que
  exigen las versiones actuales de AndroidX. Justificación (principio V de la constitución): los
  usuarios de Android 5.x son muy pocos y mantenerlos obligaría a dejar bibliotecas desactualizadas.
- **FR-011**: Al actualizar desde la versión anterior se DEBEN conservar la configuración del
  usuario, las reglas del cortafuegos y el historial de consumo.
- **FR-012**: Las pruebas unitarias existentes DEBEN pasar, y toda lógica nueva añadida para la
  compatibilidad (por ejemplo, decidir cuándo pedir un permiso) DEBE tener pruebas unitarias.
- **FR-013**: Las variantes de depuración y publicación DEBEN compilar sin errores, sin desactivar
  comprobaciones de lint salvo justificación documentada.

### Key Entities

- **Regla del cortafuegos**: qué apps están bloqueadas o permitidas; debe conservarse tras la
  actualización.
- **Registro de consumo**: datos consumidos por app y por periodo; debe conservarse y seguir
  creciendo tras la actualización.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En Android 14, 15 y 16, el cortafuegos sigue activo y bloqueando tras 24 horas de uso
  normal con la pantalla apagada y tras reiniciar el teléfono, en el 100 % de las pruebas.
- **SC-002**: El consumo registrado por la app no difiere en más de un 5 % del que muestra el
  sistema (ajustes de uso de datos) en un día de prueba.
- **SC-003**: 0 cierres inesperados al recorrer todas las pantallas y funciones principales en
  Android 14, 15 y 16, con permisos concedidos y con permisos rechazados.
- **SC-004**: El 100 % de las pantallas muestran todo su contenido interactivo sin quedar tapado
  por las barras del sistema en Android 15 y 16.
- **SC-005**: Ninguna biblioteca del proyecto queda con más de un año de retraso respecto a su
  última versión estable, salvo las tres incluidas como archivos locales (FR-009) y otras
  excepciones documentadas.
- **SC-006**: Los usuarios que actualizan desde la versión anterior conservan el 100 % de sus
  reglas del cortafuegos e historial de consumo.

## Assumptions

- El alcance de los permisos se limita a lo que exige Android para la nueva versión objetivo
  (notificaciones, tipos de servicio en primer plano). La revisión completa de permisos según las
  políticas de Google Play es la fase 3 del roadmap.
- La actualización de los paquetes de ETECSA (oferta, precios, códigos) es la fase 2; aquí solo se
  garantiza que la consulta y la compra siguen funcionando con los datos actuales.
- Los ajustes de UI se limitan a que el contenido no quede tapado (pantalla completa obligatoria);
  el rediseño es la fase 4.
- No se migran las bibliotecas a tecnologías distintas (por ejemplo, cambiar el modelo de
  programación asíncrona) salvo que la versión actual deje de ser compatible; se aplica KISS.
- La verificación en Android 14, 15 y 16 se hace en dispositivos reales o emuladores con esas
  versiones; la compra real de paquetes requiere una SIM de Cubacel.
- La app sigue distribuyéndose por Apklis.
