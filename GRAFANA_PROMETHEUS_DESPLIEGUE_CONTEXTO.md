# SERVLY - GRAFANA & PROMETHEUS CON DOCKER
## Documento Explicativo para Despliegue en AWS EC2

---

## 1. DESCRIPCIÓN GENERAL DEL SISTEMA

**Servly** es una aplicación Spring Boot 3.x (Java 17+) para gestión de restaurantes que implementa **observabilidad completa** usando:

- **Backend**: Spring Boot en puerto `8081`
- **Observability Stack**: 
  - **Prometheus** (puerto `9090`) → Recolecta y almacena métricas
  - **Grafana** (puerto `3000`) → Visualiza las métricas
- **Base de datos**: PostgreSQL en Supabase (nube)
- **Orquestación**: Docker + Docker Compose

---

## 2. CÓMO FUNCIONA ACTUALMENTE (EN LOCAL)

### Diagrama de Flujo:

```
┌──────────────────────────────────────────────────────────────────┐
│                  MÁQUINA LOCAL (Windows)                         │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  DOCKER COMPOSE (servicio orquestador)                  │   │
│  ├─────────────────────────────────────────────────────────┤   │
│  │                                                         │   │
│  │  1. BACKEND (servly-backend)                           │   │
│  │     ├─ Puerto: 8081                                    │   │
│  │     ├─ Imagen: Dockerfile local (multi-stage build)    │   │
│  │     ├─ Expone endpoint: /actuator/prometheus           │   │
│  │     ├─ Conecta a: Supabase PostgreSQL (remoto)         │   │
│  │     └─ Genera métricas: auth, inventory, 2FA, etc      │   │
│  │                                                         │   │
│  │     ↓ (Métrica: /actuator/prometheus)                  │   │
│  │                                                         │   │
│  │  2. PROMETHEUS (prom/prometheus:latest)                │   │
│  │     ├─ Puerto: 9090                                    │   │
│  │     ├─ Config: prometheus.yml                          │   │
│  │     ├─ Scrapeá cada 15 segundos: servly-backend:8081  │   │
│  │     ├─ Almacena: métricas en volumen local             │   │
│  │     └─ Retiene: datos por 15 días (por defecto)        │   │
│  │                                                         │   │
│  │     ↓ (Datasource: Prometheus)                         │   │
│  │                                                         │   │
│  │  3. GRAFANA (grafana/grafana:latest)                   │   │
│  │     ├─ Puerto: 3000                                    │   │
│  │     ├─ Datasource: Auto-provisionado (Prometheus)      │   │
│  │     ├─ Dashboards: servly-dashboard.json               │   │
│  │     │  ├─ Login Duration (p95)                         │   │
│  │     │  ├─ Login Success Rate by Role                   │   │
│  │     │  ├─ 2FA Code Expiration Rate                     │   │
│  │     │  ├─ Item Creation Duration (p95)                 │   │
│  │     │  ├─ Category Creation Success Rate               │   │
│  │     │  └─ Paginated Query Duration (p95)               │   │
│  │     └─ Usuario/Pass: admin/admin (por defecto)         │   │
│  │                                                         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ↓ (JDBC Connection String)                                    │
│                                                                  │
│  Supabase PostgreSQL (AWS, externo)                            │
│  URL: aws-1-sa-east-1.pooler.supabase.com:5432                │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Flujo de Datos Detallado:

1. **Backend generador de métricas**:
   - Usuario hace login → `AuthMetricsService` registra duración y éxito
   - Se crea un item → `InventoryMetricsService` registra duración
   - Las métricas se acumulan en memoria de la app

2. **Prometheus scraper** (cada 15 segundos):
   - Hace GET a `http://servly-backend:8081/actuator/prometheus`
   - Obtiene todas las métricas en formato Prometheus
   - Las almacena en disco (volumen `prometheus_data`)
   - Comprime datos antiguos automáticamente

3. **Grafana visualizador** (en tiempo real):
   - Consulta Prometheus cada vez que se abre un dashboard
   - Ejecuta PromQL queries: `histogram_quantile(0.95, auth_login_duration_seconds_bucket)`
   - Dibuja gráficos interactivos

---

## 3. ARCHIVOS Y CONFIGURACIÓN CRÍTICOS

