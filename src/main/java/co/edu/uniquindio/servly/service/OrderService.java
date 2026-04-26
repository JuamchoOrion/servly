package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Order.*;
import co.edu.uniquindio.servly.exception.NotFoundException;
import co.edu.uniquindio.servly.exception.ValidationException;
import co.edu.uniquindio.servly.metrics.InventoryMetricsService;
import co.edu.uniquindio.servly.model.entity.*;
import co.edu.uniquindio.servly.model.enums.OrderTableState;
import co.edu.uniquindio.servly.model.enums.OrderType;
import co.edu.uniquindio.servly.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Servicio de órdenes
 * IMPORTANTE: Cliente elige PRODUCTOS (no items)
 * Cada producto tiene RECIPE con ITEM_DETAIL_LIST
 * Validar disponibilidad ANTES de crear
 * Descontar items AL CAMBIAR A SERVED
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final RestaurantTableRepository tableRepository;
    private final TableSourceRepository tableSourceRepository;
    private final InventoryMetricsService metricsService;
    private final ProductAvailabilityService availabilityService;
    private final OrderNotificationService notificationService;
    private final HelpAlertService helpAlertService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        // Obtener o crear TableSource (debe existir solo uno por mesa)
        TableSource tableSource = tableSourceRepository.findByTableNumber(request.getTableNumber())
                .orElseGet(() -> {
                    TableSource newTableSource = new TableSource();
                    newTableSource.setRestaurantTable(table);
                    newTableSource.setTableNumber(request.getTableNumber());
                    return tableSourceRepository.save(newTableSource);
                });

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

            // Convertir items opcionales a JSON
            String optionalItemsJson = convertOptionalItemsToJson(product, productRequest.getItemQuantityOverrides());

            Order_detail detail = Order_detail.builder()
                    .quantity(productRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .taxPercent(BigDecimal.valueOf(0.08))
                    .annotations(productRequest.getAnnotations())
                    .subtotal(unitPrice.multiply(BigDecimal.valueOf(productRequest.getQuantity())))
                    .product(product)
                    .optionalItems(optionalItemsJson)
                    .build();

            details.add(detail);
            total = total.add(detail.getSubtotal());
        }

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

        // Guardar orden
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

            // Convertir items opcionales a JSON
            String optionalItemsJson = convertOptionalItemsToJson(product, productRequest.getItemQuantityOverrides());

            Order_detail detail = Order_detail.builder()
                    .quantity(productRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .taxPercent(BigDecimal.valueOf(0.08))
                    .subtotal(unitPrice.multiply(BigDecimal.valueOf(productRequest.getQuantity())))
                    .product(product)
                    .optionalItems(optionalItemsJson)
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
     * Confirma el pago y cambia el estado a PAID
     * Solo ejecuta si la orden está en estado SERVED
     * El descuento de inventario se realiza cuando la orden cambia a SERVED
     */
    @Transactional
    public OrderDTO confirmPayment(Long orderId) {
        log.info("Confirmando pago para orden: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada: " + orderId));

        // Validar que está en estado SERVED antes de cambiar a PAID
        if (order.getStatus() != OrderTableState.SERVED) {
            throw new ValidationException("La orden debe estar en estado SERVED para confirmar el pago. Estado actual: " + order.getStatus());
        }

        // Cambiar estado a PAID
        order.setStatus(OrderTableState.PAID);
        Order updated = orderRepository.save(order);
        log.info("Pago confirmado para orden: {} - Estado cambió a PAID", orderId);

        // 🟢 LIBERAR LA MESA: Cambiar estado a AVAILABLE
        if (order.getSource() != null && order.getSource() instanceof TableSource) {
            TableSource tableSource = (TableSource) order.getSource();
            if (tableSource.getRestaurantTable() != null) {
                RestaurantTable table = tableSource.getRestaurantTable();
                table.setStatus(RestaurantTable.TableStatus.AVAILABLE);
                tableRepository.save(table);
                log.info("🟢 Mesa {} liberada - Estado cambió a AVAILABLE (pago confirmado)", table.getTableNumber());
            }
        }

        // Notificar al cliente SSE una vez que la transacción se confirme
        OrderDTO result = toDTO(updated);
        emitNotificationAfterCommit(result);

        return result;
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
     * PENDING → IN_PREPARATION → SERVED → PAID
     * Cuando cambia a SERVED se descuenta el inventario
     * Cuando cambia a PAID y es de mesa, se libera la mesa
     */
    public OrderDTO updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        log.info("=== INICIANDO ACTUALIZACIÓN DE ESTADO DE ORDEN ===");
        log.info("Orden ID: {}, Nuevo estado solicitado: {}", orderId, request.getStatus());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada: " + orderId));

        log.debug("Orden encontrada - Tipo: {}, Estado actual: {}", order.getOrderType(), order.getStatus());

        // Validar transición de estado
        validateStatusTransition(order.getStatus(), request.getStatus());

        // Si cambia a SERVED, descontar inventario
        if (request.getStatus().equals(OrderTableState.SERVED) && !order.getStatus().equals(OrderTableState.SERVED)) {
            log.info("Descuentando inventario para orden: {} al cambiar a SERVED", orderId);
            for (Order_detail detail : order.getOrderDetailList()) {
                availabilityService.deductInventoryForProduct(detail.getProduct(), detail.getQuantity());
                log.debug("✓ Inventario descontado: {} x{}", detail.getProduct().getName(), detail.getQuantity());
            }
            log.info("✓ Inventario completamente descontado para orden {}", orderId);
        }

        // Si cambia a PAID y es una orden de mesa, liberar la mesa
        if (request.getStatus().equals(OrderTableState.PAID) && order.getOrderType().equals(OrderType.TABLE)) {
            log.info("EVENTO: Orden {} marcada como PAGADA - Procesando liberación de mesa", orderId);
            if (order.getSource() instanceof TableSource) {
                TableSource tableSource = (TableSource) order.getSource();
                RestaurantTable table = tableSource.getRestaurantTable();
                if (table != null) {
                    log.info("Mesa {} (ID: {}) - Estado anterior: OCCUPIED → AVAILABLE",
                             table.getTableNumber(), table.getId());
                    table.setStatus(RestaurantTable.TableStatus.AVAILABLE);
                    RestaurantTable savedTable = tableRepository.save(table);
                    log.info("✓ Mesa {} ahora está {} después del pago de orden {}",
                             savedTable.getTableNumber(), savedTable.getStatus(), orderId);
                    log.info("✓ Mesa disponible nuevamente para nuevos clientes");
                } else {
                    log.warn("No se encontró RestaurantTable para la orden de mesa {}", orderId);
                }
            }
        } else if (request.getStatus().equals(OrderTableState.PAID)) {
            log.debug("Orden {} es de tipo {} - No se libera mesa (solo órdenes de mesa se liberan)",
                     orderId, order.getOrderType());
        }

        // Actualizar estado
        order.setStatus(request.getStatus());
        Order updated = orderRepository.save(order);

        log.info("✓ Orden {} actualizada a estado: {}", orderId, request.getStatus());
        log.info("=== ACTUALIZACIÓN COMPLETADA ===");

        // Notificar al cliente SSE una vez que la transacción se confirme
        OrderDTO result = toDTO(updated);
        emitNotificationAfterCommit(result);

        return result;
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
     * Maneja productos eliminados (soft delete) gracefully
     */
    private OrderDTO toDTO(Order order) {
        List<OrderDetailDTO> details = order.getOrderDetailList().stream()
                .map(d -> {
                    try {
                        // Intentar cargar el producto
                        Long productId = d.getProduct().getId();
                        String productName = d.getProduct().getName();

                        return OrderDetailDTO.builder()
                                .id(d.getId())
                                .itemId(productId)
                                .itemName(productName)
                                .quantity(d.getQuantity())
                                .unitPrice(d.getUnitPrice())
                                .taxPercent(d.getTaxPercent())
                                .subtotal(d.getSubtotal())
                                .annotations(d.getAnnotations())
                                .optionalItems(d.getOptionalItems())
                                .build();
                    } catch (org.hibernate.ObjectNotFoundException | jakarta.persistence.EntityNotFoundException ex) {
                        // El producto fue eliminado (soft delete), usar info del detalle
                        log.warn("Producto eliminado en orden detail ID: {}", d.getId());
                        return OrderDetailDTO.builder()
                                .id(d.getId())
                                .itemId(null)
                                .itemName("[Producto Eliminado]")
                                .quantity(d.getQuantity())
                                .unitPrice(d.getUnitPrice())
                                .taxPercent(d.getTaxPercent())
                                .subtotal(d.getSubtotal())
                                .annotations(d.getAnnotations())
                                .optionalItems(d.getOptionalItems())
                                .build();
                    }
                })
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
     * IMPORTANTE: CANCELLED puede ocurrir desde cualquier estado EXCEPTO PAID
     */
    private void validateStatusTransition(OrderTableState currentStatus, OrderTableState newStatus) {
        // Permitir cancelación desde cualquier estado EXCEPTO PAID y CANCELLED
        if (newStatus.equals(OrderTableState.CANCELLED)) {
            if (currentStatus.equals(OrderTableState.PAID)) {
                throw new IllegalStateException("No se puede cancelar una orden ya PAGADA");
            }
            if (currentStatus.equals(OrderTableState.CANCELLED)) {
                throw new IllegalStateException("La orden ya está CANCELADA");
            }
            return; // Cancelación permitida desde PENDING, IN_PREPARATION, SERVED
        }

        // Validar transiciones normales (no cancelación)
        switch (currentStatus) {
            case PENDING -> {
                if (!newStatus.equals(OrderTableState.IN_PREPARATION)) {
                    throw new IllegalStateException("De PENDING solo se puede ir a IN_PREPARATION");
                }
            }
            case IN_PREPARATION -> {
                if (!newStatus.equals(OrderTableState.SERVED)) {
                    throw new IllegalStateException("De IN_PREPARATION solo se puede ir a SERVED");
                }
            }
            case SERVED -> {
                if (!newStatus.equals(OrderTableState.PAID)) {
                    throw new IllegalStateException("De SERVED solo se puede ir a PAID");
                }
            }
            case PAID -> {
                throw new IllegalStateException("No se puede cambiar el estado de una orden PAID");
            }
            case CANCELLED -> {
                throw new IllegalStateException("No se puede cambiar el estado de una orden CANCELLED");
            }
        }
    }

    /**
     * Valida que haya items suficientes para la receta con variaciones
     */
    private void validateRecipeWithVariations(Product product, Integer quantity, Map<Long, Integer> itemQuantityOverrides) {
        if (product.getRecipe() == null) return;
        if (product.getRecipe().getItemDetailList() == null || product.getRecipe().getItemDetailList().isEmpty()) return;

        for (ItemDetail itemDetail : product.getRecipe().getItemDetailList()) {
            if (itemDetail == null || itemDetail.getItem() == null) {
                throw new ValidationException("Receta inválida: item detalles incompletos");
            }

            Item item = itemDetail.getItem();
            boolean isOptional = Boolean.TRUE.equals(itemDetail.getIsOptional());

            Integer selectedQuantity;

            if (itemQuantityOverrides != null && itemQuantityOverrides.containsKey(item.getId())) {
                selectedQuantity = itemQuantityOverrides.get(item.getId());
                // Si es opcional y el cliente eligió 0, simplemente no se valida ni se descuenta
                if (selectedQuantity == 0) continue;
            } else if (isOptional) {
                // Item opcional no incluido en overrides = el cliente no lo quiere
                continue;
            } else {
                // Item obligatorio sin override = usar cantidad base
                selectedQuantity = itemDetail.getQuantity();
            }

            Integer requiredQty = selectedQuantity * quantity;
            if (!availabilityService.isItemAvailable(item.getId(), requiredQty)) {
                String itemName = item.getName() != null ? item.getName() : "Item " + item.getId();
                throw new ValidationException("No hay " + itemName + " suficientes en inventario");
            }
        }
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
     * Crea una alerta de ayuda para una orden específica.
     * No cambia el estado de la orden, solo registra la solicitud de ayuda.
     * 
     * @param orderId ID de la orden
     * @param tableNumber Número de mesa
     */
    public void createHelpAlert(Long orderId, Integer tableNumber) {
        helpAlertService.createHelpAlert(orderId, tableNumber);
        log.info("Alerta de ayuda creada para orden: {} de mesa: {}", orderId, tableNumber);
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

    /**
     * MESERO: Crear orden de mesa
     * El mesero (staff) usa esta función para crear una orden directamente
     * sin que el cliente escanee QR
     * 
     * Flujo:
     * 1. Validar que la mesa existe y está disponible
     * 2. Validar productos y sus items
     * 3. Crear orden en estado PENDING
     * 4. NO descontar inventory (se descuenta al servir)
     * 
     * @param request Contiene tableNumber, products y notas
     * @return OrderDTO con detalles de la orden creada
     */
    public OrderDTO createTableOrderFromStaff(CreateStaffOrderRequest request) {
        log.info("Mesero creando orden para mesa: {}", request.getTableNumber());

        // Validar que la mesa existe
        RestaurantTable table = tableRepository.findByTableNumber(request.getTableNumber())
                .orElseThrow(() -> new NotFoundException("Mesa no encontrada: " + request.getTableNumber()));

        // Obtener o crear TableSource
        TableSource tableSource = tableSourceRepository.findByTableNumber(request.getTableNumber())
                .orElseGet(() -> {
                    TableSource newTableSource = new TableSource();
                    newTableSource.setRestaurantTable(table);
                    newTableSource.setTableNumber(request.getTableNumber());
                    return tableSourceRepository.save(newTableSource);
                });

        // Procesar productos
        List<Order_detail> details = new java.util.ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemVariationDTO productRequest : request.getProducts()) {
            Product product = productRepository.findById(productRequest.getProductId())
                    .orElseThrow(() -> new NotFoundException(
                            "Producto no encontrado: " + productRequest.getProductId()));

            // VALIDACIÓN: ¿Hay items para la receta con las variaciones?
            validateRecipeWithVariations(product, productRequest.getQuantity(), productRequest.getItemQuantityOverrides());

            // Calcular precio: base + items extras
            BigDecimal unitPrice = calculatePriceWithVariations(product, productRequest.getItemQuantityOverrides());

            // Convertir items opcionales a JSON
            String optionalItemsJson = convertOptionalItemsToJson(product, productRequest.getItemQuantityOverrides());

            Order_detail detail = Order_detail.builder()
                    .quantity(productRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .taxPercent(BigDecimal.valueOf(0.08))
                    .subtotal(unitPrice.multiply(BigDecimal.valueOf(productRequest.getQuantity())))
                    .product(product)
                    .optionalItems(optionalItemsJson)
                    .build();

            details.add(detail);
            total = total.add(detail.getSubtotal());
        }

        // Crear orden
        Order order = Order.builder()
                .date(LocalDate.now())
                .total(total)
                .orderType(OrderType.TABLE)
                .status(OrderTableState.PENDING)
                .source(tableSource)
                .orderDetailList(details)

                .build();

        // Asignar orden a cada detalle (relación bidireccional)
        details.forEach(detail -> detail.setOrder(order));

        // Guardar orden
        Order savedOrder = orderRepository.save(order);
        log.info("Orden creada para mesa {} con ID: {}", request.getTableNumber(), savedOrder.getId());

        // Convertir a DTO
        OrderDTO orderDTO = toDTO(savedOrder);

        // Emitir notificación a cliente (si está en la mesa)
        emitNotificationAfterCommit(orderDTO);

        return orderDTO;
    }

    /**
     * Emite la notificación SSE al cliente de la mesa DESPUÉS de que la transacción
     * se haya confirmado en BD (afterCommit), evitando notificar un estado que aún
     * podría revertirse por rollback.
     */
    private void emitNotificationAfterCommit(OrderDTO order) {
        if (order.getTableNumber() == null) {
            return; // órdenes delivery no tienen mesa
        }
        Integer tableNumber = order.getTableNumber();
        OrderTableState status = order.getStatus();
        Long orderId = order.getId();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationService.notifyOrderStatusChange(tableNumber, orderId, status);
            }
        });
    }

    /**
     * Convierte los items opcionales elegidos a formato JSON
     * Busca el nombre del item en la receta del producto
     *
     * @param product Producto con su receta
     * @param itemQuantityOverrides Mapa de itemId a cantidad elegida
     * @return String JSON con items opcionales: [{"itemId": 5, "itemName": "Queso", "quantity": 2}]
     */
    private String convertOptionalItemsToJson(Product product, Map<Long, Integer> itemQuantityOverrides) {
        if (itemQuantityOverrides == null || itemQuantityOverrides.isEmpty()) {
            return null;
        }

        try {
            List<Map<String, Object>> optionalItemsList = new java.util.ArrayList<>();

            // Si el producto tiene receta, obtener los nombres de los items
            if (product.getRecipe() != null && product.getRecipe().getItemDetailList() != null) {
                for (Map.Entry<Long, Integer> entry : itemQuantityOverrides.entrySet()) {
                    Long itemId = entry.getKey();
                    Integer quantity = entry.getValue();

                    // No guardar items con cantidad 0 (el cliente los deseleccionó)
                    if (quantity == null || quantity <= 0) continue;

                    String itemName = product.getRecipe().getItemDetailList().stream()
                            .filter(id -> id.getItem() != null && id.getItem().getId().equals(itemId))
                            .map(id -> id.getItem().getName())
                            .findFirst()
                            .orElse("Item " + itemId);

                    Map<String, Object> optionalItem = new HashMap<>();
                    optionalItem.put("itemId", itemId);
                    optionalItem.put("itemName", itemName);
                    optionalItem.put("quantity", quantity);
                    optionalItemsList.add(optionalItem);
                }
            }

            // Convertir a JSON
            if (optionalItemsList.isEmpty()) {
                return null;
            }

            String json = objectMapper.writeValueAsString(optionalItemsList);
            log.debug("Items opcionales convertidos a JSON: {}", json);
            return json;

        } catch (Exception e) {
            log.error("Error al convertir items opcionales a JSON", e);
            return null;
        }
    }
}

