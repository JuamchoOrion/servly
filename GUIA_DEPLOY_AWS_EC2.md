# 🚀 Guía Completa: Deploy en AWS EC2 y Testing

## 1️⃣ PREPARAR LA INSTANCIA EC2

### 1.1 Crear instancia EC2
```bash
# En AWS Console:
# - Type: t2.micro (free tier) o t2.small
# - AMI: Amazon Linux 2 o Ubuntu 22.04
# - Storage: 20-30 GB
# - Security Group: Abrir puertos:
#   - 22 (SSH)
#   - 8080 (Aplicación Spring)
#   - 5432 (PostgreSQL si es local)
```

### 1.2 Conectar a la instancia
```bash
# Desde Windows PowerShell
ssh -i "tu-key.pem" ec2-user@tu-ip-publica

# O si usas Ubuntu
ssh -i "tu-key.pem" ubuntu@tu-ip-publica
```

---

## 2️⃣ INSTALAR DEPENDENCIAS EN EC2

```bash
# Actualizar sistema
sudo yum update -y
# O si es Ubuntu:
sudo apt update && sudo apt upgrade -y

# Instalar Java 17
sudo yum install java-17-amazon-corretto -y
# O Ubuntu:
sudo apt install openjdk-17-jdk -y

# Verificar instalación
java -version

# Instalar Maven (opcional, si necesitas compilar)
sudo yum install maven -y
```

---

## 3️⃣ DESPLEGAR LA APLICACIÓN

### Opción A: Subir el JAR compilado

```bash
# En tu máquina local, compilar:
./gradlew clean build

# Copiar JAR a EC2
scp -i "tu-key.pem" build/libs/servly-0.0.1-SNAPSHOT.jar ec2-user@tu-ip:/home/ec2-user/

# En EC2, ejecutar:
ssh -i "tu-key.pem" ec2-user@tu-ip

# Ejecutar la aplicación
java -jar servly-0.0.1-SNAPSHOT.jar
```

### Opción B: Usar el código fuente (con Git)

```bash
# En EC2:
sudo yum install git -y

# Clonar el repositorio
git clone https://github.com/tu-usuario/servly.git
cd servly

# Compilar y ejecutar
./gradlew bootRun
```

---

## 4️⃣ EJECUTAR EN BACKGROUND (Usando nohup)

```bash
# En EC2:
nohup java -jar servly-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# Ver los logs en tiempo real:
tail -f app.log

# Verificar que está corriendo:
ps aux | grep java
```

---

## 5️⃣ EJECUTAR COMO SERVICIO SYSTEMD (Recomendado)

```bash
# Crear archivo de servicio
sudo nano /etc/systemd/system/servly.service
```

**Contenido del archivo:**
```ini
[Unit]
Description=Servly Spring Boot Application
After=network.target

[Service]
Type=simple
User=ec2-user
ExecStart=/usr/bin/java -jar /home/ec2-user/servly-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=10
StandardOutput=append:/var/log/servly.log
StandardError=append:/var/log/servly.log

[Install]
WantedBy=multi-user.target
```

**Ejecutar el servicio:**
```bash
# Habilitar y iniciar
sudo systemctl daemon-reload
sudo systemctl enable servly
sudo systemctl start servly

# Ver estado
sudo systemctl status servly

# Ver logs
sudo tail -f /var/log/servly.log
```

---

## 6️⃣ CONFIGURAR BASE DE DATOS

### Opción A: PostgreSQL en RDS (Recomendado)
```bash
# En AWS Console -> RDS:
# - Engine: PostgreSQL
# - DB instance class: db.t3.micro (free tier)
# - Multi-AZ: No
# - Storage: 20 GB
# - Publicly accessible: Sí (para testing)

# Actualizar application.properties:
spring.datasource.url=jdbc:postgresql://tu-rds-endpoint:5432/servly
spring.datasource.username=postgres
spring.datasource.password=tu-contraseña
```

