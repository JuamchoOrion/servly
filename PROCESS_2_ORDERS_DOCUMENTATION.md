# Proceso 2: Gestión de Mesas y Pedidos - Documentación Completa

## 📋 Resumen Ejecutivo

El proceso de gestión de mesas y pedidos permite que los clientes escaneen un código QR en su mesa, vean el menú digital (filtrando productos según disponibilidad de inventario), confirmen un pedido con variaciones opcionales de items, y el sistema notifique a cocina. El pedido pasa por estados: PENDING → IN_PREPARATION → SERVED. El cliente se autentica de forma anónima usando un sessionToken obtenido del QR.

---

## 🔄 Flujo Completo del Proceso

### 1️⃣ CLIENTE ESCANEA QR (Sin Login)

**Endpoint:** `GET /api/tables/{tableCode}/session`

```
Cliente escanea QR con código único de mesa
                    ↓
TableSessionFilter genera sessionToken
                    ↓
Cliente recibe sessionToken + tableNumber
                    ↓
Autenticación: ROLE_CLIENT (sin login)
```

**Seguridad implementada:**
- TableSessionFilter valida el código QR
- SessionToken temporal valida al cliente por mesa
- ROLE_CLIENT asignado automáticamente
- Solo puede acceder a su mesa

---

### 2️⃣ CLIENTE VE MENÚ DIGITAL CON DISPONIBILIDAD Y VARIACIONES

**Endpoint:** `GET /api/menu/products`

```
Cliente solicita menú con detalles de variaciones
                    ↓
Cada producto devuelve:
  - Precio base
  - Lista de Items en la Recipe
  - Para cada Item: si es opcional, rango de cantidad
                    ↓
ProductAvailabilityService valida:
  - ¿Existen todos los ITEMS para la RECIPE del producto?
  - ¿Hay CANTIDAD suficiente de cada ITEM en inventario?
                    ↓
Si hay stock para todos los items → Producto aparece en menú
Si NO hay stock para algún item → Producto ocultado
```

**Estructura de datos:**
```
Product (Lo que pide el cliente)
    ↓
Recipe (Receta del producto)
    ↓
ItemDetailList (Items que se necesitan)
    ├─ id: identificador único del item
    ├─ quantity: cantidad BASE que siempre va
    ├─ isOptional: ¿El cliente puede modificarlo?
    ├─ minQuantity: cantidad mínima que puede elegir (0 si no requiere)
    ├─ maxQuantity: cantidad máxima que puede elegir
    ↓
ItemStock (Inventario real)
```

**Ejemplo completo:**
```json
GET /api/menu/products/1

{
  "id": 1,
  "name": "Hamburguesa Clásica",
  "basePrice": 25000.00,
  "recipeItems": [
    {
      "id": 20,
      "itemId": 1,
      "itemName": "Pan",
      "baseQuantity": 2,
      "isOptional": false,
      "minQuantity": null,
      "maxQuantity": null,
      "annotation": "Pan para hamburguesa"
    },
    {
      "id": 22,
      "itemId": 10,
      "itemName": "Queso",
      "baseQuantity": 1,
      "isOptional": true,
      "minQuantity": 0,
      "maxQuantity": 3,
      "annotation": "Queso opcional, puedes agregar hasta 3"
    },
    {
      "id": 24,
      "itemId": 12,
      "itemName": "Cebolla",
      "baseQuantity": 0,
      "isOptional": true,
      "minQuantity": 0,
      "maxQuantity": 2,
      "annotation": "Cebolla extra, no viene por defecto"
    }
  ]
}
```

**Validación de disponibilidad:**
```
Para mostrar este producto, debe haber:
  - Pan (id:1): 2 unidades mínimo ✓
  - Queso (id:10): 1 unidad mínimo ✓
  - Cebolla (id:12): 0 unidades (opcional)

Si itemStock(id:1) < 2 → NO mostrar producto
Si itemStock(id:10) < 1 → NO mostrar producto
Si itemStock(id:12) < 0 → OK, es opcional
```

