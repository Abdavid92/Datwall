# Feature Specification: Convertir las 4 pantallas restantes a estado y eventos

**Feature Branch**: `005-convertir-pantallas-restantes`

**Created**: 2026-10-16

**Status**: Draft

**Input**: User description: "Terminar el paso 1 de la fase 4 (separar la lógica de la UI) convirtiendo las 4 pantallas que
quedaron fuera: Permisos, Historial, Eventos y Setup. Igual que las 6 pantallas ya convertidas: cada pantalla observa un
único estado que le prepara su lógica de pantalla y recibe eventos puntuales; la lógica de pantalla no recibe vistas,
fragmentos ni gestores de pantallas ni guarda referencias a ellos; las preferencias pasan por el repositorio común; sin
esperas bloqueantes; cada lógica de pantalla lleva su prueba unitaria. La app debe verse y comportarse igual, salvo
errores confirmados. Como cierre, limpiar los pendientes menores anotados: nombres antiguos de permisos, textos
LTE/Mensajería en notificaciones y estadísticas, y avisos de lint previos. Fuera de alcance: elegir entre Compose y XML,
rehacer el aspecto visual y los `runBlocking` de servicios y constructores de notificaciones."

## Contexto

El paso 1 de la fase 4 ([004-separar-logica-ui](../004-separar-logica-ui/spec.md)) dejó la lógica de negocio fuera de las
pantallas y convirtió a "estado único + eventos" las 6 más acopladas (Controles, Consumo, Resumen, Aplicaciones, Ajustes y
Paquetes). Quedaron cuatro, que se difirieron para convertirlas antes de rehacer la interfaz. Este paso las termina, para
que ninguna pantalla conserve el patrón antiguo y la decisión entre Compose y XML se tome sobre una base uniforme.

Estado actual de las 4 pantallas (2026-10-16):

- **Permisos**: la lista de permisos (`PermissionsActivity`) y dos hojas inferiores que pide cualquier otra pantalla,
  una para un permiso concreto (`SinglePermissionFragment`) y otra para activar el servicio de accesibilidad
  (`StartAccessibilityServiceFragment`).
  - Las lógicas de las dos hojas guardan un objeto de respuesta (`callback`, `listener`) de quien las abre, y la de
    accesibilidad recibe el fragmento en una de sus funciones.
  - Los fragmentos leen los gestores a través de la lógica de pantalla (`viewModel.permissionsManager`,
    `configurationManager`).
  - La lógica de la lista escribe en las preferencias por su cuenta, a través de `Application`.
  - Quien abre una hoja le pasa un objeto de respuesta: lo hacen Resumen, Controles, Paquetes y la configuración de
    líneas.
- **Historial**: la lógica expone `LiveData`; las pestañas por línea, la gráfica de barras por mes y la lista con
  cabeceras mensuales ya calculan sus datos en el dominio (paso 1), pero la pantalla sigue sin estado único.
- **Eventos**: la lógica crea por su cuenta el gestor de registros y expone `LiveData`.
- **Setup**: la lógica usa `Application` y `LiveData`, y los pasos de configuración avisan a la actividad con una
  interfaz que la actividad implementa (`OnCompletedListener`).

Pendientes menores anotados al cerrar las fases 2 y 3 que se limpian aquí: el nombre antiguo del permiso de sobreponerse
a otras apps ("...Burbuja Flotante o el Cortafuegos"), los textos de LTE y mensajería que quedaron en notificaciones y
estadísticas tras quitar esos planes, y los avisos de lint `UnusedResources` y `SetTextI18n`.

## Clarifications

### Session 2026-10-16

- Elección de tecnología de interfaz y rediseño visual: fuera de este paso; se deciden después con una pantalla piloto.
- Los `runBlocking` que quedan en servicios y constructores de notificaciones: fuera de este paso (anotados en
  [tasks.md de la 004](../004-separar-logica-ui/tasks.md), T047).
- Q: ¿Qué se hace con los textos de LTE y mensajería de notificaciones y estadísticas? → A: Solo reescribir los textos
  que prometen planes que ya no se venden; las categorías se conservan mientras haya datos de ese tipo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Pedir un permiso no ata la pantalla que lo pide (Priority: P1)

Quien mantiene la app puede abrir la hoja de un permiso (llamadas, sobreponerse, VPN, acceso de uso o accesibilidad) desde
cualquier pantalla y recibir el resultado sin que la pantalla de origen ni la hoja se guarden referencias una a la otra.
Para el usuario nada cambia: la hoja explica el permiso, lo pide, lleva a los ajustes si fue rechazado de forma
permanente y avisa del resultado.

