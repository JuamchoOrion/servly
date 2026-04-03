package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Order.*;
import co.edu.uniquindio.servly.exception.NotFoundException;
import co.edu.uniquindio.servly.exception.ValidationException;
import co.edu.uniquindio.servly.metrics.InventoryMetricsService;
import co.edu.uniquindio.servly.model.entity.*;
import co.edu.uniquindio.servly.model.enums.OrderTableState;
import co.edu.uniquindio.servly.model.enums.OrderType;
import co.edu.uniquindio.servly.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de órdenes
 * IMPORTANTE: Cliente elige PRODUCTOS (no items)
 * Cada producto tiene RECIPE con ITEM_DETAIL_LIST
 * Validar disponibilidad ANTES de crear
 * Descontar items DESPUÉS de pagar
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final RestaurantTableRepository tableRepository;
    private final InventoryMetricsService metricsService;
    private final ProductAvailabilityService availabilityService;

    /**
     * Crea orden de mesa
     * 1. Valida PRODUCTOS existen
     * 2. Valida ITEMS suficientes para recetas
     * 3. Crea orden PENDING (sin descontar aún)
     */
    public OrderDTO createTableOrder(CreateTableOrderRequest request) {
        log.info("Creando orden para mesa: {}", request.getTableNumber());

        RestaurantTable table = tableRepository.findByTableNumber(request.getTableNumber())
                .orElseThrow(() -> new NotFoundException("Mesa no encontrada: " + request.getTableNumber()));

        List<Order_detail> details = new java.util.ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemVariationDTO productRequest : request.getItems()) {
            Product product = productRepository.findById(productRequest.getProductId())
                    .orElseThrow(() -> new NotFoundException(
                            "Producto no encontrado: " + productRequest.getProductId()));

            // VALIDACIÓN: ¿Hay items para la receta con las variaciones?
            validateRecipeWithVariations(product, productRequest.getQuantity(), productRequest.getItemQuantityOverrides());

            // Calcular precio: base + items extras
            BigDecimal unitPrice = calculatePriceWithVariations(product, productRequest.getItemQuantityOverrides());

            Order_detail detail = Order_detail.builder()
                    .quantity(productRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .taxPercent(BigDecimal.valueOf(0.08))
                    .subtotal(unitPrice.multiply(BigDecimal.valueOf(productRequest.getQuantity())))
                    .product(product)
                    .build();

            details.add(detail);
            total = total.add(detail.getSubtotal());
        }

        // Crear y construir todas las relaciones ANTES de persistir
        TableSource tableSource = new TableSource();
        tableSource.setRestaurantTable(table);
        tableSource.setTableNumber(table.getTableNumber());

        Order order = Order.builder()
                .date(LocalDate.now())
                .total(total)
                .orderType(OrderType.TABLE)
                .status(OrderTableState.PENDING)
                .source(tableSource)
                .orderDetailList(details)
                .build();

        // Asignar orden a cada detalle (relación bidireccional)
        details.forEach(d -> d.setOrder(order));

        // Guardar orden (cascada debería manejar tableSource y details)
        Order savedOrder = orderRepository.save(order);
        log.info("Orden creada: {}", savedOrder.getId());

        return toDTO(savedOrder);
    }

    /**
     * Crea orden delivery
     */
    public OrderDTO createDeliveryOrder(CreateDeliveryOrderRequest request) {
        log.info("Creando orden delivery para: {}", request.getClientName());

        List<Order_detail> details = new java.util.ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemVariationDTO productRequest : request.getItems()) {
            Product product = productRepository.findById(productRequest.getProductId())
                    .orElseThrow(() -> new NotFoundException(
                            "Producto no encontrado: " + productRequest.getProductId()));

            // VALIDACIÓN: ¿Hay items para la receta con las variaciones?
            validateRecipeWithVariations(product, productRequest.getQuantity(), productRequest.getItemQuantityOverrides());

            // Calcular precio: base + items extras
            BigDecimal unitPrice = calculatePriceWithVariations(product, productRequest.getItemQuantityOverrides());

            Order_detail detail = Order_detail.builder()
                    .quantity(productRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .taxPercent(BigDecimal.valueOf(0.08))
                    .subtotal(unitPrice.multiply(BigDecimal.valueOf(productRequest.getQuantity())))
                    .product(product)
                    .build();


            details.add(detail);
            total = total.add(detail.getSubtotal());
        }

        DeliverySource deliverySource = new DeliverySource();
        deliverySource.setAddress(request.getAddress());
        deliverySource.setPhoneNumber(request.getPhoneNumber());
        deliverySource.setClientName(request.getClientName());
        deliverySource.setDeliveryTime(request.getDeliveryTime());

        Order order = Order.builder()
                .date(LocalDate.now())
                .total(total)
                .orderType(OrderType.DELIVERY)
                .status(OrderTableState.PENDING)
                .source(deliverySource)
                .orderDetailList(details)
                .build();

        details.forEach(d -> d.setOrder(order));

        Order savedOrder = orderRepository.save(order);
        log.info("Orden delivery creada: {}", savedOrder.getId());

        return toDTO(savedOrder);
    }

    /**
     * DESCUENTA INVENTARIO cuando pasa a IN_PREPARATION
     */
    @Transactional
    public void confirmPaymentAndDeductInventory(Long orderId) {
        log.info("Descontando inventario para orden: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada: " + orderId));

        for (Order_detail detail : order.getOrderDetailList()) {
            availabilityService.deductInventoryForProduct(detail.getProduct(), detail.getQuantity());
        }

        log.info("Inventario descontado: {}", orderId);
    }

    /**
     * Obtiene una orden por ID
     */
    public OrderDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada: " + orderId));
        return toDTO(order);
    }

    // ... resto de métodos:

    /**
     * Obtiene órdenes de una mesa
     */
    public List<OrderDTO> getOrdersByTableNumber(Integer tableNumber) {
        return orderRepository.findByTableNumber(tableNumber).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene órdenes activas de una mesa
     */
    public List<OrderDTO> getActiveOrdersByTableNumber(Integer tableNumber) {
        return orderRepository.findByTableNumber(tableNumber).stream()
                .filter(o -> !o.getStatus().equals(OrderTableState.CANCELLED))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene órdenes pendientes (para cocina)
     */
    public List<OrderDTO> getPendingOrders() {
        return orderRepository.findByStatus(OrderTableState.PENDING)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene órdenes en preparación
     */
    public List<OrderDTO> getInPreparationOrders() {
        return orderRepository.findByStatus(OrderTableState.IN_PREPARATION)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Actualiza el estado de una orden
     * PENDING → IN_PREPARATION → SERVED → (Cliente confirma entrega)
     */
    public OrderDTO updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada: " + orderId));

        // Validar transición de estado
        validateStatusTransition(order.getStatus(), request.getStatus());

        order.setStatus(request.getStatus());
        Order updated = orderRepository.save(order);

        log.info("Orden {} actualizada a estado: {}", orderId, request.getStatus());
        return toDTO(updated);
    }

    /**
     * Marca una orden como entregada
     */
    public OrderDTO confirmDelivery(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada: " + orderId));

        if (!order.getStatus().equals(OrderTableState.SERVED)) {
            throw new IllegalStateException("Solo se pueden confirmar entregas de órdenes SERVED");
        }

        order.setStatus(OrderTableState.SERVED); // Ya está lista
        orderRepository.save(order);

        log.info("Entrega confirmada para orden: {}", orderId);
        return toDTO(order);
    }

    /**
     * Cancela una orden
     */
    public OrderDTO cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada: " + orderId));

        if (order.getStatus().equals(OrderTableState.SERVED) ||
            order.getStatus().equals(OrderTableState.CANCELLED)) {
            throw new IllegalStateException("No se puede cancelar una orden " + order.getStatus());
        }

        order.setStatus(OrderTableState.CANCELLED);
        orderRepository.save(order);

        log.info("Orden cancelada: {}", orderId);
        return toDTO(order);
    }

    /**
     * Convierte una orden a DTO
     */
    private OrderDTO toDTO(Order order) {
        List<OrderDetailDTO> details = order.getOrderDetailList().stream()
                .map(d -> OrderDetailDTO.builder()
                        .id(d.getId())
                        .itemId(d.getProduct().getId())
                        .itemName(d.getProduct().getName())
                        .quantity(d.getQuantity())
                        .unitPrice(d.getUnitPrice())
                        .taxPercent(d.getTaxPercent())
                        .subtotal(d.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        // Obtener número de mesa si es orden de mesa
        Integer tableNumber = null;
        if (order.getOrderType().equals(OrderType.TABLE) && order.getSource() instanceof TableSource) {
            TableSource ts = (TableSource) order.getSource();
            // Usar el tableNumber directamente de TableSource, no de la relación
            tableNumber = ts.getTableNumber();
            if (tableNumber == null && ts.getRestaurantTable() != null) {
                tableNumber = ts.getRestaurantTable().getTableNumber();
            }
        }

        return OrderDTO.builder()
                .id(order.getId())
                .tableNumber(tableNumber)
                .items(details)
                .status(order.getStatus())
                .total(order.getTotal())
                .createdAt(order.getDate().atStartOfDay())
                .orderType(order.getOrderType().toString())
                .build();
    }

    /**
     * Valida las transiciones de estado válidas
     */
    private void validateStatusTransition(OrderTableState currentStatus, OrderTableState newStatus) {
        switch (currentStatus) {
            case PENDING -> {
                if (!newStatus.equals(OrderTableState.IN_PREPARATION) &&
                    !newStatus.equals(OrderTableState.CANCELLED)) {
                    throw new IllegalStateException("De PENDING solo se puede ir a IN_PREPARATION o CANCELLED");
                }
            }
            case IN_PREPARATION -> {
                if (!newStatus.equals(OrderTableState.SERVED) &&
                    !newStatus.equals(OrderTableState.CANCELLED)) {
                    throw new IllegalStateException("De IN_PREPARATION solo se puede ir a SERVED o CANCELLED");
                }
            }
            case SERVED -> {
                throw new IllegalStateException("No se puede cambiar el estado de una orden SERVED");
            }
            case CANCELLED -> {
                throw new IllegalStateException("No se puede cambiar el estado de una orden CANCELLED");
            }
        }
    }

    /**
     * Valida que haya items suficientes para la receta con variaciones
     */
    private void validateRecipeWithVariations(Product product, Integer quantity, java.util.Map<Long, Integer> itemQuantityOverrides) {
        log.debug("Validando receta para producto: {} ({}) con cantidad: {}", product.getId(), product.getName(), quantity);

        // Si no tiene receta, se permite (producto simple sin items)
        if (product.getRecipe() == null) {
            log.debug("Producto {} no tiene receta asignada (producto simple)", product.getId());
            return;
        }

        if (product.getRecipe().getItemDetailList() == null || product.getRecipe().getItemDetailList().isEmpty()) {
            log.debug("Receta del producto {} está vacía", product.getId());
            return;
        }

        for (ItemDetail itemDetail : product.getRecipe().getItemDetailList()) {
            if (itemDetail == null || itemDetail.getItem() == null) {
                log.error("ItemDetail o Item nulo en receta de producto: {}", product.getId());
                throw new ValidationException("Receta inválida: item detalles incompletos");
            }

            Item item = itemDetail.getItem();
            // Obtener cantidad elegida o usar la cantidad base
            Integer selectedQuantity = itemQuantityOverrides != null && itemQuantityOverrides.containsKey(item.getId())
                    ? itemQuantityOverrides.get(item.getId())
                    : itemDetail.getQuantity();

            Integer requiredQty = selectedQuantity * quantity;
            log.debug("Validando disponibilidad de item {} ({}) - Requerido: {} (base: {}, qty: {})",
                    item.getId(), item.getName(), requiredQty, selectedQuantity, quantity);

            // Validar disponibilidad
            if (!availabilityService.isItemAvailable(item.getId(), requiredQty)) {
                String itemName = item.getName() != null ? item.getName() : "Item " + item.getId();
                log.warn("Stock insuficiente para item: {} (ID: {}, Requerido: {})", itemName, item.getId(), requiredQty);
                throw new ValidationException("No hay " + itemName + " suficientes en inventario");
            }
        }
        log.debug("Validación de receta exitosa para producto: {}", product.getId());
    }

    /**
     * Retorna el precio del producto (sin cálculo de extras)
     * Las variaciones de items NO afectan el precio, solo la cantidad de items del inventario
     */
    private BigDecimal calculatePriceWithVariations(Product product, java.util.Map<Long, Integer> itemQuantityOverrides) {
        // El precio es fijo del producto, las variaciones no lo modifican
        return product.getPrice();
    }

    /**
     * Crea orden de mesa desde cliente (solo necesita tableNumber)
     * Convierte CreateClientOrderRequest a CreateTableOrderRequest
     */
    public OrderDTO createTableOrderFromClient(Integer tableNumber, CreateClientOrderRequest request) {
        CreateTableOrderRequest tableOrderRequest = CreateTableOrderRequest.builder()
                .tableNumber(tableNumber)
                .items(request.getProducts())
                .build();
        return createTableOrder(tableOrderRequest);
    }
}
