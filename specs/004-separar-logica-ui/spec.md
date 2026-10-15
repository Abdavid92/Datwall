# Feature Specification: Separar la lógica de negocio de la UI

**Feature Branch**: `004-separar-logica-ui`

**Created**: 2026-10-14

**Status**: Draft

**Input**: User description: "Fase 4, primer paso: separar la lógica de negocio de la UI, crear las piezas que faltan y limpiar el
código muerto, sin cambiar el aspecto ni el comportamiento visible de la app. Prepara la reescritura de las pantallas (el
segundo paso decidirá entre Jetpack Compose y vistas XML con una pantalla piloto). [...] Objetivo: que cada pantalla
dependa solo de su ViewModel, que cada ViewModel exponga un único estado observable y eventos sin recibir vistas ni
fragmentos, que toda regla de negocio viva en el dominio con pruebas unitarias en la JVM, y que el dominio no dependa de
clases de UI. La app debe verse y comportarse igual para el usuario, salvo la corrección de los errores confirmados."

## Contexto

La fase 4 del roadmap rehace la interfaz, que se ha quedado anticuada. Antes de rehacerla hay que dejar la lógica de
negocio fuera de las pantallas, para que la interfaz nueva (con la tecnología que se elija en el segundo paso) se
construya encima sin tocar reglas de negocio.

Diagnóstico del 2026-10-14:

- **Dominio** (gestores, repositorios, servicios, VPN): casi independiente de la interfaz. Se acopla en cinco puntos:
  1. los pasos de configuración se identifican por la pantalla que los muestra;
  2. los permisos y la excepción de permiso que falta saben pedir el permiso desde una pantalla;
  3. el dominio produce textos visibles: nombres de permisos y de tipos de datos, y mensajes de error de USSD y de
     compra;
  4. el arranque decide a qué pantalla ir abriendo pantallas concretas;
  5. servicios y utilidades abren pantallas concretas desde notificaciones.

  Además, la línea guarda su icono como imagen y hay utilidades de interfaz mezcladas con utilidades de dominio.
- **Interfaz** (unas 9.400 líneas), de más a menos acoplada: Dashboard, Consumo, Resumen, Aplicaciones, Ajustes,
  Paquetes, Permisos, Historial, Eventos y Setup. Problemas que se repiten:
  - lógica de pantalla que recibe vistas y muestra diálogos;
  - cálculos de negocio en pantallas y listas;
  - respuestas pasadas a la pantalla mediante interfaces que la pantalla implementa;
  - acceso a los datos saltándose los gestores;
  - preferencias leídas desde unos 10 sitios y usadas para pasar filtros entre pantallas;
  - esperas que bloquean el hilo de la interfaz;
  - formato de bytes, porcentajes y fechas repetido, con textos en español escritos en el código.
- **Piezas que faltan**:
  - gestión de apps: filtrar, agrupar y cambiar el acceso;
  - consulta de los datos del usuario: totales, orden y validación al editar;
  - acceso común a las preferencias;
  - formato común de bytes y fechas.
- **Código muerto**: siete diseños de pantalla sin uso, una versión antigua del Dashboard con sus pantallas de controles,
  búsquedas y filtros sin llamadas, dos bibliotecas sin uso e importaciones sobrantes.

## Clarifications

### Session 2026-10-14

- Elección de tecnología de interfaz: se decide en el segundo paso de la fase 4, con una pantalla piloto. Este paso
  sirve para las dos opciones.
- Q: ¿En este paso pasamos las 10 pantallas a "estado único + eventos" (US3), o solo algunas? → A: Solo las 6 más
  acopladas (Dashboard, Consumo, Resumen, Aplicaciones, Ajustes, Paquetes); Permisos, Historial, Eventos y Setup se
  convierten en el segundo paso, al reescribir cada pantalla. Las reglas de negocio de todas las pantallas sí pasan
  al dominio en este paso.
- Q: ¿Las lógicas de pantalla convertidas (las 6 de US3) también llevan pruebas unitarias, o solo el dominio? → A:
  Dominio completo y, en cada una de las 6 lógicas de pantalla, su estado inicial y al menos una acción principal,
  usando dobles de los gestores.

## User Scenarios & Testing *(mandatory)*

En esta fase el usuario principal es quien mantiene la app. El usuario final no debe notar cambios, salvo la corrección
de los errores confirmados.

