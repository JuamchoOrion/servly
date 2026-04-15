# QR y Configuración Técnica - Mesas

## 📲 QR por Mesa

Cada mesa debe tener un **código QR único** impreso en la mesa que apunte a:

```
https://tudominio.com/mesa/{numeroMesa}
o
https://api.tudominio.com/client/session?table={numeroMesa}
```

### Tabla de QRs por Mesa

| Mesa | URL | QR |
|------|-----|-----|
| 1 | https://app.com/mesa/1 | [QR1] |
| 2 | https://app.com/mesa/2 | [QR2] |
| 3 | https://app.com/mesa/3 | [QR3] |
| 4 | https://app.com/mesa/4 | [QR4] |
| 5 | https://app.com/mesa/5 | [QR5] |
| 6 | https://app.com/mesa/6 | [QR6] |
| 7 | https://app.com/mesa/7 | [QR7] |
| 8 | https://app.com/mesa/8 | [QR8] |
| 9 | https://app.com/mesa/9 | [QR9] |
| 10 | https://app.com/mesa/10 | [QR10] |

---

## 🔗 Flujo al Escanear QR

```
Cliente escanea QR
        ↓
Navegador abre: https://app.com/mesa/5
        ↓
Frontend hace: GET /api/client/session?table=5
        ↓
Backend responde con sessionToken (en cookie)
        ↓
Frontend redirige a /menu o /dashboard
        ↓
Cliente ve el menú y puede hacer pedidos
```

### Implementación en Frontend

```javascript
// En la página principal (index.html o app.js)
const urlParams = new URLSearchParams(window.location.search);
let tableNumber = urlParams.get('mesa') || urlParams.get('table');

if (tableNumber) {
  // Abrir sesión
  openTableSession(tableNumber).then(() => {
    // Redirigir a menú
    window.location.href = '/menu';
  });
}
```

---

## 🎨 Generador de QRs

### Con JavaScript

```javascript
// Instalar: npm install qrcode

import QRCode from 'qrcode';

async function generateQRForTable(tableNumber) {
  const url = `https://tudominio.com/mesa/${tableNumber}`;
  
  const canvas = document.getElementById(`qr-table-${tableNumber}`);
  
  await QRCode.toCanvas(canvas, url, {
    errorCorrectionLevel: 'H',
    type: 'image/jpeg',
    quality: 1,
    margin: 1,
    width: 300,
    color: {
      dark: "#000000",
      light: "#FFFFFF"
    }
  });
}

// Generar QRs para todas las mesas
for (let i = 1; i <= 10; i++) {
  generateQRForTable(i);
}
```

### Con Backend (Java)

```java
@GetMapping("/api/admin/tables/{number}/qr")
public ResponseEntity<BufferedImage> generateTableQR(@PathVariable Integer number) 
        throws WriterException {
    
    String url = "https://tudominio.com/mesa/" + number;
    
    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    BitMatrix bitMatrix = qrCodeWriter.encode(
        url, 
        BarcodeFormat.QR_CODE, 
        300, 300
    );
    
    BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
    
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_JPEG)
        .body(qrImage);
}
```

---

## 🖨️ Imprimir Etiquetas QR

### Python para Generar PDF con QRs

```python
from reportlab.lib.pagesizes import letter
from reportlab.lib.units import cm
from reportlab.pdfgen import canvas
from reportlab.platypus import Table, TableStyle
from qrcode import QRCode
import io

def generate_table_qr_labels(total_tables=10):
    # Crear PDF
    pdf_filename = "table_qr_labels.pdf"
    c = canvas.Canvas(pdf_filename, pagesize=letter)
    
    x_position = 1 * cm
    y_position = 27 * cm
    qr_size = 5 * cm
    
    for table_num in range(1, total_tables + 1):
        # Generar QR
        qr = QRCode()
        qr.add_data(f"https://tudominio.com/mesa/{table_num}")
        qr.make()
        
        qr_image = qr.make_image(fill_color="black", back_color="white")
        
        # Guardar QR temporalmente
        qr_io = io.BytesIO()
        qr_image.save(qr_io, format='PNG')
        qr_io.seek(0)
        
        # Dibujar en PDF
        c.drawString(x_position, y_position + qr_size + 0.5*cm, f"Mesa {table_num}")
        c.drawImage(qr_io, x_position, y_position - qr_size, 
                    width=qr_size, height=qr_size)
        
        # Siguiente posición
        x_position += qr_size + 1 * cm
        
        if x_position > 19 * cm:  # Siguiente fila
            x_position = 1 * cm
            y_position -= qr_size + 3 * cm
    
    c.save()
    print(f"PDF generado: {pdf_filename}")

# Generar etiquetas
generate_table_qr_labels(10)
```

---

## 🔐 Seguridad del QR

### Cookie de Sesión

```javascript
// Cookie automática establecida por el servidor
// Set-Cookie: sessionToken=...; HttpOnly; Secure; Path=/; Max-Age=14400

