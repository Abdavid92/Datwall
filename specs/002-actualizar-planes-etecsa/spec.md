# Feature Specification: Actualizar los planes de ETECSA

**Feature Branch**: `002-actualizar-planes-etecsa`

**Created**: 2026-10-13

**Status**: Draft

**Input**: User description: "Actualizar los planes de ETECSA que ofrece la app (fase 2 del roadmap). La oferta cambió: ya no existen los paquetes solo LTE, ni la Bolsa Diaria LTE, ni los planes combinados Básico/Medio/Extra, ni la Bolsa de Mensajería. Solo se ofrecerán los planes que se compran por USSD (*133#) [...]. Los usuarios que ya tengan registrados paquetes antiguos deben conservar su historial. No se conoce el texto del SMS de confirmación de compra [...]. Además, corregir que la compra y consulta por USSD ignoran la SIM seleccionada en teléfonos con dos SIM."

## Oferta vigente

Verificada en etecsa.cu y en el menú `*133#` el 2026-10-13. Todos los planes valen para todas las redes;
los de 35 días suman recursos y extienden la vigencia si se compra otro estando activo.

| Plan | Precio | Datos | Minutos / SMS | Vigencia | Código de compra |
|---|---|---|---|---|---|
| Plan Diario | 25 CUP | 200 MB | — | 24 horas | `*133*1*3#` |
| Plan 4.5 GB | 240 CUP | 4.5 GB + 300 MB .cu | — | 35 días | `*133*1*4*1#` |
| Plan 120 | 120 CUP | 2 GB + 300 MB .cu | 15 min / 20 SMS | 35 días | `*133*1*4*2#` |
| Plan 240 | 240 CUP | 4 GB + 300 MB .cu | 35 min / 40 SMS | 35 días | `*133*1*4*3#` |
| Plan 360 | 360 CUP | 6 GB + 300 MB .cu | 60 min / 70 SMS | 35 días | `*133*1*4*4#` |

Quedan obsoletos: Bolsa Diaria LTE, Plan Básico, Plan Medio, Plan Extra (combinados de 110/250/500 CUP),
Paquete 1 GB LTE, Paquete 2.5 GB LTE, Paquete 16 GB y Bolsa Mensajería.

## Ejemplos reales de mensajes del operador

Aportados por el usuario; las cifras personales están ocultas.

**SMS de confirmación de compra — Plan 120 CUP** (2026-10-13):

```text
ETECSA Plan Nacional: Se ha comprado Plan 2GB+15MIN+20SMS valido por 35 dias, se activa con el primer uso. Se desconto 120.00 CUP. Saldo actual ****.** CUP
```

**SMS de confirmación de compra — Plan 4.5 GB, Plan 240 y Plan 360** (2026-10-13):

```text
ETECSA Plan Nacional: Se ha comprado Plan 4.5GB valido por 35 dias, se activa con el primer uso. Se desconto 240.00 CUP. Saldo actual ****.** CUP
ETECSA Plan Nacional: Se ha comprado Plan 4GB+35MIN+40SMS valido por 35 dias, se activa con el primer uso. Se desconto 240.00 CUP. Saldo actual ****.** CUP
ETECSA Plan Nacional: Se ha comprado Plan 6GB+60MIN+70SMS valido por 35 dias, se activa con el primer uso. Se desconto 360.00 CUP. Saldo actual ****.** CUP
```

**Respuesta de `*222*328#` sin plan activo** (SIM 2, 2026-10-13):

```text
Tarifa: No activa. Ud debe adquirir una oferta. Para una nueva compra marque *133#
```

**Respuesta de `*222*328#` con datos disponibles** (2026-10-13):

```text
Tarifa: No activa. Datos: 130.08 MB validos 24 dias.
```

Los datos aparecen tras `Datos:` (antes era `Paquetes:`), y la vigencia se indica en días restantes
("validos 24 dias"), no como fecha.

**Respuesta de `*222*266#` (bonos activos)** (2026-10-13):

```text
Datos.cu 273 MB vence 06-11-26.
```

Muestra los bonos activos; aquí, los datos nacionales (.cu) con fecha de vencimiento en formato
`dd-MM-yy`. Sin bono de promoción activo, ese bloque no aparece.

Pendientes: SMS de compra del Plan Diario y una respuesta de `*222*266#` con un bono de promoción activo
(si alguien lo tiene).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver y comprar los planes actuales (Priority: P1)