---

### 3️⃣ CLIENTE CONFIRMA PEDIDO CON VARIACIONES DE ITEMS

**Endpoint:** `POST /api/client/orders`

**Importante sobre VARIACIONES:**
```
Las variaciones de items NO AFECTAN EL PRECIO del producto.
Solo afectan la CANTIDAD de items descontados del inventario.

Ejemplo:
Hamburguesa base: $25,000 (siempre)
  - Con 1 queso: $25,000
  - Con 3 quesos: $25,000 (MISMO PRECIO)
  
Lo que cambia:
  - Stock descontado: 1 queso vs 3 quesos
```

**Request - Cliente elige cantidades de items opcionales:**
```json
{
  "tableNumber": 5,
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "itemQuantityOverrides": {
        "10": 3,
        "12": 2
      }
    }
  ]
}
```

**Desglose:**
```
Producto: Hamburguesa Clásica (ID: 1), cantidad: 2
Items por hamburgesa:
  - Pan: 2 (base, no se puede cambiar)
  - Queso: cliente elige 3 en lugar de 1 → +2 extras
  - Cebolla: cliente elige 2 en lugar de 0 → +2 extras

Total de items a descontar cuando pague:
  - Pan: 2 × 2 = 4 unidades
  - Queso: 3 × 2 = 6 unidades
  - Cebolla: 2 × 2 = 4 unidades
```

**Cálculo automático de precio:**
```
Precio base: 25,000
Items extras:
  - Queso: +2 × 3,500 = +7,000
  - Cebolla: +2 × 1,500 = +3,000
Precio unitario: 35,000
Total por 2: 70,000
```

**Validaciones:**
1. Verifica que EXISTAN los productos
2. **Valida que haya ITEMS suficientes** en inventario para cantidades elegidas
3. Crea Order en estado: **PENDING**
4. **NO descuenta inventario aún** (solo después de pagar)

**Response:**
```json
{
  "id": 123,
  "tableNumber": 5,
  "status": "PENDING",
  "items": [
    {
      "id": 456,
      "productId": 1,
      "productName": "Hamburguesa Clásica",
      "quantity": 2,
      "unitPrice": 35000,
      "subtotal": 70000
    }
  ],
  "total": 70000,
  "createdAt": "2026-04-02T15:30:00"
}
```

---

### 4️⃣ CLIENTE PAGA LA ORDEN

**Endpoint:** `POST /api/client/orders/{orderId}/confirm-payment`

```
Cliente paga (sistema externo: PayPal, Stripe, etc.)
                    ↓
confirmPaymentAndDeductInventory() se ejecuta
                    ↓
Itera Order_detail:
  Para cada Product en la orden:
    - Obtiene Recipe del producto
    - Por cada ItemDetail de la Recipe:
      - Descuenta itemStock.quantity
      - FIFO: Primero expiran antes
                    ↓
Order pasa a estado: IN_PREPARATION
                    ↓
Notificación enviada a cocina
```

**Ejemplo de descuento CON VARIACIONES:**
```
Pide: 2x Hamburguesa Clásica
Con variaciones:
  - Queso: 3 en lugar de 1 (base)
  - Cebolla: 2 en lugar de 0 (base)

Se descuenta:
  ItemStock(pan) -= 4         (2 × 2 - no se varió)
  ItemStock(carne) -= 2       (1 × 2 - no se varió)
  ItemStock(queso) -= 6       (3 × 2 - variación aplicada)
  ItemStock(cebolla) -= 4     (2 × 2 - agregado por cliente)
  
El descuento SIEMPRE respeta las variaciones elegidas por el cliente
```

---

### 5️⃣ COCINA NOTIFICADA - PREPARE LA ORDEN

**Endpoint (Cocina):** `GET /api/staff/orders/pending`

