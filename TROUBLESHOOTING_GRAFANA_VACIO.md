# TROUBLESHOOTING: GRAFANA VACÍO DESPUÉS DE DESPLEGAR EN AWS

## 🔍 PROBLEMA
Prometheus tiene datos pero Grafana está vacío después del despliegue en AWS EC2.

---

## ✅ SOLUCIONES (en orden de probabilidad)

### Solución 1: Reiniciar Grafana (La más común - 80% de casos)

**En AWS EC2, conectado vía SSH:**

```bash
# Detener Grafana
docker-compose stop grafana

# Esperar 3 segundos
sleep 3

# Reiniciar Grafana
docker-compose up -d grafana

# Esperar a que esté listo (unos 15 segundos)
sleep 15

# Ver logs
docker-compose logs grafana | tail -20
```

**Luego accede:** `http://ec2-ip:3000` (admin/admin)

---

### Solución 2: Verificar que Prometheus tiene datos

**En AWS EC2:**

```bash
# Entrar al contenedor de Prometheus
docker-compose exec prometheus bash

# Dentro del contenedor, ejecutar:
curl http://localhost:9090/api/v1/query?query=up

# Deberías ver algo como:
# {"status":"success","data":{"resultType":"vector","result":[...]}}
```

**Si ves error:**
- Prometheus no está recolectando datos
- Verifica que `prometheus.yml` apunta correctamente al backend

---

### Solución 3: Verificar conectividad Grafana → Prometheus

**En AWS EC2:**

```bash
# Entrar a Grafana
docker-compose exec grafana bash

# Dentro del contenedor, ejecutar:
curl http://servly-prometheus:9090

# Deberías ver la página HTML de Prometheus (sin errores)
```

**Si dice "Connection refused":**
- El servicio de Prometheus no se llama `servly-prometheus`
- Ejecuta `docker-compose ps` para ver el nombre exacto
- Actualiza: `grafana/provisioning/datasources/prometheus.yml`

---

### Solución 4: Verificar archivo datasource de Grafana

**En tu máquina local:**

```bash
cat grafana/provisioning/datasources/prometheus.yml
```

Debe decir exactamente:
```yaml
url: http://servly-prometheus:9090
```

No:
```yaml
url: http://prometheus:9090        # ❌ INCORRECTO
url: http://localhost:9090         # ❌ INCORRECTO
url: http://ec2-ip:9090           # ❌ INCORRECTO
```

---

### Solución 5: Limpiar y reconstruir Grafana (Nuclear option)

**En AWS EC2:**

```bash
# Detener todo
docker-compose down

# Eliminar volumen de Grafana
docker volume rm servly_grafana_data

# Reconstruir
docker-compose up --build -d

# Esperar 30 segundos
sleep 30

# Ver logs
docker-compose logs grafana
```

---

### Solución 6: Verificar que Backend está enviando métricas

**En AWS EC2:**

```bash
# Desde EC2, conectar al backend
docker-compose exec servly-app bash

# Dentro del contenedor:
curl http://localhost:8081/actuator/prometheus | head -20
```

Deberías ver líneas como:
```
# HELP auth_login_duration_seconds ...
# TYPE auth_login_duration_seconds histogram
auth_login_duration_seconds_bucket{...} 0
```

**Si no hay nada:**
- El backend no está generando métricas
- Intenta hacer login o crear una categoría primero para generar actividad

---

## 🚀 CHECKLIST RÁPIDO

Ejecuta esto **en AWS EC2** para verificar todo:

```bash
echo "1. ¿Prometheus está activo?"
docker-compose ps | grep prometheus

echo ""
echo "2. ¿Prometheus tiene datos?"
docker-compose exec prometheus curl -s http://localhost:9090/api/v1/query?query=up | head -20

echo ""
echo "3. ¿Grafana puede conectar a Prometheus?"
docker-compose exec grafana curl -s http://servly-prometheus:9090 | head -5

echo ""
echo "4. ¿Backend está enviando métricas?"
docker-compose exec servly-app curl -s http://localhost:8081/actuator/prometheus | wc -l

echo ""
echo "5. ¿Grafana tiene el datasource?"
docker-compose logs grafana | grep -i "datasource\|prometheus" | tail -5
```

---

## 📊 SI SIGUE VACÍO DESPUÉS DE TODO

Accede a **Grafana (http://ec2-ip:3000)** y:

1. Ve a **Configuration** → **Data Sources**
2. Busca "Prometheus"
3. Si no existe:
   - Click en "Add Data Source"
   - Type: Prometheus
   - URL: `http://servly-prometheus:9090`
   - Save & Test
4. Ve a **Dashboards** → busca "servly"
5. Si no existe:
   - Click en "+" → "Import"
   - Paste JSON desde `grafana/dashboards/servly-dashboard.json`

---

## 🆘 SI AÚN NO FUNCIONA

Proporciona el output de estos comandos (ejecutados en AWS):

```bash
docker-compose ps
docker-compose logs grafana | tail -30
docker-compose logs prometheus | tail -30
docker-compose logs servly-app | tail -30
cat grafana/provisioning/datasources/prometheus.yml
docker volume ls
```

