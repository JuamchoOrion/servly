# Script para generar datos en Grafana

Write-Host "🔍 Verificando que el backend responde..." -ForegroundColor Cyan
$health = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -ErrorAction SilentlyContinue
if ($health.StatusCode -eq 200) {
    Write-Host "✅ Backend respondiendo correctamente" -ForegroundColor Green
} else {
    Write-Host "❌ Backend no responde" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "🔍 Verificando que Prometheus ve el backend..." -ForegroundColor Cyan
try {
    $metrics = Invoke-WebRequest -Uri "http://localhost:8081/actuator/prometheus" -ErrorAction SilentlyContinue
    if ($metrics.StatusCode -eq 200) {
        Write-Host "✅ Prometheus puede leer métricas" -ForegroundColor Green
        Write-Host ""
        Write-Host "📊 Primeras líneas de métricas:" -ForegroundColor Yellow
        $metrics.Content | Select-Object -First 20
    }
} catch {
    Write-Host "❌ Error accediendo a métricas" -ForegroundColor Red
}

Write-Host ""
Write-Host "📱 Acceso a herramientas:" -ForegroundColor Cyan
Write-Host "   Grafana:    http://localhost:3000 (admin/admin)" -ForegroundColor Green
Write-Host "   Prometheus: http://localhost:9090" -ForegroundColor Green
Write-Host "   Backend:    http://localhost:8081" -ForegroundColor Green
Write-Host ""
Write-Host "💡 Si Grafana está vacío:" -ForegroundColor Yellow
Write-Host "   1. Ve a Grafana → + → Dashboard → New" -ForegroundColor Yellow
Write-Host "   2. Add panel → Query" -ForegroundColor Yellow
Write-Host "   3. Métrica: http_requests_total" -ForegroundColor Yellow
Write-Host "   4. Ctrl + Shift + Enter" -ForegroundColor Yellow

