package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.model.entity.HelpAlert;
import co.edu.uniquindio.servly.model.enums.OrderTableState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Gestiona las conexiones SSE activas de los clientes por mesa.
 *
 * Una mesa puede tener múltiples pestañas/dispositivos conectados (CopyOnWriteArrayList).
 * Cuando el estado de una orden cambia, se notifica a todos los listeners de esa mesa.
 *
 * Ciclo de vida del SseEmitter:
 *  - Cliente conecta → se registra en el mapa
 *  - Servidor emite evento → SseEmitter.send()
 *  - Cliente desconecta / timeout → onCompletion/onTimeout limpian el mapa
 */
@Slf4j
@Service
public class OrderNotificationService {

    // tableNumber -> lista de emitters activos para esa mesa
    private final Map<Integer, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // Timeout de 4 horas — igual que la sesión de mesa
    private static final long SSE_TIMEOUT_MS = 4 * 60 * 60 * 1000L;

    /**
     * Registra un nuevo SseEmitter para la mesa indicada.
     * Se llama cuando el cliente hace GET /api/client/notifications/subscribe
     */
    public SseEmitter subscribe(Integer tableNumber) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        // Agregar al mapa de la mesa
        emitters.computeIfAbsent(tableNumber, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("Cliente suscrito a notificaciones de mesa {}", tableNumber);

        // Limpiar al completar o timeout
        Runnable cleanup = () -> {
            List<SseEmitter> list = emitters.get(tableNumber);
            if (list != null) {
                list.remove(emitter);
                if (list.isEmpty()) {
                    emitters.remove(tableNumber);
                }
            }
            log.debug("Emitter eliminado para mesa {}", tableNumber);
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ex -> {
            log.debug("Error en emitter de mesa {}: {}", tableNumber, ex.getMessage());
            cleanup.run();
        });

        // Enviar evento inicial de conexión confirmada
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of(
                            "message", "Conectado a notificaciones de mesa " + tableNumber,
                            "tableNumber", tableNumber
                    )));
        } catch (IOException e) {
            log.warn("No se pudo enviar evento de conexión a mesa {}: {}", tableNumber, e.getMessage());
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Emite una notificación de cambio de estado de orden a todos los listeners de la mesa.
     * Se llama desde OrderService cuando cambia el estado de una orden.
     */
    public void notifyOrderStatusChange(Integer tableNumber, Long orderId, OrderTableState newStatus) {
        List<SseEmitter> tableEmitters = emitters.get(tableNumber);
        if (tableEmitters == null || tableEmitters.isEmpty()) {
            log.debug("Sin suscriptores activos para mesa {} al cambiar orden {} a {}", tableNumber, orderId, newStatus);
            return;
        }

        OrderStatusEvent event = new OrderStatusEvent(orderId, newStatus, tableNumber, buildStatusMessage(newStatus));

        // Iterar en copia para evitar ConcurrentModificationException
        List<SseEmitter> snapshot = List.copyOf(tableEmitters);
        for (SseEmitter emitter : snapshot) {
            try {
                emitter.send(SseEmitter.event()
                        .name("order-status")
                        .data(event));
                log.debug("Notificación enviada a mesa {} → orden {} estado {}", tableNumber, orderId, newStatus);
            } catch (IOException e) {
                log.warn("Error enviando notificación a mesa {}: {}. Eliminando emitter.", tableNumber, e.getMessage());
                tableEmitters.remove(emitter);
            }
        }
    }

    /**
     * Retorna cuántas mesas tienen listeners activos (útil para diagnóstico).
     */
    public int getActiveConnectionCount() {
        return emitters.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Mensaje amigable para el cliente según el estado.
     */
    private String buildStatusMessage(OrderTableState status) {
        return switch (status) {
            case IN_PREPARATION -> "Tu pedido está siendo preparado en cocina 👨‍🍳";
            case SERVED         -> "¡Tu pedido está listo! El mesero lo llevará a tu mesa 🍽️";
            case PAID           -> "Pago confirmado. ¡Gracias por tu visita! 🎉";
            case CANCELLED      -> "Tu pedido fue cancelado. Consulta con el personal.";
            default             -> "El estado de tu pedido fue actualizado.";
        };
    }

    /**
     * Payload del evento SSE enviado al cliente.
     */
    public record OrderStatusEvent(
            Long orderId,
            OrderTableState status,
            Integer tableNumber,
            String message
    ) {}

    /**
     * Payload del evento SSE para alertas de ayuda.
     */
    public record HelpAlertEvent(
            Long alertId,
            Long orderId,
            Integer tableNumber,
            String status,
            String message
    ) {}

    /**
     * Emite una notificación de nueva alerta de ayuda a todos los listeners de la mesa.
     * Se llama desde HelpAlertService cuando el cliente solicita ayuda.
     */
    public void notifyHelpAlert(Integer tableNumber, Long orderId, Long alertId) {
        List<SseEmitter> tableEmitters = emitters.get(tableNumber);
        if (tableEmitters == null || tableEmitters.isEmpty()) {
            log.debug("Sin suscriptores activos para mesa {} al crear alerta de ayuda {}", tableNumber, alertId);
            return;
        }

        HelpAlertEvent event = new HelpAlertEvent(
                alertId, orderId, tableNumber, "PENDING",
                "Se ha solicitado ayuda del mesero. El personal se acercará pronto."
        );

        // Iterar en copia para evitar ConcurrentModificationException
        List<SseEmitter> snapshot = List.copyOf(tableEmitters);
        for (SseEmitter emitter : snapshot) {
            try {
                emitter.send(SseEmitter.event()
                        .name("help-alert")
                        .data(event));
                log.debug("Notificación de ayuda enviada a mesa {} → alerta {}", tableNumber, alertId);
            } catch (IOException e) {
                log.warn("Error enviando notificación de ayuda a mesa {}: {}. Eliminando emitter.", tableNumber, e.getMessage());
                tableEmitters.remove(emitter);
            }
        }
    }

    /**
     * Emite una notificación de cambio de estado de alerta de ayuda.
     * Se llama desde HelpAlertService cuando se actualiza el estado de la alerta.
     */
    public void notifyHelpAlertStatusChange(Integer tableNumber, Long orderId, Long alertId, HelpAlert.AlertStatus newStatus) {
        List<SseEmitter> tableEmitters = emitters.get(tableNumber);
        if (tableEmitters == null || tableEmitters.isEmpty()) {
            log.debug("Sin suscriptores activos para mesa {} al cambiar alerta {} a {}", tableNumber, alertId, newStatus);
            return;
        }

        String message = switch (newStatus) {
            case ATTENDED -> "El mesero está llegando a su mesa. ¡Mantente atento!";
            case RESOLVED -> "Tu solicitud de ayuda ha sido resuelta. ¿Necesitas algo más?";
            default -> "El estado de tu solicitud de ayuda ha sido actualizado.";
        };

        HelpAlertEvent event = new HelpAlertEvent(
                alertId, orderId, tableNumber, newStatus.toString(), message
        );

        // Iterar en copia para evitar ConcurrentModificationException
        List<SseEmitter> snapshot = List.copyOf(tableEmitters);
        for (SseEmitter emitter : snapshot) {
            try {
                emitter.send(SseEmitter.event()
                        .name("help-alert-status")
                        .data(event));
                log.debug("Notificación de cambio de estado de ayuda enviada a mesa {} → alerta {} estado {}", 
                        tableNumber, alertId, newStatus);
            } catch (IOException e) {
                log.warn("Error enviando notificación de cambio de estado a mesa {}: {}. Eliminando emitter.", tableNumber, e.getMessage());
                tableEmitters.remove(emitter);
            }
        }
    }
}