El usuario abre la pantalla de compra y ve exactamente los cinco planes vigentes, con su precio, datos,
minutos, SMS y vigencia correctos. Al elegir uno, la app marca el código USSD que corresponde a ese plan,
y el usuario confirma la compra en el diálogo del operador.

**Why this priority**: hoy la app muestra planes que ya no existen y marca códigos que no corresponden;
el usuario podría comprar un plan distinto del que eligió.

**Independent Test**: abrir la pantalla de compra y comparar los planes con la tabla de oferta vigente;
elegir cada plan y comprobar que el código marcado es el de la tabla (sin confirmar la compra).

**Acceptance Scenarios**:

1. **Given** la app actualizada, **When** el usuario abre la pantalla de compra, **Then** ve los cinco
   planes vigentes y ninguno de los obsoletos.
2. **Given** la pantalla de compra, **When** el usuario elige el Plan 240 (4 GB), **Then** la app marca
   `*133*1*4*3#` y no otro código.
3. **Given** la pantalla de compra, **When** el usuario elige el Plan Diario, **Then** la app marca
   `*133*1*3#`.

---

### User Story 2 - La compra y la consulta usan la SIM elegida (Priority: P1)

En un teléfono con dos SIM, el usuario elige con qué línea comprar o consultar, y la app usa esa línea,
no la que el sistema tenga por defecto.

**Why this priority**: con dos SIM la app puede comprar un plan en la línea equivocada, con el dinero de
otra línea; es un error con consecuencias económicas.

**Independent Test**: en un teléfono con dos SIM y las llamadas en "preguntar siempre", consultar el saldo
eligiendo la SIM 2 y comprobar que la consulta sale por la SIM 2.

**Acceptance Scenarios**:

1. **Given** dos SIM y la SIM 2 seleccionada en la app, **When** el usuario sincroniza o consulta el saldo,
   **Then** la consulta se hace por la SIM 2.
2. **Given** dos SIM y la SIM 2 seleccionada, **When** el usuario compra un plan, **Then** el código se
   marca por la SIM 2.
3. **Given** un teléfono con una sola SIM, **When** el usuario compra o consulta, **Then** todo funciona
   como hasta ahora.

---

### User Story 3 - Conservar el historial de paquetes antiguos (Priority: P2)

Un usuario que actualiza la app y tenía registradas compras de paquetes que ya no existen sigue viendo
ese historial; esos paquetes ya no se ofrecen para comprar.

**Why this priority**: no se deben perder datos del usuario, pero no bloquea comprar los planes nuevos.

**Independent Test**: instalar la versión anterior, registrar una compra de un paquete antiguo, actualizar
y comprobar que el historial la sigue mostrando y que el paquete no aparece en la pantalla de compra.

**Acceptance Scenarios**:

1. **Given** una compra registrada del Plan Medio, **When** el usuario actualiza la app, **Then** el
   historial sigue mostrando esa compra con su nombre.
2. **Given** la app actualizada, **When** el usuario abre la pantalla de compra, **Then** el Plan Medio
   no aparece.

---

### User Story 4 - Saldo y compras detectados con la oferta nueva (Priority: P3)

Tras sincronizar, la app muestra los datos restantes (todas las redes, .cu y bonos) según el formato que
devuelve hoy la consulta de saldo, y detecta las compras de planes nuevos por el SMS de confirmación.

**Why this priority**: depende de formatos de respuesta del operador que no se han podido observar con un
plan activo (ver FR-010).

**Independent Test**: con un plan activo, sincronizar y comparar los datos mostrados con la respuesta del
operador; comprar un plan y comprobar que la app registra la compra.

**Acceptance Scenarios**:

1. **Given** una línea con un plan de 35 días activo, **When** el usuario sincroniza, **Then** la app
   muestra los datos restantes y la fecha de vencimiento que indica el operador.
2. **Given** una línea sin plan activo, **When** el usuario sincroniza, **Then** la app muestra que no hay
   datos disponibles, sin error.

---

### Edge Cases

- El usuario compra un plan de 35 días con otro activo: los recursos se suman y la vigencia se extiende;
  la app no debe mostrar dos planes separados con vigencias distintas como si fueran independientes.