### 3.1 `docker-compose.yml` (Orquestación)

```yaml
version: '3.8'
services:
  servly-backend:
    build: .                    # Construye desde Dockerfile local
    ports:
      - "8081:8081"           # Puerto host:contenedor
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://aws-1-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require
      SPRING_DATASOURCE_USERNAME: postgres.wglrjiiljnzemxrczwmy
      SPRING_DATASOURCE_PASSWORD: oSsZofkDxg46OsWJ
    depends_on:
      - prometheus            # Inicia Prometheus primero
    networks:
      - monitoring            # Red interna Docker

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml  # Config
      - prometheus_data:/prometheus                       # Almacenamiento
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
    networks:
      - monitoring

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    volumes:
      - ./grafana/provisioning:/etc/grafana/provisioning  # Auto-config
      - grafana_data:/var/lib/grafana                     # Datos
    networks:
      - monitoring

volumes:
  prometheus_data:    # Persiste métricas incluso si contenedor se reinicia
  grafana_data:       # Persiste configuración de Grafana

networks:
  monitoring:         # Red privada entre contenedores
```

**Qué significa cada sección:**
- `build: .` → Compila la imagen del backend desde el `Dockerfile` en la raíz
- `depends_on` → Asegura que Prometheus esté listo antes que el backend
- `volumes` → Datos persisten aunque los contenedores se detengan
- `networks` → Los 3 servicios se comunican entre sí como si fueran máquinas en la misma red

### 3.2 `prometheus.yml` (Configuración de Scrape)

```yaml
global:
  scrape_interval: 15s        # Cada 15 segundos recolecta métricas
  evaluation_interval: 15s
  
scrape_configs:
  - job_name: 'servly-backend'
    static_configs:
      - targets: ['servly-backend:8081']  # Dirección DNS Docker
    metrics_path: '/actuator/prometheus'   # Endpoint de métricas
```

**Qué hace:**
- Cada 15 segundos, envía una petición GET a `http://servly-backend:8081/actuator/prometheus`
- Guarda las métricas en la base de datos de series temporales (TSDB)

### 3.3 `application.properties` (Config del Backend)

```properties
# Configuración de Actuator (expone métricas)
management.endpoints.web.exposure.include=health,prometheus,metrics,info
management.metrics.export.prometheus.enabled=true
management.metrics.tags.application=servly
management.metrics.tags.environment=dev
management.health.mail.enabled=false

# Configuración de HikariCP (pool de conexiones)
spring.datasource.hikari.maximum-pool-size=3
spring.datasource.hikari.minimum-idle=1

# JPA & Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
```

**Qué hace:**
- `management.endpoints.web.exposure.include` → Expone `/actuator/prometheus` públicamente
- `management.metrics.tags` → Agrega tags a TODAS las métricas automáticamente
- `management.health.mail.enabled=false` → Desactiva health check de correo (que ralentiza)

### 3.4 `Dockerfile` (Build Multi-stage)

```dockerfile
# Stage 1: Compilación
FROM gradle:8.3-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# Stage 2: Runtime
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Ventajas:**
- **Multi-stage**: La imagen final solo tiene JRE, no Gradle (más pequeña)
- **EXPOSE 8081**: Documenta el puerto usado (Docker lo mapea)
- **ENTRYPOINT**: Comando que ejecuta al iniciar el contenedor

### 3.5 `grafana/provisioning/datasources/prometheus.yml` (Auto-config)

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090    # DNS del contenedor
    isDefault: true
    editable: true
```

**Qué hace:**
- Cuando Grafana inicia, automáticamente conecta a Prometheus
- No necesitas configurar manualmente en la UI

---

## 4. MÉTRICAS QUE SE RECOLECTAN

El backend expone automáticamente estas métricas (vía `MetricsConfig.java` y `@Aspect`):

### Métricas de Autenticación:
- `auth_login_duration_seconds` → Cuánto tarda el login (histogram)
- `auth_login_by_role_total` → Conteo de logins por rol
- `auth_password_recovery_duration_seconds` → Duración de recuperación de contraseña
- `auth_2fa_verification_duration_seconds` → Duración de verificación 2FA

