# 📊 GUÍA - DATASET COMPLETO (Items, ItemStock, Batches)

## 🎯 ¿QUÉ ES ESTE ARCHIVO?

Es un archivo HTTP que crea un dataset completo para testing con:
- **8 Items** (productos como Arroz, Pollo, Leche, etc.)
- **8 ItemStock** (uno por item)
- **15 Lotes** (batches con diferentes vencimientos)

---

## 📁 ARCHIVO

**Nombre:** `test-dataset-items-stock-batch.http`

**Ubicación:** Raíz del proyecto `/servly/`

---

## ✨ CONTENIDO DEL DATASET

### Items Creados (8):

| # | Nombre | Unidad | Días Exp | Stock Ideal | Categoría |
|---|--------|--------|----------|------------|-----------|
| 1 | Arroz Blanco | kg | 365 | 100 | Granos |
| 2 | Pechuga de Pollo | kg | 7 | 50 | Carnes |
| 3 | Leche Entera | litros | 14 | 80 | Lácteos |
| 4 | Queso Fresco | kg | 21 | 30 | Lácteos |
| 5 | Tomate Rojo | kg | 10 | 60 | Verduras |
| 6 | Cebolla Blanca | kg | 30 | 50 | Verduras |
| 7 | Aceite de Oliva | litros | 730 | 20 | Aceites |
| 8 | Sal Refinada | kg | 999 | 10 | Condimentos |

### Lotes Creados (15):

**ARROZ (3 lotes):**
- LOTE-ARROZ-2026-001: 100kg, Proveedor A, vence 01/Jun
- LOTE-ARROZ-2026-002: 80kg, Proveedor B, vence 15/Jun
- LOTE-ARROZ-2026-003: 60kg, Proveedor A, vence 20/Jun

**POLLO (3 lotes):**
- LOTE-POLLO-2026-001: 40kg, Carnes Premium, vence 15/Mar ⚠️
- LOTE-POLLO-2026-002: 35kg, Granja La Paz, vence 18/Mar ⚠️
- LOTE-POLLO-2026-003: 25kg, Carnes Premium, vence 20/Mar ⚠️

**LECHE (2 lotes):**
- LOTE-LECHE-2026-001: 50L, Lácteos Valley, vence 22/Mar ⚠️
- LOTE-LECHE-2026-002: 40L, Lácteos Valley, vence 25/Mar ⚠️

**QUESO (2 lotes):**
- LOTE-QUESO-2026-001: 20kg, Quesería Andina, vence 30/Mar
- LOTE-QUESO-2026-002: 15kg, Quesería Andina, vence 05/Apr

**TOMATE (2 lotes):**
- LOTE-TOMATE-2026-001: 60kg, Huerta Fresca, vence 18/Mar ⚠️
- LOTE-TOMATE-2026-002: 45kg, Productor Local, vence 20/Mar ⚠️

**CEBOLLA (2 lotes):**
- LOTE-CEBOLLA-2026-001: 70kg, Huerta Fresca, vence 10/Apr
- LOTE-CEBOLLA-2026-002: 50kg, Productor Local, vence 15/Apr

**ACEITE (2 lotes):**
- LOTE-ACEITE-2026-001: 15L, Olivares España, vence 01/Jan 2027
- LOTE-ACEITE-2026-002: 10L, Olivares España, vence 15/Jan 2027

**SAL (1 lote):**
- LOTE-SAL-2026-001: 20kg, Salinas Nacionales, vence 01/Jan 2028

---

## 🚀 CÓMO USAR

### PASO 1: Abre el Archivo

```
Ctrl+O (Windows/Linux) o Cmd+O (Mac) en tu IDE JetBrains
O simplemente haz click en el archivo en el explorador
```

### PASO 2: Actualiza el Token

En la línea 4, reemplaza el token con uno válido si es necesario:
```
@token = TU_TOKEN_JWT_AQUI
```

### PASO 3: Ejecuta los Requests

**Opción A - Crear Dataset Completo:**
1. Ejecuta "PASO 1: CREAR ITEMS" (si los items no existen)
2. Ejecuta "PASO 2-8: CREAR LOTES" (todos los lotes)
3. Ve a "VERIFICAR DATOS" para confirmar

**Opción B - Solo Crear Lotes:**
Si los items ya existen:
1. Salta el PASO 1
2. Ve directamente al PASO 2
3. Ejecuta todos los lotes

**Opción C - Ejecutar Todo:**
- Click en cualquier request y presiona Ctrl+R
- O usa "Run All" si tu IDE lo permite

### PASO 4: Verifica los Datos

Ejecuta las consultas de lectura:
```
- GET /api/items → Ver todos los items
- GET /api/stock-batch/close-to-expire → Ver alertas
- GET /api/stock-batch/item-stock/1 → Ver lotes del Arroz
```

---

## 📊 DATOS GENERADOS

### Cantidad Total en Inventario:
```
Arroz:     240 kg
Pollo:     100 kg
Leche:      90 L
Queso:      35 kg
Tomate:    105 kg
Cebolla:   120 kg
Aceite:     25 L
Sal:        20 kg
```

### Próximos a Expirar (7 días):
```
🔴 Pollo:    15, 18, 20 de Marzo
🔴 Tomate:   18, 20 de Marzo
🟡 Leche:    22, 25 de Marzo
🟡 Cebolla:  10, 15 de Abril
```