**Why this priority**: es el patrón antiguo que más se repite y que más pantallas arrastran: cuatro de las seis
pantallas ya convertidas todavía le pasan un objeto de respuesta a la hoja, y una referencia guardada en una lógica de
pantalla se pierde o fuga al girar el teléfono.

**Independent Test**: pedir cada permiso desde cada pantalla que lo hace (Resumen, Controles, Paquetes, configuración de
líneas) y completar el flujo concediendo, rechazando y girando el teléfono a mitad; el resultado llega a la pantalla de
origen una sola vez y la acción pendiente continúa.

**Acceptance Scenarios**:

1. **Given** la hoja de un permiso abierta desde una pantalla, **When** el usuario concede el permiso, **Then** la hoja
   se cierra y la pantalla de origen continúa la acción que lo pedía, una sola vez.
2. **Given** la misma hoja, **When** el usuario la cierra sin conceder (botón "Ahora no", gesto de volver o tocar fuera),
   **Then** la pantalla de origen se entera de que no se concedió y no continúa la acción.
3. **Given** un permiso rechazado de forma permanente, **When** el usuario toca "Abrir ajustes" y vuelve habiéndolo
   concedido, **Then** se detecta que ya está concedido y el resultado es el mismo que en el escenario 1.
4. **Given** la hoja abierta, **When** se gira el teléfono antes de responder, **Then** la hoja sigue mostrando el mismo
   permiso y el resultado final llega igualmente a la pantalla de origen.
5. **Given** el permiso de llamadas concedido cuando hay líneas por configurar, **When** se cierra la hoja, **Then** se
   abre la configuración de líneas antes de continuar con la acción, como ahora.
6. **Given** el servicio de accesibilidad apagado, **When** el usuario va a los ajustes y vuelve, **Then** el resultado
   (activado o no) llega a la pantalla de origen, que continúa o muestra el aviso de cancelación, como ahora.

---

### User Story 2 - La lista de permisos y el arranque de Setup dependen solo de su estado (Priority: P1)

La pantalla "Permisos y privacidad" (primer uso y desde Ajustes) y el asistente de configuración inicial (líneas y
planes) muestran lo mismo que ahora, pero cada una dibuja un único estado y reacciona a eventos, sin escribir
preferencias por su cuenta ni avisarse mediante interfaces que la actividad implementa.

**Why this priority**: son las dos pantallas del primer uso y de la recuperación de errores; cualquier fallo se nota en la
primera impresión y en los reinicios del asistente.

**Independent Test**: instalación limpia en el emulador Android 7 (primer uso → permisos → configuración de líneas →
planes → pantalla principal), "Ahora no" en cada paso, y abrir "Permisos y privacidad" desde Ajustes.

**Acceptance Scenarios**:

1. **Given** el primer uso, **When** el usuario pulsa "Continuar" o "Ahora no" en Permisos, **Then** se registra que ya se
   informó de los datos que usa la app y el arranque sigue, sin quedarse en la pantalla ni repetirla al reiniciar.
2. **Given** la lista abierta desde Ajustes, **When** el usuario concede un permiso, **Then** la fila cambia de estado al
   volver; los permisos que no existen en esa versión de Android no se listan.
3. **Given** pasos de configuración pendientes, **When** el usuario completa uno, **Then** se muestra el siguiente; al
   terminar o al omitir se sigue con el arranque normal.
4. **Given** el asistente a mitad de camino, **When** se gira el teléfono, **Then** sigue en el mismo paso sin
   reiniciarse ni saltarse ninguno.
5. **Given** que el permiso de llamadas se niega en el paso de líneas, **When** el usuario elige "Ahora no", **Then** se
   omite la configuración y se recuerda para pedirla cuando conceda el permiso, como ahora.

---

### User Story 3 - Historial y Eventos con estado y eventos (Priority: P2)

El historial de compras (pestañas por línea, gráfica mensual y lista) y la pantalla de eventos (lista del registro de la
app, con envío por correo) se dibujan desde un único estado y avisan al usuario mediante eventos, con las mismas
presentaciones que hoy.

**Why this priority**: no tienen más acoplamiento que el uso de `LiveData` y la creación del gestor de registros dentro
de la lógica de pantalla, así que son las más baratas; completan la uniformidad.