```
Sistema envía notificación a cocina
                    ↓
Cocina ve orden PENDING en su pantalla
                    ↓
Cocina selecciona orden y empieza a preparar
                    ↓
Actualiza estado a IN_PREPARATION
```

**Endpoint:** `PATCH /api/staff/orders/{orderId}/status`

```json
{
  "status": "IN_PREPARATION"
}
```

---

### 6️⃣ CLIENTE PIDE AYUDA (Opcional)

**Endpoint:** `POST /api/client/orders/{orderId}/request-help`

```
Cliente presiona botón "Pedir ayuda"
                    ↓
Sistema genera ALERTA para mesero
                    ↓
Mesero ve notificación en pantalla
                    ↓
Mesero acude a la mesa
```

---

### 7️⃣ COCINA MARCA COMO LISTO

**Endpoint:** `PATCH /api/staff/orders/{orderId}/status`

```json
{
  "status": "SERVED"
}
```

```
Cocina marca como LISTO
                    ↓
Notificación a mesero: "Orden lista para mesa X"
                    ↓
Mesero lleva orden a la mesa
```

---

### 8️⃣ CLIENTE CONFIRMA ENTREGA

**Endpoint:** `PATCH /api/client/orders/{orderId}/confirm-delivery`

```
Mesero entrega orden
                    ↓
Cliente confirma que recibió
                    ↓
Order pasa a estado: SERVED (completado)
```

---

### 9️⃣ GENERAR FACTURA

**Endpoint:** `GET /api/staff/orders/{orderId}/invoice`

```
Mesero solicita factura
                    ↓
Sistema calcula:
  - Subtotal items
  - Impuestos
  - Total a pagar
                    ↓
PDF generado
                    ↓
Se envía a cliente o impresora
```

---

### 🔟 ACTUALIZAR ESTADO DE MESA

**Endpoint:** `PATCH /api/tables/{tableNumber}/status`

```
Después de que cliente paga:
                    ↓
Mesa marca como: EN_SERVICIO (mientras se prepara)
                    ↓
Después de SERVED:
                    ↓
Mesa marca como: LIBRE
```

---

## 📊 Estados de la Orden

```
PENDING
  ↓ (después de pagar)
IN_PREPARATION (inventario ya descontado)
  ↓ (cocina termina)
SERVED
  ↓ (cliente confirma)
COMPLETADO (implícito, SERVED = finalizado)
```

---

## 🛡️ Seguridad Implementada

### Sin Login (Cliente):
```
1. TableSessionFilter valida código QR
2. Genera sessionToken temporal
3. Cliente obtiene ROLE_CLIENT automáticamente
4. Solo puede:
   - Ver menú de su mesa
   - Crear orden de su mesa
   - Ver sus propias órdenes
   - Pedir ayuda
   - Confirmar entrega
```

### Con Login (Staff):
```
1. Mesero/Cocina inicia sesión normal
2. Obtiene ROLE_STAFF o ROLE_KITCHEN
3. Puede:
   - Ver órdenes de cualquier mesa
   - Actualizar estados
   - Generar facturas
   - Ver ordenes pendientes
```

---

## 📱 Endpoints Implementados

### ✅ CLIENTE (Sin Login - con sessionToken)

| Método | Endpoint | Descripción | Protección |
|--------|----------|-------------|-----------|
| GET | `/api/menu/products` | Obtener menú con variaciones de items | Público |
| GET | `/api/menu/products/{id}` | Obtener producto con detalles de recipe | Público |
| POST | `/api/client/orders` | Crear orden de mesa con variaciones | ROLE_CLIENT (sessionToken) |
| GET | `/api/client/orders` | Listar mis órdenes | ROLE_CLIENT (sessionToken) |
| GET | `/api/client/orders/{id}` | Ver detalles orden | ROLE_CLIENT (sessionToken) |
| POST | `/api/client/orders/{id}/request-help` | Pedir ayuda | ROLE_CLIENT (sessionToken) |
| PATCH | `/api/client/orders/{id}/confirm-delivery` | Confirmar entrega | ROLE_CLIENT (sessionToken) |

