# 📨 MENSAJE PARA COMPARTIR CON EL EQUIPO FRONTEND

---

## ¡HOLA EQUIPO FRONTEND! 👋

Te compartimos la **GUÍA COMPLETA DE INTEGRACIÓN** para las APIs de **GESTIÓN DE PRODUCTOS** del AdminController.

He preparado todo lo necesario para que implementes estas funcionalidades de manera rápida y eficiente.

---

## 📦 ARCHIVOS PREPARADOS PARA TI

### 1. **GUIA_APIS_PRODUCTOS_ADMIN.md** 
**Contenido**: Documentación completa con:
- ✅ Todos los endpoints de productos (CRUD completo)
- ✅ Todos los endpoints de categorías (CRUD completo)
- ✅ Ejemplos de solicitudes y respuestas reales
- ✅ Código JavaScript/Fetch de ejemplo para cada endpoint
- ✅ Manejo de errores
- ✅ Tabla resumen rápido

**Uso**: Consulta este archivo cuando necesites:
- Entender qué parámetros enviar
- Ver ejemplos de respuestas
- Implementar manejo de errores
- Código base para copiar y pegar

---

### 2. **PROMPTS_APIS_PRODUCTOS.md**
**Contenido**: 13 prompts listos para usar:
- 8 prompts para CRUD de productos
- 5 prompts para CRUD de categorías

**Uso**: Copia y pega cada prompt en:
- Tu documentación de desarrollo
- Un LLM si usas asistencia de IA
- Tickets de desarrollo en tu gestor de proyectos
- Email al equipo frontend

**Ejemplo de cómo usar**:
```
Le paso el PROMPT 1 al equipo frontend para que implementen 
la funcionalidad de creación de productos
```

---

### 3. **test-apis-productos-admin.http**
**Contenido**: Archivo de pruebas HTTP completo con:
- ✅ Pruebas individuales de cada endpoint
- ✅ Flujo completo de creación de menú (15 pasos)
- ✅ Casos de error para debugging
- ✅ Ejemplos de paginación y ordenamiento

**Uso**: 
- Abre en Postman o VS Code (extensión REST Client)
- Reemplaza `{{token}}` con tu token JWT
- Ejecuta los requests para probar los endpoints
- Útil para verificar que tu backend funcione correctamente

---

## 🎯 ENDPOINTS DISPONIBLES (RESUMEN RÁPIDO)

### PRODUCTOS (8 operaciones)
| Operación | Método | Endpoint |
|-----------|--------|----------|
| Crear | POST | `/api/admin/products` |
| Listar | GET | `/api/admin/products` |
| Obtener uno | GET | `/api/admin/products/{id}` |
| Listar activos | GET | `/api/admin/products/active` |
| Actualizar | PUT | `/api/admin/products/{id}` |
| Activar | PATCH | `/api/admin/products/{id}/activate` |
| Desactivar | PATCH | `/api/admin/products/{id}/deactivate` |
| Eliminar | DELETE | `/api/admin/products/{id}` |

### CATEGORÍAS (5 operaciones)
| Operación | Método | Endpoint |
|-----------|--------|----------|
| Crear | POST | `/api/admin/product-categories` |
| Listar | GET | `/api/admin/product-categories` |
| Obtener uno | GET | `/api/admin/product-categories/{id}` |
| Listar activas | GET | `/api/admin/product-categories/active` |
| Actualizar | PUT | `/api/admin/product-categories/{id}` |
| Eliminar | DELETE | `/api/admin/product-categories/{id}` |

---

## 🔐 AUTENTICACIÓN REQUERIDA

**Todos los endpoints requieren**:
```javascript
headers: {
  'Authorization': 'Bearer {tu_token_jwt}',
  'Content-Type': 'application/json'
}
```

**Permisos requeridos**:
- ✅ **ADMIN**: Puede crear, actualizar y eliminar
- ✅ **STAFF**: Solo lectura (GET)

---

## 💡 CÓMO EMPEZAR

### Opción A: Usar los Prompts
1. Abre `PROMPTS_APIS_PRODUCTOS.md`
2. Copia el PROMPT 1 (Crear producto)
3. Pégalo en tu proyecto o comparte con tu equipo
4. Implementa la funcionalidad basándote en el prompt

### Opción B: Usar la Guía Completa
1. Abre `GUIA_APIS_PRODUCTOS_ADMIN.md`
2. Busca la sección que necesites (ej: "CREAR PRODUCTO")
3. Copia el código JavaScript de ejemplo
4. Adapta según tus necesidades

### Opción C: Probar Primero
1. Abre `test-apis-productos-admin.http` en VS Code
2. Instala la extensión "REST Client"
3. Reemplaza `{{token}}` con tu token JWT
4. Ejecuta los requests para probar
5. Luego implementa en el frontend basándote en las respuestas

