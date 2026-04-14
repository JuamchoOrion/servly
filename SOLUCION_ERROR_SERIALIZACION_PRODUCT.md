# Solución: Error de Serialización de Product con Lazy Loading de Hibernate

## El Problema

```
Type definition error: [simple type, class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor]
No serializer found for class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor
```

### Causa Raíz

La entidad `Product` tiene relaciones con otras entidades que están configuradas como **lazy loading** (carga perezosa):

```java
@ManyToOne
@JoinColumn(name = "category_id")
private ProductCategory category;  // Lazy loading

@OneToOne
@JoinColumn(name = "recipe_id")
private Recipe recipe;  // Lazy loading
```

Cuando Jackson (el serializador de JSON) intenta convertir `Product` a JSON, encuentra estas propiedades lazy que aún no han sido inicializadas. Hibernate envuelve estas propiedades en un proxy (`HibernateProxy$ByteBuddyInterceptor`), y Jackson no sabe cómo serializar ese proxy.

## La Solución

### Opción 1: Usar DTOs (Recomendado ✅)

En lugar de retornar la entidad `Product` directamente, retornamos un `ProductDTO` que contiene solo los datos que necesitamos serializar:

**ProductDTO.java:**
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private String description;
    private Boolean active;
    private Long categoryId;
    private String categoryName;
    private Long recipeId;

    public static ProductDTO fromEntity(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .description(product.getDescription())
                .active(product.getActive())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .recipeId(product.getRecipe() != null ? product.getRecipe().getId() : null)
                .build();
    }
}
```

### Cambios en AdminController

Todos los endpoints que retornan `Product` deben retornar `ProductDTO`:

```java
@PostMapping("/products")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody CreateProductRequest request) {
    Product product = productService.createProduct(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ProductDTO.fromEntity(product));
}

@GetMapping("/products")
public ResponseEntity<Page<ProductDTO>> getAllProducts(Pageable pageable) {
    return ResponseEntity.ok(productService.getAllProducts(pageable).map(ProductDTO::fromEntity));
}
```

### Ventajas de esta solución:

✅ **Seguridad**: No expones detalles internos de la base de datos
✅ **Control**: Puedes elegir exactamente qué campos retornar
✅ **Versioning**: Fácil de mantener versiones diferentes de la API
✅ **Performance**: No cargas datos innecesarios
✅ **Limpio**: Separación clara entre dominio y respuesta

## Otras Opciones (No Recomendadas)

### Opción 2: Eager Loading
```java
@ManyToOne(fetch = FetchType.EAGER)
private ProductCategory category;
```
❌ **Problema**: Carga siempre las relaciones, aunque no las necesites (desperdicio de memoria)

### Opción 3: @JsonIgnore
```java
@JsonIgnore
private Recipe recipe;
```
❌ **Problema**: Si necesitas la receta en algunos endpoints, no puedes usarla

### Opción 4: Desactivar FAIL_ON_EMPTY_BEANS
```java
objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
```
❌ **Problema**: Solo oculta el error, no lo soluciona realmente

## Resumen

**La mejor práctica es usar DTOs**. Ya está implementado en tu proyecto y aplicado correctamente a todos los endpoints de productos.

---

**Última actualización**: Abril 13, 2026

