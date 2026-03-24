#!/bin/bash
# Script de diagnóstico para Grafana + Prometheus en AWS

echo "=========================================="
echo "DIAGNÓSTICO GRAFANA + PROMETHEUS"
echo "=========================================="

# 1. Ver estado de contenedores
echo ""
echo "1️⃣  ESTADO DE CONTENEDORES:"
docker-compose ps
echo ""

# 2. Verificar que Prometheus tiene datos
echo "2️⃣  VERIFICAR DATOS EN PROMETHEUS:"
PROM_DATA=$(docker-compose exec -T prometheus curl -s http://localhost:9090/api/v1/query?query=up | grep -o '"value":\[\([^]]*\)\]' | head -1)
if [ ! -z "$PROM_DATA" ]; then
    echo "✅ Prometheus tiene datos"
    echo "   Respuesta: $PROM_DATA"
else
    echo "❌ Prometheus NO tiene datos"
fi
echo ""

# 3. Verificar que Grafana puede conectar a Prometheus
echo "3️⃣  VERIFICAR CONECTIVIDAD GRAFANA → PROMETHEUS:"
GRAFANA_TEST=$(docker-compose exec -T grafana curl -s -o /dev/null -w "%{http_code}" http://servly-prometheus:9090)
if [ "$GRAFANA_TEST" == "200" ]; then
    echo "✅ Grafana PUEDE conectar a Prometheus"
else
    echo "❌ Grafana NO puede conectar a Prometheus (código HTTP: $GRAFANA_TEST)"
fi
echo ""

# 4. Ver logs de Grafana
echo "4️⃣  ÚLTIMOS LOGS DE GRAFANA:"
docker-compose logs --tail 20 grafana | grep -i prometheus || echo "Sin logs recientes de Prometheus"
echo ""

# 5. Ver configuración de datasources en Grafana
echo "5️⃣  DATASOURCES CONFIGURADOS EN GRAFANA:"
docker-compose exec -T grafana curl -s -H "Authorization: Bearer $(docker-compose exec -T grafana cat /etc/environment | grep GF | head -1)" \
    http://localhost:3000/api/datasources 2>/dev/null | grep -o '"name":"[^"]*"' || echo "No se pudo obtener datasources"
echo ""

# 6. Ver volúmenes
echo "6️⃣  VOLÚMENES:"
docker volume ls | grep servly || echo "Sin volúmenes de servly"
echo ""

# 7. Verificar que el backend está enviando métricas
echo "7️⃣  VERIFICAR QUE BACKEND ENVÍA MÉTRICAS:"
BACKEND_METRICS=$(docker-compose exec -T servly-app curl -s http://localhost:8081/actuator/prometheus | head -5)
if [ ! -z "$BACKEND_METRICS" ]; then
    echo "✅ Backend está exponiendo métricas"
    echo "   Primeras líneas:"
    echo "$BACKEND_METRICS"
else
    echo "❌ Backend NO está exponiendo métricas"
fi
echo ""

echo "=========================================="
echo "FIN DEL DIAGNÓSTICO"
echo "=========================================="

