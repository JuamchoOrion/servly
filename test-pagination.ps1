param(
    [string]$Token = "eyJhbGciOiJIUzUxMiJ9.eyJwYXNzd29yZFZlcnNpb24iOjEsInN1YiI6InJhbWlyZXNqdWFuZGE0N0BnbWFpbC5jb20iLCJpYXQiOjE3NzI5NDM4OTEsImV4cCI6MTc3MzAzMDI5MX0.eputYlJKq1lKn-OdWEishbDemE0Y_m2zmIOOJZUOST9dYsbKG2lHKCWPFrg-bqrbInA2IGVdBn1PBFhNycDGRQ",
    [string]$BaseUrl = "http://localhost:8081"
)

$headers = @{
    "Authorization" = "Bearer $Token"
    "Content-Type" = "application/json"
}

function Test-Endpoint {
    param(
        [string]$Description,
        [string]$Endpoint
    )
    Write-Host ""
    Write-Host "$Description" -ForegroundColor Cyan
    Write-Host "GET $Endpoint" -ForegroundColor Yellow
    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl$Endpoint" -Headers $headers -Method Get
        Write-Host ($response | ConvertTo-Json) -ForegroundColor Green
    }
    catch {
        Write-Host "ERROR: $_" -ForegroundColor Red
    }
}

Write-Host "=================================="
Write-Host "PRUEBAS DE PAGINACION - ITEMS"
Write-Host "=================================="

Test-Endpoint "1 obtener items pagina 0 10 items" "/api/items/paginated"
Test-Endpoint "2 obtener items pagina 0 20 items" "/api/items/paginated?page=0&size=20"
Test-Endpoint "3 obtener items pagina 1 10 items" "/api/items/paginated?page=1&size=10"
Test-Endpoint "4 obtener items por categoria 11" "/api/items/category-paginated/11"
Test-Endpoint "5 buscar Arroz" "/api/items/search-paginated?name=Arroz&page=0&size=10"
Test-Endpoint "6 items ordenado por nombre ascendente" "/api/items/paginated?page=0&size=10&sort=name,asc"
Test-Endpoint "8 obtener categorias paginadas" "/api/item-categories/paginated"
Test-Endpoint "9 obtener categorias pagina 0 20 items" "/api/item-categories/paginated?page=0&size=20"
Test-Endpoint "10 categorias ordenadas por nombre" "/api/item-categories/paginated?sort=name,asc"

Write-Host ""
Write-Host "=================================="
Write-Host "FIN DE PRUEBAS" -ForegroundColor Green
Write-Host "=================================="