### ✅ STAFF (Con Login - JWT)

| Método | Endpoint | Descripción | Protección |
|--------|----------|-------------|-----------|
| GET | `/api/staff/orders/pending` | Órdenes pendientes (cocina) | ROLE_KITCHEN |
| GET | `/api/staff/orders/in-preparation` | En preparación | ROLE_KITCHEN |
| GET | `/api/staff/tables/{tableNumber}/orders` | Órdenes de una mesa | ROLE_STAFF |
| PATCH | `/api/staff/orders/{id}/status` | Actualizar estado | ROLE_KITCHEN o ROLE_STAFF |
| GET | `/api/staff/orders/{id}/invoice` | Generar factura | ROLE_STAFF |

### ⚠️ CRUD PRODUCTOS (NECESITA IMPLEMENTACIÓN)

| Método | Endpoint | Descripción | Protección Recomendada |
|--------|----------|-------------|-----------|
| POST | `/api/admin/products` | Crear nuevo producto | **ROLE_ADMIN** |
| GET | `/api/admin/products` | Listar productos (staff) | ROLE_ADMIN, ROLE_STAFF |
| GET | `/api/admin/products/{id}` | Obtener producto específico | ROLE_ADMIN, ROLE_STAFF |
| PUT | `/api/admin/products/{id}` | Actualizar producto | **ROLE_ADMIN** |
| DELETE | `/api/admin/products/{id}` | Eliminar producto | **ROLE_ADMIN** |
| POST | `/api/admin/recipes` | Crear receta | **ROLE_ADMIN** |
| POST | `/api/admin/recipes/{recipeId}/items` | Agregar items a receta | **ROLE_ADMIN** |
| PUT | `/api/admin/recipes/{recipeId}/items/{itemDetailId}` | Actualizar variación de item | **ROLE_ADMIN** |
| DELETE | `/api/admin/recipes/{recipeId}/items/{itemDetailId}` | Eliminar item de receta | **ROLE_ADMIN** |

---

## 💾 Estructura de Datos

### Order Entity
```java
Order {
  id: Long
  date: LocalDate
  total: BigDecimal
  orderType: ENUM(TABLE, DELIVERY)
  status: ENUM(PENDING, IN_PREPARATION, SERVED)  // ← NUEVO
  orderDetailList: List<Order_detail>
  source: OrderSource (TableSource o DeliverySource)
}
```

### Order_detail Entity
```java
Order_detail {
  id: Long
  order: Order
  product: Product  // ← Cliente elige PRODUCTO
  quantity: Integer
  unitPrice: BigDecimal
  subtotal: BigDecimal
}
```

### Product → Recipe → ItemDetail (CON VARIACIONES)
```java
Product {
  recipe: Recipe
}

Recipe {
  itemDetailList: List<ItemDetail>  // Items que se necesitan
}

ItemDetail {
  id: Long
  item: Item
  quantity: Integer              // Cantidad BASE que siempre va
  isOptional: Boolean            // ¿El cliente puede modificarlo?
  minQuantity: Integer           // Mínimo que puede elegir (null si no opcional)
  maxQuantity: Integer           // Máximo que puede elegir (null si no opcional)
  annotation: String             // Texto descriptivo: "Extra queso"
}

Ejemplo:
- Pan: quantity=2, isOptional=false (siempre 2, no se puede cambiar)
- Queso: quantity=1, isOptional=true, minQuantity=0, maxQuantity=3
  → Cliente puede elegir 0, 1, 2 o 3 quesos
- Cebolla: quantity=0, isOptional=true, minQuantity=0, maxQuantity=2
  → No viene por defecto, cliente puede agregar 0, 1 o 2
```

---

## 🔧 Servicios Principales

### OrderService
- `createTableOrder()` - Validar PRODUCTOS, crear orden PENDING
- `confirmPaymentAndDeductInventory()` - Descontar ITEMS del inventario
- `updateOrderStatus()` - Cambiar estado (PENDING → IN_PREPARATION → SERVED)
- `getOrdersByTableNumber()` - Obtener órdenes de una mesa
- `cancelOrder()` - Cancelar orden

