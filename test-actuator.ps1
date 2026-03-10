# Script para probar endpoints de Actuator
# Espera a que el server esté listo (máximo 60 segundos)

Write-Host "⏳ Esperando que el servidor inicie..." -ForegroundColor Yellow
$maxAttempts = 60
$attempt = 0

while ($attempt -lt $maxAttempts) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -ErrorAction Stop
        Write-Host "✅ Servidor activo!" -ForegroundColor Green
        break
    }
    catch {
        $attempt++
        Write-Host "." -NoNewline
        Start-Sleep -Seconds 1
    }
}

if ($attempt -eq $maxAttempts) {
    Write-Host "`n❌ Timeout: El servidor no respondió en 60 segundos" -ForegroundColor Red
    exit 1
}

Write-Host "`n"
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PROBANDO ENDPOINTS DE ACTUATOR" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Health Check
Write-Host "`n📊 Health Check:" -ForegroundColor Yellow
try {
    $health = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" | ConvertFrom-Json
    Write-Host ($health | ConvertTo-Json -Depth 3)
}
catch {
    Write-Host "❌ Error: $_" -ForegroundColor Red
}

# Prometheus Metrics
Write-Host "`n📈 Prometheus Metrics (primeros 20 registros):" -ForegroundColor Yellow
try {
    $metrics = Invoke-WebRequest -Uri "http://localhost:8081/actuator/prometheus" -UseBasicParsing
    $metricLines = $metrics.Content -split "`n" | Where-Object { $_ -and -not $_.StartsWith("#") } | Select-Object -First 20
    $metricLines | ForEach-Object { Write-Host $_ }
    Write-Host "`n✅ Métricas disponibles" -ForegroundColor Green
}
catch {
    Write-Host "❌ Error: $_" -ForegroundColor Red
}

# Resumen
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "✅ SERVIDOR INICIADO CORRECTAMENTE" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Backend:  http://localhost:8081" -ForegroundColor Cyan
Write-Host "Health:   http://localhost:8081/actuator/health" -ForegroundColor Cyan
Write-Host "Metrics:  http://localhost:8081/actuator/prometheus" -ForegroundColor Cyan

