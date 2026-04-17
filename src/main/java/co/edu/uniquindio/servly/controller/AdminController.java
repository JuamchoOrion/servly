package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.Product.CreateProductRequest;
import co.edu.uniquindio.servly.DTO.Product.CreateProductCategoryRequest;
import co.edu.uniquindio.servly.DTO.Product.ProductDTO;
import co.edu.uniquindio.servly.DTO.Roles.CreateEmployeeRequest;
import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.DTO.Roles.UpdateRoleRequest;
import co.edu.uniquindio.servly.DTO.Roles.UserResponse;
import co.edu.uniquindio.servly.DTO.RestaurantTableDTO;
import co.edu.uniquindio.servly.DTO.CreateRestaurantTableRequest;
import co.edu.uniquindio.servly.DTO.Inventory.RecipeCreateRequest;
import co.edu.uniquindio.servly.DTO.Inventory.RecipeDTO;
import co.edu.uniquindio.servly.DTO.Inventory.ItemDetailCreateRequest;
import co.edu.uniquindio.servly.DTO.RecipeDetailDTO;
import co.edu.uniquindio.servly.model.dto.metrics.AuthenticationMetricsDTO;
import co.edu.uniquindio.servly.model.entity.Product;
import co.edu.uniquindio.servly.model.entity.ProductCategory;
import co.edu.uniquindio.servly.model.entity.RestaurantTable;
import co.edu.uniquindio.servly.service.UserService;
import co.edu.uniquindio.servly.service.CloudinaryService;
import org.springframework.web.multipart.MultipartFile;
import co.edu.uniquindio.servly.service.RecipeService;
import co.edu.uniquindio.servly.service.AuditService;
import co.edu.uniquindio.servly.service.ProductService;
import co.edu.uniquindio.servly.service.ProductCategoryService;
import co.edu.uniquindio.servly.service.RestaurantTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Endpoints exclusivos para ADMIN.
 * 
 * - Crear empleado con contraseña temporal
 * - Listar todos los usuarios
 * - Obtener usuario por ID
 * - Cambiar rol de usuario
 * - Activar/desactivar cuenta
 * - Eliminar usuario
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final AuditService auditService;
    private final ProductService productService;
    private final ProductCategoryService productCategoryService;
    private final RecipeService recipeService;
    private final RestaurantTableService restaurantTableService;
    private final CloudinaryService cloudinaryService;
    /**
     * Crea un empleado con contraseña temporal.
     * El empleado recibirá un email con:
     *  - Contraseña temporal
     *  - Instrucciones de primer login
     *  - 2FA obligatorio
     * 
     * El empleado deberá cambiar la contraseña en su primer login.
     */
    @PostMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request) {
        
        UserResponse employee = userService.createEmployee(request);
        return ResponseEntity.ok(employee);
    }

    /**
     * Lista todos los usuarios del sistema.
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Obtiene un usuario por su ID.
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    /**
     * Cambia el rol de un usuario.
     */
    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable String userId,
            @Valid @RequestBody UpdateRoleRequest request) {
        
        return ResponseEntity.ok(userService.updateUserRole(userId, request));
    }

    /**
     * Activa o desactiva la cuenta de un usuario.
     */
    @PatchMapping("/users/{userId}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> toggleUserStatus(@PathVariable String userId) {
        return ResponseEntity.ok(userService.toggleUserStatus(userId));
    }

    /**
     * Elimina un usuario del sistema.
     */
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable String userId) {
        return ResponseEntity.ok(userService.deleteUser(userId));
    }

    /**
     * Obtiene las métricas de autenticación de los últimos 7 días.
     *
     * Métricas incluidas:
     *  - Tiempo promedio de autenticación (meta: < 2 segundos)
     *  - Tasa de accesos exitosos por rol (meta: > 95%)
     *  - Tiempo promedio de recuperación de contraseña (meta: < 5 minutos)
     *  - Tiempo de verificación en dos pasos (meta: < 60 segundos)
     *  - Tasa de expiración de códigos de verificación (meta: < 10%)
     *  - Duración promedio de sesión activa
     */
    @GetMapping("/metrics/auth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthenticationMetricsDTO> getAuthMetricsLast7Days() {
        return ResponseEntity.ok(auditService.getLast7DaysMetrics());
    }

    /**
     * Obtiene las métricas de autenticación de los últimos 30 días.
     */
    @GetMapping("/metrics/auth/30days")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthenticationMetricsDTO> getAuthMetricsLast30Days() {
        return ResponseEntity.ok(auditService.getLast30DaysMetrics());
    }

    /**
     * Obtiene las métricas de autenticación para un período personalizado.
     *
     * @param start Fecha de inicio (formato: yyyy-MM-dd'T'HH:mm:ss)
     * @param end Fecha de fin (formato: yyyy-MM-dd'T'HH:mm:ss)
     */
    @GetMapping("/metrics/auth/custom")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthenticationMetricsDTO> getAuthMetricsCustom(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return ResponseEntity.ok(auditService.getAuthenticationMetrics(start, end));
    }

    /**
     * Endpoint de health check para monitoreo.
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> healthCheck() {
        return ResponseEntity.ok(new MessageResponse("Service is healthy"));
    }

    // ==================== CRUD RECETAS ====================

    /**
     * POST /api/admin/recipes
     * Crear una nueva receta con sus ItemDetails
     */
    @PostMapping("/recipes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecipeDTO> createRecipe(@Valid @RequestBody RecipeCreateRequest request) {
        RecipeDTO recipe = recipeService.createRecipe(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(recipe);
    }

    /**
     * GET /api/admin/recipes
     * Listar todas las recetas
     */
    @GetMapping("/recipes")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<RecipeDTO>> getAllRecipes() {
        return ResponseEntity.ok(recipeService.getAllRecipes());
    }

    /**
     * GET /api/admin/recipes/{id}
     * Obtener receta por ID
     */
    @GetMapping("/recipes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<RecipeDetailDTO> getRecipeById(@PathVariable Long id) {
        return ResponseEntity.ok(recipeService.getRecipeById(id));
    }

    /**
     * PUT /api/admin/recipes/{id}
     * Actualizar receta
     */
    @PutMapping("/recipes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecipeDTO> updateRecipe(
            @PathVariable Long id,
            @Valid @RequestBody RecipeCreateRequest request) {
        return ResponseEntity.ok(recipeService.updateRecipe(id, request));
    }

    /**
     * DELETE /api/admin/recipes/{id}
     * Eliminar receta
     */
    @DeleteMapping("/recipes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteRecipe(@PathVariable Long id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.ok(new MessageResponse("Receta eliminada correctamente"));
    }

    /**
     * POST /api/admin/recipes/{recipeId}/items
     * Agregar un ItemDetail a una receta
     */
    @PostMapping("/recipes/{recipeId}/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecipeDTO> addItemDetailToRecipe(
            @PathVariable Long recipeId,
            @Valid @RequestBody ItemDetailCreateRequest request) {
        return ResponseEntity.ok(recipeService.addItemDetailToRecipe(recipeId, request));
    }

    /**
     * DELETE /api/admin/recipes/{recipeId}/items/{itemDetailId}
     * Eliminar un ItemDetail de una receta
     */
    @DeleteMapping("/recipes/{recipeId}/items/{itemDetailId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> removeItemDetailFromRecipe(
            @PathVariable Long recipeId,
            @PathVariable Long itemDetailId) {
        recipeService.removeItemDetailFromRecipe(recipeId, itemDetailId);
        return ResponseEntity.ok(new MessageResponse("Item removido de la receta"));
    }

    // ==================== CRUD CATEGORIAS DE PRODUCTOS ====================

    /**
     * POST /api/admin/product-categories
     * Crear una nueva categoría de productos
     */
    @PostMapping("/product-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductCategory> createProductCategory(
            @Valid @RequestBody CreateProductCategoryRequest request) {
        ProductCategory category = productCategoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    /**
     * GET /api/admin/product-categories
     * Listar todas las categorías de productos
     */
    @GetMapping("/product-categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<ProductCategory>> getAllProductCategories() {
        return ResponseEntity.ok(productCategoryService.getAllCategories());
    }

    /**
     * GET /api/admin/product-categories/active
     * Listar categorías activas
     */
    @GetMapping("/product-categories/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<ProductCategory>> getActiveProductCategories() {
        return ResponseEntity.ok(productCategoryService.getAllActiveCategories());
    }

    /**
     * GET /api/admin/product-categories/{id}
     * Obtener categoría por ID
     */
    @GetMapping("/product-categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ProductCategory> getProductCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(productCategoryService.getCategoryById(id));
    }

    /**
     * PUT /api/admin/product-categories/{id}
     * Actualizar categoría
     */
    @PutMapping("/product-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductCategory> updateProductCategory(
            @PathVariable Long id,
            @Valid @RequestBody CreateProductCategoryRequest request) {
        ProductCategory category = productCategoryService.updateCategory(id, request);
        return ResponseEntity.ok(category);
    }

    /**
     * DELETE /api/admin/product-categories/{id}
     * Eliminar categoría
     */
    @DeleteMapping("/product-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteProductCategory(@PathVariable Long id) {
        productCategoryService.deleteCategory(id);
        return ResponseEntity.ok(new MessageResponse("Categoría de producto eliminada correctamente"));
    }

    // ==================== CRUD PRODUCTOS ====================

    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody CreateProductRequest request) {
        Product product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductDTO.fromEntity(product));
    }

    /**
     * POST /api/admin/products/with-image
     * Crear producto con imagen (FormData)
     *
     * Ejemplo de uso con curl:
     * curl -X POST http://localhost:8081/api/admin/products/with-image \
     *   -H "Authorization: Bearer YOUR_TOKEN" \
     *   -F "name=Pasta Carbonara" \
     *   -F "price=12.99" \
     *   -F "description=Pasta clásica italiana" \
     *   -F "categoryId=1" \
     *   -F "active=true" \
     *   -F "recipeId=1" \
     *   -F "image=@/ruta/a/imagen.jpg"
     */
    @PostMapping(value = "/products/with-image", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> createProductWithImage(
            @RequestParam("name") String name,
            @RequestParam("price") java.math.BigDecimal price,
            @RequestParam("description") String description,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("active") Boolean active,
            @RequestParam(value = "recipeId", required = false) Long recipeId,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        CreateProductRequest request = CreateProductRequest.builder()
                .name(name)
                .price(price)
                .description(description)
                .productCategoryId(categoryId)
                .active(active)
                .recipeId(recipeId)
                .build();

        // Subir imagen a Cloudinary si existe
        if (image != null && !image.isEmpty()) {
            java.util.Map<String, String> uploadResult = cloudinaryService.uploadImage(image, "products");
            String imageUrl = uploadResult.get("imageUrl");
            request.setImageUrl(imageUrl);
        }

        Product product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductDTO.fromEntity(product));
    }

    /**
     * GET /api/admin/products
     * Listar productos (paginado)
     */
    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Page<ProductDTO>> getAllProducts(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable).map(ProductDTO::fromEntity));
    }

    /**
     * GET /api/admin/products/active
     * Listar productos activos (paginado)
     */
    @GetMapping("/products/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Page<ProductDTO>> getActiveProducts(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(productService.getActiveProducts(pageable).map(ProductDTO::fromEntity));
    }

    /**
     * GET /api/admin/products/{id}
     * Obtener producto por ID
     */
    @GetMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ProductDTO.fromEntity(productService.getProductById(id)));
    }

    /**
     * PUT /api/admin/products/{id}
     * Actualizar producto
     */
    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) java.math.BigDecimal price,
            @RequestParam(required = false) Boolean active) {

        Product product = productService.updateProduct(id, name, description, price, active);
        return ResponseEntity.ok(ProductDTO.fromEntity(product));
    }

    /**
     * PUT /api/admin/products/{id}/with-image
     * Actualizar producto con imagen
     *
     * Ejemplo de uso con curl:
     * curl -X PUT http://localhost:8081/api/admin/products/1/with-image \
     *   -H "Authorization: Bearer YOUR_TOKEN" \
     *   -F "name=Pasta Carbonara Actualizada" \
     *   -F "price=13.99" \
     *   -F "description=Pasta clásica italiana mejorada" \
     *   -F "active=true" \
     *   -F "image=@/ruta/a/nueva-imagen.jpg"
     */
    @PutMapping(value = "/products/{id}/with-image", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> updateProductWithImage(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) java.math.BigDecimal price,
            @RequestParam(required = false) Boolean active,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        Product product = productService.getProductById(id);

        // Actualizar campos básicos
        if (name != null) product.setName(name);
        if (description != null) product.setDescription(description);
        if (price != null) product.setPrice(price);
        if (active != null) product.setActive(active);

        // Subir nueva imagen a Cloudinary si existe
        if (image != null && !image.isEmpty()) {
            java.util.Map<String, String> uploadResult = cloudinaryService.uploadImage(image, "products");
            String imageUrl = uploadResult.get("imageUrl");
            product.setImageUrl(imageUrl);
        }

        Product updatedProduct = productService.updateProduct(id, product.getName(), product.getDescription(), product.getPrice(), product.getActive());
        return ResponseEntity.ok(ProductDTO.fromEntity(updatedProduct));
    }

    /**
     * DELETE /api/admin/products/{id}
     * Eliminar producto (borrado lógico)
     */
    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(new MessageResponse("Producto eliminado correctamente"));
    }

    /**
     * PATCH /api/admin/products/{id}/activate
     * Activar producto
     */
    @PatchMapping("/products/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> activateProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ProductDTO.fromEntity(productService.activateProduct(id)));
    }

    /**
     * PATCH /api/admin/products/{id}/deactivate
     * Desactivar producto
     */
    @PatchMapping("/products/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> deactivateProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ProductDTO.fromEntity(productService.deactivateProduct(id)));
    }

    // ============ ENDPOINTS PARA MESAS ============

    /**
     * POST /api/admin/tables
     * Crear una nueva mesa
     */
    @PostMapping("/tables")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestaurantTableDTO> createTable(
            @Valid @RequestBody CreateRestaurantTableRequest request) {
        RestaurantTable table = restaurantTableService.createTable(request.getTableNumber(), request.getCapacity(), request.getLocation());
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(table));
    }

    /**
     * GET /api/admin/tables
     * Obtener todas las mesas
     */
    @GetMapping("/tables")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<RestaurantTableDTO>> getAllTables() {
        List<RestaurantTableDTO> tables = restaurantTableService.getAllTables()
                .stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(tables);
    }

    /**
     * GET /api/admin/tables/{tableNumber}
     * Obtener mesa por número
     */
    @GetMapping("/tables/{tableNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<RestaurantTableDTO> getTableByNumber(@PathVariable Integer tableNumber) {
        RestaurantTable table = restaurantTableService.getTableByNumber(tableNumber);
        return ResponseEntity.ok(convertToDTO(table));
    }

    /**
     * PATCH /api/admin/tables/{tableNumber}/status
     * Cambiar estado de una mesa
     */
    @PatchMapping("/tables/{tableNumber}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestaurantTableDTO> updateTableStatus(
            @PathVariable Integer tableNumber,
            @RequestParam RestaurantTable.TableStatus status) {
        RestaurantTable updated = restaurantTableService.updateTableStatus(tableNumber, status);
        return ResponseEntity.ok(convertToDTO(updated));
    }

    /**
     * DELETE /api/admin/tables/{tableNumber}
     * Eliminar una mesa
     */
    @DeleteMapping("/tables/{tableNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteTable(@PathVariable Integer tableNumber) {
        restaurantTableService.deleteTable(tableNumber);
        return ResponseEntity.ok(new MessageResponse("Mesa " + tableNumber + " eliminada correctamente"));
    }

    private RestaurantTableDTO convertToDTO(RestaurantTable table) {
        return RestaurantTableDTO.builder()
                .id(table.getId())
                .tableNumber(table.getTableNumber())
                .capacity(table.getCapacity())
                .location(table.getLocation())
                .status(table.getStatus())
                .createdAt(table.getCreatedAt())
                .build();
    }
}
