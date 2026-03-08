#!/bin/bash
# Ejemplos de cURL para probar la paginación de Items
# Reemplaza TOKEN con tu token JWT actual

TOKEN="eyJhbGciOiJIUzUxMiJ9.eyJwYXNzd29yZFZlcnNpb24iOjEsInN1YiI6InJhbWlyZXNqdWFuZGE0N0BnbWFpbC5jb20iLCJpYXQiOjE3NzI5NDM4OTEsImV4cCI6MTc3MzAzMDI5MX0.eputYlJKq1lKn-OdWEishbDemE0Y_m2zmIOOJZUOST9dYsbKG2lHKCWPFrg-bqrbInA2IGVdBn1PBFhNycDGRQ"
BASEURL="http://localhost:8081"

echo "=================================="
echo "PRUEBAS DE PAGINACIÓN - ITEMS"
echo "=================================="

echo ""
echo "1. Obtener items (página 0, 10 items)"
curl -H "Authorization: Bearer $TOKEN" \
  "$BASEURL/api/items/paginated"

echo ""
echo ""
echo "2. Obtener items (página 0, 20 items)"
curl -H "Authorization: Bearer $TOKEN" \
  "$BASEURL/api/items/paginated?page=0&size=20"

echo ""
echo ""
echo "3. Obtener items (página 1, 10 items)"
curl -H "Authorization: Bearer $TOKEN" \
  "$BASEURL/api/items/paginated?page=1&size=10"

echo ""
echo ""
echo "4. Obtener items por categoría 11 (página 0, 10 items)"
curl -H "Authorization: Bearer $TOKEN" \
  "$BASEURL/api/items/category-paginated/11"

echo ""
echo ""
echo "5. Buscar 'Arroz' (página 0, 10 items)"
curl -H "Authorization: Bearer $TOKEN" \
  "$BASEURL/api/items/search-paginated?name=Arroz&page=0&size=10"

echo ""
echo ""
echo "6. Obtener items ordenado por nombre ascendente"
curl -H "Authorization: Bearer $TOKEN" \
  "$BASEURL/api/items/paginated?page=0&size=10&sort=name,asc"

echo ""
echo ""
echo "7. Obtener items ordenado por ID descendente"
curl -H "Authorization: Bearer $TOKEN" \
  "$BASEURL/api/items/paginated?page=0&size=10&sort=id,desc"

echo ""
echo "=================================="
echo "FIN DE PRUEBAS"
echo "=================================="

