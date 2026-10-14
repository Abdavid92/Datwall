# Feature Specification: Permisos conformes con Google Play

**Feature Branch**: `003-permisos-play`

**Created**: 2026-10-14

**Status**: Draft

**Input**: User description: "Fase 3 del roadmap: que el sistema de permisos cumpla las políticas de Google Play, aunque
la app siga publicándose solo en Apklis. [...] SMS: se eliminan RECEIVE_SMS y READ_SMS; la confirmación de compra se lee
con la API SMS User Consent [...]. Accesibilidad: se mantiene solo para leer respuestas USSD en Android 6–7 o en el modo
legacy, con explicación y consentimiento previos. QUERY_ALL_PACKAGES: se mantiene y se justifica. [...]"

## Situación actual

Revisada en el código el 2026-10-14:

- Al abrir la app por primera vez se bloquea su uso hasta conceder cuatro permisos obligatorios: acceso de uso,
  llamadas y estado del teléfono, SMS e ignorar la optimización de batería. La única explicación es una línea por
  permiso.
- Los SMS se usan para dos cosas: detectar compras y bonos al recibir el SMS de ETECSA, y rellenar el historial leyendo
  toda la bandeja de entrada.
- La exención de batería se pide con el diálogo directo del sistema.
- El servicio de accesibilidad solo hace falta para leer la respuesta USSD en Android 6–7 o en el modo USSD "legacy", y
  se pide sin una explicación previa.
- Se declaran permisos que no se usan: estado privilegiado del teléfono, instalación de paquetes y cambio y lectura del
  estado de la wifi.
- La app tiene un escáner QR (con la cámara) para recargar saldo con las tarjetas de recarga de ETECSA, que ya no
  existen.
- Quedan restos sin uso que afectan a la privacidad: dominios con tráfico sin cifrar permitido, bibliotecas de red y los
  textos de un licenciamiento con Transfermóvil que ya no existe.

## Permisos y su uso

| Permiso o acceso | Función que lo usa | Cuándo se pide |
|---|---|---|
| Acceso de uso | Estadísticas de consumo por app; cortafuegos dinámico | Al inicio, con explicación previa (función principal) |
| Llamadas y estado del teléfono | Consultar saldo y comprar por USSD; reconocer las SIM | Al configurar las SIM, sincronizar o comprar |
| Lista de apps instaladas | Cortafuegos por app y estadísticas por app | Se declara; se explica en la explicación inicial |
| Superposición | Burbuja flotante | Al activar la burbuja |
| Notificaciones (Android 13+) | Notificación de consumo y avisos | En el primer uso, después del acceso de uso, como paso opcional |
| VPN | Cortafuegos | Al activar el cortafuegos |
| Accesibilidad | Leer la respuesta USSD en Android 6–7 o en modo legacy | Al sincronizar en esos casos, tras una explicación y consentimiento |
| Optimización de batería | Mantener la medición activa | Recomendación opcional que abre los ajustes |
| SMS | — (se elimina) | Nunca |
| Cámara | — (se elimina con el escáner QR) | Nunca |

## Clarifications

### Session 2026-10-14

- Q: ¿Qué hacemos con una compra hecha desde la app que no se confirma por SMS? → A: Sale en el historial marcada
  "sin confirmar"; si llega el SMS compartido, pasa a confirmada.
- Q: ¿La sincronización debe añadir al historial los planes comprados fuera de la app, deduciéndolos del saldo? → A:
  No; las compras hechas fuera de la app solo se reflejan en el saldo.
- Q: ¿Cuándo pedimos el permiso de notificaciones (Android 13+)? → A: En el primer uso, después del acceso de uso,
  como paso opcional con explicación.
- Nota del usuario: las tarjetas de recarga de ETECSA ya no existen; se elimina el escáner QR de recarga y con él la
  cámara.
- Q: ¿Debe haber en Ajustes una sección "Permisos y privacidad"? → A: Sí; muestra la explicación del uso de datos y
  cada permiso con su uso, su estado y un botón para concederlo, además de la recomendación de batería.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Primer uso sin bloqueos (Priority: P1)

Un usuario nuevo instala la app. Ve una explicación clara de qué datos usa la app y para qué: el consumo por
aplicación, la lista de apps instaladas y que todo se procesa en el teléfono. Después se le pide el acceso de uso y,
en Android 13 o superior, las notificaciones como paso opcional. Conceda o no, puede entrar a la app. Ninguna otra
autorización se pide en este momento.

**Why this priority**: hoy la app no se puede usar sin conceder cuatro permisos, entre ellos los SMS. Es el
incumplimiento más grave y lo primero que ve cualquier usuario.

