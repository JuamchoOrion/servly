# Observabilidad Servly - Implementación Completa

## 📊 Resumen de Implementación

Se ha implementado un sistema completo de observabilidad para Servly basado en **ISO/IEC 25010** utilizando:
- **Micrometer** para registro de métricas personalizadas
- **Prometheus** para scraping y almacenamiento de métricas
- **Grafana** para visualización y alertas
- **Spring Boot Actuator** para exposición de métricas
- **AOP (Aspect-Oriented Programming)** para interceptación automática

---

## 📁 Estructura de Archivos Creados

```
servly/
├── build.gradle (ACTUALIZADO)
│   └── + Dependencias de Micrometer, Prometheus, Actuator
│
├── src/main/resources/
│   ├── application.yml (CREADO)
│   │   └── Configuración de Actuator, Prometheus, métricas
│   │
├── src/main/java/co/edu/uniquindio/servly/
│   ├── config/
│   │   └── MetricsConfig.java (CREADO)
│   │       └── Registro de todas las métricas personalizadas
│   │
│   ├── metrics/
│   │   ├── AuthMetricsService.java (CREADO)
│   │   │   └── Métricas de autenticación
│   │   └── InventoryMetricsService.java (CREADO)
│   │       └── Métricas de inventario
│   │
│   └── aspect/
│       └── MetricsAspect.java (CREADO)
│           └── AOP para interceptación automática
│
├── docker-compose.yml (CREADO)
│   └── Servicios: Servly App, Prometheus, Grafana
│
├── prometheus.yml (CREADO)
│   └── Configuración de scraping de Prometheus
│
├── Dockerfile (CREADO)
│   └── Construcción multi-etapa de imagen Docker
│
└── grafana/
    ├── provisioning/
    │   ├── datasources/
    │   │   └── prometheus.yml (CREADO)
    │   │       └── Auto-provisión de datasource Prometheus
    │   └── dashboards/
    │       └── dashboards.yml (CREADO)
    │           └── Configuración de dashboard provider
    └── dashboards/
        └── servly-dashboard.json (CREADO)
            └── Dashboard con 8 paneles de calidad ISO/IEC 25010
```

---

## 🎯 Métricas Implementadas

### AUTENTICACIÓN (6 métricas)
| Métrica | Tipo | Umbral | Descripción |
|---------|------|--------|-------------|
| `auth.login.duration` | Timer | < 2000ms (p95) | Duración de intentos de login |
| `auth.login.success` | Counter | > 95% | Tasa de éxito de login |
| `auth.password.recovery.duration` | Timer | < 5 min | Recuperación de contraseña |
| `auth.2fa.verification.duration` | Timer | < 60s | Verificación de código 2FA |
| `auth.2fa.codes.expired/generated` | Counter | < 10% expiration | Tasa de expiración 2FA |
| `auth.session.duration` | Timer | ≈ shift duration | Duración de sesiones |

### INVENTARIO (8 métricas)
| Métrica | Tipo | Umbral | Descripción |
|---------|------|--------|-------------|
| `inventory.item.creation.duration` | Timer | < 500ms (p95) | Creación de items |
| `inventory.item.creation.success` | Counter | >= 95% | Tasa de éxito items |
| `inventory.category.creation.duration` | Timer | < 300ms (p95) | Creación de categorías |
| `inventory.category.creation.success` | Counter | >= 90% | Tasa de éxito categorías |
| `inventory.category.duplicate.errors` | Counter | < 10% | Errores de duplicación |
| `inventory.category.toggle.success` | Counter | >= 98% | Tasa de toggle exitoso |
| `inventory.items.paginated.query.duration` | Timer | < 150ms (p95) | Consultas paginadas |
| `inventory.crud.endpoints.tested` | Gauge | = 100% | Cobertura de tests |

---

## 🚀 Instrucciones de Deployment

### 1. Compilación Local
```bash
# Navegar al directorio del proyecto
cd C:\Users\ramir\Documents\7mo\ Semestre\Ing\ de\ software\ III\servly

# Compilar con Gradle
./gradlew build

# O en Windows:
gradlew.bat build
```

### 2. Levantar Stack Completo con Docker Compose
```bash
# Asegurarse de tener Docker Desktop instalado y corriendo

# Construir e iniciar todos los servicios
docker-compose up --build

# En background:
docker-compose up --build -d
```

