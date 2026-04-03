# Sistema de QR para Mesas - Arquitectura y Funcionamiento

## 1. Flujo General del QR

```
┌─────────────────────────────────────────────────────────┐
│ 1. CLIENTE ESCANEA QR FÍSICO EN LA MESA                │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ 2. QR CONTIENE URL: https://app.com/?table=5            │
│    (Código encriptado con mesa + timestamp)             │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ 3. NAVEGADOR ABRE FRONTEND EN /menu?table=5             │
│    Frontend hace GET /api/client/session?table=5        │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ 4. BACKEND CREA TableSession + genera JWT               │
│    Respuesta: { sessionToken, tableNumber, expiresAt }  │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ 5. FRONTEND GUARDA sessionToken en localStorage         │
│    TODAS LAS PETICIONES USAN: Bearer <sessionToken>     │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ 6. CLIENTE NAVEGA MENÚ, CREA ORDEN                      │
│    POST /api/client/orders                              │
│    Usa tableNumber del sessionToken                     │
└─────────────────────────────────────────────────────────┘
```

---

## 2. Componentes del Sistema

### 🔧 Backend (Implementado)

#### Endpoint de Sesión
```
GET /api/client/session?table={n}

Respuesta:
{
  "sessionToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tableNumber": 5,
  "sessionId": "uuid-xxx",
  "expiresAt": "2026-04-02T18:30:00",
  "tokenType": "Bearer"
}
```

**Lógica:**
1. Valida que mesa existe y está disponible
2. Crea `TableSession` con estado `active=true`
3. Genera JWT firmado con tabla + expiración (4 horas)
4. Almacena en BD para auditoría

**Seguridad:**
- Token expira automáticamente
- Una mesa = máx 1 sesión activa simultanea
- Se registra hora de apertura/cierre

---

#### Endpoints de Órdenes (Nuevos)
```
POST /api/client/orders
{
  "tableNumber": 5,
  "items": [
    { "itemId": 15, "quantity": 2 },
    { "itemId": 18, "quantity": 1 }
  ]
}

GET /api/client/orders?tableNumber=5
GET /api/client/orders/{orderId}
PATCH /api/client/orders/{orderId}/confirm-delivery
POST /api/client/orders/{orderId}/request-help
```

---

### 📱 Frontend (Parte que Implementa el IA)

#### 1. Escáner QR
```typescript
// Usar librería: @capacitor-community/barcode-scanner (Mobile)
// O: html5-qrcode (Web)

const scannedUrl = await BarcodeScanner.scan();
// Resultado: https://app.com/?table=5
```

#### 2. Extrae parámetros
```typescript
const tableNumber = new URLSearchParams(location.search).get('table');
// tableNumber = "5"
```

#### 3. Abre sesión
```typescript
// GET http://localhost:8081/api/client/session?table=5
const response = await fetch(`/api/client/session?table=${tableNumber}`);
const data = await response.json();

// Guardar token
localStorage.setItem('sessionToken', data.sessionToken);
localStorage.setItem('tableNumber', data.tableNumber);
```

#### 4. Configura headers para peticiones futuras
```typescript
// Interceptor HTTP
export function authInterceptor(req, next) {
  const token = localStorage.getItem('sessionToken');
  if (token) {
    req.headers['Authorization'] = `Bearer ${token}`;
  }
  return next(req);
}
```

#### 5. Navega a menú
```typescript
// Mostrar items disponibles
GET /api/items/paginated?page=0&size=20

// Cliente selecciona items y crea orden
POST /api/client/orders
{
  "tableNumber": 5,
  "items": [...]
}
```

---

## 3. ¿Qué Contiene el QR?

### Opción A: URL Simple (Actual)
```
https://app.com/?table=5&code=abc123xyz

Ventajas:
- Simple de generar e imprimir
- Valida en backend (mesa existe?)
- No expone datos sensibles

Desventajas:
- Cualquiera con la URL accede a esa mesa
```

### Opción B: URL Encriptada (Recomendada)
```
https://app.com/?code=eyJtZXNhIjo1LCJ0czoxNzAwMDAwMDAwLCJzaWduIjoiYWJjMTIzIn0=

El código encriptado contiene:
{
  "mesa": 5,
  "timestamp": 1700000000,
  "sign": "abc123"  // HMAC para validar integridad
}

Ventajas:
- No se ve el número de mesa
- Válido solo temporalmente
- Código de validación HMAC

Desventajas:
- Más complejo de generar
```

### Opción C: QR Dinámico (Mejor para rotación)
```
QR → https://app.com/qr/mesa-5-abc123xyz

En backend:
GET /qr/{code}
→ Valida código
→ Redirige a /api/client/session?table=5

Ventajas:
- Se puede revocar códigos
- Rotación de QR (cambiar mensualmente)
- Análisis de uso (cuántos scanes por QR)

Desventajas:
- Requiere base de datos para códigos
```

