# 📚 Guía Completa: Desplegar Servly en AWS EC2 con Prometheus y Grafana

## Índice
1. [Prerequisitos](#prerequisitos)
2. [Crear Instancia EC2](#crear-instancia-ec2)
3. [Conectarse a EC2](#conectarse-a-ec2)
4. [Instalar Docker](#instalar-docker)
5. [Preparar el Código](#preparar-el-código)
6. [Configurar prometheus.yml](#configurar-prometheusyml)
7. [Desplegar con Docker Compose](#desplegar-con-docker-compose)
8. [Acceder a las Aplicaciones](#acceder-a-las-aplicaciones)
9. [Comandos Útiles](#comandos-útiles)
10. [Troubleshooting](#troubleshooting)

---

## Prerequisitos

- Cuenta de AWS (puede ser free tier)
- Archivos de tu proyecto Servly listos
- Terminal/Git Bash instalado en tu máquina local
- Un archivo `.pem` para SSH (se descarga al crear la instancia)

---

## Crear Instancia EC2

### Paso 1.1: Ir a AWS Console

1. Abre https://console.aws.amazon.com
2. Busca "EC2" en la barra de búsqueda
3. Click en "Instances"

### Paso 1.2: Launch Instance

1. Click en "Launch instances"
2. **Name**: Escribe "servly-app"
3. **AMI**: Selecciona "Ubuntu Server 22.04 LTS" (free tier)
4. **Instance Type**: Selecciona "t2.micro" (free tier)
5. **Key pair**: 
   - Click en "Create new key pair"
   - Name: "servly-key"
   - Type: RSA
   - Format: .pem (para Linux/Mac) o .ppk (para Windows con PuTTY)
   - Click "Create key pair" → Se descargará automáticamente
6. **Network settings**:
   - Create security group ✓
   - Allow SSH traffic from anywhere ✓
   - Click "Add security group rule"
   - Type: Custom TCP
   - Port: 8081
   - Source: 0.0.0.0/0 (Anywhere)
   - Click "Add security group rule"
   - Type: Custom TCP
   - Port: 9090
   - Source: 0.0.0.0/0
   - Click "Add security group rule"
   - Type: Custom TCP
   - Port: 3000
   - Source: 0.0.0.0/0
7. **Storage**: Dejar default (20 GB)
8. Click "Launch instance"

### Paso 1.3: Esperar a que inicie

- Espera ~2 minutos a que la instancia esté "running"
- Copia la **IP pública** (Public IPv4)

---

## Conectarse a EC2

### En Windows (PowerShell):

```powershell
# Navega a la carpeta donde está tu archivo .pem
cd C:\Users\<tu-usuario>\Downloads

# Conectarse a la instancia
ssh -i servly-key.pem ubuntu@<TU-IP-PUBLICA-EC2>

# Ejemplo:
# ssh -i servly-key.pem ubuntu@54.123.45.678
```

### En Mac/Linux:

```bash
# Cambiar permisos del archivo .pem
chmod 400 ~/Downloads/servly-key.pem

# Conectarse
ssh -i ~/Downloads/servly-key.pem ubuntu@<TU-IP-PUBLICA-EC2>
```

Cuando pida confirmación, escribe: `yes` y presiona Enter

---

## Instalar Docker

Una vez conectado a la EC2, ejecuta estos comandos:

```bash
# Actualizar sistema
sudo apt update && sudo apt upgrade -y

# Instalar Docker
sudo apt install -y docker.io docker-compose

# Agregar tu usuario al grupo docker (para no usar sudo)
sudo usermod -aG docker ubuntu

# Salir y volver a conectar
exit
```

Luego reconéctate:
```bash
ssh -i servly-key.pem ubuntu@<TU-IP-PUBLICA-EC2>
```

Verifica que Docker está instalado:
```bash
docker --version
docker-compose --version
```

---

## Preparar el Código

### Opción A: Clonar de GitHub (Recomendado)

```bash
# Instalar Git
sudo apt install -y git

# Clonar tu repositorio
git clone https://github.com/<tu-usuario>/servly.git
cd servly
```

### Opción B: Subir archivos con SCP

En tu máquina local:
```bash
# Windows (PowerShell)
scp -i C:\Users\<usuario>\Downloads\servly-key.pem -r C:\Users\<usuario>\Documents\servly ubuntu@<IP-EC2>:~/

# Mac/Linux
scp -i ~/Downloads/servly-key.pem -r ~/Documents/servly ubuntu@<IP-EC2>:~/
```

Luego en la EC2:
```bash
cd servly
```

---

## Configurar prometheus.yml

El `prometheus.yml` debe tener el `servly-app` apuntando al nombre del contenedor (no a `localhost`):

```bash
# Editar el archivo
nano prometheus.yml
```

Busca la sección de `scrape_configs` y asegúrate que diga:

```yaml
scrape_configs:
  - job_name: 'servly-backend'
    static_configs:
      - targets: ['servly-app:8081']  # ← Nombre del contenedor, no localhost
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    scrape_timeout: 10s
```

Presiona:
- `Ctrl + X`
- `Y` (para guardar)
- `Enter` (para confirmar)

---

## Desplegar con Docker Compose

En la carpeta `servly` de la EC2:

```bash
# Ver el contenido
ls -la

# Debe mostrar: docker-compose.yml, Dockerfile, prometheus.yml, etc.

# Construir e iniciar los contenedores
docker-compose up -d --build

# Esperar ~1-2 minutos mientras compila el backend

# Verificar que todo esté corriendo
docker ps

# Debe mostrar 3 contenedores:
# - servly-backend (puerto 8081)
# - servly-prometheus (puerto 9090)
# - servly-grafana (puerto 3000)
```

Si ves error en la compilación:
```bash
# Ver logs del backend
docker logs servly-backend

# O reintentar
docker-compose down
docker-compose up -d --build
```

---

## Acceder a las Aplicaciones

Abre en tu navegador (reemplaza `<IP-EC2>` con tu IP pública):

| Aplicación | URL |
|-----------|-----|
| Backend Health | http://`<IP-EC2>`:8081/actuator/health |
| Prometheus | http://`<IP-EC2>`:9090 |
| Grafana | http://`<IP-EC2>`:3000 |

**Grafana Login:**
- Usuario: `admin`
- Contraseña: `admin`

---

## Comandos Útiles

```bash
# Ver estado de contenedores
docker ps -a

# Ver logs en tiempo real
docker logs -f servly-backend
docker logs -f servly-prometheus
docker logs -f servly-grafana

# Detener todo
docker-compose down

# Reiniciar un contenedor específico
docker-compose restart servly-backend

# Actualizar código (si es desde GitHub)
git pull origin main
docker-compose up -d --build

# Ver uso de recursos
docker stats

# Acceder a la terminal del contenedor
docker exec -it servly-backend bash
```

---

## Troubleshooting

### ❌ Puerto ya está en uso

```bash
# Cambiar puerto en docker-compose.yml
# Ejemplo: 8081:8081 → 8082:8081
nano docker-compose.yml

# Cambiar la primera línea de puertos y guardar
docker-compose down
docker-compose up -d --build
```

### ❌ Backend no responde

```bash
# Ver logs
docker logs servly-backend

# Verificar que Supabase está accesible
curl https://aws-1-sa-east-1.pooler.supabase.com:5432/

# Si falla, revisar variables de entorno en docker-compose.yml
```

### ❌ Prometheus no ve métricas

```bash
# En Prometheus, ve a http://<IP>:9090/targets
# Si dice "down", revisar que el target sea 'servly-app:8081'

# Verificar que el backend expone /actuator/prometheus
curl http://servly-app:8081/actuator/prometheus

# O desde la EC2:
docker exec servly-backend curl http://localhost:8081/actuator/prometheus
```

### ❌ Grafana no muestra datos

1. Ve a http://`<IP>`:3000/datasources
2. Verifica que Prometheus esté conectado
3. Si falta, agrégalo:
   - Name: Prometheus
   - URL: http://servly-prometheus:9090
   - Save & Test

4. Recarga el dashboard

### ❌ Error: "unable to obtain isolated JDBC connection"

Esto significa que la BD (Supabase) está rechazando conexiones. Verifica:

```bash
# En docker-compose.yml, verifica las credenciales:
# SPRING_DATASOURCE_URL
# SPRING_DATASOURCE_USERNAME
# SPRING_DATASOURCE_PASSWORD

# Deben coincidir con tu BD de Supabase
```

---

## Resumen Final

| Paso | Comando |
|------|---------|
| 1. SSH | `ssh -i key.pem ubuntu@IP` |
| 2. Instalar Docker | `sudo apt install docker.io docker-compose` |
| 3. Clonar proyecto | `git clone ...` |
| 4. Revisar prometheus.yml | `nano prometheus.yml` |
| 5. Desplegar | `docker-compose up -d --build` |
| 6. Verificar | `docker ps` |
| 7. Acceder | http://IP:3000 (Grafana) |

---

## URLs Finales

Una vez desplegado, tienes:

- **Grafana**: http://`<IP-EC2>`:3000 (admin:admin)
- **Prometheus**: http://`<IP-EC2>`:9090
- **Backend API**: http://`<IP-EC2>`:8081
- **Health Check**: http://`<IP-EC2>`:8081/actuator/health

---

**Fecha**: March 10, 2026
**Status**: Ready for Production ✅

