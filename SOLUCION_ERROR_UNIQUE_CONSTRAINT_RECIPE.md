# Solución: Error de Restricción Única en Product-Recipe

## ❌ El Problema

```
ERROR: duplicate key value violates unique constraint "ukn7l68cove4a44kdep3s30rikv"
Detail: Key (recipe_id)=(2) already exists.
```

### Causa Raíz

La entidad `Product` tenía una relación **`@OneToOne`** con `Recipe`:

```java
@OneToOne
@JoinColumn(name = "recipe_id", nullable = true)
private Recipe recipe;
```

Esto creaba una restricción única en la base de datos que impedía que **dos productos diferentes compartieran la misma receta**.

Cuando intentaste crear dos productos con `recipeId=2`, la base de datos rechazó la operación porque la receta ya estaba asignada a otro producto.

---

## ✅ La Solución

### Cambio 1: Modificar la Entidad Product

```java
// ANTES:
@OneToOne
@JoinColumn(name = "recipe_id", nullable = true)
private Recipe recipe;

// DESPUÉS:
@ManyToOne
@JoinColumn(name = "recipe_id", nullable = true)
private Recipe recipe;
```

**Razón**: Una receta puede ser compartida por múltiples productos. Por ejemplo:
- Múltiples "Pastas" pueden usar la receta "Pasta Básica"
- Múltiples "Pizzas" pueden usar la receta "Masa de Pizza"

### Cambio 2: Agregar Relación Inversa en Recipe

```java
// Se agregó en Recipe.java:
@OneToMany(mappedBy = "recipe")
private List<Product> products;
```

**Razón**: Para mantener la coherencia bidireccional de la relación.

### Cambio 3: Crear Migration de Base de Datos

Archivo: `V2__Fix_Product_Recipe_Relationship.sql`

```sql
-- Eliminar la restricción única que impedía múltiples productos por receta
ALTER TABLE product DROP CONSTRAINT IF EXISTS ukn7l68cove4a44kdep3s30rikv;
```

---

## 📝 Pasos para Aplicar la Solución

1. **Compilar el proyecto**: El cambio de `@OneToOne` a `@ManyToOne` se aplicará automáticamente
2. **Ejecutar la migración**: Flyway ejecutará automáticamente `V2__Fix_Product_Recipe_Relationship.sql`
3. **Reiniciar la aplicación**: La base de datos se actualizará automáticamente

---

## 🔍 Explicación de "quantity" en Recipe

La propiedad `quantity` en `Recipe` representa **la cantidad total de artículos que se producen con esta receta**.

Por ejemplo:
```json
{
  "id": 2,
  "name": "Pasta Carbonara",
  "quantity": 2,  // Esta receta produce 2 porciones
  "description": "Pasta clásica con huevo, queso y panceta"
}
```

Cuando creas un producto que usa esta receta, ese producto se prepara usando estos ingredientes en las cantidades especificadas en `ItemDetail`.

---

## 📚 Resumen de la Arquitectura

```
Recipe (1)
   ↓ (OneToMany)
Product (*)  ← Múltiples productos pueden usar la misma receta
   ↓ (OneToMany)
ItemDetail  ← Detalles de ingredientes y cantidades
   ↓ (ManyToOne)
Item        ← El ingrediente (ej: "Pasta", "Huevo")
```

---

**Última actualización**: Abril 13, 2026