**Independent Test**: instalar la app limpia, recorrer el primer uso y comprobar que solo se piden el acceso de uso
y, en Android 13+, las notificaciones, tras la explicación, y que al rechazarlos se llega igualmente a la pantalla
principal.

**Acceptance Scenarios**:

1. **Given** una instalación limpia, **When** el usuario abre la app, **Then** ve la explicación del uso de datos
   antes de cualquier petición de permiso.
2. **Given** la explicación aceptada, **When** se piden permisos, **Then** solo se piden el acceso de uso y, en
   Android 13+, las notificaciones (que se pueden omitir); no se piden llamadas, SMS, superposición, VPN ni la
   exención de batería.
3. **Given** el acceso de uso rechazado, **When** el usuario continúa, **Then** entra a la app; las pantallas de
   estadísticas explican por qué necesitan el acceso de uso y ofrecen concederlo.
4. **Given** el usuario rechaza la explicación inicial, **When** intenta continuar, **Then** la app no pide ningún
   permiso y le indica que puede revisarla más tarde en Ajustes → "Permisos y privacidad".

---

### User Story 2 - Cada permiso se pide al usar su función (Priority: P1)

El usuario usa una función que necesita un permiso que aún no ha concedido. La app le explica en una frase por qué
lo necesita y luego se lo pide. Si lo rechaza, la función no se ejecuta y la app sigue funcionando; si lo ha rechazado
de forma permanente, la app le indica cómo concederlo desde los ajustes.

**Why this priority**: es el requisito central de Play (petición en contexto y app usable sin el permiso), y
reemplaza el bloqueo inicial de la historia 1.

**Independent Test**: con todos los permisos denegados, usar cada función de la tabla "Permisos y su uso" y
comprobar que se explica, se pide el permiso y, al rechazarlo, no hay bloqueos ni bucles.

**Acceptance Scenarios**:

1. **Given** el permiso de llamadas sin conceder, **When** el usuario pulsa Sincronizar o Comprar, **Then** la app
   explica que necesita marcar un código USSD y pide el permiso; si se concede, la acción continúa.
2. **Given** la superposición sin conceder, **When** el usuario activa la burbuja, **Then** se explica y se abre el
   ajuste correspondiente; si no se concede, la burbuja queda desactivada.
3. **Given** un permiso rechazado de forma permanente, **When** el usuario vuelve a usar su función, **Then** la app
   explica cómo concederlo y ofrece abrir los ajustes de la app, sin volver a mostrar el diálogo del sistema.
4. **Given** la sincronización automática activada y el permiso de llamadas sin conceder, **When** llega la hora de
   sincronizar en segundo plano, **Then** no se sincroniza ni se muestra un error repetido.
5. **Given** la app instalada, **When** el usuario abre la recomendación de batería, **Then** se abre la lista de
   optimización de batería del sistema; la app nunca muestra el diálogo directo de exención.
6. **Given** varios permisos sin conceder, **When** el usuario abre Ajustes → "Permisos y privacidad", **Then** ve
   cada permiso con su uso y su estado, y puede concederlo desde ahí.

---

### User Story 3 - Confirmar la compra sin permisos de SMS (Priority: P2)

El usuario compra un plan desde la app. Durante unos minutos la app espera el SMS de ETECSA; cuando llega, el sistema
le pregunta si quiere compartir ese único mensaje con la app. Si acepta, la compra queda confirmada con el plan del
SMS. Si no acepta, o el teléfono no tiene Google Play Services, la compra queda en el historial marcada "sin confirmar" y el
saldo se actualiza al sincronizar por USSD.

**Why this priority**: los permisos de SMS son el motivo principal de rechazo en Play; esta historia los elimina sin
perder la confirmación de compra donde es posible.

**Independent Test**: en un teléfono con Google Play Services, comprar un plan y aceptar el diálogo de compartir el
SMS; en uno sin Google Play Services (o el emulador sin ellos), comprar y comprobar que la compra aparece en el historial
"sin confirmar" y sin errores.

**Acceptance Scenarios**:

1. **Given** un teléfono con Google Play Services, **When** el usuario compra un plan y llega el SMS de ETECSA,
   **Then** el sistema pide permiso para compartir ese mensaje; al aceptarlo la compra se confirma.
2. **Given** el diálogo de compartir rechazado, **When** el usuario sincroniza después, **Then** la compra aparece en
   el historial marcada "sin confirmar" y el saldo se actualiza por USSD.
3. **Given** un teléfono sin Google Play Services, **When** el usuario compra un plan, **Then** la compra aparece en
   el historial marcada "sin confirmar" y la app no muestra errores por la falta de esos servicios.
