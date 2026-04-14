# 📋 Resumen de Cambios Realizados

## Archivos Modificados

### 1. **Product.java** ✏️
**Ubicación**: `src/main/java/co/edu/uniquindio/servly/model/entity/Product.java`

```diff
- @OneToOne
+ @ManyToOne
  @JoinColumn(name = "recipe_id", nullable = true)
  private Recipe recipe;
```

**Impacto**: Ahora múltiples productos pueden compartir la misma receta.

---

### 2. **Recipe.java** ✏️
**Ubicación**: `src/main/java/co/edu/uniquindio/servly/model/entity/Recipe.java`

```diff
  @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ItemDetail> itemDetailList;
  
+ @OneToMany(mappedBy = "recipe")
+ private List<Product> products;
```

**Impacto**: La relación es ahora bidireccional.

---

### 3. **V2__Fix_Product_Recipe_Relationship.sql** 📄 (Nuevo)
**Ubicación**: `src/main/resources/db/migration/V2__Fix_Product_Recipe_Relationship.sql`

```sql
ALTER TABLE product DROP CONSTRAINT IF EXISTS ukn7l68cove4a44kdep3s30rikv;
```

**Impacto**: Elimina la restricción única que impedía múltiples productos con la misma receta.

---

## ✨ Lo que Cambia en tu Aplicación

### ANTES (OneToOne)
```
Recipe #1 "Pasta" → puede usarse SOLO en UN producto
Recipe #2 "Arroz" → puede usarse SOLO en UN producto
```

### DESPUÉS (ManyToOne)
```
Recipe #1 "Pasta" → puede usarse en MÚLTIPLES productos
                     - Producto: "Pasta Carbonara"
                     - Producto: "Pasta Boloñesa"
                     - Producto: "Pasta Alfredo"
```

---

## 🚀 Próximos Pasos

1. **Compilar**: `./gradlew clean build` (o `gradle clean build` en Windows)
2. **Ejecutar**: La aplicación aplicará automáticamente la migración
3. **Probar**: Ya podrás crear múltiples productos con la misma receta

---

## 📊 Ejemplo de Uso en la API

**Antes (Fallaba)**:
```bash
# Crear Producto 1 con Receta 2
POST /api/admin/products
{
  "name": "Pasta Carbonara",
  "price": 12.99,
  "categoryId": 1,
  "recipeId": 2  ← Receta 2
}
# ✅ Éxito

# Crear Producto 2 con la misma Receta 2
POST /api/admin/products
{
  "name": "Pasta Boloñesa",
  "price": 13.99,
  "categoryId": 1,
  "recipeId": 2  ← Receta 2
}
# ❌ Error: duplicate key value violates unique constraint
```

**Después (Funciona)**:
```bash
# Crear Producto 1 con Receta 2
POST /api/admin/products
{
  "name": "Pasta Carbonara",
  "price": 12.99,
  "categoryId": 1,
  "recipeId": 2
}
# ✅ Éxito

# Crear Producto 2 con la misma Receta 2
POST /api/admin/products
{
  "name": "Pasta Boloñesa",
  "price": 13.99,
  "categoryId": 1,
  "recipeId": 2
}
# ✅ Éxito también
```

---

**Última actualización**: Abril 13, 2026