### User Story 1 - El dominio no depende de la interfaz (Priority: P1)

Quien mantiene la app puede construir una interfaz nueva encima del dominio sin modificarlo:
- **Configuración y permisos**: los pasos de configuración y los permisos se describen con datos, no con pantallas.
- **Textos**: el dominio devuelve códigos y valores; la interfaz decide los textos.
- **Arranque**: indica a qué parte de la app ir, no qué pantalla abrir.
- **Notificaciones**: abren la app mediante un único punto que pertenece a la interfaz.

**Why this priority**: es la condición para rehacer la interfaz en el segundo paso sin reescribir reglas de negocio.

**Independent Test**: revisar que ninguna clase de gestores, repositorios, modelos, servicios de negocio, trabajos en
segundo plano ni el arranque haga referencia a pantallas, componentes visuales ni textos de la interfaz, y que la app
siga funcionando igual.

**Acceptance Scenarios**:

1. **Given** el código del dominio, **When** se busca cualquier referencia a pantallas o componentes visuales,
   **Then** no aparece ninguna, salvo en el punto único de navegación que implementa la interfaz.
2. **Given** un error de USSD o de compra, **When** el dominio lo informa, **Then** entrega un código de error y la
   interfaz muestra el mismo texto que hoy.
3. **Given** el primer arranque, **When** el dominio decide el destino, **Then** se llega a la misma pantalla que hoy en
   cada caso (primer uso, configuración o principal).

---

### User Story 2 - Las reglas de negocio de las pantallas viven en el dominio y tienen pruebas (Priority: P1)

Todo cálculo o regla que hoy está en una pantalla o una lista pasa al dominio y se comprueba con pruebas que no
necesitan dispositivo:
- totales, porcentajes, medias y días restantes;
- unión del consumo con las apps y su agrupación;
- búsqueda, filtro y agrupación de apps;
- conversión de unidades y validación al editar los datos;
- regla de "línea que no puede comprar planes";
- formato de bytes, porcentajes y fechas.

Las piezas que faltan se crean aquí: gestión de apps, consulta de los datos del usuario, acceso común a las
preferencias y formato común.

**Why this priority**: sin esto, cualquier interfaz nueva tendría que volver a escribir esas reglas.

**Independent Test**: ejecutar las pruebas sin dispositivo y comprobar que cubren cada regla movida, con casos
normales, límite y de error.

**Acceptance Scenarios**:

1. **Given** una línea con datos de varios tipos, **When** la pantalla Resumen muestra totales y porcentajes, **Then**
   los valores son los mismos que hoy y salen de una regla del dominio con pruebas.
2. **Given** la lista de apps con un filtro y una búsqueda, **When** el usuario la ordena o filtra, **Then** el
   resultado es el mismo que hoy y lo calcula el dominio.
3. **Given** la edición manual de los datos de una línea, **When** el usuario introduce valores no válidos, **Then** se
   rechazan con las mismas reglas que hoy, aplicadas por el dominio.
4. **Given** cualquier cantidad de bytes o fecha que se muestra, **When** aparece en dos pantallas distintas, **Then**
   se formatea igual en las dos.

---

### User Story 3 - Cada pantalla depende solo de su estado (Priority: P2)

Alcance: Dashboard, Consumo, Resumen, Aplicaciones, Ajustes y Paquetes. Permisos, Historial, Eventos y Setup se
convierten en el segundo paso.

Cada pantalla observa un único estado que le prepara su lógica de pantalla y recibe eventos puntuales: mensajes,
peticiones de permiso y navegación.
- La lógica de pantalla no recibe vistas, pantallas ni gestores de pantallas, ni guarda referencias a ellos.
- Ninguna pantalla espera bloqueando el hilo de la interfaz.
- Las preferencias no se usan para pasar datos entre pantallas.

**Why this priority**: es el contrato sobre el que se construirá la interfaz nueva; cualquier tecnología de interfaz
solo tiene que dibujar ese estado.

**Independent Test**: girar la pantalla o forzar que el sistema recree la app en cada pantalla del alcance y comprobar que conserva
su estado sin repetir acciones ni perder respuestas pendientes (por ejemplo, el resultado de una petición de permiso).

**Acceptance Scenarios**:

1. **Given** cualquier pantalla del alcance, **When** se recrea por un giro o por falta de memoria, **Then** muestra el mismo estado
   y no repite acciones.