### Opción B: PostgreSQL en la misma EC2
```bash
# Instalar PostgreSQL
sudo yum install postgresql15-server -y

# Iniciar servicio
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Crear base de datos
sudo -u postgres psql
CREATE DATABASE servly;
CREATE USER servly_user WITH PASSWORD 'tu-contraseña';
GRANT ALL PRIVILEGES ON DATABASE servly TO servly_user;
\q
```

---

## 7️⃣ ACTUALIZAR URL EN EL ARCHIVO HTTP DE TESTING

**Cambiar en `test-forgot-password.http`:**

```http
# LOCAL (desarrollo)
@baseUrl = http://localhost:8080

# AWS EC2 (producción)
@baseUrl = http://tu-ip-publica-ec2:8080

# O si tienes dominio
@baseUrl = https://tu-dominio.com
```

---

## 8️⃣ PROBAR EL ENDPOINT FORGOT PASSWORD

### Usando IntelliJ HTTP Client:

```http
### Test en AWS EC2
POST http://tu-ip-publica-ec2:8080/api/auth/forgot-password
Content-Type: application/json

{
  "email": "ramiresj@gmail.com"
}
```

### Usando cURL desde terminal:

```bash
# En tu máquina local
curl -X POST http://tu-ip-publica-ec2:8080/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "ramiresj@gmail.com"
  }'
```

### Usando Postman:

1. **URL**: `http://tu-ip-publica-ec2:8080/api/auth/forgot-password`
2. **Método**: POST
3. **Headers**: 
   ```
   Content-Type: application/json
   ```
4. **Body** (raw JSON):
   ```json
   {
     "email": "ramiresj@gmail.com"
   }
   ```
5. **Click**: Send

---

## 9️⃣ OBTENER TU IP PÚBLICA DE EC2

```bash
# En AWS Console:
# EC2 -> Instances -> Tu instancia -> Details -> Public IPv4 address

# O desde terminal (si estás en la instancia):
curl http://169.254.169.254/latest/meta-data/public-ipv4
```

---

## 🔟 SOLUCIONAR PROBLEMAS

### ❌ Conexión rechazada (Connection refused)
```bash
# Verificar que la aplicación está ejecutándose
ps aux | grep java

# Ver logs
tail -f app.log

# Verificar puerto 8080 está escuchando
netstat -tlnp | grep 8080
```

### ❌ Error de base de datos
```bash
# Verificar conexión a PostgreSQL
psql -h tu-rds-endpoint -U postgres -d servly

# Ver logs de la aplicación
tail -f /var/log/servly.log
```

### ❌ Seguridad Group bloqueando puerto
```bash
# En AWS Console:
# EC2 -> Security Groups -> Tu grupo -> Inbound Rules
# Agregar regla:
# - Type: Custom TCP
# - Port Range: 8080
# - Source: 0.0.0.0/0 (o tu IP específica)
```

---

## 📊 VARIABLES DE ENTORNO ÚTILES

**Para ejecutar sin editar application.properties:**

```bash
java -jar servly-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:postgresql://tu-rds:5432/servly \
  --spring.datasource.username=postgres \
  --spring.datasource.password=tu-contraseña \
  --server.port=8080
```

---

## 📝 CHECKLIST ANTES DE DESPLEGAR

- ✅ Compilar localmente sin errores
- ✅ Crear instancia EC2 y security groups
- ✅ Instalar Java 17 en EC2
- ✅ Configurar base de datos (RDS o local)
- ✅ Subir JAR a EC2
- ✅ Ejecutar aplicación y ver logs
- ✅ Abrir puerto 8080 en security group
- ✅ Actualizar URL en test-forgot-password.http
- ✅ Probar endpoint desde tu máquina local
- ✅ Verificar email se envía correctamente

---

## 🎯 PRÓXIMOS PASOS

1. Configurar HTTPS con certificado SSL
2. Usar Route 53 para dominio personalizado
3. Configurar Load Balancer
4. Implementar CI/CD con GitHub Actions
5. Monitorear con CloudWatch


