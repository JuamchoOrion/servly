════════════════════════════════════════════════════════════════════════════════
                           🚀 SERVLY + GRAFANA
                      CÓMO INICIAR Y FUNCIONAMIENTO
════════════════════════════════════════════════════════════════════════════════

## ⚡ COMANDO PARA INICIAR TODO

```powershell
cd "C:\Users\ramir\Documents\7mo Semestre\Ing de software III\servly"
docker-compose up --build
```

Espera 3-5 minutos mientras compila.

════════════════════════════════════════════════════════════════════════════════

## 🎯 QUÉ SUCEDE

✅ Se compila tu código Java (build.gradle)
✅ Se levanta PostgreSQL (base de datos)
✅ Se levanta tu Backend en puerto 8081
✅ Se levanta Prometheus en puerto 9090
✅ Se levanta Grafana en puerto 3000

TODO EN UN COMANDO. SIMULTANEAMENTE.

════════════════════════════════════════════════════════════════════════════════

## 🌐 CUANDO ESTÉ LISTO

```
http://localhost:3000
usuario: admin
contraseña: admin
```

Verifica que Prometheus vea tu backend:
```
http://localhost:9090
Status → Targets → "servly-backend" debe estar UP
```

════════════════════════════════════════════════════════════════════════════════

## 📊 CREAR TU PRIMER GRÁFICO EN GRAFANA

1. Haz clic en **+** (lado izquierdo)
2. Selecciona **Dashboard** → **New**
3. Haz clic en **Add panel** → **Query**
4. En el campo **Metrics**, copia:
   ```
   histogram_quantile(0.95, auth_login_duration_seconds_max)
   ```
5. Presiona **Ctrl + Shift + Enter**
6. Haz clic en **Save** y nombra tu dashboard

════════════════════════════════════════════════════════════════════════════════

## 🛑 PARA DETENER TODO

```powershell
docker-compose down
```

════════════════════════════════════════════════════════════════════════════════

## 🚀 PARA FUTURO DEPLOY EN AWS EC2

```bash
# En tu EC2 Linux
docker-compose up -d
```

**MISMO COMANDO. NO CAMBIES NADA.**

La razón: Todo está en Docker, por lo que funciona idéntico en tu laptop y en AWS.

════════════════════════════════════════════════════════════════════════════════

## 📝 CAMBIOS QUE HICIMOS

✅ docker-compose.yml       → Reescrito para incluir Backend
✅ prometheus.yml          → Apunta a servly-app:8081
✅ SecurityConfig.java     → Permite /actuator/prometheus
✅ application.yml         → Configurado para Prometheus
✅ build.gradle            → Ya tiene dependencias necesarias

════════════════════════════════════════════════════════════════════════════════

## 📱 URLS IMPORTANTES

| Servicio | URL |
|----------|-----|
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| Backend | http://localhost:8081 |
| Métricas | http://localhost:8081/actuator/prometheus |
| Health Check | http://localhost:8081/actuator/health |

════════════════════════════════════════════════════════════════════════════════

## ❓ PROBLEMAS COMUNES

**"Compilation taking too long"**
→ Normal, primera compilación tarda 3-5 minutos. Espera.

**"Prometheus targets showing DOWN"**
→ El backend aún se está levantando. Espera más.

**"No data in Grafana"**
→ Haz un login en tu app para generar métricas.
→ Espera 1-2 minutos.
→ Refresca la página (F5).

**"Port 3000 already in use"**
→ docker-compose down
→ Espera 10 segundos
→ docker-compose up --build

════════════════════════════════════════════════════════════════════════════════

## ✨ LISTO

Solo ejecuta:
```powershell
docker-compose up --build
```

Y accede a:
```
http://localhost:3000
```

**DONE** ✅

════════════════════════════════════════════════════════════════════════════════