2. **Given** una sincronización o una compra en curso, **When** termina, **Then** el resultado llega a la pantalla
   aunque esta se haya recreado mientras tanto.
3. **Given** el Dashboard, **When** el usuario activa la burbuja o el cortafuegos, **Then** el comportamiento es el
   mismo que hoy, sin que la lógica de pantalla manipule controles visuales.
4. **Given** cualquier pantalla al abrirse, **When** se cargan las preferencias, **Then** la interfaz no se congela.

---

### User Story 4 - Errores confirmados corregidos (Priority: P2)

Se comprueban los tres posibles errores del diagnóstico. Cada error confirmado se corrige con una prueba que lo
reproduce primero:
- la gráfica de Resumen pierde el último grupo al agrupar por horas o días;
- en Consumo, la eliminación de apps duplicadas parece desplazada en uno;
- en el Historial, la lista cuenta con unos datos y dibuja con otros.

**Why this priority**: al mover esas reglas al dominio salen a la luz. Corregirlos ahora evita llevarlos a la interfaz
nueva.

**Independent Test**: por cada error, una prueba que falla con el comportamiento actual y pasa tras la corrección; los
descartados se anotan con el motivo.

**Acceptance Scenarios**:

1. **Given** consumo repartido en varias horas, **When** se agrupa para la gráfica, **Then** aparecen todos los grupos,
   incluido el último.
2. **Given** apps repetidas en el consumo, **When** se eliminan duplicados, **Then** no se pierde ni se repite ninguna
   app.
3. **Given** un posible error que no se confirma, **When** se revisa, **Then** queda documentado como descartado y no se
   cambia el comportamiento.

---

### User Story 5 - Sin código muerto (Priority: P3)

Se eliminan los diseños, pantallas, búsquedas, filtros, bibliotecas e importaciones que no se usan, sin cambiar nada
visible.

**Why this priority**: reduce lo que habrá que migrar o revisar en el segundo paso, pero no bloquea las demás historias.

**Independent Test**: tras eliminarlo, la app compila, pasa las pruebas y se ve y funciona igual; la herramienta de
análisis estático no informa de recursos nuevos sin uso.

**Acceptance Scenarios**:

1. **Given** la lista de código muerto del diagnóstico, **When** termina la fase, **Then** ninguno de esos elementos
   existe.
2. **Given** el Dashboard, **When** se abre, **Then** se ve y funciona igual que antes de eliminar la versión antigua.

### Edge Cases

- Pantallas que hoy reciben datos de otras a través de preferencias (periodo y filtro de Consumo, filtro de Resumen,
  filtro de Aplicaciones): deben seguir recordando la elección del usuario entre sesiones, aunque ya no las usen para
  comunicarse.
- La recreación de una pantalla con una petición de permiso abierta: la respuesta no se pierde.
- Notificaciones antiguas, creadas antes de actualizar la app, que apuntan a pantallas: deben seguir abriendo la app.
- Usuarios que actualizan con preferencias guardadas: conservan sus valores (tema, filtros, burbuja, sincronización).
- Textos que hoy salen del dominio: se muestran idénticos tras la separación.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El aspecto y el comportamiento visible de la app MUST mantenerse, salvo la corrección de los errores
  confirmados (US4).
- **FR-002**: El dominio MUST NOT hacer referencia a pantallas, componentes visuales ni textos de la interfaz, excepto:
  - las notificaciones, que construyen su propio contenido;
  - el punto único de navegación, que define el dominio e implementa la interfaz.
- **FR-003**: Los pasos de configuración y los permisos MUST describirse con datos. La interfaz decide qué pantalla los
  muestra y cómo se piden.
- **FR-004**: Los errores del dominio MUST entregarse como códigos. La interfaz MUST traducirlos a los mismos textos que
  hoy.
- **FR-005**: El arranque MUST indicar el destino (primer uso, configuración o principal) sin abrir pantallas concretas.
- **FR-006**: Las notificaciones y los servicios MUST abrir la app a través de un único punto de navegación.
- **FR-007**: Las reglas y cálculos de negocio de las pantallas, listas y lógica de pantalla MUST pasar al dominio, con
  pruebas unitarias que no necesiten dispositivo (casos normales, límite y error).
