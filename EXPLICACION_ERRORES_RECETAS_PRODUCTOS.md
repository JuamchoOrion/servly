# Explicación de Errores - Recetas y Productos

## Error #1: Duplicate Key en recipe_id

```
ERROR: duplicate key value violates unique constraint "ukn7l68cove4a44kdep3s30rikv"
Detail: Key (recipe_id)=(1) already exists.
```

### ¿Qué significa?
La tabla `product` tenía una restricción UNIQUE en el campo `recipe_id`. Esto significa que cada receta podía ser asignada a **UN SOLO producto**.

### ¿Por qué ocurrió?
Intentaste crear dos productos con la misma `recipe_id=1`. La BD lo rechazó porque viola la restricción.

### ✓ Solución
Se eliminó la restricción UNIQUE:
```sql
ALTER TABLE product DROP CONSTRAINT IF EXISTS ukn7l68cove4a44kdep3s30rikv;
```

Ahora **múltiples productos pueden compartir la misma receta**.

---

## Error #2: Foreign Key Constraint Violation

```
ERROR: update or delete on table "recipe" violates foreign key constraint "fkq23s1pgic4dt19yh7naql4svm"
Detail: Key (id)=(1) is still referenced from table "product".
```

### ¿Qué significa?
Intentaste eliminar una receta que está siendo usada por uno o más productos. La BD impide esto para mantener la **integridad referencial**.

Relación:
```
Product --> recipe_id (Foreign Key) --> Recipe.id
```

Si eliminas una receta, los productos quedarían huérfanos (pointing a nothing).

### ¿Por qué ocurrió?
- Producto A usa Recipe#1
- Intentas DELETE Recipe#1
- ❌ No permitido porque Product A sigue referenciando Recipe#1

### ✓ Solución
Implementar **Soft Delete** (eliminación lógica):

En lugar de:
```sql
DELETE FROM recipe WHERE id = 1;  -- ❌ Error
```

Hacer:
```sql
UPDATE recipe SET status = 'DELETED', deleted_at = NOW() WHERE id = 1;  -- ✓ OK
```

**Beneficios:**
- Los productos pueden seguir referenciando la receta
- El dato no se pierde
- Puedes reactivar la receta después
- La integridad referencial se mantiene

---

## Error #3: Hibernate Lazy Initialization

```
Type definition error: [simple type, class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor]
```

### ¿Qué significa?
Hibernate intenta serializar (convertir a JSON) una receta con sus relaciones, pero la sesión ya está cerrada. Los datos relacionados (itemDetailList) no se pueden cargar lazy.

### ¿Por qué ocurrió?
Al devolver una `Recipe` desde el controlador, Spring intenta convertirla a JSON. Hibernate usa lazy loading para `itemDetailList`, pero la sesión ya cerró.

### ✓ Soluciones Alternativas

**Opción 1: Usar DTOs (Recomendado)**
```java
@GetMapping("/{id}")
public RecipeDTO getRecipe(@PathVariable Long id) {
    Recipe recipe = recipeRepository.findById(id).orElseThrow();
    return RecipeDTO.fromEntity(recipe);  // DTO sin Hibernate proxies
}
```

**Opción 2: Eager Loading**
```java
@OneToMany(mappedBy = "recipe", fetch = FetchType.EAGER)
private List<ItemDetail> itemDetailList;
```

---

## Error #4: Check Constraint Violation en Orders

```
ERROR: new row for relation "orders" violates check constraint "orders_status_check"
Detail: Failing row contains (5, 2026-04-03, TABLE, 5.99, 6, PAID).
```

### ¿Qué significa?
La tabla `orders` tiene una restricción CHECK que valida los valores permitidos para el campo `status`.

```sql
ALTER TABLE orders ADD CONSTRAINT orders_status_check 
  CHECK (status IN ('PENDING', 'IN_PREPARATION', 'SERVED'));
```

Intentaste cambiar el status a `PAID`, que NO está permitido por la restricción.

### ¿Por qué ocurrió?
Tu lógica de negocio espera:
```
PENDING → IN_PREPARATION → SERVED → PAID
```

Pero la BD solo permite:
```
PENDING, IN_PREPARATION, SERVED
```

### ✓ Solución
Actualizar la restricción CHECK en la BD:

```sql
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;

ALTER TABLE orders ADD CONSTRAINT orders_status_check 
  CHECK (status IN ('PENDING', 'IN_PREPARATION', 'SERVED', 'PAID', 'CANCELLED'));
```

---

## Resumen de Cambios Necesarios

### En Supabase (SQL):
```sql
-- 1. Eliminar UNIQUE constraint en products
ALTER TABLE product DROP CONSTRAINT IF EXISTS ukn7l68cove4a44kdep3s30rikv;

-- 2. Agregar soft delete a recipes
ALTER TABLE recipe ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE recipe ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

-- 3. Actualizar check constraint en orders (si es necesario)
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_status_check 
  CHECK (status IN ('PENDING', 'IN_PREPARATION', 'SERVED', 'PAID', 'CANCELLED'));
```

### En Java:
- ✓ `Recipe.java` - Añadido status y deletedAt
- ✓ `RecipeService.java` - deleteRecipe() ahora hace soft delete
- Usa DTOs para evitar problemas de serialización

---

## Tabla Comparativa

| Restricción | Tipo | Acción | Solución |
|-------------|------|--------|----------|
| UNIQUE(recipe_id) | Estructura | Rechaza duplicados | DROP CONSTRAINT |
| Foreign Key | Integridad | Rechaza si hay refs | Soft Delete |
| CHECK(status) | Validación | Rechaza valores inválidos | Expandir valores permitidos |