### ProductAvailabilityService
- `isProductAvailable()` - Validar si hay items para la receta
- `deductInventoryForProduct()` - Descontar items después de pagar
- `validateOrderItemsAvailability()` - Validar disponibilidad antes de crear orden

### OrderRepository
- `findByTableNumber()` - Obtener órdenes de una mesa
- `findByStatus()` - Obtener órdenes por estado
- `findByDate()` - Obtener órdenes de un día

---

## ✨ Características Especiales

### 1. Sin descuento hasta pagar
- Orden creada en PENDING (SIN descontar inventario)
- Cuando se confirma pago → automáticamente descuenta

### 2. Variaciones simples de items (SIN Customizaciones)
- Admin define items OPCIONALES en la recipe
- Cliente elige cantidades al hacer orden
- Precio se calcula automáticamente
- Descuento respeta las variaciones elegidas

Ejemplo:
```
Hamburguesa:
  Base: Pan (2), Carne (1), Queso (1)
  Opcionales: 
    - Queso: 0-3 unidades
    - Cebolla: 0-2 unidades

Cliente elige:
  - Queso: 3 → Costo extra = 2 × precio_queso
  - Cebolla: 2 → Costo extra = 2 × precio_cebolla
  
Total automático = 25,000 + 7,000 + 3,000 = 35,000
```

### 3. FIFO automático
- Descuenta items por fecha de expiración
- Los que vencen primero se usan primero

### 4. Validación de disponibilidad CON VARIACIONES
- ANTES de crear orden: verifica que haya items para cantidades elegidas
- Si no hay items suficientes → producto no aparece en menú
- Ejemplo: Si solo hay 2 quesos y el máximo es 3, producto sigue visible
- Solo se oculta si falta el MÍNIMO requerido

### 5. Control por mesa
- Cliente solo ve/modifica SU mesa
- Mesero puede ver cualquier mesa
- TableSessionFilter valida todo

---

## ⚙️ Cómo Configurar Variaciones en Items

### Para el ADMIN (Crear/Editar Producto):

1. **Crear Recipe** con los items base
   ```
   POST /api/staff/recipes
   {
     "name": "Hamburguesa Clásica",
     "description": "Recipe base"
   }
   ```

2. **Agregar ItemDetails con configuración**
   ```
   POST /api/staff/recipes/{recipeId}/items
   {
     "itemId": 1,
     "quantity": 2,           // Cantidad base (Pan)
     "isOptional": false,     // No se puede variar
     "annotation": "Pan para hamburguesa"
   },
   {
     "itemId": 10,
     "quantity": 1,           // Cantidad base (Queso)
     "isOptional": true,      // SÍ se puede variar
     "minQuantity": 0,        // Cliente puede quitar
     "maxQuantity": 3,        // O agregar hasta 3
     "annotation": "Queso opcional"
   }
   ```

3. **Crear Producto asociando Recipe**
   ```
   POST /api/staff/products
   {
     "name": "Hamburguesa Clásica",
     "price": 25000,
     "recipeId": 5,
     "categoryId": 1
   }
   ```

### Para el CLIENTE (Ordenar):

1. **Obtener menú** con variaciones disponibles
   ```
   GET /api/menu/products
   ```

2. **Crear orden** especificando variaciones
   ```
   POST /api/client/orders
   {
     "tableNumber": 5,
     "items": [{
       "productId": 1,
       "quantity": 2,
       "itemQuantityOverrides": {
         "10": 3,  // Queso: 3 en lugar de 1
         "12": 2   // Cebolla: 2 en lugar de 0
       }
     }]
   }
   ```

---

## 🔐 Resumen de Protecciones por Rol

