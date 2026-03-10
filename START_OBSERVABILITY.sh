#!/bin/bash

# Script para iniciar Prometheus y Grafana localmente (sin Docker)
# mientras ejecutas Spring Boot con gradlew bootRun

echo "🚀 Iniciando Prometheus..."
# Descarga y ejecuta Prometheus (si no lo tienes, descárgalo desde https://prometheus.io/download/)
# Ejemplo para Windows: descarga prometheus.exe desde el sitio oficial
# Luego en PowerShell:
# .\prometheus.exe --config.file=prometheus.yml

echo "🚀 Iniciando Grafana..."
# Para Windows, descarga Grafana desde https://grafana.com/grafana/download
# Luego ejecuta: grafana-server.exe

echo "✅ Tanto Prometheus como Grafana deben estar corriendo"
echo "   - Prometheus: http://localhost:9090"
echo "   - Grafana: http://localhost:3000"
echo ""
echo "En otra terminal, inicia tu Spring Boot:"
echo "   cd /ruta/a/servly && ./gradlew bootRun"

