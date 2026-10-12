# Constitución de Datwall

## Principios fundamentales

### I. KISS: mantenerlo simple

- Se implementa la solución más simple que cumpla el requisito. No se añaden capas, abstracciones,
  patrones ni configuraciones "por si acaso".
- Una abstracción nueva (interfaz, clase base, módulo) solo se crea cuando hay al menos dos usos
  reales que la necesitan.
- Se prefieren las APIs de Android y Jetpack a las bibliotecas de terceros cuando cubren la
  necesidad.
- Toda complejidad adicional DEBE justificarse en el plan de la característica, indicando la
  alternativa más simple y por qué no basta.

### II. DRY: no repetirse

- Cada conocimiento del sistema (una regla de negocio, un cálculo, una constante, un texto) DEBE
  vivir en un único lugar.
- La lógica duplicada que se encuentre al tocar código DEBE unificarse si el cambio es razonable
  dentro del alcance de la tarea.
- Los datos de ETECSA (paquetes, precios, vigencias, códigos USSD) se definen en una única fuente
  y el resto de la app los consume desde ahí.
- DRY no justifica acoplar código no relacionado: si dos fragmentos se parecen pero cambian por
  motivos distintos, se mantienen separados (KISS prevalece).

### III. Pruebas unitarias en toda característica nueva (NO NEGOCIABLE)

- Toda característica nueva DEBE incluir pruebas unitarias que cubran su lógica: casos normales,
  casos límite y errores.
- Las correcciones de errores DEBEN incluir una prueba que reproduzca el fallo antes del arreglo.
- La lógica DEBE escribirse de forma que se pueda probar sin dispositivo (en `src/test`): las
  dependencias de Android se aíslan detrás de clases inyectadas con Hilt.
- Una característica no se considera terminada si sus pruebas no pasan.

### IV. Privacidad: todo se procesa en el dispositivo

- La VPN local NUNCA envía tráfico a servidores externos ni almacena su contenido.
- Las estadísticas, la configuración y los datos de ETECSA no salen del dispositivo. No se
  incluyen SDKs de analítica ni de rastreo.
- Las funciones que leen datos sensibles (como los SMS) son opcionales y las activa el usuario.

### V. Android actual y políticas de Google Play

- `targetSdk` se mantiene al nivel que exige Google Play, atendiendo los cambios de comportamiento
  de cada versión.
- Aunque la app se distribuye en Apklis, DEBE cumplir las políticas de Google Play: cada permiso
  tiene un uso justificado, se pide en tiempo de ejecución y en contexto, y la app sigue
  funcionando si el usuario lo rechaza.
- Lo que solo sea válido fuera de Play (por ejemplo, la autoactualización) se aísla en una
  variante de build para Apklis.

## Estándares técnicos

- Kotlin para todo el código nuevo; Hilt para la inyección de dependencias; Room para la
  persistencia; ViewModel en la capa de presentación.
- `vpncore` contiene solo la VPN y el filtrado de tráfico, sin dependencias de UI ni de ETECSA.
- Las dependencias provienen solo de `google()`, `mavenCentral()` y JitPack.
- Los textos visibles van en recursos de strings, en español e inglés.

## Flujo de desarrollo

- Cada fase del roadmap se trabaja en su propia rama y se integra mediante pull request.
- Antes de integrar: las pruebas unitarias pasan y `assembleDebug` y `assembleRelease` compilan.
- Los cambios que afecten a la VPN o a los servicios en primer plano se verifican además en un
  dispositivo o emulador.
- Commits y documentación en español.

## Gobernanza

- Esta constitución prevalece sobre cualquier otra práctica del proyecto. Cada plan de Spec Kit
  DEBE verificar el cumplimiento de los cinco principios.
- Las excepciones se documentan en el plan de la característica con su justificación.
- Las enmiendas se hacen con `/speckit-constitution` y siguen versionado semántico: MAJOR para
  eliminar o redefinir principios, MINOR para añadir principios o secciones, PATCH para
  aclaraciones.

**Versión**: 1.0.0 | **Ratificada**: 2026-10-12 | **Última enmienda**: 2026-10-12