### ROLE_CLIENT (Autenticación: TableSessionToken)
✅ **Puede hacer:**
- Ver menú (`GET /api/menu/products`)
- Crear orden de su mesa (`POST /api/client/orders`)
- Ver sus órdenes (`GET /api/client/orders`)
- Pedir ayuda (`POST /api/client/orders/{id}/request-help`)
- Confirmar entrega (`PATCH /api/client/orders/{id}/confirm-delivery`)

❌ **NO puede:**
- Ver órdenes de otras mesas
- Cambiar estado de órdenes
- Acceder a staff endpoints

---

### ROLE_KITCHEN (Autenticación: JWT + rol KITCHEN)
✅ **Puede hacer:**
- Ver órdenes pendientes (`GET /api/staff/orders/pending`)
- Ver órdenes en preparación (`GET /api/staff/orders/in-preparation`)
- Actualizar estado de orden (`PATCH /api/staff/orders/{id}/status`)

❌ **NO puede:**
- Crear órdenes
- Acceder a endpoints de ADMIN
- Ver menú de clientes (pero puede verlo via `/api/menu/products` público)

---

### ROLE_STAFF (Autenticación: JWT + rol STAFF)
✅ **Puede hacer:**
- Ver órdenes de cualquier mesa (`GET /api/staff/tables/{tableNumber}/orders`)
- Generar facturas (`GET /api/staff/orders/{id}/invoice`)
- Actualizar estado de orden (`PATCH /api/staff/orders/{id}/status`)

❌ **NO puede:**
- Crear productos
- Modificar recetas
- Acceder a endpoints de ADMIN

---

### ROLE_ADMIN (Autenticación: JWT + rol ADMIN)
✅ **Puede hacer:**
- ✨ CREAR productos (`POST /api/admin/products`) - **NECESITA IMPLEMENTACIÓN**
- ✨ ACTUALIZAR productos (`PUT /api/admin/products/{id}`) - **NECESITA IMPLEMENTACIÓN**
- ✨ ELIMINAR productos (`DELETE /api/admin/products/{id}`) - **NECESITA IMPLEMENTACIÓN**
- ✨ CREAR recetas (`POST /api/admin/recipes`) - **NECESITA IMPLEMENTACIÓN**
- ✨ AGREGAR items a recetas con variaciones (`POST /api/admin/recipes/{recipeId}/items`) - **NECESITA IMPLEMENTACIÓN**
- ✨ ACTUALIZAR variaciones (`PUT /api/admin/recipes/{recipeId}/items/{itemDetailId}`) - **NECESITA IMPLEMENTACIÓN**
- Crear empleados
- Cambiar roles
- Ver auditoría

---

## 📋 Checklist de Endpoints Faltantes

- **PayPal/Stripe:** Webhook para confirmar pago
- **Notificaciones:** WebSocket o Push para alertas
- **Impresoras:** Envío de facturas a cocina/caja
- **Gráficos:** Dashboard con órdenes en tiempo real

---

## 🔗 Sistema de Códigos QR para Mesas

### ¿Qué es el QR?

El código QR es una herramienta que permite a los clientes acceder al menú sin iniciar sesión. Cada mesa tiene un **código QR único** que apunta a una URL con un **parámetro tableNumber**.

```
QR en mesa → URL: https://tuapp.com/menu?table=5
                    ↓
           Cliente escanea
                    ↓
           Se abre en navegador
                    ↓
           Se llama a: GET /api/client/session?table=5
                    ↓
           Obtiene sessionToken
                    ↓
           Puede hacer pedidos para mesa 5
```

---

### Paso 1: Generar QRs (Admin - Una sola vez)

**Endpoint a crear:** `POST /api/admin/qr/generate`

```json
{
  "startTable": 1,
  "endTable": 20
}
```

**Lógica:**
```
Para cada tabla (1 a 20):
  1. Generar URL: https://tuapp.com/menu?table={tableNumber}
  2. Codificar a QR usando librería ZXing
  3. Guardar imagen PNG en: /resources/static/qr-codes/table-{tableNumber}.png
  4. Guardar referencia en BD (opcional)
```