### 3. URLs de Acceso
- **Aplicación Servly**: http://localhost:8081
- **Métricas Prometheus**: http://localhost:9090
- **Dashboard Grafana**: http://localhost:3000
  - Usuario: `admin`
  - Contraseña: `admin`

### 4. Endpoints Disponibles
```
GET  http://localhost:8081/actuator                          # Index de endpoints
GET  http://localhost:8081/actuator/health                   # Health check
GET  http://localhost:8081/actuator/health/liveness          # Liveness probe
GET  http://localhost:8081/actuator/health/readiness         # Readiness probe
GET  http://localhost:8081/actuator/prometheus               # Métricas Prometheus
GET  http://localhost:8081/actuator/metrics                  # Disponibles todas las métricas
GET  http://localhost:8081/actuator/metrics/{metric.name}   # Métrica específica
GET  http://localhost:8081/actuator/info                     # Información de app
```

---

## 📊 Dashboard Grafana - Paneles

El dashboard `Servly - Observability Dashboard (ISO/IEC 25010)` contiene 8 paneles:

### 1. **Login Duration (p95)** - Panel de línea
- **Métrica**: `histogram_quantile(0.95, rate(auth_login_duration_seconds_bucket[5m])) * 1000`
- **Umbral**: 2000ms (línea roja)
- **Alerta**: Si p95 > 2000ms

### 2. **Login Success Rate** - Panel gauge
- **Métrica**: `(auth_login_success_total / auth_login_total_total) * 100`
- **Umbral**: > 95% (verde), < 90% (rojo)
- **Alerta**: Si tasa < 95%

### 3. **2FA Code Expiration Rate** - Panel gauge
- **Métrica**: `(auth_2fa_codes_expired_total / auth_2fa_codes_generated_total) * 100`
- **Umbral**: < 10% (verde), > 10% (rojo)
- **Alerta**: Si tasa > 10%

### 4. **Item Creation Duration (p95)** - Panel de línea
- **Métrica**: `histogram_quantile(0.95, rate(inventory_item_creation_duration_seconds_bucket[5m])) * 1000`
- **Umbral**: 500ms (línea roja)
- **Alerta**: Si p95 > 500ms

### 5. **Category Creation Success Rate** - Panel gauge
- **Métrica**: `(inventory_category_creation_success_total / inventory_category_creation_total_total) * 100`
- **Umbral**: > 90% (verde), < 85% (rojo)
- **Alerta**: Si tasa < 90%

### 6. **Paginated Query Duration (p95)** - Panel de línea
- **Métrica**: `histogram_quantile(0.95, rate(inventory_items_paginated_query_duration_seconds_bucket[5m])) * 1000`
- **Umbral**: 150ms (línea roja)
- **Alerta**: Si p95 > 150ms

### 7. **Category Creation Duration (p95)** - Panel de línea
- **Métrica**: `histogram_quantile(0.95, rate(inventory_category_creation_duration_seconds_bucket[5m])) * 1000`
- **Umbral**: 300ms (línea roja)
- **Alerta**: Si p95 > 300ms

### 8. **Category Toggle Success Rate** - Panel gauge
- **Métrica**: `(inventory_category_toggle_success_total / inventory_category_toggle_total_total) * 100`
- **Umbral**: > 98% (verde), < 95% (rojo)
- **Alerta**: Si tasa < 98%

---

## 🔧 Configuración de Prometheus

**Scrape Interval**: 15 segundos
**Target**: `servly-app:8081/actuator/prometheus`
**Retention**: 30 días

Prometheus automáticamente scrapeará las métricas cada 15 segundos y las almacenará en su TSDB (Time Series Database).

---

## 🎯 AOP - Interceptación Automática

El `MetricsAspect.java` intercepta automáticamente:

### AuthService
- `login()` → Registra duración y resultado
- `authenticate()` → Registra duración y resultado
- `resetPassword()` → Registra duración
- `forgotPassword()` → Registra duración
- `verify2FA()` → Registra duración
- `validateTwoFACode()` → Registra duración

### ItemService
- `create()` / `save()` → Registra duración y resultado
- `findAll()` / `getAllPaginated()` → Registra duración de query

