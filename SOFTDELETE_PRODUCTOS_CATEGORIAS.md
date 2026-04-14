# Implementación de Soft Delete para Productos y Categorías

## Resumen de Cambios

Se ha implementado **soft delete** (borrado lógico) para las entidades de **Productos** y **Categorías de Productos**, manteniéndose coherente con la implementación ya existente en **Categorías de Items**.

## Entidades Modificadas

### 1. **Product.java**
- ✅ Agregados campos:
  - `deleted` (Boolean, NOT NULL, DEFAULT false)
  - `deletedAt` (LocalDateTime, nullable)

### 2. **ProductCategory.java**
- ✅ Agregados campos:
  - `deleted` (Boolean, NOT NULL, DEFAULT false)
  - `deletedAt` (LocalDateTime, nullable)
  - `active` ahora es NOT NULL con DEFAULT true (antes era nullable)

### 3. **ItemCategory.java**
- ✅ Ya tenía soft delete implementado (sin cambios)

## Repositorios Modificados

### ProductRepository.java
Nuevos métodos para filtrar por `deleted = false`:
- `findByCategory_IdAndDeletedFalse(Long categoryId)`
- `findByNameContainingIgnoreCaseAndDeletedFalse(String name)`
- `findByActiveTrueAndDeletedFalse()`
- `findByActiveTrueAndDeletedFalse(Pageable pageable)`
- `findByCategoryIdAndDeletedFalse(Long categoryId)`
- `findByIdAndDeletedFalse(Long id)`
- `findByDeletedFalse(Pageable pageable)`

### ProductCategoryRepository.java
Nuevos métodos para filtrar por `deleted = false`:
- `findByNameAndDeletedFalse(String name)`
- `findByActiveTrueAndDeletedFalse()`
- `findByDeletedFalse()`
- `findByIdAndDeletedFalse(Long id)`

## Servicios Modificados

### ProductService.java
- ✅ `createProduct()`: Inicializa `deleted = false` y `deletedAt = null`
- ✅ `getAllProducts()`: Usa `findByDeletedFalse(pageable)`
- ✅ `getActiveProducts()`: Usa `findByActiveTrueAndDeletedFalse(pageable)`
- ✅ `getProductById()`: Usa `findByIdAndDeletedFalse(id)`
- ✅ `deleteProduct()`: Implementa soft delete (marca como `deleted = true`, `deletedAt = now()`)
- ✅ `restoreProduct()`: Nuevo método para restaurar productos eliminados
- ✅ Otros métodos actualizados para usar las nuevas queries

### ProductCategoryService.java
- ✅ `createCategory()`: Inicializa `deleted = false` y `deletedAt = null`
- ✅ `getCategoryById()`: Usa `findByIdAndDeletedFalse(id)`
- ✅ `getAllActiveCategories()`: Usa `findByActiveTrueAndDeletedFalse()`
- ✅ `getAllCategories()`: Usa `findByDeletedFalse()`
- ✅ `deleteCategory()`: Implementa soft delete (marca como `deleted = true`, `deletedAt = now()`)
- ✅ `restoreCategory()`: Nuevo método para restaurar categorías eliminadas
- ✅ Validaciones de nombre actualizadas para filtrar por soft delete

### ItemCategoryService.java
- ✅ Ya tenía soft delete implementado (sin cambios)

## Migración de Base de Datos

Se creó el archivo de migración `V3__Add_Soft_Delete_To_Products_And_Categories.sql` que:

1. Agrega columnas `deleted` y `deleted_at` a la tabla `product`
2. Agrega columnas `deleted` y `deleted_at` a la tabla `product_categories`
3. Crea índices para mejorar performance en consultas con `deleted = false`

## Controladores (Sin Cambios)

Los controladores (`AdminController`) no requieren cambios ya que:
- Utilizan los métodos de servicio actualizados
- Los servicios manejan la lógica de soft delete automáticamente
- Los métodos DELETE ahora hacen soft delete en lugar de hard delete

## Consistencia

Todas las entidades relacionadas ahora implementan soft delete de manera consistente:
- ✅ ItemCategory (ya implementado)
- ✅ Product (nuevo)
- ✅ ProductCategory (nuevo)

## Beneficios

1. **Datos Seguros**: Los datos eliminados se conservan en la BD para auditoría
2. **Reversibilidad**: Los datos pueden ser restaurados si es necesario
3. **Integridad Referencial**: No hay conflictos con relaciones de clave foránea
4. **Auditoría**: Se preserva `deletedAt` para registrar cuándo se eliminó
5. **Compatibilidad**: Sigue el patrón ya implementado en ItemCategory

## Próximos Pasos (Opcionales)

- Implementar endpoints de restauración en los controladores
- Agregar filtros en búsquedas para mostrar/ocultar elementos eliminados según permisos
- Implementar lógica de "eliminación permanente" después de cierto tiempo (si es necesario)

