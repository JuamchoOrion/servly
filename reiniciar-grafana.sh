#!/bin/bash

# Reiniciar solo Grafana para que cargue el dashboard
docker compose restart grafana

echo "Grafana reiniciado. Espera 10 segundos..."
sleep 10

# Verificar que Grafana está arriba
docker compose ps grafana

echo "Accede a http://localhost:3000 (o tu IP:3000)"
echo "Usuario: admin"
echo "Contraseña: admin"

