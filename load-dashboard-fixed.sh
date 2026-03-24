#!/bin/bash

# Script para crear dashboard en Grafana (funciona sin API key)
cd ~/app

echo "🔄 Cargando dashboard en Grafana..."

# Crear el archivo JSON del dashboard
cat > /tmp/dashboard.json << 'EOF'
{
  "dashboard": {
    "title": "Servly - Observability Dashboard (ISO/IEC 25010)",
    "uid": "servly-dashboard",
    "version": 1,
    "timezone": "",
    "panels": [
      {
        "datasource": "Prometheus",
        "targets": [{"expr": "histogram_quantile(0.95, rate(auth_login_duration_seconds_bucket[5m])) * 1000"}],
        "title": "Login Duration (p95) - Threshold: 2000ms",
        "type": "timeseries",
        "gridPos": {"x": 0, "y": 0, "w": 12, "h": 8},
        "id": 1
      },
      {
        "datasource": "Prometheus",
        "targets": [{"expr": "(auth_login_success_total / auth_login_total) * 100"}],
        "title": "Login Success Rate by Role - Threshold: >95%",
        "type": "gauge",
        "gridPos": {"x": 12, "y": 0, "w": 12, "h": 8},
        "id": 2
      },
      {
        "datasource": "Prometheus",
        "targets": [{"expr": "(auth_2fa_codes_expired_total / auth_2fa_codes_generated_total) * 100"}],
        "title": "2FA Code Expiration Rate - Threshold: <10%",
        "type": "gauge",
        "gridPos": {"x": 0, "y": 8, "w": 12, "h": 8},
        "id": 3
      },
      {
        "datasource": "Prometheus",
        "targets": [{"expr": "histogram_quantile(0.95, rate(inventory_item_creation_duration_seconds_bucket[5m])) * 1000"}],
        "title": "Item Creation Duration (p95) - Threshold: 500ms",
        "type": "timeseries",
        "gridPos": {"x": 12, "y": 8, "w": 12, "h": 8},
        "id": 4
      },
      {
        "datasource": "Prometheus",
        "targets": [{"expr": "(inventory_category_creation_success_total / inventory_category_creation_total) * 100"}],
        "title": "Category Creation Success Rate - Threshold: >90%",
        "type": "gauge",
        "gridPos": {"x": 0, "y": 16, "w": 12, "h": 8},
        "id": 5
      },
      {
        "datasource": "Prometheus",
        "targets": [{"expr": "histogram_quantile(0.95, rate(inventory_items_paginated_query_duration_seconds_bucket[5m])) * 1000"}],
        "title": "Paginated Query Duration (p95) - Threshold: 150ms",
        "type": "timeseries",
        "gridPos": {"x": 12, "y": 16, "w": 12, "h": 8},
        "id": 6
      },
      {
        "datasource": "Prometheus",
        "targets": [{"expr": "histogram_quantile(0.95, rate(inventory_category_creation_duration_seconds_bucket[5m])) * 1000"}],
        "title": "Category Creation Duration (p95) - Threshold: 300ms",
        "type": "timeseries",
        "gridPos": {"x": 0, "y": 24, "w": 12, "h": 8},
        "id": 7
      },
      {
        "datasource": "Prometheus",
        "targets": [{"expr": "(inventory_category_toggle_success_total / inventory_category_toggle_total) * 100"}],
        "title": "Category Toggle Success Rate - Threshold: >98%",
        "type": "gauge",
        "gridPos": {"x": 12, "y": 24, "w": 12, "h": 8},
        "id": 8
      }
    ],
    "schemaVersion": 30,
    "style": "dark",
    "tags": ["servly", "observability", "iso-25010"]
  },
  "overwrite": true
}
EOF

echo "📤 Enviando dashboard a Grafana (sin autenticación)..."

# Intentar sin autenticación primero
RESPONSE=$(curl -s -X POST http://localhost:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @/tmp/dashboard.json)

echo "$RESPONSE"

# Si falla, generar una API key y reintentar
if echo "$RESPONSE" | grep -q "Invalid API key\|Unauthorized"; then
  echo ""
  echo "⚙️  Generando API key en Grafana..."

  # Crear una API key usando curl con autenticación básica
  API_KEY=$(curl -s -X POST http://localhost:3000/api/auth/keys \
    -H "Content-Type: application/json" \
    -u "admin:admin" \
    -d '{"name":"dashboard-loader","role":"Admin"}' | grep -o '"key":"[^"]*' | cut -d'"' -f4)

  if [ -n "$API_KEY" ]; then
    echo "✅ API key generada: $API_KEY"
    echo ""
    echo "📤 Reintentando con API key..."

    curl -X POST http://localhost:3000/api/dashboards/db \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $API_KEY" \
      -d @/tmp/dashboard.json
  fi
fi

echo ""
echo "✅ Proceso completado"
echo ""
echo "Accede a: http://localhost:3000"
echo "Usuario: admin"
echo "Contraseña: admin"

