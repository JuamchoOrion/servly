#!/usr/bin/env pwsh

Write-Host "🚀 Iniciando Servly Backend Server..."
Write-Host "==========================================="

$jar = "C:\Users\ramir\Documents\7mo Semestre\Ing de software III\servly\build\libs\servly-0.0.1-SNAPSHOT.jar"

if (-Not (Test-Path $jar)) {
    Write-Host "❌ JAR no encontrado en: $jar"
    exit 1
}

Write-Host "✓ JAR encontrado"
Write-Host "Ejecutando: java -jar $jar"
Write-Host ""

& java -jar $jar