### Métricas de Inventario:
- `inventory_item_creation_duration_seconds` → Cuánto tarda crear un item
- `inventory_category_creation_duration_seconds` → Cuánto tarda crear categoría
- `inventory_items_paginated_query_duration_seconds` → Cuánto tarda listar items
- `inventory_category_duplicate_errors_total` → Conteo de errores por categoría duplicada

### Métricas del Sistema:
- `jvm_memory_used_bytes` → Memoria JVM usada
- `process_uptime_seconds` → Tiempo que lleva corriendo
- `http_requests_total` → Total de peticiones HTTP

---

## 5. CÓMO ACCEDER LOCALMENTE

### Iniciar todo:
```bash
# En la raíz del proyecto
docker-compose up --build -d

# Espera ~30 segundos a que se inicien
```

### URLs locales:
| Servicio | URL | Usuario | Contraseña |
|----------|-----|---------|-----------|
| Backend | http://localhost:8081 | - | - |
| Backend Health | http://localhost:8081/actuator/health | - | - |
| Backend Metrics | http://localhost:8081/actuator/prometheus | - | - |
| Prometheus | http://localhost:9090 | - | - |
| Grafana | http://localhost:3000 | admin | admin |

---

## 6. CÓMO FUNCIONARÍA EN AWS EC2

### Cambios Necesarios:

1. **Security Group de EC2**:
   - Puerto 22 (SSH): Para conectarse remotamente
   - Puerto 4200: Abierto a `0.0.0.0/0` (público, para el Frontend Angular)
   - Puerto 8081: Abierto a `0.0.0.0/0` (público, para la API Backend)
   - Puerto 3000: Abierto solo a tu IP (privado, para Grafana)
   - Puerto 9090: Abierto solo a tu IP (privado, para Prometheus)

2. **Docker Compose en EC2**:
   - Los 3 servicios se ejecutan IGUAL que en local
   - Los volúmenes persisten en el almacenamiento de EC2
   - El networking es idéntico

3. **Acceso desde Internet**:
   - **Frontend Angular** (en EC2 puerto 4200) → Conecta a `http://ec2-public-ip:8081/api/*`
   - **Backend API** (en EC2 puerto 8081) → Disponible públicamente
   - **Grafana** (en EC2 puerto 3000) → Accesible solo desde tu IP (privado)
   - **Prometheus** (en EC2 puerto 9090) → No es accesible públicamente (solo desde Grafana)

### Diagrama AWS:

```
┌──────────────────────────────────────────────────────────────┐
│                     AWS ACCOUNT                              │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  EC2 Instance (t3.medium, Ubuntu 22.04)                │ │
│  │  Elastic IP: 54.xxx.xxx.xxx                            │ │
│  │                                                        │ │
│  │  ┌──────────────────────────────────────────────────┐ │ │
│  │  │ Docker Compose                                   │ │ │
│  │  │                                                  │ │ │
│  │  │  servly-backend:8081    ← (público)            │ │ │
│  │  │  prometheus:9090        ← (privado en VPC)     │ │ │
│  │  │  grafana:3000           ← (restringido a IP)   │ │ │
│  │  └──────────────────────────────────────────────────┘ │ │
│  │                                                        │ │
│  │  ┌──────────────────────────────────────────────────┐ │ │
│  │  │ Node.js (ng serve o serve estático)              │ │ │
│  │  │                                                  │ │ │
│  │  │  Frontend Angular:4200  ← (público)            │ │ │
│  │  │  Conecta a: localhost:8081/api (internamente)  │ │ │
│  │  └──────────────────────────────────────────────────┘ │ │
│  │                                                        │ │
│  │  ↓ (JDBC Connection)                                  │ │
│  │                                                        │ │
│  │  Supabase PostgreSQL (remoto)                         │ │
│  │  URL: aws-1-sa-east-1.pooler.supabase.com:5432       │ │
│  │                                                        │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
└──────────────────────────────────────────────────────────────┘

Usuario desde Navegador
  ↓
http://54.xxx.xxx.xxx:4200  (Frontend Angular)
  ↓
Peticiones API: http://54.xxx.xxx.xxx:8081/api/*  (Backend)
```

---

## 7. PROCESO DE DESPLIEGUE RESUMIDO

### Pasos (para que la IA genere el paso a paso):

1. **Crear EC2 instance** en AWS
   - Tipo: t3.medium (mínimo recomendado)
   - AMI: Ubuntu 22.04 LTS
   - Storage: 30GB
   - Security Group: (ver punto 1 arriba)
   - Elastic IP: Asignar una

