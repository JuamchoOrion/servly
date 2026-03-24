#!/bin/bash

# Script para arreglar el dashboard en Grafana
cd ~/app

# 1. Parar y eliminar el contenedor de Grafana (pero no los volúmenes)
echo "Deteniendo Grafana..."
docker compose stop grafana

# 2. Verificar que el archivo del dashboard está en el host
echo "Verificando archivo del dashboard..."
if [ ! -f "./grafana/dashboards/servly-dashboard.json" ]; then
    echo "ERROR: El archivo del dashboard no existe en ./grafana/dashboards/servly-dashboard.json"
    exit 1
fi

echo "Dashboard encontrado ✓"

# 3. Copiar el archivo directamente al volumen de Grafana
echo "Copiando dashboard al volumen..."
docker compose start grafana
sleep 5

# 4. Copiar el archivo al contenedor
docker cp ./grafana/dashboards/servly-dashboard.json servly-grafana:/var/lib/grafana/dashboards/

echo "Dashboard copiado ✓"

# 5. Verificar que está ahí
echo "Verificando en el contenedor..."
docker exec servly-grafana ls -la /var/lib/grafana/dashboards/

# 6. Esperar a que Grafana se reinicie
echo "Esperando que Grafana procese el dashboard..."
sleep 10

# 7. Reiniciar Grafana para que cargue el dashboard
echo "Reiniciando Grafana..."
docker compose restart grafana

echo ""
echo "✓ Completado. Accede a http://tu-ip:3000"
echo "  - Usuario: admin"
echo "  - Contraseña: admin"
echo ""
echo "El dashboard debería aparecer en: Dashboards → Browse → Servly"

