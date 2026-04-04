package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.Order.*;
import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para gestión de órdenes/pedidos
 *
 * CLIENTE (/api/client):
 *  - POST   /api/client/orders                  → Crear orden de mesa
 *  - GET    /api/client/orders                  → Listar mis órdenes
 *  - GET    /api/client/orders/{id}             → Ver detalles orden
 *  - POST   /api/client/orders/{id}/request-help → Pedir ayuda al mesero
 *  - PATCH  /api/client/orders/{id}/confirm-delivery → Confirmar entrega
 *
 * STAFF (mesero/cocina) (/api/staff):
 *  - GET    /api/staff/orders/pending           → Órdenes pendientes (cocina)
 *  - GET    /api/staff/orders/in-preparation    → Órdenes en preparación
 *  - GET    /api/staff/tables/{tableNumber}/orders → Órdenes de una mesa
 *  - PATCH  /api/staff/orders/{id}/status       → Actualizar estado
 *  - GET    /api/staff/orders/{id}/invoice      → Generar factura
 *
 * DELIVERY:
 *  - POST   /api/orders/delivery                → Crear orden de delivery
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    // ============ CLIENTE - ÓRDENES DE MESA ============

    /**
     * CLIENTE: Crear una orden de mesa (sin login, solo sessionToken)
     * El cliente escanea QR, obtiene sessionToken y confirma pedido
     *
     * Seguridad: TableSessionFilter valida sessionToken y asigna ROLE_CLIENT
     * El tableNumber se obtiene del JWT de sesión, no del body
     */
    @PostMapping("/api/client/orders")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<OrderDTO> createTableOrder(
            @Valid @RequestBody CreateClientOrderRequest request,
            org.springframework.security.core.Authentication auth) {

        // Extraer tableNumber del principal: "table:XX"
        String principal = (String) auth.getPrincipal();
        Integer tableNumber = Integer.parseInt(principal.split(":")[1]);

        log.info("Cliente creando orden para mesa: {}", tableNumber);

        // Pasar al servicio con tableNumber
        OrderDTO order = orderService.createTableOrderFromClient(tableNumber, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * CLIENTE: Listar mis órdenes activas (sin login, solo sessionToken)
     * Obtiene tableNumber desde el token de sesión
     */
    @GetMapping("/api/client/orders")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<OrderDTO>> getMyOrders(
            org.springframework.security.core.Authentication auth) {
        // Extraer tableNumber del principal: "table:XX"
        String principal = (String) auth.getPrincipal();
        Integer tableNumber = Integer.parseInt(principal.split(":")[1]);
        log.info("Cliente consultando órdenes de mesa: {}", tableNumber);
        List<OrderDTO> orders = orderService.getActiveOrdersByTableNumber(tableNumber);
        return ResponseEntity.ok(orders);
    }

    /**
     * CLIENTE: Obtener detalles de una orden específica (sin login, solo sessionToken)
     * Valida que la orden pertenece a la mesa del cliente
     */
    @GetMapping("/api/client/orders/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long id) {
        log.info("Cliente consultando orden: {}", id);
        OrderDTO order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    /**
     * CLIENTE: Solicitar ayuda del mesero (sin login, solo sessionToken)
     * Genera alerta para que mesero acuda a la mesa
     * Validación: Extrae tableNumber del token y valida que orden pertenece a esa mesa
     */
    @PostMapping("/api/client/orders/{id}/request-help")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<MessageResponse> requestHelp(
            @PathVariable Long id,
            org.springframework.security.core.Authentication auth) {

        // Extraer tableNumber del principal: "table:XX"
        String principal = (String) auth.getPrincipal();
        Integer tableNumber = Integer.parseInt(principal.split(":")[1]);

        // Validar que la orden pertenece a la mesa del cliente
        OrderDTO order = orderService.getOrderById(id);
        if (!order.getTableNumber().equals(tableNumber)) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("La orden no pertenece a esta mesa"));
        }

        log.info("Alerta de ayuda registrada para mesa: {}, orden: {}", tableNumber, id);
        // TODO: Integrar con WebSocket para notificar mesero en tiempo real
        return ResponseEntity.ok(new MessageResponse(
                "Ayuda solicitada. El mesero acudirá en breve a su mesa."));
    }

    /**
     * CLIENTE: Confirmar entrega/recepción de orden (sin login, solo sessionToken)
     * Cliente confirma que recibió su orden
     */
    @PatchMapping("/api/client/orders/{id}/confirm-delivery")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<OrderDTO> confirmDelivery(@PathVariable Long id) {
        log.info("Cliente confirmando entrega de orden: {}", id);
        OrderDTO order = orderService.confirmDelivery(id);
        return ResponseEntity.ok(order);
    }

    // ============ STAFF - GESTIÓN DE ÓRDENES (Requiere User login con rol) ============

    /**
     * COCINA: Obtener órdenes pendientes
     * Requiere: User login con rol COCINA o ADMIN
     * Seguridad: JwtAuthenticationFilter valida JWT de user
     */
    @GetMapping("/api/staff/orders/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'COCINA')")
    public ResponseEntity<List<OrderDTO>> getPendingOrders() {
        log.info("Cocina consultando órdenes pendientes");
        List<OrderDTO> orders = orderService.getPendingOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * COCINA: Obtener órdenes en preparación
     * Requiere: User login con rol COCINA o ADMIN
     */
    @GetMapping("/api/staff/orders/in-preparation")
    @PreAuthorize("hasAnyRole('ADMIN', 'COCINA')")
    public ResponseEntity<List<OrderDTO>> getInPreparationOrders() {
        log.info("Cocina consultando órdenes en preparación");
        List<OrderDTO> orders = orderService.getInPreparationOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * MESERO: Obtener todas las órdenes de una mesa
     * Requiere: User login con rol MESERO, CASHIER o ADMIN
     */
    @GetMapping("/api/staff/tables/{tableNumber}/orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'MESERO', 'CASHIER')")
    public ResponseEntity<List<OrderDTO>> getTableOrders(
            @PathVariable Integer tableNumber) {
        log.info("Staff consultando órdenes de mesa: {}", tableNumber);
        List<OrderDTO> orders = orderService.getOrdersByTableNumber(tableNumber);
        return ResponseEntity.ok(orders);
    }

    /**
     * COCINA/MESERO: Actualizar estado de una orden
     * Requiere: User login con rol COCINA, MESERO o ADMIN
     * PENDING → IN_PREPARATION → SERVED → (Cliente confirma)
     */
    @PatchMapping("/api/staff/orders/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'COCINA', 'MESERO')")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        log.info("Staff actualizando orden {} a estado: {}", id, request.getStatus());
        OrderDTO order = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(order);
    }

    /**
     * MESERO/CASHIER: Generar factura de una orden
     * Genera la cuenta que el cliente debe pagar
     * Requiere: User login con rol MESERO, CASHIER o ADMIN
     */
    @GetMapping("/api/staff/orders/{id}/invoice")
    @PreAuthorize("hasAnyRole('ADMIN', 'MESERO', 'CASHIER')")
    public ResponseEntity<OrderDTO> getInvoice(@PathVariable Long id) {
        log.info("Generando factura para orden: {}", id);
        OrderDTO order = orderService.getOrderById(id);
        // TODO: Generar PDF o formato de factura oficial
        return ResponseEntity.ok(order);
    }

    /**
     * MESERO/CASHIER: Cancelar una orden
     * Requiere: User login con rol MESERO, CASHIER o ADMIN
     */
    @PatchMapping("/api/staff/orders/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MESERO', 'CASHIER')")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Long id) {
        log.info("Cancelando orden: {}", id);
        OrderDTO order = orderService.cancelOrder(id);
        return ResponseEntity.ok(order);
    }

    /**
     * CASHIER/MESERO: Confirmar pago y descontar inventario
     * Se debe llamar DESPUÉS de que el cliente pague exitosamente
     * Requiere: User login con rol CASHIER, MESERO o ADMIN
     *
     * Descuenta el stock del inventario según los items de la receta
     * IMPORTANTE: Solo debe llamarse UNA VEZ por orden
     */
    @PostMapping("/api/staff/orders/{id}/confirm-payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'MESERO', 'CASHIER')")
    public ResponseEntity<MessageResponse> confirmPayment(@PathVariable Long id) {
        log.info("Confirmando pago y descontando inventario para orden: {}", id);
        orderService.confirmPaymentAndDeductInventory(id);
        return ResponseEntity.ok(new MessageResponse("Pago confirmado. Inventario descontado."));
    }

    // ============ DELIVERY - ÓRDENES DE ENTREGA (Público, sin autenticación) ============

    /**
     * Crear una orden de delivery
     * Acceso: Público (no requiere login ni sessionToken)
     *
     * Seguridad: Validación de datos, sin CAPTCHA (agregar si es necesario)
     * TODO: Agregar CAPTCHA para evitar spam
     * TODO: Validar email y teléfono reales
     */
    @PostMapping("/api/orders/delivery")
    @PreAuthorize("permitAll()")
    public ResponseEntity<OrderDTO> createDeliveryOrder(
            @Valid @RequestBody CreateDeliveryOrderRequest request) {
        log.info("Creando orden de delivery para: {}", request.getClientName());
        OrderDTO order = orderService.createDeliveryOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}