### Con Mucho Tiempo:
```
🟢 Arroz:    01, 15, 20 de Junio
🟢 Aceite:   01, 15 de Enero 2027
🟢 Sal:      01 de Enero 2028
```

---

## 🔍 EJEMPLOS DE CONSULTAS

### Después de crear el dataset, prueba estas consultas:

#### 1. Ver todos los items
```
GET {{baseUrl}}/api/items?page=0&size=20
```

#### 2. Ver lotes próximos a expirar (alertas)
```
GET {{baseUrl}}/api/stock-batch/close-to-expire
```
Resultado esperado: Pollo, Tomate, Leche

#### 3. Ver todos los lotes del Arroz
```
GET {{baseUrl}}/api/stock-batch/item-stock/1
```
Resultado esperado: 3 lotes de Arroz

#### 4. Consumir 30kg de Arroz (FIFO)
```
PUT {{baseUrl}}/api/stock-batch/item-stock/1/decrease?quantity=30
```
Resultado: Reduce 30kg del LOTE-ARROZ-001

#### 5. Ver lote que vence primero del Pollo
```
GET {{baseUrl}}/api/stock-batch/item-stock/2/next-to-expire
```
Resultado: LOTE-POLLO-001 (vence 15/Mar)

---

## 💡 CASOS DE USO

### Caso 1: Simular Vencimientos Próximos
✅ El dataset tiene muchos items que vencen pronto (Pollo, Tomate)
✅ Perfecto para testing de alertas en el dashboard

### Caso 2: Testing de FIFO
✅ El Pollo tiene 3 lotes con vencimientos diferentes (15, 18, 20 Mar)
✅ Usa `/decrease` para ver FIFO en acción

### Caso 3: Pruebas de Consumo
✅ El Arroz tiene 240kg total en 3 lotes
✅ Prueba consumir cantidades diferentes (30kg, 50kg, 100kg)

### Caso 4: Dashboard de Alertas
✅ Usa `/close-to-expire` para obtener lotes con alerta
✅ Verás 8 lotes próximos a expirar (4 items)

### Caso 5: Reportes
✅ Usa `/expired` después de cambiar fecha del sistema
✅ Verás lotes vencidos para reportes de merma

---

## 🔧 MODIFICAR EL DATASET

Si quieres cambiar algo:

### Cambiar fechas de vencimiento:
```json
// En cualquier POST /api/stock-batch, cambia:
"expiryDate": "2026-06-01"
// A la fecha que desees
```

### Cambiar cantidades:
```json
// En cualquier POST /api/stock-batch, cambia:
"quantity": 100
// A la cantidad que desees
```

### Cambiar proveedores:
```json
// En cualquier POST /api/stock-batch, cambia:
"supplierId": 1
// A otro proveedor (asume que existen con ID 1-10)
```

---

## ⚠️ NOTAS IMPORTANTES

1. **ItemStock se crean automáticamente** cuando creas un Item
2. **Los IDs se asignan automáticamente** por la BD (no los especifiques)
3. **El token debe ser válido** - comparte el mismo de login
4. **Los proveedores deben existir** (IDs 1-10 en el dataset)
5. **Las categorías deben existir** (IDs 11-22 en el ejemplo)

---

## 🚨 ERRORES COMUNES

### Error: "Invalid token"
**Solución:** Actualiza el @token con uno válido de tu login

### Error: "ItemStock not found"
**Solución:** Los Items se crean primero, luego los Lotes. Ejecuta PASO 1 primero

### Error: "Supplier not found"
**Solución:** Los proveedores deben existir. Verifica los IDs (1-10)

### Error: "Category not found"
**Solución:** Las categorías deben existir. Verifica los IDs de categoría

---

## 📈 ESTADÍSTICAS DEL DATASET

```
Total Items:        8
Total ItemStock:    8
Total Lotes:        15
Total Cantidad:     835 kg/L
Lotes Próximos:     8 (en alerta)
Lotes Vigentes:     15
Lotes Expirados:    0
Pérdida Estimada:   0 kg/L
```

---

## 🎯 RECOMENDACIONES

1. **Para Testing:** Ejecuta el dataset completo
2. **Para Demo:** Usa solo Items + algunos Lotes
3. **Para Producción:** Crea manualmente desde la app
4. **Para Frontend:** Usa este dataset para probar dashboard

---

## 📝 CHECKLIST

- [ ] Abrí el archivo `test-dataset-items-stock-batch.http`
- [ ] Actualicé el token
- [ ] Ejecuté PASO 1 (Items)
- [ ] Ejecuté PASO 2-8 (Lotes)
- [ ] Ejecuté las consultas de verificación
- [ ] Vi los datos en el dashboard
- [ ] Probé consumo con `/decrease`
- [ ] Probé alertas con `/close-to-expire`

---

## 🎉 LISTO

Una vez ejecutado todo, tendrás un dataset completo para testing.

Úsalo para:
- ✅ Testing de dashboard
- ✅ Testing de alertas
- ✅ Testing de consumo (FIFO)
- ✅ Testing de reportes
- ✅ Demo a cliente

¡Adelante! 🚀

