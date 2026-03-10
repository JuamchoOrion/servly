# SERVLY - CONTEXTO PARA DESPLIEGUE EN AWS EC2

## 1. DESCRIPCIÓN GENERAL DE LA APLICACIÓN

**Servly** es un sistema de gestión de restaurante construido con:
- **Backend**: Spring Boot 3.x, Java 17+
- **Base de datos**: PostgreSQL (actualmente en Supabase)
- **Autenticación**: JWT + OAuth2 (Google)
- **Observabilidad**: Micrometer + Prometheus + Grafana
- **Puerto del backend**: 8081

### Módulos principales:
1. **Authentication (Auth)**: Login, 2FA, recuperación de contraseña, OAuth2
2. **Inventory**: Items, Categorías, Stock, Lotes de productos
3. **Invoices**: Generación de PDF
4. **Observability**: Métricas de rendimiento

---

## 2. ARQUITECTURA ACTUAL (LOCAL)

### Stack Local:
```
┌─────────────────────────────────────────────────────────────┐
│                    MÁQUINA LOCAL (Windows)                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Backend (Spring Boot)                                      │
│  Port: 8081                                                 │
│  ✓ Metrics endpoint: /actuator/prometheus                   │
│  ✓ Health check: /actuator/health                           │
│                                                             │
│  ↓↑ (JDBC Connection)                                       │
│                                                             │
│  Supabase PostgreSQL (Cloud)                                │
│  Host: aws-1-sa-east-1.pooler.supabase.com:5432            │
│  Database: postgres                                         │
│                                                             │
│  ↓↑ (opcional: Docker)                                      │
│                                                             │
│  Docker Compose (cuando se inicia con `docker-compose up`) │
│  ├─ servly-backend (8081)                                   │
│  ├─ prometheus (9090)     ← Scrapeá métricas del backend    │
│  └─ grafana (3000)        ← Visualiza Prometheus            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Flujo de Métricas Actual:
1. **Backend** expone métricas en `/actuator/prometheus`
2. **Prometheus** (localhost:9090) scrapeá cada 15 segundos
3. **Grafana** (localhost:3000) consulta Prometheus y muestra dashboards

---

## 3. DESPLIEGUE EN AWS EC2 - ARQUITECTURA OBJETIVO

### Stack en AWS:
```
┌──────────────────────────────────────────────────────────────────────────┐
│                          AWS ACCOUNT                                     │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌────────────────────────────────────────────────────────────┐         │
│  │  EC2 Instance (Linux)                                      │         │
│  │  Type: t3.medium (recomendado) o t3.small                  │         │
│  │  AMI: Ubuntu 22.04 LTS o Amazon Linux 2023                 │         │
│  │  Security Group: Puertos abiertos (ver abajo)              │         │
│  │  Storage: 30 GB (root volume)                              │         │
│  │                                                            │         │
│  │  ┌──────────────────────────────────────────────────────┐ │         │
│  │  │  Dentro de la instancia EC2:                        │ │         │
│  │  │                                                      │ │         │
│  │  │  Docker + Docker Compose                            │ │         │
│  │  │  ├─ servly-backend:8081                             │ │         │
│  │  │  │  ✓ Conecta a Supabase PostgreSQL (remoto)       │ │         │
│  │  │  │  ✓ Expone: /actuator/prometheus                 │ │         │
│  │  │  │                                                  │ │         │
│  │  │  ├─ prometheus:9090                                 │ │         │
│  │  │  │  ✓ Scrapeá http://servly-backend:8081/actuator │ │         │
│  │  │  │  ✓ Almacena datos en volumen local              │ │         │
│  │  │  │                                                  │ │         │
│  │  │  └─ grafana:3000                                    │ │         │
│  │  │     ✓ Datasource: Prometheus                        │ │         │
│  │  │     ✓ Dashboards: servly-dashboard.json             │ │         │
│  │  └──────────────────────────────────────────────────────┘ │         │
│  │                                                            │         │
│  │  ┌────────────────────────────────────────────────────┐   │         │
│  │  │ Security Considerations:                           │   │         │
│  │  │ • Prometheus protegido (solo acceso local o VPN)   │   │         │
│  │  │ • Grafana con contraseña (no admin/admin)          │   │         │
│  │  │ • Backend con JWT + OAuth2 (se mantiene igual)     │   │         │
│  │  │ • Supabase como BD (no se migra, se mantiene)      │   │         │
│  │  └────────────────────────────────────────────────────┘   │         │
│  │                                                            │         │
│  └────────────────────────────────────────────────────────────┘         │
│                                                                          │
│  ↓ (conexión JDBC/SSL)                                                  │
│                                                                          │
│  Supabase PostgreSQL (externo)                                          │
│  ✓ Se mantiene igual, NO se migra                                       │
│  ✓ URL: aws-1-sa-east-1.pooler.supabase.com:5432                       │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│  Frontend (Angular)                                                      │
│  Deployado en: CloudFront + S3 o EC2 adicional en puerto 4200            │
│  Conecta a: Backend en http://ec2-public-ip:8081                         │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 4. PUERTOS Y SEGURIDAD EN AWS

