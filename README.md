# notificaciones_service
# 🔔 Notificacion Service

Microservicio de notificaciones basado en eventos para alertas de stock bajo en la plataforma de e-commerce ARKA. Escucha eventos de RabbitMQ, registra notificaciones en base de datos y envía webhooks a sistemas externos de automatización.

## 📋 Tabla de Contenidos

- [Descripción General](#-descripción-general)
- [Arquitectura](#-arquitectura)
- [Tecnologías](#-tecnologías)
- [Requisitos Previos](#-requisitos-previos)
- [Configuración](#-configuración)
- [API Endpoints](#-api-endpoints)
- [Event-Driven Architecture](#-event-driven-architecture)
- [Integración con otros Servicios](#-integración-con-otros-servicios)
- [Webhooks](#-webhooks)
- [Manejo de Errores](#-manejo-de-errores)
- [Ejecución](#-ejecución)

---

## 🎯 Descripción General

**Notificacion Service** es un microservicio event-driven que monitorea el inventario del sistema y gestiona notificaciones automáticas cuando el stock de productos alcanza niveles críticos. El servicio forma parte de una arquitectura de microservicios reactiva y utiliza mensajería asíncrona para desacoplar la lógica de negocio.

### Funcionalidades Principales

- 📩 **Escucha eventos de RabbitMQ** - Consume mensajes de stock bajo desde catalog-service
- 💾 **Registro de notificaciones** - Guarda alertas en base de datos MySQL
- 🔗 **Integración con otros servicios** - Consulta información de productos y proveedores vía Eureka
- 🌐 **Webhooks a n8n** - Envía notificaciones a flujos de automatización externos
- ♻️ **Retry con backoff** - Reintenta mensajes fallidos hasta 3 veces
- ✅ **Manual ACK** - Control preciso del procesamiento de mensajes

---

## 🏗️ Arquitectura

### Event-Driven Architecture con RabbitMQ

Este servicio implementa un patrón **Consumer** puro que reacciona a eventos del sistema sin exponer endpoints de escritura.

```
┌─────────────────────────────────────────────────────────────┐
│                    Event Flow                                │
└─────────────────────────────────────────────────────────────┘

catalog-service (Publisher)
        │
        │ Publica evento: ProductRunningLowStock
        ▼
┌──────────────────────────┐
│   RabbitMQ Exchange      │
│  "notifications.exchange"│
└────────────┬─────────────┘
             │ Routing Key: "stock.low"
             ▼
┌──────────────────────────┐
│   RabbitMQ Queue         │
│ "notifications.stock.    │
│      low.queue"          │
└────────────┬─────────────┘
             │
             │ Consume con Manual ACK
             ▼
┌──────────────────────────────────────────┐
│   StockLowEventListener                  │
│   (RabbitMQ Consumer)                    │
└────────────┬─────────────────────────────┘
             │
             ├──► 1. Consulta proveedor (usuario-service via Eureka)
             │
             ├──► 2. Valida producto (catalog-service via Eureka)
             │
             ├──► 3. Guarda notificación (MySQL)
             │
             ├──► 4. Envía webhook (n8n)
             │
             └──► 5. ACK/NACK mensaje
```

---

### Componentes Principales

```
notificacion-service/
├── listener/                # Event Consumers
│   └── StockLowEventListener.java
├── service/                 # Lógica de negocio
│   └── NotificacionService.java
├── controller/              # API REST (solo lectura)
│   └── NotificacionController.java
├── repository/              # Acceso a datos
│   └── NotificacionAbastesimientoRepository.java
├── model/                   # Entidades de dominio
│   └── NotificacionAbastesimiento.java
├── dto/                     # Data Transfer Objects
│   ├── ProductRunningLowStock.java
│   ├── PostAutomationLowStock.java
│   └── ProveedorDto.java
├── config/                  # Configuraciones
│   ├── RabbitMQConfig.java
│   └── RestClientConfig.java
└── exceptions/              # Excepciones custom
    ├── ProductNotFound.java
    └── ProveedorNotFound.java
```

---

## 🛠️ Tecnologías

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Spring Boot** | 3.5.5 | Framework base |
| **Spring AMQP** | 3.x | Integración con RabbitMQ |
| **Spring Data JPA** | 3.x | Persistencia |
| **MySQL** | 8.x | Base de datos |
| **RabbitMQ** | 3.x | Mensajería asíncrona |
| **Spring Cloud Eureka** | 2025.0.0 | Service Discovery |
| **Spring Cloud Config** | 2025.0.0 | Configuración centralizada |
| **RestClient** | Spring 6.2+ | Cliente HTTP (reemplazo de RestTemplate) |
| **Lombok** | - | Reducción de boilerplate |
| **SpringDoc OpenAPI** | 2.7.0 | Documentación API |

---

## ⚙️ Requisitos Previos

- ☕ **Java 21+**
- 🐳 **Docker** (para MySQL y RabbitMQ)
- 🔧 **Maven 3.8+**
- 🌐 **Eureka Server** corriendo en `http://localhost:8761`
- ⚙️ **Config Server** corriendo en `http://localhost:8888`
- 🐰 **RabbitMQ** corriendo en `localhost:5672`
- 🗄️ **MySQL** corriendo en `localhost:3306`
- 📦 **catalog-service** corriendo (publica eventos)
- 👥 **usuario-service** corriendo (consulta proveedores)

---

## 🔧 Configuración

### Variables de Entorno (application.yml)

```yaml
server:
  port: 8083

spring:
  application:
    name: notificacion-service
  
  config:
    import: optional:configserver:http://localhost:8888
  
  profiles:
    active: dev

# Configuración en Config Server (notificacion-service-dev.yml)
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/sistema_almacen
    username: root
    password: ${MYSQL_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect

  rabbitmq:
    listener:
      simple:
        acknowledge-mode: manual  # Control manual de ACK
    host: localhost
    port: 5672
    username: arka
    password: arka123
    virtual-host: /

eureka:
  client:
    service-url:
      defaultZone: http://admin:admin123@localhost:8761/eureka/

# Webhook para n8n
notificacion:
  webhook:
    url: https://n8nworkflows1.app.n8n.cloud/webhook/stock-bajo
```

### Base de Datos

El servicio requiere la siguiente tabla:

```sql
CREATE TABLE notificacion_abastecimiento (
    id_notificacion INT AUTO_INCREMENT PRIMARY KEY,
    mensaje TEXT NOT NULL,
    id_producto INT NOT NULL,
    INDEX idx_producto (id_producto)
);
```

---

## 📡 API Endpoints

### Base URLs

**Acceso directo (desarrollo):**
```
http://localhost:8083/api/notificaciones
```

**A través del API Gateway (recomendado):**
```
http://localhost:8090/arka/notificaciones
```

**Swagger UI:**
```
http://localhost:8083/swagger-ui.html
```

---

### 🔐 Autenticación

El endpoint requiere autenticación mediante **JWT Token** y rol de **ADMINISTRADOR**:

```http
Authorization: Bearer <tu_token_jwt>
X-User-Roles: ROLE_ADMINISTRADOR
```

---

### 📋 Endpoint

#### Obtener todas las notificaciones

```http
GET /arka/notificaciones
```

**Autorización:** 🔴 SOLO ADMIN

**Ejemplo:**
```bash
curl -X GET "http://localhost:8090/arka/notificaciones" \
  -H "Authorization: Bearer eyJhbGc..."
```

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "mensaje": "El producto Laptop HP tiene stock bajo (3 unidades). Proveedor: TechSupply (ID: 5)",
    "id_producto": 10
  },
  {
    "id": 2,
    "mensaje": "El producto Mouse Logitech tiene stock bajo (2 unidades). Proveedor: OfficeMax (ID: 3)",
    "id_producto": 25
  }
]
```

**Response 403 Forbidden (si no es admin):**
```json
{
  "timestamp": "2025-10-27T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied. Admin role required"
}
```

---

## 🔄 Event-Driven Architecture

### RabbitMQ Configuration

```java
Exchange: "notifications.exchange" (TopicExchange)
Queue: "notifications.stock.low.queue"
Routing Key: "stock.low"
Message Format: JSON
ACK Mode: MANUAL
```

### Evento Consumido

**Nombre:** `ProductRunningLowStock`

**Publicado por:** catalog-service (cuando stock <= umbral configurado)

**Formato:**
```json
{
  "producto_id": 10,
  "nombre_producto": "Laptop HP Pavilion",
  "stock_Actual": 3,
  "proveedor_id": 5
}
```

### Flujo de Procesamiento

```
1. Mensaje llega a la cola
   ↓
2. StockLowEventListener recibe evento
   ↓
3. Consulta información del proveedor (usuario-service)
   ↓
4. Valida que el producto existe (catalog-service)
   ↓
5. Crea y guarda notificación en BD
   ↓
6. Envía webhook a n8n con datos completos
   ↓
7. ACK mensaje (o NACK si falla con retry)
```

### Manejo de ACK/NACK

```java
// ✅ Procesamiento exitoso
channel.basicAck(deliveryTag, false);

// ❌ Error de validación (no reintentar)
channel.basicReject(deliveryTag, false);

// ⚠️ Error temporal (reintentar hasta 3 veces)
if (retryCount < MAX_RETRIES) {
    channel.basicNack(deliveryTag, false, true);
} else {
    channel.basicReject(deliveryTag, false);
}
```

---

## 🔗 Integración con otros Servicios

### 1. catalog-service (REST - Eureka)

**Propósito:** Validar que el producto existe antes de crear la notificación

**Endpoint consumido:**
```
GET http://CATALOG-SERVICE/api/productos/id/{id}
```

**Configuración:**
```java
RestClient catalogRestClient = restClientBuilder
    .baseUrl("http://CATALOG-SERVICE")  // ← Service discovery
    .build();
```

**Uso:**
```java
catalogRestClient.get()
    .uri("/api/productos/id/{id}", productoId)
    .retrieve()
    .body(String.class);
```

---

### 2. usuario-service (REST - Eureka)

**Propósito:** Obtener información del proveedor para el webhook

**Endpoint consumido:**
```
GET http://USUARIO-SERVICE/api/proveedores/{id}
```

**Configuración:**
```java
RestClient usuarioRestClient = restClientBuilder
    .baseUrl("http://USUARIO-SERVICE")  // ← Service discovery
    .build();
```

**Uso:**
```java
ProveedorDto proveedor = usuarioRestClient.get()
    .uri("/api/proveedores/{id}", proveedorId)
    .retrieve()
    .body(ProveedorDto.class);
```

---

### 3. RabbitMQ (Event Consumer)

**Exchange:** `notifications.exchange`
**Queue:** `notifications.stock.low.queue`
**Routing Key:** `stock.low`

**Listener:**
```java
@RabbitListener(queues = RabbitMQConfig.STOCK_LOW_QUEUE)
public void handleLowStockEvent(ProductRunningLowStock event, 
                                Channel channel, 
                                Message message) {
    // Procesar evento
}
```

---

## 🌐 Webhooks

### n8n Webhook para Automatización

**URL:** Configurada en `notificacion.webhook.url`

**Método:** POST

**Payload enviado:**
```json
{
  "producto_id": 10,
  "nombre_producto": "Laptop HP Pavilion",
  "stock_Actual": 3,
  "proveedor_id": 5,
  "proovedor_email": "+573001234567",
  "nombre_proveedor": "TechSupply S.A."
}
```

**Configuración:**
```java
@Service
public class StockLowEventListener {
    
    private final RestClient restClient;
    
    // RestClient para webhook externo (sin @LoadBalanced)
    public StockLowEventListener(
            @Qualifier("externalRestClientBuilder") RestClient.Builder builder,
            @Value("${notificacion.webhook.url}") String webhookUrl) {
        this.restClient = builder.build();
    }
    
    private void enviarWebhook(PostAutomationLowStock data) {
        restClient.post()
            .uri(webhookUrl)
            .body(data)
            .retrieve()
            .toBodilessEntity();
    }
}
```

**Manejo de errores:**
- El webhook se ejecuta de forma **fire-and-forget**
- Si falla, se registra en logs pero no afecta el procesamiento del mensaje
- El mensaje sigue siendo ACK si la BD se guardó correctamente

---

## ⚠️ Manejo de Errores

### Excepciones Personalizadas

| Excepción | Descripción | Acción |
|-----------|-------------|--------|
| `ProductNotFound` | Producto no existe en catalog-service | REJECT mensaje (no reintentar) |
| `ProveedorNotFound` | Proveedor no existe en usuario-service | REJECT mensaje (no reintentar) |
| `IllegalArgumentException` | Validación de datos fallida | REJECT mensaje (no reintentar) |
| `Exception` (genérica) | Error inesperado | NACK mensaje (reintentar hasta 3 veces) |

### Estrategia de Retry

```java
private static final int MAX_RETRIES = 3;

try {
    // Procesar mensaje
    channel.basicAck(deliveryTag, false);
} catch (IllegalArgumentException e) {
    // Error de negocio: no reintentar
    channel.basicReject(deliveryTag, false);
} catch (Exception e) {
    // Error temporal: reintentar
    Integer retryCount = message.getHeaders().get("x-retry-count");
    if (retryCount == null) retryCount = 0;
    
    if (retryCount < MAX_RETRIES) {
        channel.basicNack(deliveryTag, false, true);  // Reencolar
    } else {
        channel.basicReject(deliveryTag, false);  // Dead letter
    }
}
```

---

## 🚀 Ejecución

### 1. Clonar el repositorio

```bash
git clone https://github.com/tu-org/notificacion-service.git
cd notificacion-service
```

### 2. Configurar variables de entorno

Crear archivo `.env` con:
```properties
MYSQL_PASSWORD=tu_password
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=arka
RABBITMQ_PASSWORD=arka123
```

### 3. Iniciar dependencias (Docker)

```bash
# MySQL
docker run -d --name mysql \
  -e MYSQL_ROOT_PASSWORD=${MYSQL_PASSWORD} \
  -e MYSQL_DATABASE=sistema_almacen \
  -p 3306:3306 \
  mysql:8

# RabbitMQ
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
```

### 4. Compilar y ejecutar

```bash
mvn clean install
mvn spring-boot:run
```

### 5. Verificar que está corriendo

```bash
# Health check
curl http://localhost:8083/actuator/health

# Swagger UI
open http://localhost:8083/swagger-ui.html

# Eureka Dashboard
open http://localhost:8761
```

---

## 📊 Monitoring y Logs

### Logs importantes

```bash
# Ver logs en tiempo real
tail -f logs/notificacion-service.log

# Eventos procesados
grep "Evento de stock bajo recibido" logs/notificacion-service.log

# Webhooks enviados
grep "Webhook enviado exitosamente" logs/notificacion-service.log

# Errores
grep "ERROR" logs/notificacion-service.log
```

### Métricas clave

- ✅ Mensajes procesados por minuto
- ✅ Tasa de ACK vs NACK
- ✅ Tiempo promedio de procesamiento
- ✅ Webhooks exitosos vs fallidos
- ✅ Latencia de consultas a otros servicios

---

## 🧪 Testing

### Simular evento de stock bajo

Para probar el flujo completo, publica un mensaje manualmente en RabbitMQ:

```bash
# Usando RabbitMQ Management UI (http://localhost:15672)
Exchange: notifications.exchange
Routing Key: stock.low
Payload:
{
  "producto_id": 10,
  "nombre_producto": "Test Product",
  "stock_Actual": 2,
  "proveedor_id": 1
}
```

O reduce el stock de un producto en catalog-service para que publique el evento automáticamente.

---

## 🔐 Seguridad

**Nota:** Este servicio actualmente NO implementa autenticación/autorización en sus endpoints internos. En producción se recomienda:

- 🔒 Agregar Spring Security
- 🔒 Validar JWT en endpoints REST
- 🔒 Proteger RabbitMQ con credenciales seguras
- 🔒 HTTPS obligatorio
- 🔒 Rate limiting para prevenir abuso

---

## 📝 Notas Técnicas

### RestClient vs RestTemplate

Este servicio usa **RestClient** (introducido en Spring 6.1) en lugar de `RestTemplate` (deprecated):

```java
// ✅ NUEVO: RestClient
@Configuration
public class RestClientConfig {
    
    @Bean
    @LoadBalanced  // Para Eureka
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
    
    @Bean("externalRestClientBuilder")  // Para webhooks externos
    public RestClient.Builder externalRestClientBuilder() {
        return RestClient.builder();
    }
}
```

**Ventajas:**
- API fluida y moderna
- Mejor manejo de errores
- Soporte para reactive cuando sea necesario
- Menor boilerplate que RestTemplate

### Manual ACK en RabbitMQ

El servicio usa **manual acknowledgment** para control preciso:

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: manual
```

**Beneficios:**
- Control explícito de cuándo se completa el procesamiento
- Posibilidad de reencolar mensajes fallidos
- Evita pérdida de mensajes en caso de errores

---

## 🔗 Configuración del Gateway

Las rutas en el API Gateway están configuradas de la siguiente manera:

```yaml
spring:
  cloud:
    gateway:
      routes:
        # Listar notificaciones (SOLO ADMIN)
        - id: notificaciones-listar
          uri: lb://NOTIFICACION-SERVICE
          predicates:
            - Path=/arka/notificaciones
            - Method=GET
          filters:
            - JwtAuthenticationFilter
            - AdminAuthorizationFilter
            - RewritePath=/arka/notificaciones, /api/notificaciones
```

**Última actualización:** Octubre 2025  
**Versión:** 1.0.0