### CategoryService
- `create()` / `save()` → Registra duración y resultado
- `toggleActive()` / `toggle()` → Registra resultado
- `findAll()` / `getAllPaginated()` → Registra duración de query

**Nota**: Las métricas se registran automáticamente sin cambios de código en los servicios.

---

## 📈 Cómo Consultar Métricas

### Opción 1: Directamente en Prometheus (http://localhost:9090)
```
# Query examples
auth_login_success_total
auth_login_total_total
inventory_item_creation_duration_seconds_bucket
rate(auth_login_success_total[5m])
histogram_quantile(0.95, rate(auth_login_duration_seconds_bucket[5m]))
```

### Opción 2: En Grafana (http://localhost:3000)
- Dashboard ya pre-configurado
- Paneles con umbrales visuales
- Tooltips con valores en tiempo real

### Opción 3: Endpoint del Actuator
```bash
curl http://localhost:8081/actuator/prometheus
```

---

## 🧪 Testing de Métricas

### Test 1: Login Success Rate
```bash
# Hacer varios login fallidos y exitosos
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"wrong"}'

# Verificar en Prometheus:
# (auth_login_success_total / auth_login_total_total) * 100
```

### Test 2: Item Creation Duration
```bash
# Crear varios items
curl -X POST http://localhost:8081/api/items \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Item","description":"Test","categoryId":1}'

# Verificar en Prometheus:
# histogram_quantile(0.95, rate(inventory_item_creation_duration_seconds_bucket[5m]))
```

### Test 3: Paginated Queries
```bash
# Consultar items paginados
curl http://localhost:8081/api/items?page=0&size=10 \
  -H "Authorization: Bearer {token}"

# Verificar en Prometheus:
# histogram_quantile(0.95, rate(inventory_items_paginated_query_duration_seconds_bucket[5m]))
```

---

## 🛑 Troubleshooting

### 1. Prometheus no conecta a Servly
```
Error: "context deadline exceeded"
```
**Solución**: Esperar a que la app esté lista (healthcheck)
```bash
docker-compose logs servly-app
```

### 2. Grafana no muestra datos
**Solución**: Verificar datasource Prometheus
```
http://localhost:3000 → Configuration → Data Sources → Prometheus
Test: http://prometheus:9090
```

### 3. Métricas no aparecen en Prometheus
**Solución**: Verificar endpoint del actuator
```bash
curl http://localhost:8081/actuator/prometheus | grep "auth_login"
```

### 4. AOP no intercepta métodos
**Solución**: Verificar que `@EnableAspectJAutoProxy` está habilitado (automático en Spring Boot 3.x)

---

## 📋 Dependencias Agregadas

```gradle
// Observability - Micrometer, Prometheus, Actuator
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'
implementation 'org.springframework.boot:spring-boot-starter-aop'
```

---

## 📝 Próximos Pasos (Opcional)

1. **Alertas en Prometheus**:
   - Crear reglas en `prometheus.yml` con `alert_rules.yml`
   - Configurar Alertmanager

2. **Persistencia de Datos**:
   - Prometheus almacena en `/prometheus` (volumen Docker)
   - Grafana almacena en `/var/lib/grafana` (volumen Docker)

3. **Monitoring Avanzado**:
   - Agregar métricas de base de datos
   - Agregar métricas de JVM (heap, GC)
   - Agregar custom alerts

4. **Integración CI/CD**:
   - Incluir en pipeline de Jenkins/GitHub Actions
   - Validar umbrales como part of build

---

## ✅ Validación de Implementación

Todos los archivos han sido creados con:
- ✅ Declaraciones de paquete correctas
- ✅ Imports necesarios
- ✅ Javadoc en cada método
- ✅ Inyección de dependencias por constructor
- ✅ Comentarios con umbrales ISO/IEC 25010
- ✅ Configuración de Prometheus con scrape interval 15s
- ✅ Dashboard Grafana con 8 paneles
- ✅ Docker Compose con 3 servicios (app, prometheus, grafana)
- ✅ AOP para interceptación automática
- ✅ Métricas personalizadas con Tags

---

**Creado**: Marzo 9, 2026
**Versión**: 1.0.0
**ISO/IEC 25010**: Completo