**Librerías en build.gradle:**

```gradle
// QR Code Generation
implementation 'com.google.zxing:core:3.5.1'
implementation 'com.google.zxing:javase:3.5.1'
```

**Servicio Java:**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class QRCodeService {

    @Value("${app.url:https://tuapp.com}")
    private String appUrl;
    
    @Value("${qr.storage.path:src/main/resources/static/qr-codes}")
    private String qrStoragePath;

    public void generateQRsForTables(Integer startTable, Integer endTable) {
        Files.createDirectories(Paths.get(qrStoragePath));
        
        for (int tableNum = startTable; tableNum <= endTable; tableNum++) {
            String qrUrl = appUrl + "/menu?table=" + tableNum;
            generateQRCode(qrUrl, "table-" + tableNum);
        }
    }

    private void generateQRCode(String text, String filename) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 300, 300);
            Path path = Paths.get(qrStoragePath, filename + ".png");
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
            log.info("QR generado: {} ({})", filename, text);
        } catch (Exception e) {
            log.error("Error generando QR para mesa: {}", filename, e);
        }
    }
}
```

**Controller:**

```java
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminQRController {
    
    private final QRCodeService qrCodeService;

    @PostMapping("/qr/generate")
    public ResponseEntity<MessageResponse> generateQRCodes(
            @RequestParam Integer startTable,
            @RequestParam Integer endTable) {
        qrCodeService.generateQRsForTables(startTable, endTable);
        return ResponseEntity.ok(new MessageResponse(
            "✅ QRs generados para mesas " + startTable + " a " + endTable
        ));
    }
    
    @GetMapping("/qr/download/{tableNumber}")
    public ResponseEntity<Resource> downloadQR(@PathVariable Integer tableNumber) {
        // Descargar imagen del QR para imprimir
        File qrFile = new File("src/main/resources/static/qr-codes/table-" + tableNumber + ".png");
        // Retornar como recurso para descargar
        return ResponseEntity.ok(new FileSystemResource(qrFile));
    }
}
```

---

### Paso 2: Imprimir QRs en las Mesas

**Opción A: Impresión Manual (Recomendado para empezar)**
```
1. Admin ejecuta: POST /api/admin/qr/generate?startTable=1&endTable=20
2. Los QRs se generan en: src/main/resources/static/qr-codes/
3. Admin descarga cada imagen desde: GET /api/admin/qr/download/1 ... /20
4. Imprime en papel blanco/color
5. Pega lamina en cada mesa con pegamento transparente
```

**Opción B: Impresora Térmica (Para futuros)**
```java
@PostMapping("/qr/print/{tableNumber}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<MessageResponse> printQRForTable(
        @PathVariable Integer tableNumber) {
    // Conectar a impresora térmica
    // Enviar archivo PNG
    // Imprimir automáticamente
    return ResponseEntity.ok(new MessageResponse(
        "Imprimiendo QR para mesa " + tableNumber
    ));
}
```

**Opción C: Generar PDF con todos los QRs**
```java
@GetMapping("/qr/export-pdf")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Resource> exportAllQRsAsPDF() {
    // Usar iText o PdfBox para generar PDF
    // Incluir todos los QRs en una sola hoja
    // Retornar como descarga
    return ResponseEntity.ok(pdfResource);
}
```

---

### Paso 3: Cliente Escanea QR

**Lo que pasa cuando cliente escanea:**

```
Cliente abre navegador
   ↓
Escanea QR de su mesa (mesa 5)
   ↓
Se abre: https://tuapp.com/menu?table=5
   ↓
Frontend lee parámetro: table=5
   ↓
Frontend hace: GET /api/client/session?table=5
   ↓
Backend retorna:
{
  "sessionToken": "eyJ0eXAi...",
  "tableNumber": 5,
  "expiresAt": "2026-04-02T23:59:59"
}
   ↓