---

## 4. Generación de QR en Backend

Para imprimir QRs en mesas:

### Dependencia Maven
```xml
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.2</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.2</version>
</dependency>
```

### Servicio de Generación
```java
@Service
public class QRCodeService {
    
    public BufferedImage generateQRCode(Integer tableNumber) throws WriterException {
        String url = "https://app.com/?table=" + tableNumber;
        
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, 300, 300);
        
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
}
```

### Endpoint para descargar QRs
```java
@GetMapping("/api/admin/qr/{tableNumber}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<byte[]> downloadQRCode(@PathVariable Integer tableNumber) 
        throws IOException, WriterException {
    
    BufferedImage qr = qrCodeService.generateQRCode(tableNumber);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(qr, "PNG", baos);
    
    return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=mesa-" + tableNumber + ".png")
            .contentType(MediaType.IMAGE_PNG)
            .body(baos.toByteArray());
}
```

---

## 5. Ciclo de Vida Completo

### A. Setup Inicial (Admin)
```
1. Admin crea mesa: POST /api/staff/tables
   → Mesa 5, capacidad 4
   
2. Admin descarga QR: GET /api/admin/qr/5
   → Recibe PNG, lo imprime y plastifica
   
3. QR pegado en mesa física
```

### B. Cliente Usa Mesa
```
1. Cliente escanea QR físico
   → Smartphone abre: https://app.com/?table=5
   
2. Frontend hace: GET /api/client/session?table=5
   → Backend crea TableSession
   → Retorna sessionToken JWT (4 horas)
   
3. Frontend guarda token en localStorage
   
4. Cliente ve menú: GET /api/items/paginated
   
5. Cliente confirma orden: POST /api/client/orders
   → Usa tableNumber del token
   → Orden creada con estado PENDING
   
6. Cocina ve orden: GET /api/staff/orders/pending
   
7. Cocina prepara y actualiza: PATCH /api/staff/orders/{id}/status
   → Estado: PENDING → IN_PREPARATION → SERVED
   
8. Mesero acerca platos, cliente confirma: PATCH /api/client/orders/{id}/confirm-delivery
   
9. Mesero genera factura: GET /api/staff/orders/{id}/invoice
   
10. Cashier cobra y cierra sesión: DELETE /api/staff/tables/5/session
```

---

## 6. Seguridad Consideraciones

| Aspecto | Medida |
|---|---|
| **Acceso no autorizado** | JWT con expiración + tabla validada en backend |
| **Reuso de token** | Token válido solo para tabla asignada |
| **Sesiones concurrentes** | Una mesa = máx 1 sesión active=true |
| **Expulsión de cliente** | Admin puede cerrar sesión: DELETE /api/staff/tables/{n}/session |
| **Auditoría** | TableSession registra openedAt, closedAt |

---

## 7. Implementación Recomendada

**Backend:** ✅ Completamente implementado
- TableSession creada automáticamente
- Órdenes funcionan con tableNumber del token
- OrderSource.TABLE vinculan orden a mesa

**Frontend:** Debe implementar IA
1. Scanner QR (leer código de mesa)
2. Llamada a `/api/client/session?table={n}`
3. Guardar token en localStorage
4. Configurar interceptor HTTP para Authorization header
5. Mostrar menú desde `/api/items/paginated`
6. Crear orden POST `/api/client/orders`
7. Actualizar UI con estado de orden en tiempo real

**Impresión de QRs:** Endpoint GET `/api/admin/qr/{tableNumber}` (opcional, agregar si se necesita)

---

## 8. Flujo en Tiempo Real (Recomendación)

Para mejorar UX, implementar WebSocket o Server-Sent Events:

```typescript
// Frontend escucha cambios en orden
const eventSource = new EventSource(`/api/client/orders/${orderId}/events`);

eventSource.onmessage = (event) => {
  const order = JSON.parse(event.data);
  // Actualizar UI: "Tu orden está en preparación..."
};
```

En backend:
```java
@GetMapping("/api/client/orders/{id}/events")
public SseEmitter subscribeToOrderUpdates(@PathVariable Long id) {
    // Cliente recibe notificaciones en tiempo real
}
```

---

## Conclusión

El QR es simplemente un **redirector a la URL de sesión**. Todo lo demás (seguridad, validación, órdenes) está en el backend.

**Lo importante:** El token JWT vinculado a la mesa garantiza que:
- Solo esa mesa puede confirmar órdenes
- La sesión expira automáticamente
- Se puede auditar quién pidió qué y cuándo