4. **Given** la app actualizada, **When** el usuario revisa los permisos de la app en el sistema, **Then** no aparece
   ningún permiso de SMS.
5. **Given** compras registradas antes de la actualización a partir de SMS, **When** el usuario abre el historial,
   **Then** siguen ahí.

---

### User Story 4 - Accesibilidad solo cuando hace falta y con consentimiento (Priority: P2)

En Android 6–7, o con el modo USSD "legacy" activado, la app necesita el servicio de accesibilidad para leer la
respuesta del operador. Antes de enviar al usuario a los ajustes de accesibilidad, la app muestra una pantalla que
explica qué lee el servicio (solo el texto de la respuesta USSD del marcador), para qué, y que no se usa para nada
más; el usuario debe aceptar expresamente.

**Why this priority**: Play exige una divulgación destacada y consentimiento para usar accesibilidad en una app que no
es de accesibilidad; afecta a pocos usuarios (Android 6–7 o modo legacy).

**Independent Test**: en el emulador Android 7, sincronizar con el servicio apagado y comprobar que aparece la
explicación con aceptar y rechazar, y que al rechazar no se abre ningún ajuste.

**Acceptance Scenarios**:

1. **Given** Android 7 y el servicio apagado, **When** el usuario sincroniza, **Then** ve la explicación con las
   opciones aceptar y rechazar antes de cualquier ajuste del sistema.
2. **Given** la explicación rechazada, **When** termina el flujo, **Then** no se abre ningún ajuste y la
   sincronización se cancela con un mensaje claro.
3. **Given** Android 8 o superior en modo moderno, **When** el usuario usa cualquier función, **Then** nunca se le
   pide el servicio de accesibilidad.
4. **Given** los ajustes de accesibilidad del sistema, **When** el usuario abre el servicio de la app, **Then** su
   descripción explica el uso real, sin mencionar licencias.

---

### User Story 5 - Sin permisos ni restos innecesarios (Priority: P3)

La app declara solo los permisos que usa y no conserva configuraciones o bibliotecas de red sin uso. Quien revise la
app (Play, Apklis o un usuario) encuentra una justificación para cada permiso restringido.

**Why this priority**: no cambia lo que ve el usuario, pero reduce la superficie de riesgo y facilita una revisión.

**Independent Test**: revisar la lista de permisos de la app instalada y la documentación de justificaciones.

**Acceptance Scenarios**:

1. **Given** la app instalada, **When** se listan sus permisos, **Then** no aparecen el estado privilegiado del
   teléfono, la instalación de paquetes, el cambio o lectura del estado de la wifi, los SMS ni la cámara.
2. **Given** la app, **When** intenta conectar con un dominio sin cifrar, **Then** la conexión no se permite.
3. **Given** la documentación de la fase, **When** se busca la justificación de la lista de apps instaladas, del
   acceso de uso, de la accesibilidad y de los servicios en primer plano, **Then** cada una tiene su texto.

### Edge Cases

- Un usuario que actualiza tenía concedidos todos los permisos actuales: no se le vuelve a mostrar el primer uso; los
  permisos de SMS desaparecen y nada falla por ello.
- El usuario retira el acceso de uso desde los ajustes con la app en marcha: las estadísticas y el cortafuegos
  dinámico se detienen sin cierres inesperados y explican cómo volver a concederlo.
- El usuario retira el permiso de llamadas con la sincronización automática activa: la sincronización en segundo
  plano se omite sin notificaciones repetidas.
- Llegan varios SMS de ETECSA en la ventana de espera (por ejemplo, una promoción y la compra): solo la compra
  reconocida confirma un plan; los demás no generan registros.
- No llega ningún SMS en la ventana de espera: la compra queda "sin confirmar" sin avisos.
- El usuario concede el acceso de uso más tarde desde los ajustes: al volver a la app, las estadísticas funcionan sin
  reiniciarla.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La app MUST permitir usarse sin conceder ningún permiso; ninguna pantalla bloquea el acceso a las demás
  por un permiso denegado.
- **FR-002**: En el primer uso, la app MUST mostrar una explicación destacada del uso de datos (consumo por app, lista
  de apps instaladas, procesamiento en el teléfono) antes de pedir cualquier permiso, y pedir en ese momento solo el
  acceso de uso y, en Android 13+, las notificaciones como paso que se puede omitir.
- **FR-003**: Cada permiso de la tabla "Permisos y su uso" MUST pedirse en el momento indicado, precedido de una
  explicación breve de para qué se usa.