**Independent Test**: abrir Historial sin compras, con compras de una línea y con compras de varias; abrir Eventos con y
sin registros y enviarlos por correo con y sin app de correo instalada.

**Acceptance Scenarios**:

1. **Given** una línea sin compras, **When** se abre su pestaña, **Then** se muestra el mensaje de "No hay datos" y la
   gráfica queda oculta.
2. **Given** compras de varios meses, **When** se abre la pestaña, **Then** se ve la gráfica por mes y la lista agrupada
   por mes con su total, igual que ahora.
3. **Given** que se confirma una compra pendiente, **When** vuelve el usuario a Historial, **Then** la lista y la gráfica
   incluyen la compra sin reabrir la app.
4. **Given** registros de eventos, **When** se abre Eventos, **Then** aparecen del más reciente al más antiguo.
5. **Given** Eventos abierta, **When** el usuario elige enviar por correo y no hay una app de correo, **Then** se avisa
   con el mensaje que ya existe y no se cierra la pantalla.

---

### User Story 4 - Cierre del paso: pendientes menores (Priority: P3)

Quedan limpios los pendientes menores que se anotaron al cerrar las fases 2 y 3, para empezar el rediseño sin deuda
conocida.

**Why this priority**: son mejoras pequeñas y sin riesgo que no bloquean la conversión; se hacen al final.

**Independent Test**: revisar los textos y ejecutar el análisis estático.

**Acceptance Scenarios**:

1. **Given** la lista de permisos, **When** se mira el permiso de sobreponerse a otras apps, **Then** su nombre y su
   descripción dicen para qué se usa hoy, sin mencionar el cortafuegos como algo que lo necesite.
2. **Given** las notificaciones y las estadísticas, **When** se revisan los textos de LTE y de mensajería,
   **Then** ningún texto ofrece planes LTE ni de mensajería que ya no se venden; las categorías siguen apareciendo
   solo si el usuario tiene datos de ese tipo, y los datos antiguos no se pierden.
3. **Given** el análisis estático, **When** se ejecuta, **Then** no quedan avisos de recursos sin uso ni de textos
   escritos directamente al asignar texto a una vista.

---

### Edge Cases

- Se pide un permiso y el sistema mata la app en segundo plano: al volver la hoja no se queda abierta sin saber qué
  hacer ni se pierde la acción.
- Dos hojas de permisos seguidas (por ejemplo, llamadas y luego accesibilidad): cada una entrega su resultado a su
  pantalla, sin mezclarlos.
- La pantalla de origen deja de existir mientras la hoja está abierta (se vuelve atrás o se cambia de pestaña): el
  resultado se descarta sin errores.
- Permiso que no existe en esa versión de Android: no se lista ni se puede pedir.
- Setup sin ningún paso pendiente: se pasa directamente al arranque normal.
- Historial de una línea eliminada o sin compras; compras antiguas de planes que ya no se ofrecen siguen mostrándose con
  su nombre.
- Eventos con un registro muy grande: la pantalla sigue respondiendo.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Ninguna de las lógicas de pantalla de Permisos (lista, hoja de un permiso y hoja de accesibilidad),
  Historial, Eventos y Setup MUST recibir ni guardar referencias a vistas, fragmentos, actividades ni objetos de respuesta
  de quien las abre.
- **FR-002**: La hoja de un permiso y la de accesibilidad MUST entregar su resultado a la pantalla de origen sin que esta
  les pase un objeto de respuesta, y el resultado MUST llegar una sola vez aunque se gire el teléfono o se recree la
  pantalla de origen.
- **FR-003**: Las pantallas que hoy abren una hoja con un objeto de respuesta (Resumen, Controles, Paquetes y la
  configuración de líneas) MUST pasar al mecanismo de FR-002, conservando su comportamiento.
- **FR-004**: Cada pantalla MUST dibujar un único estado observable y reaccionar a eventos puntuales (mensajes,
  peticiones de permiso, navegación y cierre), sin `LiveData` mezclados ni suscripciones a varias fuentes.
- **FR-005**: Los fragmentos y actividades de estas pantallas MUST NOT leer gestores ni repositorios a través de la lógica
  de pantalla; lo que necesiten lo recibe en el estado o mediante acciones.
- **FR-006**: Las preferencias que usan estas pantallas (por ejemplo, haber informado de los datos que usa la app) MUST
  leerse y escribirse por el repositorio común de preferencias, sin acceder al almacén de datos desde la lógica de
  pantalla ni depender de `Application`.