- La línea no tiene ningún plan activo: la consulta de saldo responde "Tarifa: No activa..." y la app debe
  mostrar cero datos, no un error.
- ETECSA cambia de nuevo el orden del menú `*133#`: la app compraría el plan equivocado; debe ser posible
  corregir la oferta cambiando los datos de los planes en un solo lugar.
- El teléfono tiene una sola SIM o la SIM elegida ya no está insertada.
- El operador responde con error o no responde (mala cobertura): mensaje claro, sin cierre.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La pantalla de compra DEBE mostrar exactamente los cinco planes de la tabla de oferta
  vigente, con precio, datos para todas las redes, datos .cu, minutos, SMS y vigencia.
- **FR-002**: Al comprar, la app DEBE marcar el código USSD de la tabla para el plan elegido.
- **FR-003**: Los paquetes obsoletos NO DEBEN ofrecerse para comprar.
- **FR-004**: Las compras registradas de paquetes obsoletos DEBEN conservarse y seguir visibles en el
  historial tras actualizar.
- **FR-005**: La compra y la consulta de saldo por USSD DEBEN hacerse por la SIM que el usuario eligió en
  la app, también cuando el sistema no tiene una SIM predeterminada para llamadas.
- **FR-006**: La configuración por SIM DEBE reducirse a indicar si la línea puede comprar planes (sí/no),
  sustituyendo la elección entre planes combinados y paquetes LTE. Las líneas que no pueden comprar planes
  (por ejemplo, las empresariales, que solo admiten la tarifa por consumo) no muestran planes para comprar.
- **FR-007**: La sincronización DEBE mostrar cero datos, sin error, cuando la línea no tiene ningún plan
  activo ("Tarifa: No activa").
- **FR-008**: La oferta (nombres, precios, recursos, vigencia y códigos) DEBE definirse en un único lugar
  de la app, de forma que un cambio futuro de ETECSA se corrija editando solo ese lugar.
- **FR-009**: Las descripciones de los planes DEBEN reflejar la vigencia de 35 días (24 horas para el
  Plan Diario) y que los recursos se suman al comprar otro plan estando uno activo.
- **FR-010**: La lectura del saldo y la detección de compras por SMS DEBEN adaptarse a la oferta nueva a
  partir de ejemplos reales de la respuesta de saldo con un plan activo y de los SMS de confirmación de
  compra, aportados por el usuario (obtenidos de otra persona que tiene planes). Cada ejemplo DEBE quedar
  cubierto por una prueba unitaria.
- **FR-011**: Toda lógica nueva o modificada (catálogo de planes, construcción del código USSD, lectura
  del saldo, detección por SMS) DEBE tener pruebas unitarias.

### Key Entities

- **Plan de datos**: nombre, precio, datos para todas las redes, datos .cu, minutos, SMS, vigencia,
  código de compra, si está obsoleto.
- **Compra registrada**: plan, línea, fecha; debe seguir siendo válida aunque el plan pase a obsoleto.
- **Línea (SIM)**: identifica por qué línea se compra y se consulta.
- **Saldo de datos**: datos restantes por tipo (todas las redes, .cu, bonos) y fecha de vencimiento.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de los planes mostrados coincide con la oferta vigente (precio, recursos,
  vigencia), y no se muestra ningún plan obsoleto.
- **SC-002**: En el 100 % de las pruebas, el código marcado al elegir un plan es el que corresponde a ese
  plan en el menú `*133#`.
- **SC-003**: En un teléfono con dos SIM, el 100 % de las compras y consultas se hacen por la SIM elegida
  en la app.
- **SC-004**: El 100 % del historial de compras anterior a la actualización sigue visible.
- **SC-005**: Ante una línea sin plan activo, la sincronización termina sin mensaje de error.

## Assumptions

- Solo se ofrecen los planes que se compran por USSD; Plan Adicional, Planes Extra, Plan Sectorial y los
  planes de SMS y voz quedan fuera de esta fase (decisión del usuario, 2026-10-13).
- La compra la confirma el usuario en el diálogo del operador; la app solo marca el código.
- La SIM 1 del teléfono de pruebas es una línea empresarial y no se usa para pruebas.
- Las líneas empresariales solo admiten la tarifa por consumo y no pueden comprar planes.
- El catálogo se versiona como hasta ahora para que los usuarios existentes reciban la oferta nueva al
  actualizar.
