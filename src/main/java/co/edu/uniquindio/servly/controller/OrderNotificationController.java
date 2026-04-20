package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.service.OrderNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Endpoint SSE para que el cliente reciba notificaciones de estado de su pedido.
 *
 * El cliente hace una sola petición GET y mantiene la conexión abierta.
 * El servidor empuja eventos cuando el estado de cualquier orden de esa mesa cambia.
 *
 * Autenticación: reutiliza el sessionToken de mesa (ROLE_CLIENT) que ya valida
 * TableSessionFilter. El tableNumber se extrae del principal "table:N".
 *
 * Uso desde el frontend:
 *   const es = new EventSource('/api/client/notifications/subscribe', { withCredentials: true });
 *   es.addEventListener('order-status', e => console.log(JSON.parse(e.data)));
 *   es.addEventListener('connected',    e => console.log('Conectado'));
 */
@Slf4j
@RestController
@RequestMapping("/api/client/notifications")
@RequiredArgsConstructor
public class OrderNotificationController {

    private final OrderNotificationService notificationService;

    /**
     * El cliente se suscribe a notificaciones de su mesa.
     * Requiere sessionToken válido (igual que cualquier otro endpoint /api/client/**).
     *
     * Eventos emitidos:
     *  - "connected"     → confirmación de conexión establecida
     *  - "order-status"  → { orderId, status, tableNumber, message }
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('CLIENT')")
    public SseEmitter subscribe(Authentication auth) {
        // El principal viene de TableSessionFilter: "table:5"
        String principal = (String) auth.getPrincipal();
        Integer tableNumber = Integer.parseInt(principal.split(":")[1]);

        log.info("Mesa {} suscribiéndose a notificaciones SSE", tableNumber);
        return notificationService.subscribe(tableNumber);
    }
}
