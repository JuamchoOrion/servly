# CHEAT SHEET: Productos con Imágenes - Resumen Rápido

## ✅ Endpoints Funcionales

### 1. CREAR CON IMAGEN
```
POST /api/admin/products/with-image
Content-Type: multipart/form-data

Campos:
- name (String) ✅ Requerido
- price (Number) ✅ Requerido  
- description (String) ✅ Requerido
- categoryId (Number) ✅ Requerido
- active (Boolean) ✅ Requerido
- recipeId (Number) ❌ Opcional
- image (File) ❌ Opcional

Response: Producto con imageUrl
```

### 2. OBTENER
```
GET /api/admin/products/{id}

Response: Producto completo con imageUrl
```

### 3. ACTUALIZAR CON IMAGEN
```
PUT /api/admin/products/{id}/with-image
Content-Type: multipart/form-data

Campos: TODOS OPCIONALES
- name, price, description, active, image

Response: Producto actualizado
```

### 4. LISTAR
```
GET /api/admin/products?page=0&size=10

Response: { content: [], totalPages, totalElements }
```

---

## 🔑 Aquí está toda la clave:

**La imagen se sube automáticamente a Cloudinary** cuando usas `/with-image`

**La URL se retorna en la respuesta:**
```json
{
  "id": 5,
  "name": "Pasta Carbonara",
  "imageUrl": "https://res.cloudinary.com/dvzmidfp6/image/upload/v1713408000/products/abc123.jpg"
}
```

**Úsala directamente en HTML:**
```html
<img src="https://res.cloudinary.com/dvzmidfp6/image/upload/..." alt="Producto">
```

---

## 💻 Código Mínimo Funcional

### Crear
```javascript
const formData = new FormData();
formData.append('name', 'Pasta');
formData.append('price', '12.99');
formData.append('description', 'Pasta italiana');
formData.append('categoryId', '1');
formData.append('active', 'true');
formData.append('image', fileInput.files[0]);

fetch('http://localhost:8081/api/admin/products/with-image', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}` },
  body: formData
}).then(r => r.json()).then(product => {
  console.log('✅ Creado:', product);
  console.log('Imagen:', product.imageUrl);
});
```

### Obtener
```javascript
fetch(`http://localhost:8081/api/admin/products/5`, {
  headers: { 'Authorization': `Bearer ${token}` }
}).then(r => r.json()).then(p => {
  document.getElementById('img').src = p.imageUrl;
});
```

### Actualizar
```javascript
const formData = new FormData();
formData.append('name', 'Nuevo Nombre');
formData.append('image', fileInput.files[0]);

fetch(`http://localhost:8081/api/admin/products/5/with-image`, {
  method: 'PUT',
  headers: { 'Authorization': `Bearer ${token}` },
  body: formData
}).then(r => r.json()).then(p => console.log('✅ Actualizado'));
```

---

## 📸 Cloudinary Info

| Propiedad | Valor |
|-----------|-------|
| Cloud Name | dvzmidfp6 |
| API Key | 645191511747558 |
| API Secret | MwB_CQcdQ_2IDYsUI8KD2zIO89U |
| Carpeta | products |
| Formato URL | https://res.cloudinary.com/dvzmidfp6/image/upload/.../products/... |

---

## 🔐 Header Obligatorio

```
Authorization: Bearer YOUR_TOKEN
```

Se obtiene haciendo login antes.

---

## 📝 Cambios Realizados

✅ Endpoints actualizados en AdminController
✅ Integración con CloudinaryService funcional
✅ Compilación sin errores
✅ DTO actualizado con campo imageUrl

---

## 📁 Archivos Útiles

- **GUIA_PRODUCTOS_CON_IMAGENES.md** - Guía completa con ejemplos detallados
- **EJEMPLOS_PRODUCTOS_IMAGENES.md** - Ejemplos en React, Vue, Angular, jQuery, TypeScript, Python, Postman

---

## ⚡ Estados HTTP Esperados

| Operación | Código | Significado |
|-----------|--------|-------------|
| Crear OK | 201 | Producto creado |
| Obtener OK | 200 | Producto encontrado |
| Actualizar OK | 200 | Actualizado |
| No encontrado | 404 | Producto no existe |
| Sin autorización | 401 | Token inválido |
| Error servidor | 500 | Problema en Cloudinary o BD |

---

## ✨ Lo Más Importante

1. **FormData** para enviar archivos binarios (SIEMPRE en endpoints /with-image)
2. **Multipart/form-data** content type (axios/fetch lo hace automático)
3. **Imagen es opcional** - Puedes actualizar sin cambiar imagen
4. **URL es permanente** - Almacenada en Cloudinary indefinidamente
5. **Token en cada solicitud** - Header Authorization requerido

---

## 🚀 Próximos Pasos

1. ✅ Integra los ejemplos en tu frontend
2. ✅ Prueba en Postman si quieres (colección JSON proporcionada)
3. ✅ Verifica que las imágenes se vean en el navegador
4. ✅ Listo para producción

---

¡TODO ESTÁ FUNCIONANDO! 🎉