Frontend guarda en localStorage:
  - sessionToken → para autenticación
  - tableNumber → 5
   ↓
Cliente ya puede ver MENÚ y crear ÓRDENES
```

---

### Paso 4: Implementación en Frontend

**component.ts (Menu Component):**

```typescript
export class MenuComponent implements OnInit {
  tableNumber: number;
  sessionToken: string;

  constructor(
    private route: ActivatedRoute,
    private sessionService: SessionService,
    private router: Router
  ) {}

  ngOnInit() {
    // Leer tableNumber de URL: ?table=5
    this.route.queryParams.subscribe(params => {
      this.tableNumber = params['table'];
      
      if (!this.tableNumber) {
        this.router.navigate(['/error']);
        return;
      }
      
      // Abrir sesión
      this.openSession();
    });
  }

  openSession() {
    this.sessionService.openSession(this.tableNumber).subscribe({
      next: (response) => {
        // Guardar token
        localStorage.setItem('sessionToken', response.sessionToken);
        localStorage.setItem('tableNumber', this.tableNumber.toString());
        
        // Ahora puede ver menú y hacer órdenes
        this.loadMenu();
      },
      error: (err) => {
        console.error('Error abriendo sesión:', err);
        this.router.navigate(['/error']);
      }
    });
  }

  loadMenu() {
    this.sessionService.getMenuProducts().subscribe(products => {
      this.products = products;
      // Mostrar menú
    });
  }
}
```

**http-client.interceptor.ts:**

```typescript
@Injectable()
export class HttpClientInterceptor implements HttpInterceptor {
  
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Obtener token de sesión
    const sessionToken = localStorage.getItem('sessionToken');
    
    if (sessionToken) {
      // Agregar header de autenticación
      req = req.clone({
        setHeaders: {
          'Authorization': `Bearer ${sessionToken}`
        }
      });
    }
    
    return next.handle(req);
  }
}
```

---

### Paso 5: URL en el Frontend

La aplicación frontend debe servir esta ruta:

```
http://localhost:4200/menu?table=1
http://localhost:4200/menu?table=2
...
http://localhost:4200/menu?table=20

En producción:
https://tuapp.com/menu?table=1
https://tuapp.com/menu?table=2
...
```

El QR codifica exactamente esa URL con el número de mesa.

---

## 🎯 Flujo Completo del QR

| # | Paso | Quién | Acción |
|---|------|-------|--------|
| 1 | **Generación** | Admin | `POST /api/admin/qr/generate?startTable=1&endTable=20` |
| 2 | **Almacenamiento** | Backend | Se guardan PNGs en `/static/qr-codes/` |
| 3 | **Descarga** | Admin | `GET /api/admin/qr/download/{tableNumber}` |
| 4 | **Impresión** | Admin | Imprime y pega en mesas |
| 5 | **Escaneo** | Cliente | Abre https://tuapp.com/menu?table=5 |
| 6 | **Lectura de URL** | Frontend | Lee `?table=5` |
| 7 | **Validación** | Backend | `GET /api/client/session?table=5` |
| 8 | **Autenticación** | Backend | Genera sessionToken |
| 9 | **Almacenamiento** | Frontend | Guarda sessionToken en localStorage |
| 10 | **Autorización** | Frontend | Incluye sessionToken en headers |
| 11 | **Uso** | Cliente | Ahora puede crear órdenes `POST /api/client/orders` |

---

## 💡 Consideraciones de Seguridad del QR

```
✅ SÍ es seguro:
  - QR solo codifica la URL pública
  - URL incluye tableNumber (PÚBLICO)
  - SessionToken se genera en backend
  - SessionToken válido por tiempo limitado (2 horas típico)
  - Cliente solo puede acceder a su mesa

⚠️ Precautions:
  - QR en la mesa física es seguro
  - No incluir tokens en el QR (solo en respuesta del backend)
  - SessionToken debe ser temporal (validez 2-24 horas)
  - Cambiar QRs si se cambia dominio de la app
```
