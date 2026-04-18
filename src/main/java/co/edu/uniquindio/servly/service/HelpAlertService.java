package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.HelpAlert.HelpAlertResponseDTO;
import co.edu.uniquindio.servly.exception.NotFoundException;
import co.edu.uniquindio.servly.model.entity.HelpAlert;
import co.edu.uniquindio.servly.model.entity.Order;
import co.edu.uniquindio.servly.model.entity.TableSource;
import co.edu.uniquindio.servly.repository.HelpAlertRepository;
import co.edu.uniquindio.servly.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar alertas de ayuda de los clientes.
 *
 * Cuando un cliente presiona el botón de ayuda en una orden:
 * 1. Se crea una HelpAlert en estado PENDING
 * 2. Se notifica al mesero en tiempo real (SSE)
 * 3. El mesero puede marcar la alerta como ATTENDED o RESOLVED
 *
 * Características:
 * - Lightweight: no cambia el estado de la orden
 * - No afecta inventario
 * - Notificación en tiempo real a meseros
 * - Histórico de alertas por mesa
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class HelpAlertService {

    private final HelpAlertRepository helpAlertRepository;
    private final OrderRepository orderRepository;
    private final OrderNotificationService notificationService;

    /**
     * Crea una alerta de ayuda para una orden específica.
     * 
     * @param orderId ID de la orden
     * @param tableNumber Número de mesa
     * @return HelpAlertResponseDTO con los detalles de la alerta creada
     */
    @Transactional
    public HelpAlertResponseDTO createHelpAlert(Long orderId, Integer tableNumber) {
        log.info("Creando alerta de ayuda para orden: {} de mesa: {}", orderId, tableNumber);

        // Validar que la orden existe
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada: " + orderId));

        // Validar que la orden pertenece a la mesa
        if (order.getOrderType().equals(co.edu.uniquindio.servly.model.enums.OrderType.TABLE)) {
            if (order.getSource() instanceof TableSource) {
                TableSource tableSource = (TableSource) order.getSource();
                if (!tableSource.getTableNumber().equals(tableNumber)) {
                    throw new IllegalArgumentException("La orden no pertenece a esta mesa");
                }
            }
        }

        // Crear alerta de ayuda
        HelpAlert alert = HelpAlert.builder()
                .order(order)
                .tableNumber(tableNumber)
                .status(HelpAlert.AlertStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        HelpAlert savedAlert = helpAlertRepository.save(alert);
        log.info("Alerta de ayuda creada con ID: {}", savedAlert.getId());

        // Notificar a meseros que tienen órdenes pendientes en esta mesa
        notifyWaitersOfHelpAlert(tableNumber, orderId, savedAlert.getId());

        return toDTO(savedAlert);
    }

    /**
     * Notifica a los meseros que tienen una alerta de ayuda pendiente.
     * Usa SSE para notificación en tiempo real.
     * 
     * @param tableNumber Número de mesa
     * @param orderId ID de la orden
     * @param alertId ID de la alerta
     */
    private void notifyWaitersOfHelpAlert(Integer tableNumber, Long orderId, Long alertId) {
        // Notificar a todos los clientes de la mesa (para que sepan que se envió la ayuda)
        notificationService.notifyHelpAlert(tableNumber, orderId, alertId);
        log.info("Notificación de ayuda enviada a mesa: {}", tableNumber);
    }

    /**
     * Obtiene todas las alertas pendientes de una mesa.
     * 
     * @param tableNumber Número de mesa
     * @return Lista de HelpAlertResponseDTO
     */
    public List<HelpAlertResponseDTO> getPendingAlertsByTable(Integer tableNumber) {
        log.info("Obteniendo alertas pendientes de mesa: {}", tableNumber);
        
        List<HelpAlert> alerts = helpAlertRepository.findByTableNumberAndStatusOrderByCreatedAtDesc(
                tableNumber, HelpAlert.AlertStatus.PENDING);
        
        return alerts.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene todas las alertas pendientes de todas las mesas.
     * Útil para que los meseros vean todas las solicitudes de ayuda.
     * 
     * @return Lista de HelpAlertResponseDTO
     */
    public List<HelpAlertResponseDTO> getAllPendingAlerts() {
        log.info("Obteniendo todas las alertas pendientes");
        
        List<HelpAlert> alerts = helpAlertRepository.findByStatusOrderByCreatedAtDesc(HelpAlert.AlertStatus.PENDING);
        
        return alerts.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Marca una alerta como atendida.
     * 
     * @param alertId ID de la alerta
     * @return HelpAlertResponseDTO con el estado actualizado
     */
    @Transactional
    public HelpAlertResponseDTO markAsAttended(Long alertId) {
        log.info("Marcando alerta como atendida: {}", alertId);
        
        HelpAlert alert = helpAlertRepository.findById(alertId)
                .orElseThrow(() -> new NotFoundException("Alerta no encontrada: " + alertId));
        
        alert.setStatus(HelpAlert.AlertStatus.ATTENDED);
        alert.setResolvedAt(LocalDateTime.now());
        
        HelpAlert updated = helpAlertRepository.save(alert);
        
        // Notificar al cliente que su ayuda está siendo atendida
        notificationService.notifyHelpAlertStatusChange(
                alert.getTableNumber(), 
                alert.getOrder().getId(), 
                alert.getId(), 
                HelpAlert.AlertStatus.ATTENDED
        );
        
        return toDTO(updated);
    }

    /**
     * Marca una alerta como resuelta.
     * 
     * @param alertId ID de la alerta
     * @return HelpAlertResponseDTO con el estado actualizado
     */
    @Transactional
    public HelpAlertResponseDTO markAsResolved(Long alertId) {
        log.info("Marcando alerta como resuelta: {}", alertId);
        
        HelpAlert alert = helpAlertRepository.findById(alertId)
                .orElseThrow(() -> new NotFoundException("Alerta no encontrada: " + alertId));
        
        alert.setStatus(HelpAlert.AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        
        HelpAlert updated = helpAlertRepository.save(alert);
        
        // Notificar al cliente que su ayuda ha sido resuelta
        notificationService.notifyHelpAlertStatusChange(
                alert.getTableNumber(), 
                alert.getOrder().getId(), 
                alert.getId(), 
                HelpAlert.AlertStatus.RESOLVED
        );
        
        return toDTO(updated);
    }

    /**
     * Convierte HelpAlert a HelpAlertResponseDTO.
     * 
     * @param alert HelpAlert entity
     * @return HelpAlertResponseDTO
     */
    private HelpAlertResponseDTO toDTO(HelpAlert alert) {
        return HelpAlertResponseDTO.builder()
                .alertId(alert.getId())
                .orderId(alert.getOrder().getId())
                .tableNumber(alert.getTableNumber())
                .status(alert.getStatus().toString())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