### Security Group (Inbound Rules):
```
┌─────────┬──────────────┬───────────────┬─────────────────────────────┐
│ Protocol│ Port         │ Source        │ Propósito                   │
├─────────┼──────────────┼───────────────┼─────────────────────────────┤
│ TCP     │ 22           │ Your IP       │ SSH (acceso administrativo) │
│ TCP     │ 8081         │ 0.0.0.0/0     │ Backend API (público)       │
│ TCP     │ 3000         │ Your IP/VPN   │ Grafana (privado)           │
│ TCP     │ 9090         │ Your IP/VPN   │ Prometheus (privado)        │
│ TCP     │ 4200         │ 0.0.0.0/0     │ Frontend (si está en EC2)   │
└─────────┴──────────────┴───────────────┴─────────────────────────────┘
```

### Variables de Entorno en EC2:
El archivo `application.properties` se copia tal cual del proyecto local. Las credenciales sensibles se pueden:
- **Opción A**: Mantener en application.properties (menos seguro)
- **Opción B**: Usar variables de entorno en el docker-compose.yml (recomendado)
- **Opción C**: Usar AWS Secrets Manager

---

## 5. FLUJO DE DESPLIEGUE PASO A PASO (QUÉ HACER)

### FASE 1: Preparación en Local
1. Compilar el proyecto: `./gradlew clean build -x test`
2. Verificar que application.properties tenga credenciales Supabase correctas
3. Verificar que docker-compose.yml está en la raíz del proyecto

### FASE 2: Crear Instancia EC2
1. Crear instancia Ubuntu 22.04 LTS (t3.medium mínimo)
2. Configurar Security Group con puertos (22, 8081, 3000, 9090)
3. Crear/usar par de claves SSH
4. Asignar Elastic IP (opcional pero recomendado)

### FASE 3: Configurar EC2
1. Conectar vía SSH
2. Actualizar sistema: `sudo apt update && sudo apt upgrade -y`
3. Instalar Docker y Docker Compose
4. Clonar repositorio del proyecto desde GitHub
5. Navegar a raíz del proyecto

### FASE 4: Configurar Variables en Docker
1. Crear archivo `.env` en la raíz con:
   ```
   DB_URL=jdbc:postgresql://aws-1-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require
   DB_USER=postgres.wglrjiiljnzemxrczwmy
   DB_PASSWORD=oSsZofkDxg46OsWJ
   JWT_SECRET=kfTX9fz50vunotayU072fDiAHF1J9R30FdPMV19s+0om/41RUoS6L1Lf4LROUc1XR2eC4EFyCb4zJzMb/2oN/w==
   GRAFANA_ADMIN_PASSWORD=TuContraseñaSegura
   ```
2. Actualizar docker-compose.yml para usar estas variables

### FASE 5: Desplegar con Docker Compose
1. `docker-compose up --build -d`
2. Esperar a que se inicien los 3 servicios
3. Verificar logs: `docker-compose logs -f`

### FASE 6: Verificar Despliegue
1. **Backend**: `curl http://ec2-public-ip:8081/actuator/health`
2. **Prometheus**: `curl http://ec2-public-ip:9090` (desde dentro de EC2)
3. **Grafana**: Acceder a `http://ec2-public-ip:3000` desde navegador
4. **Métricas**: Verificar en Prometheus que hay datos de `servly-backend`

---

## 6. CONSIDERACIONES ESPECIALES

### Base de Datos (Supabase)
- **NO se migra a EC2**: Se mantiene en Supabase (AWS)
- **Conexión**: El backend en EC2 se conecta por TCP a Supabase
- **Ventaja**: Escalabilidad, backups automáticos, mantenimiento by Supabase

### Almacenamiento de Métricas en Prometheus
- Los datos se guardan en un volumen Docker local en EC2
- **Persistencia**: Se pierde si se borra el volumen
- **Solución**: Usar AWS EBS o backups periódicos