// En el cliente, la cookie se envía automáticamente
fetch('/api/client/orders', {
  credentials: 'include'  // ⭐ IMPORTANTE
})
```

### Validaciones

1. **TableSessionFilter** - Valida que:
   - Cookie `sessionToken` es válida
   - No ha expirado
   - Corresponde a una mesa existente

2. **Autorización en Endpoints** - Cada request:
   - Extrae tableNumber del token
   - Valida que la orden pertenece a esa mesa
   - Rechaza accesos no autorizados

### Ataques Prevenidos

- ✅ XSS (Cookie HttpOnly)
- ✅ CSRF (SameSite attribute)
- ✅ Token stealing (Secure flag)
- ✅ Mesa spoofing (Validación en backend)

---

## 📐 Especificación de Impresión

### Dimensiones Recomendadas

**Tamaño:** 10 cm × 10 cm (para imprimir en papel adhesivo)

**Contenido:**
```
┌─────────────────┐
│    Mesa 5       │
│                 │
│  [QR CODE]      │
│    10×10cm      │
│                 │
│ Escanea el QR   │
└─────────────────┘
```

### Materiales Sugeridos

- Papel adhesivo mate blanco: 10 cm × 10 cm
- Tinta de impresora color negro
- Laminado plastificado (protección)
- Ubicación: Centro superior de cada mesa

---

## 🌍 Variables de Entorno

```env
# URL Base de la Aplicación
APP_BASE_URL=https://tudominio.com

# URL de la API
API_BASE_URL=https://api.tudominio.com

# Puerto Backend
SERVER_PORT=8081

# Token Expiration (segundos)
SESSION_TOKEN_EXPIRY=14400  # 4 horas

# QR Code Size (píxeles)
QR_CODE_SIZE=300

# QR Error Correction Level
QR_ERROR_CORRECTION=H  # L, M, Q, H
```

---

## 📊 Casos de Uso

### Caso 1: Cliente Escanea y Ordena

```
T=0s: Cliente abre cámara, escanea QR
      ↓
T=1s: Browser abre /mesa/5
      ↓
T=2s: Frontend GET /api/client/session?table=5
      ↓
T=3s: Backend crea sessionToken, retorna
      ↓
T=4s: Cookie sessionToken se establece automáticamente
      ↓
T=5s: Cliente ve menú
      ↓
T=10s: Cliente crea orden
       POST /api/client/orders
       (Cookie se envía automáticamente)
      ↓
T=11s: Orden creada, mesero ve en cocina
```

### Caso 2: Mesa sin sesión activa

```
Caso: Cliente intenta acceder a /api/client/orders sin sessionToken

T=0s: Cliente hace GET /api/client/orders
      (Sin cookie sessionToken)
      ↓
T=1s: TableSessionFilter intercepta request
      ↓
T=2s: No encuentra sessionToken válido
      ↓
T=3s: Retorna 401 Unauthorized
      ↓
T=4s: Frontend redirige a /mesa/{table}
      para obtener nueva sesión
```

---

## 🎬 Flujo Completo en Diagrama

```
┌─ Cliente Llega ─────────────────────────────────────┐
│                                                      │
│  1. Escanea QR en mesa                              │
│     GET /mesa/{table}                               │
│         ↓                                            │
│  2. Frontend obtiene sesión                         │
│     GET /api/client/session?table=5                 │
│         ↓                                            │
│  3. Backend crea session + token                    │
│     Response: {sessionToken, tableNumber, ...}     │
│         ↓ (Cookie establecida automáticamente)      │
│  4. Cliente redirigido a /menu                      │
│         ↓                                            │
│  5. Cliente ve productos                           │
│     GET /api/menu/products                         │
│         ↓ (Cookie enviada automáticamente)          │
│  6. Cliente selecciona items y crea orden           │
│     POST /api/client/orders                        │
│     {items, paymentMethod}                         │
│         ↓ (Cookie validada en backend)              │
│  7. Orden creada exitosamente                      │
│     Mesero recibe alerta en cocina                 │
│                                                      │
└──────────────────────────────────────────────────────┘
```

---

## 🔧 Troubleshooting

### Problema: "sessionToken inválido"

**Causa:** Cookie no se envió con el request

**Solución:**
```javascript
// ❌ Incorrecto
fetch('/api/client/orders')

// ✅ Correcto
fetch('/api/client/orders', {
  credentials: 'include'  // Agregar esta línea
})
```

### Problema: "No se puede acceder desde otra mesa"

**Causa:** Cliente intenta acceder a orden de otra mesa

**Solución:**
- Validar que tableNumber en token coincide con la orden
- Esto es por diseño (seguridad)

### Problema: "Sesión expirada"

**Causa:** Han pasado 4 horas desde que abrió sesión

**Solución:**
- Cliente debe escanear QR de nuevo
- Obtiene nuevo sessionToken
- Puede continuar

---

## 📱 Apps Recomendadas para Generar/Leer QRs

### Android/iOS - Leer QR
- **Nativa:** Camera app (iOS 11+)
- **Google Lens**
- **QR Code Reader** (aplicación)

### Desktop - Generar QR
- https://qr-server.com/
- https://www.qr-code-generator.com/
- CLI: `qrencode -o mesa1.png "https://..."`

---

## 📋 Checklist de Implementación

- [ ] Generar QRs para todas las mesas
- [ ] Imprimir y plastificar etiquetas
- [ ] Colocar QRs en mesas
- [ ] Probar escaneo con cámara real
- [ ] Verificar que cookie se establece
- [ ] Verificar que cliente ve menú
- [ ] Verificar que orden se crea correctamente
- [ ] Verificar que mesero recibe orden
- [ ] Probar flujo completo cliente-mesero
- [ ] Documentar cambios en producción