2. **Conectar vía SSH** a la instancia

3. **Instalar Docker y Docker Compose** en la instancia

4. **Clonar el repositorio** de GitHub

5. **Configurar variables de entorno** (BD, JWT, etc.) en `.env` o `docker-compose.yml`

6. **Ejecutar Backend + Observability con Docker Compose**:
   - `docker-compose up --build -d`
   - Verifica que Backend (8081), Prometheus (9090) y Grafana (3000) estén corriendo

7. **Instalar Node.js y npm** en EC2

8. **Instalar dependencias del Frontend**:
   - `cd path/to/frontend`
   - `npm install`

9. **Ejecutar Frontend Angular en puerto 4200**:
   - `ng serve --host 0.0.0.0 --port 4200`
   - O en background: `nohup ng serve --host 0.0.0.0 --port 4200 &`

10. **Verificar que funciona**:
    - Backend: `curl http://localhost:8081/actuator/health`
    - Frontend: `curl http://localhost:4200`
    - Prometheus: `curl http://localhost:9090`
    - Grafana: Abrir navegador: `http://ec2-public-ip:3000`

11. **Acceder desde Internet**:
    - Frontend: `http://ec2-public-ip:4200`
    - Grafana: `http://ec2-public-ip:3000` (solo desde tu IP)
    - Los dashboards ya están auto-provisionados

---

## 8. NOTAS IMPORTANTES PARA EL DESPLIEGUE

### Seguridad:
- **Cambiar contraseña de Grafana** en docker-compose ANTES de desplegar
- **Restringir puerto 3000** solo a tu IP (no público)
- **Restringir puerto 9090** solo a tu IP (no público)
- **Puerto 8081** puede ser público (es la API)

### Persistencia:
- Los volúmenes Docker persisten automáticamente
- Las métricas se guardan en `prometheus_data`
- La config de Grafana se guarda en `grafana_data`
- Si necesitas backup: `docker-compose exec prometheus tar czf /backups/prometheus.tar.gz /prometheus`

### Performance:
- HikariCP limita a 3 conexiones (suficiente para una instancia t3.medium)
- Prometheus retiene 15 días de datos por defecto
- Grafana es lightweight (consume ~200MB RAM)

### Costo Estimado (AWS):
- EC2 t3.medium: ~$30/mes
- Supabase PostgreSQL: ~$5-25/mes
- Total: ~$35-55/mes

---

## 9. COMANDOS ÚTILES EN EC2

```bash
# Ver logs en tiempo real
docker-compose logs -f

# Ver solo logs del backend
docker-compose logs -f servly-backend

# Reiniciar un servicio
docker-compose restart servly-backend

# Detener todo
docker-compose down

# Ver estado de los contenedores
docker-compose ps

# Entrar a un contenedor
docker-compose exec servly-backend bash
```

---

## 10. RESUMEN EJECUTIVO

**Servly** usa Docker Compose para orquestar 3 servicios + Frontend Angular:
1. **Backend** (Spring Boot) → Genera métricas (puerto 8081)
2. **Prometheus** → Almacena métricas (puerto 9090)
3. **Grafana** → Visualiza métricas (puerto 3000)
4. **Frontend** (Angular) → Interfaz web (puerto 4200)

En **AWS EC2**, todo está en la misma instancia:
- Backend, Prometheus y Grafana corren en Docker Compose
- Frontend corre con `ng serve` o en un servidor estático (Node.js)

**Acceso en AWS:**
- **Frontend Angular**: Público en `http://ec2-public-ip:4200`
- **API Backend**: Público en puerto 8081 (las peticiones del frontend van aquí)
- **Grafana**: Privado en puerto 3000 (solo desde tu IP)
- **Prometheus**: Privado en puerto 9090 (solo desde Grafana)
- **Base de datos**: Supabase remoto (no cambia)

El **dashboard de Grafana** se auto-provisiona, mostrando métricas de rendimiento de tu aplicación en tiempo real.

**Acceso desde usuario final:**
- Abre `http://ec2-public-ip:4200` en el navegador
- El frontend hace peticiones a `http://ec2-public-ip:8081/api/*` (automáticamente mapeadas)