- **FR-004**: Si un permiso se rechaza, la función que lo necesita MUST no ejecutarse y MUST indicar cómo concederlo;
  si se rechazó de forma permanente, MUST ofrecer abrir los ajustes de la app en lugar de repetir la petición.
- **FR-005**: Las tareas en segundo plano que necesitan un permiso no concedido MUST omitirse sin notificar
  repetidamente.
- **FR-006**: La app MUST NOT declarar permisos de SMS ni leer la bandeja de entrada.
- **FR-007**: Tras una compra hecha desde la app, MUST esperar durante 5 minutos el SMS de ETECSA mediante el
  consentimiento por mensaje del sistema; un SMS compartido que contenga una compra reconocida MUST confirmarla.
- **FR-008**: Sin Google Play Services, o si el usuario no comparte el SMS, la compra MUST quedar registrada como
  "sin confirmar" y la app MUST NOT mostrar errores por ello.
- **FR-008a**: El historial MUST mostrar las compras sin confirmar, marcadas como tales; cuando llega el SMS compartido
  de esa compra, MUST pasar a confirmada.
- **FR-009**: El servicio de accesibilidad MUST ofrecerse solo en Android 6–7 o con el modo USSD legacy activo, tras
  una pantalla de explicación con aceptación expresa; MUST leer solo la respuesta USSD del marcador.
- **FR-010**: La descripción del servicio de accesibilidad MUST describir su uso real.
- **FR-011**: La exención de optimización de batería MUST ser una recomendación opcional que abre la lista del sistema;
  la app MUST NOT usar el diálogo directo de exención.
- **FR-011a**: Ajustes MUST tener una sección "Permisos y privacidad" con la explicación del uso de datos y, para cada
  permiso de la tabla, su uso, su estado (concedido o no) y una acción para concederlo; incluye la recomendación de
  batería.
- **FR-012**: La app MUST NOT declarar el estado privilegiado del teléfono, la instalación de paquetes ni el cambio o
  lectura del estado de la wifi.
- **FR-012a**: La app MUST NOT incluir el escáner QR de recarga ni declarar el permiso de cámara.
- **FR-013**: La app MUST NOT permitir tráfico sin cifrar ni incluir bibliotecas de red o textos de funciones que no
  existen (licencia con Transfermóvil).
- **FR-014**: Las únicas conexiones de red MUST seguir siendo la comprobación de actualizaciones en Apklis y el envío
  de errores por correo que inicia el usuario.
- **FR-015**: Cada permiso restringido (lista de apps instaladas, acceso de uso, accesibilidad, servicios en primer
  plano de uso especial, VPN) MUST tener su justificación documentada en la fase.

### Key Entities

- **Permiso**: autorización del sistema que necesita una función; tiene una explicación, el momento en que se pide y
  el estado concedido, rechazado o rechazado de forma permanente.
- **Compra sin confirmar**: compra hecha desde la app que aún no tiene SMS compartido; se muestra en el historial con
  esa marca y pasa a confirmada si llega el SMS.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un usuario nuevo llega a la pantalla principal pidiéndosele como máximo dos permisos (acceso de uso y,
  en Android 13+, notificaciones), aunque los rechace.
- **SC-002**: Con todos los permisos denegados, el 100 % de las pantallas se abren y ninguna función provoca un cierre
  inesperado o un bucle de peticiones.
- **SC-003**: La app instalada declara 0 permisos de SMS, 0 de cámara y 0 permisos sin uso de los listados en FR-012.
- **SC-004**: El 100 % de las peticiones de permiso van precedidas de una explicación visible.
- **SC-005**: En un teléfono con Google Play Services, una compra hecha desde la app se confirma con un solo toque del
  usuario en el diálogo de compartir el SMS.

## Assumptions

- La app se sigue distribuyendo en Apklis; cumplir Play es un requisito de calidad, no una publicación en Play.
- Muchos teléfonos en Cuba no tienen Google Play Services; la confirmación por SMS es una mejora donde existan, y la
  sincronización por USSD sigue siendo la vía principal.
- Las compras de planes hechas fuera de la app ya no se detectan por SMS: la sincronización periódica automática
  (cada 15 minutos en Android 8 o superior, con el permiso de llamadas concedido) las refleja en el saldo sin que el
  usuario haga nada. No se añaden al historial de compras ni se deducen del saldo, porque el saldo no indica qué plan
  se compró (si había un plan activo, los datos se suman).
- Los servicios en primer plano de medición, burbuja y cortafuegos conservan el tipo de uso especial con su
  justificación; cambiar su arquitectura queda fuera de esta fase.
- El rediseño visual de las pantallas de permisos queda para la fase 4; aquí se cambia el flujo y los textos.