---

## 📋 ORDEN RECOMENDADO DE IMPLEMENTACIÓN

### Fase 1: Productos Básicos
1. ✅ Implementar listado de productos (GET `/products`)
2. ✅ Implementar detalle de producto (GET `/products/{id}`)
3. ✅ Implementar crear producto (POST `/products`)
4. ✅ Implementar editar producto (PUT `/products/{id}`)
5. ✅ Implementar eliminar producto (DELETE `/products/{id}`)

### Fase 2: Activación/Desactivación
6. ✅ Implementar activar producto (PATCH `/products/{id}/activate`)
7. ✅ Implementar desactivar producto (PATCH `/products/{id}/deactivate`)
8. ✅ Mostrar solo productos activos en listado (GET `/products/active`)

### Fase 3: Categorías
9. ✅ Implementar CRUD de categorías
10. ✅ Cargar categorías en select al crear/editar producto
11. ✅ Mostrar categoría en cada producto

---

## 🛠️ EJEMPLO RÁPIDO (JavaScript)

```javascript
// Obtener todos los productos
const token = 'tu_token_aqui';
const response = await fetch('http://localhost:8081/api/admin/products', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
const productos = await response.json();
console.log(productos);

// Crear un producto
const nuevoProducto = await fetch('http://localhost:8081/api/admin/products', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    name: 'Hamburguesa',
    description: 'Deliciosa hamburguesa',
    price: 25000,
    categoryId: 1,
    recipeId: 5,
    active: true
  })
});
const producto = await nuevoProducto.json();
console.log('Producto creado:', producto);
```

---

## ❓ PREGUNTAS FRECUENTES

### ¿Qué es `categoryId` y `recipeId`?
- **categoryId**: ID de la categoría (ej: "Bebidas", "Comidas")
- **recipeId**: ID de la receta de preparación
- Ambos deben existir antes de crear un producto

### ¿Qué significa "borrado lógico"?
- Cuando eliminas un producto con DELETE, NO se borra de la BD
- Solo se marca como inactivo
- Se puede reactivar después

### ¿Puedo ver solo productos activos?
- Sí, usa el endpoint `GET /api/admin/products/active`
- Retorna solo productos con `active: true`

### ¿Qué significa paginación?
- Los resultados se dividen en páginas
- Por defecto: 10 productos por página
- Puedes cambiar con `?page=0&size=10`

### ¿Cómo ordeno los resultados?
- Usa `?sort=name,asc` para ordenar por nombre (ascendente)
- Usa `?sort=price,desc` para ordenar por precio (descendente)
- Ejemplo: `GET /api/admin/products?sort=price,asc`

---

## 📞 SOPORTE

Si encuentras problemas:

1. **Revisa la guía**: Busca en `GUIA_APIS_PRODUCTOS_ADMIN.md`
2. **Verifica el token**: Asegúrate de que el JWT sea válido
3. **Revisa los logs**: El servidor muestra errores detallados
4. **Prueba con el archivo HTTP**: Usa `test-apis-productos-admin.http` para verificar
5. **Revisa los ejemplos**: Copia código de ejemplo y adapta

---

## 📚 ARCHIVOS INCLUIDOS

```
📦 Tu Proyecto
 ├─ GUIA_APIS_PRODUCTOS_ADMIN.md          ← Documentación completa
 ├─ PROMPTS_APIS_PRODUCTOS.md            ← 13 prompts listos
 ├─ test-apis-productos-admin.http       ← Pruebas HTTP
 └─ MENSAJE_PARA_COMPARTIR.md            ← Este archivo
```

---

## ✅ CHECKLIST DE IMPLEMENTACIÓN

Cuando hayas terminado, verifica:

- [ ] Puedo crear productos
- [ ] Puedo ver la lista de productos
- [ ] Puedo ver detalles de un producto
- [ ] Puedo editar un producto
- [ ] Puedo activar/desactivar productos
- [ ] Puedo eliminar productos
- [ ] Puedo crear categorías
- [ ] Puedo ver la lista de categorías
- [ ] Puedo editar categorías
- [ ] Puedo eliminar categorías
- [ ] Las categorías aparecen en el select al crear/editar
- [ ] La paginación funciona
- [ ] El ordenamiento funciona
- [ ] El filtro de activos funciona
- [ ] Los mensajes de error se muestran correctamente

---

## 🚀 ¡YA ESTÁS LISTO!

Tienes todo lo necesario para implementar la gestión de productos en el frontend.

**Próximos pasos**:
1. Lee la guía rápidamente
2. Copia los prompts a tus tickets
3. Prueba los endpoints con el archivo HTTP
4. Implementa en el frontend
5. ¡Diviértete! 🎉

---

**Preparado por**: DevOps Team
**Fecha**: 2026-04-13
**Estado**: ✅ Listo para usar