### SSL/HTTPS
- **En desarrollo**: HTTP (8081)
- **En producción**: Agregar Nginx como reverse proxy con certificado SSL (Let's Encrypt)

### Monitoreo del Despliegue
- Ver logs en tiempo real: `docker-compose logs -f servly-backend`
- Ver métricas en vivo: Grafana (http://ec2-ip:3000)
- Ver health check: Backend expone `/actuator/health`

---

## 7. DOCKERFILE Y DOCKER-COMPOSE ACTUALES

### Dockerfile (Multistage - Build + Runtime)
```dockerfile
# Stage 1: Build
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

### Docker-compose.yml (Actual - Funcional Localmente)
```yaml
version: '3.8'
services:
  servly-backend:
    build: .
    ports:
      - "8081:8081"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://aws-1-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require
      SPRING_DATASOURCE_USERNAME: postgres.wglrjiiljnzemxrczwmy
      SPRING_DATASOURCE_PASSWORD: oSsZofkDxg46OsWJ
    depends_on:
      - prometheus
    networks:
      - monitoring

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
    networks:
      - monitoring

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    volumes:
      - ./grafana/provisioning:/etc/grafana/provisioning
      - grafana_data:/var/lib/grafana
    networks:
      - monitoring

volumes:
  prometheus_data:
  grafana_data:

networks:
  monitoring:
```

---

## 8. ARCHIVOS CRÍTICOS PARA EL DESPLIEGUE

```
servly/
├── Dockerfile                          ✓ Build de la app
├── docker-compose.yml                  ✓ Orquestación de contenedores
├── build.gradle                        ✓ Dependencias del proyecto
├── src/main/resources/
│   ├── application.properties           ✓ Configuración (BD, JWT, OAuth2, Actuator)
│   ├── openapi.yaml                     ✓ Documentación Swagger
│   └── ...
├── prometheus.yml                       ✓ Configuración de scrape
├── grafana/
│   ├── dashboards/
│   │   └── servly-dashboard.json       ✓ Dashboard con métricas
│   └── provisioning/
│       ├── dashboards/
│       │   └── dashboards.yml          ✓ Auto-provisión dashboards
│       └── datasources/
│           └── prometheus.yml          ✓ Datasource automático
└── ...
```

---

## 9. ENDPOINTS CLAVE DESPUÉS DEL DESPLIEGUE

```
Backend API:
  POST   /api/auth/login                          → Autenticación
  GET    /api/items                               → Listar items (con paginación)
  POST   /api/items                               → Crear item
  GET    /api/categories                          → Listar categorías
  POST   /api/categories                          → Crear categoría
  GET    /api/admin/**                            → Endpoints de admin

Observability:
  GET    /actuator/health                         → Health check
  GET    /actuator/prometheus                     → Métricas Prometheus
  GET    /actuator/metrics                        → Listado de métricas
  
Prometheus:
  GET    http://ec2-ip:9090                       → UI Prometheus
  
Grafana:
  GET    http://ec2-ip:3000                       → Dashboards
```

---

## 10. TIMELINE ESTIMADO

| Fase | Duración | Actividad |
|------|----------|-----------|
| Preparación Local | 5-10 min | Compilar, verificar archivos |
| Crear EC2 + VPC | 10-15 min | Creación de instancia en AWS |
| Configurar EC2 | 15-20 min | SSH, Docker, Docker Compose |
| Clonar Repo | 2-5 min | Git clone del proyecto |
| Desplegar | 10-15 min | `docker-compose up --build` |
| Verificación | 5-10 min | Health checks y Grafana |
| **Total** | **45-75 minutos** | **Despliegue completo** |

---

## 11. TROUBLESHOOTING COMÚN

| Problema | Causa | Solución |
|----------|-------|----------|
| Conexión BD rechazada | Supabase no responde | Verificar IP, credenciales, SSL |
| Puerto 8081 en uso | Otro proceso usa el puerto | `sudo lsof -i :8081` y matar |
| Prometheus no scrapeá | Configuración yml incorrecta | Validar `prometheus.yml` |
| Grafana vacío | Dashboard no importado | Importar `servly-dashboard.json` |
| Docker sin permisos | Usuario no en grupo docker | `sudo usermod -aG docker $USER` |

---

## 12. PRÓXIMOS PASOS DESPUÉS DEL DESPLIEGUE

1. **Monitoreo**: Verificar Grafana diariamente
2. **Backups**: Configurar backups de datos en Supabase
3. **SSL**: Agregar Nginx + Let's Encrypt para HTTPS
4. **Auto-scaling**: Si crece el tráfico, usar Auto Scaling Groups
5. **CI/CD**: Configurar GitHub Actions para despliegues automáticos
6. **Logging**: Implementar ELK (Elasticsearch, Logstash, Kibana)

---

## RESUMEN EJECUTIVO

**Servly** es una aplicación Spring Boot 3.x con BD en Supabase, que expone métricas vía Prometheus/Grafana.

**El despliegue en AWS EC2 consiste en:**
1. Instancia EC2 (Ubuntu/Amazon Linux)
2. Docker Compose con 3 servicios (Backend, Prometheus, Grafana)
3. Backend conecta a Supabase (BD se mantiene externa)
4. Métricas se recolectan, almacenan y visualizan localmente

**Duración total**: ~1 hora desde cero
**Complejidad**: Media (requiere conocimiento de Docker, AWS, Linux)
**Costo estimado**: $10-20/mes (t3.small) + Supabase (~$5-25/mes)

