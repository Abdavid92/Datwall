# 📡 Datwall  
**Firewall + Monitor de Datos para Android (VPN Local)**  
Controla el consumo de datos, bloquea apps y gestiona tu paquete de ETECSA con precisión y privacidad.

---

## 🚀 Descripción

**Datwall** es una aplicación Android diseñada para ofrecer un control total sobre el consumo de datos móviles. Utiliza una **VPN local** para implementar un cortafuegos por aplicación, permitiendo decidir qué apps pueden conectarse en primer plano, segundo plano o nunca.  

Además, incluye un sistema avanzado de estadísticas que muestra el uso de datos por app, por hora y por día, junto con herramientas para gestionar paquetes de ETECSA mediante USSD y SMS.

El objetivo del proyecto es ofrecer una solución **ligera, transparente y confiable**, especialmente optimizada para el contexto cubano.

---

## ✨ Características principales

### 🔥 Cortafuegos (Firewall)
- Implementado mediante **VPN local** (no se envía tráfico a servidores externos).  
- Bloqueo por aplicación:
  - Permitir siempre  
  - Permitir solo en primer plano  
  - Bloquear completamente  
- Control granular del tráfico saliente.

### 📊 Estadísticas de consumo
- Consumo total del paquete.  
- Consumo por aplicación.  
- Consumo por hora del día.  
- Promedio diario y estimación de duración del paquete.  
- Compatible desde Android 6+.

### 🧩 Integración con ETECSA
- Consulta automática del paquete mediante **USSD**.  
- Lectura opcional de SMS para detectar mensajes de saldo y paquetes.  
- Compra rápida de paquetes desde la app.

### 🟣 Burbuja flotante
- Muestra el consumo en tiempo real mientras usas el dispositivo.  
- Personalizable y ligera.

### 🔔 Notificación persistente
- Velocidad de datos en tiempo real.  
- Indicadores de tráfico por segundo.

---

## 🛠️ Arquitectura del proyecto

El proyecto está dividido en módulos para mantener claridad y escalabilidad:

```
Datwall/
│
├── app/               # UI, lógica de presentación y capa de interacción
├── vpncore/           # Implementación del firewall basado en VPN local
├── data/              # Repositorios, fuentes de datos, persistencia
├── domain/            # Casos de uso y lógica de negocio
└── common/            # Utilidades compartidas
```

### 🧱 vpncore
- Implementa el túnel VPN local.  
- Intercepta y filtra tráfico sin enviarlo a terceros.  
- Gestiona reglas de bloqueo por aplicación.

### 🎨 app
- Jetpack + Kotlin.  
- Navegación modular.  
- UI optimizada para dispositivos de gama media/baja.

---

## 🔐 Privacidad

Datwall está diseñado con un enfoque de **privacidad primero**:

- La VPN es **local**, no se conecta a servidores externos.  
- No se recolecta ni envía información del usuario.  
- Los SMS solo se leen si el usuario activa la función de detección de mensajes de ETECSA.  
- No se almacena contenido de tráfico ni estadísticas fuera del dispositivo.

---

## 📦 Permisos utilizados

| Permiso | Uso |
|--------|-----|
| `PACKAGE_USAGE_STATS` | Estadísticas de uso por app |
| `BIND_VPN_SERVICE` | Cortafuegos local |
| `READ_SMS` (opcional) | Detectar mensajes de ETECSA |
| `CALL_PHONE` | Ejecutar USSD |
| `SYSTEM_ALERT_WINDOW` | Burbuja flotante |
| `FOREGROUND_SERVICE` | Notificación persistente |

---

## 🧪 Compilación y ejecución

### Requisitos
- Android Studio Flamingo o superior  
- Kotlin 1.8+  
- Android 6.0 (API 23) mínimo

### Pasos
```bash
git clone https://github.com/Abdavid92/Datwall
cd Datwall
./gradlew assembleDebug
```

---

## 🗺️ Roadmap

- [ ] Modo privacidad (desactivar lectura de SMS)  
- [ ] Exportación de estadísticas  
- [ ] Reglas avanzadas por tipo de red (LTE/WiFi)  
- [ ] Tema oscuro completo  
- [ ] Mejoras en el motor VPN para menor consumo  

---

## 🤝 Contribuciones

Las contribuciones son bienvenidas.  
Puedes abrir un **issue**, enviar un **pull request** o proponer mejoras en la sección de discusiones.

---

## 📄 Licencia

MIT License — ver archivo `LICENSE` para más detalles.