- **FR-007**: Los pasos de configuración de Setup MUST comunicar que terminaron u omitieron mediante eventos a la lógica
  de pantalla, no mediante una interfaz que implemente la actividad.
- **FR-008**: Setup MUST conservar el paso actual al girar el teléfono o recrear la pantalla.
- **FR-009**: Eventos MUST obtener el registro de la app a través de un gestor inyectado, no creándolo dentro de la
  lógica de pantalla.
- **FR-010**: Ninguna de estas pantallas MUST bloquear el hilo de la interfaz esperando datos o preferencias.
- **FR-011**: Cada lógica de pantalla MUST tener pruebas unitarias en la JVM con dobles de los gestores que cubran su
  estado inicial y al menos una acción principal; las de las hojas de permisos cubren además conceder, rechazar y
  rechazar de forma permanente.
- **FR-012**: La app MUST verse y comportarse igual que antes para el usuario, salvo los errores confirmados que se
  corrijan y que quedan anotados.
- **FR-013**: El permiso de sobreponerse a otras apps MUST tener un nombre y una descripción que reflejen su uso actual.
- **FR-014**: Los textos de LTE y mensajería de notificaciones y estadísticas MUST dejar de ofrecer planes que ya no se
  venden, conservando las categorías mientras el usuario tenga datos de ese tipo.
- **FR-015**: El análisis estático MUST no reportar avisos de `UnusedResources` ni de `SetTextI18n` en el proyecto.

### Key Entities

- **Estado de la pantalla**: lo único que dibuja cada pantalla; para Permisos, la lista con la acción posible de cada
  fila; para la hoja, el permiso con su texto y la acción que ofrece; para Historial, las pestañas, la gráfica y la lista;
  para Eventos, la lista ordenada; para Setup, el paso actual y los pendientes.
- **Evento de pantalla**: aviso puntual a la interfaz (mensaje, petición de permiso, abrir ajustes, cerrar la hoja,
  seguir con el arranque).
- **Resultado de permiso**: respuesta de una hoja (concedido, rechazado o rechazado de forma permanente) que recibe la
  pantalla que la pidió.
- **Paso de configuración**: cada elemento de Setup (líneas, planes) con su orden y su estado pendiente.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 0 lógicas de pantalla de Permisos, Historial, Eventos y Setup guardan referencias a vistas, fragmentos,
  actividades u objetos de respuesta (comprobable por búsqueda en el código).
- **SC-002**: 0 de esas lógicas dependen de `Application`, del almacén de datos o de `LiveData`; las 10 pantallas de la
  app exponen estado y eventos con el mismo patrón.
- **SC-003**: 100 % de las lógicas de pantalla de estas 4 pantallas tienen prueba de estado inicial y una acción
  principal; el conjunto de pruebas de la app sigue en verde.
- **SC-004**: En cada una de las 4 pantallas, girar el teléfono en cualquier momento no pierde el estado ni el
  resultado de un permiso en curso (probado en teléfono y emulador).
- **SC-005**: El recorrido de primer uso y de Permisos y privacidad en el emulador Android 7 y en el teléfono Android 16
  no muestra diferencias visibles respecto a la versión anterior, salvo los textos corregidos.
- **SC-006**: 0 avisos de `UnusedResources` y `SetTextI18n` en el análisis estático de la versión de lanzamiento.

## Assumptions

- Quien usa esta spec es el mantenedor: lo "visible para el usuario" es que la app se vea y se comporte igual.
- Se mantiene el mismo estilo de arquitectura del paso 1 (estado como flujo observable, eventos puntuales, pruebas con
  MockK y Turbine); no se introducen bibliotecas nuevas.
- Las hojas de permisos siguen siendo hojas inferiores; el cambio es cómo devuelven su resultado, no su aspecto.
- Las lógicas de pantalla se comprueban con pruebas de unidad; el comportamiento de actividades y fragmentos se valida
  en el teléfono y en el emulador, como en el paso 1.
- No se tocan los servicios de fondo ni el dominio salvo lo estrictamente necesario para sacar de las pantallas lo que
  aún tengan (por ejemplo, un gestor de registros inyectable).
- Los tipos de datos antiguos (LTE, Bolsa Mensajería) se siguen mostrando mientras el usuario tenga datos de ese tipo,
  para no perder su historial (decisión de la fase 2); el cambio solo afecta a los textos que ofrecen esas categorías
  en notificaciones y estadísticas.