- **FR-008**: MUST existir:
  - una gestión de apps (filtrar, agrupar, buscar y cambiar el acceso);
  - una consulta de los datos del usuario (totales, porcentajes, orden y validación al editar);
  - un acceso común a las preferencias;
  - un formato común de bytes, porcentajes y fechas.

  Las pantallas MUST usar estas piezas en lugar de los repositorios y las preferencias directamente.
- **FR-009**: Cada pantalla del alcance de US3 (Dashboard, Consumo, Resumen, Aplicaciones, Ajustes, Paquetes) MUST
  observar un único estado y recibir eventos puntuales. Su lógica de pantalla MUST NOT
  recibir ni guardar vistas, pantallas ni gestores de pantallas, ni comunicarse mediante interfaces implementadas por la
  pantalla.
- **FR-009a**: Cada una de las 6 lógicas de pantalla convertidas MUST tener pruebas unitarias sin dispositivo de su
  estado inicial y de al menos una acción principal, usando dobles de los gestores.
- **FR-010**: Ninguna pantalla MUST bloquear el hilo de la interfaz esperando preferencias u otros datos.
- **FR-011**: Las preferencias MUST NOT usarse para pasar datos entre pantallas. Las elecciones del usuario que hoy se
  recuerdan MUST seguir recordándose.
- **FR-012**: Cada posible error del diagnóstico MUST comprobarse. Los confirmados MUST corregirse con una prueba que
  falle antes de la corrección; los descartados MUST documentarse.
- **FR-013**: El código muerto del diagnóstico MUST eliminarse sin cambios visibles.
- **FR-014**: Las preferencias guardadas por versiones anteriores MUST conservarse al actualizar.

### Key Entities

- **Destino de arranque**: a qué parte de la app debe ir el usuario al abrirla (primer uso, configuración pendiente con
  su paso, principal).
- **Paso de configuración**: qué falta configurar (líneas o planes) y si está completo, sin referencia a pantallas.
- **Código de error**: motivo de un fallo del dominio (permiso, servicio no disponible, código USSD fallido, tiempo
  agotado, plan no configurado…) que la interfaz traduce a texto.
- **Estado de pantalla**: los datos que una pantalla necesita para dibujarse.
- **Evento de pantalla**: algo puntual que la pantalla debe hacer una sola vez (mostrar un mensaje, pedir un permiso,
  navegar).
- **Preferencias del usuario**: tema, filtros, periodos, burbuja, sincronización y demás elecciones que se recuerdan.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 0 referencias a pantallas o componentes visuales desde el dominio, fuera de las notificaciones y del punto
  único de navegación.
- **SC-002**: El 100 % de las reglas de negocio movidas desde la interfaz tienen pruebas que se ejecutan sin
  dispositivo.
- **SC-003**: 0 lógicas de pantalla que reciban o guarden vistas, pantallas o gestores de pantallas en las 6 pantallas
  del alcance de US3.
- **SC-003a**: Las 6 lógicas de pantalla convertidas tienen pruebas de su estado inicial y de al menos una acción
  principal.
- **SC-004**: 0 esperas bloqueantes en el hilo de la interfaz.
- **SC-005**: En una revisión pantalla por pantalla en el teléfono y en el emulador de Android 7, todas las pantallas se
  ven y se comportan igual que antes (salvo los errores corregidos), incluido tras girar el dispositivo.
- **SC-006**: 0 elementos de la lista de código muerto presentes al terminar.

## Assumptions

- La tecnología de la interfaz nueva (Compose o vistas XML) se decide en el segundo paso. Este paso no cambia diseños
  ni componentes visuales, salvo lo imprescindible para que las pantallas dibujen el estado.
- Las notificaciones (incluida la vista previa de su estilo en Ajustes) y la burbuja flotante conservan su forma
  actual. Solo se cambia cómo abren la app y de dónde sacan los datos.
- Los modelos de datos siguen pudiendo pasarse entre pantallas como hoy. Quitar esa capacidad y el icono guardado en la
  línea se hará cuando las pantallas nuevas pasen identificadores (segundo paso).
- La gráfica de consumo y la de historial siguen usando la biblioteca actual de gráficas. Solo se separa la preparación
  de sus datos.
- El esquema de la base de datos no cambia.
- Para las pruebas del dominio se pueden añadir herramientas de prueba de corrutinas y dobles de prueba.
