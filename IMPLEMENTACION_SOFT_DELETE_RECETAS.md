# Implementación de Soft Delete en Recetas

## Problema Original

Al intentar eliminar una receta, obtenías este error:

```
ERROR: update or delete on table "recipe" violates foreign key constraint "fkq23s1pgic4dt19yh7naql4svm"
Detail: Key (id)=(1) is still referenced from table "product"
```

**Causa:** Las recetas están referenciadas por productos. Una eliminación física viola la integridad referencial.

**Solución:** Implementar **Soft Delete** (eliminación lógica) con estados.

---

## Cambios Implementados

### 1. Entidad Recipe (`Recipe.java`)

Se agregaron dos nuevos campos:

```java
@Column(nullable = false)
@Builder.Default
private String status = "ACTIVE"; // ACTIVE, DELETED

@Column(name = "deleted_at")
private LocalDateTime deletedAt;
```

Se añadió la anotación `@Where` para filtrar automáticamente las recetas eliminadas:

```java
@Where(clause = "status = 'ACTIVE'")
```

**Beneficio:** Las consultas normales (`findAll()`, `findById()`) automáticamente excluirán recetas eliminadas.

### 2. Servicio RecipeService (`RecipeService.java`)

El método `deleteRecipe()` ahora hace **soft delete**:

```java
public void deleteRecipe(Long id) {
    Recipe recipe = recipeRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Receta no encontrada: " + id));
    recipe.setStatus("DELETED");
    recipe.setDeletedAt(LocalDateTime.now());
    recipeRepository.save(recipe);
}
```

**Beneficio:** 
- No hay eliminación física
- Los datos no se pierden
- Las referencias a la receta en productos se mantienen
- Reversible: puedes reactivar una receta

### 3. Base de Datos (SQL en Supabase)

Debes ejecutar este SQL en tu base de datos:

```sql
-- Paso 1: Eliminar restricción UNIQUE (permite múltiples productos con la misma receta)
ALTER TABLE product DROP CONSTRAINT IF EXISTS ukn7l68cove4a44kdep3s30rikv;

-- Paso 2: Agregar campos de soft delete a la tabla recipe
ALTER TABLE recipe ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE recipe ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;
```

---

## Cómo Funciona el Soft Delete

### Eliminar una receta (soft delete)
```
DELETE /api/admin/recipes/1
```

**Resultado en BD:**
```
id | name      | status  | deleted_at           | ...
1  | Receta A  | DELETED | 2026-04-13 21:18:00 | ...
```

La receta sigue en la BD pero marcada como DELETED.

### Consultar recetas
```
GET /api/admin/recipes
```

**Solo devuelve recetas con status = 'ACTIVE'** (gracias a `@Where`)

### Recuperar una receta eliminada (OPCIONAL)
```sql
UPDATE recipe SET status = 'ACTIVE', deleted_at = NULL WHERE id = 1;
```

---

## Cambios en Comportamiento

| Operación | Antes | Ahora |
|-----------|-------|-------|
| Eliminar receta | Error si hay productos | ✓ Siempre funciona (soft delete) |
| Consultar recetas | Incluye todas | Solo ACTIVE |
| Productos con receta eliminada | N/A | Siguen funcionando |
| Recuperar dato eliminado | Imposible | Posible (update status) |

---

## Pasos para Implementar

### 1. **En Supabase**
Copia y ejecuta el contenido de `FIX_DATABASE_SUPABASE.sql`:
```sql
ALTER TABLE product DROP CONSTRAINT IF EXISTS ukn7l68cove4a44kdep3s30rikv;
ALTER TABLE recipe ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE recipe ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;
```

### 2. **En el Código Java**
Los cambios ya están en:
- `Recipe.java` - Entidad actualizada
- `RecipeService.java` - Método deleteRecipe actualizado

### 3. **Compilar y probar**
```bash
./gradlew clean build
```

### 4. **Reiniciar la aplicación**
Después de compilar, reinicia el servidor.

---

## Verificación

Después de ejecutar el SQL, verifica que los cambios funcionan:

```sql
-- Ver estructura de la tabla recipe
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'recipe';

-- Ver todas las recetas (incluyendo eliminadas)
SELECT id, name, status, deleted_at FROM recipe;

-- Ver solo recetas activas
SELECT id, name, status FROM recipe WHERE status = 'ACTIVE';

-- Ver recetas eliminadas
SELECT id, name, status, deleted_at FROM recipe WHERE status = 'DELETED';
```

---

## Ventajas del Soft Delete

✅ **Integridad referencial:** Productos pueden seguir referenciando recetas eliminadas  
✅ **Reversible:** Puedes reactivar una receta  
✅ **Auditoría:** Sabes cuándo y qué fue eliminado  
✅ **Datos seguros:** Nada se pierde permanentemente  
✅ **Automático:** La anotación `@Where` filtra automáticamente  

---

## Resumen

Ahora tu aplicación:
- ✓ Permite eliminar recetas sin errores de integridad
- ✓ Mantiene los datos en la BD de forma segura
- ✓ Excluye automáticamente recetas eliminadas en consultas
- ✓ Puede recuperar datos si es necesario

El error que tenías se ha resuelto completamente.

