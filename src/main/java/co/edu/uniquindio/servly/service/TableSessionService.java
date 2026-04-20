package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.DTO.TableSessionResponse;
import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.metrics.TableMetricsService;
import co.edu.uniquindio.servly.model.entity.RestaurantTable;
import co.edu.uniquindio.servly.model.entity.TableSession;
import co.edu.uniquindio.servly.repository.RestaurantTableRepository;
import co.edu.uniquindio.servly.repository.TableSessionRepository;
import co.edu.uniquindio.servly.security.TableJwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Gestiona el ciclo de vida de las sesiones de mesa.
 *
 * APERTURA: cliente escanea QR → GET /api/client/session?table=5
 *   - Si la mesa ya tiene sesión activa → retorna esa sesión (cliente reconectado)
 *   - Si no → crea nueva sesión + genera sessionToken JWT
 *
 * CIERRE: cajero/mesero cierra al facturar → DELETE /api/staff/tables/{n}/session
 *   - Marca la sesión como active=false
 *   - El sessionToken queda inválido de inmediato
 *
 * EXPIRACIÓN: el scheduler cierra sesiones cuyo expiresAt ya pasó
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TableSessionService {

    private final TableSessionRepository sessionRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final TableJwtProvider tableJwtProvider;
    private final TableMetricsService tableMetricsService;

    public TableSessionResponse openSession(Integer tableNumber) {
        long startTime = System.currentTimeMillis();
        boolean success = false;

        try {
            log.info("════════════════════════════════════════════════════════════════");
            log.info("🔓 INICIANDO APERTURA DE SESIÓN DE MESA");
            log.info("════════════════════════════════════════════════════════════════");
            log.info("Número de mesa solicitada: {}", tableNumber);

            if (tableNumber == null || tableNumber < 1) {
                log.error("❌ Número de mesa inválido: {}", tableNumber);
                throw new AuthException("Número de mesa inválido");
            }

            log.debug("Buscando mesa {} en la base de datos...", tableNumber);
            RestaurantTable table = restaurantTableRepository.findByTableNumber(tableNumber)
                    .orElseThrow(() -> {
                        log.error("❌ Mesa número {} no existe en la base de datos", tableNumber);
                        return new AuthException("Mesa número " + tableNumber + " no existe");
                    });

            log.info("✓ Mesa encontrada: ID={}, Número={}, Estado actual={}",
                    table.getId(), table.getTableNumber(), table.getStatus());

            // VALIDACIÓN: Verificar que la mesa no esté ocupada
            if (RestaurantTable.TableStatus.OCCUPIED.equals(table.getStatus())) {
                log.warn("⚠️  INTENTO DE APERTURA EN MESA OCUPADA");
                log.warn("Mesa número {} tiene estado: {}", tableNumber, table.getStatus());
                throw new AuthException("La mesa número " + tableNumber + " ya está ocupada. No se puede crear una nueva sesión.");
            }

            Optional<TableSession> existing = sessionRepository.findByRestaurantTableAndActiveTrue(table);

            if (existing.isPresent()) {
                log.info("⏮️  SESIÓN ACTIVA EXISTENTE ENCONTRADA");
                log.info("Reutilizando sesión existente para mesa {}", tableNumber);
                success = true;
                return toResponse(existing.get());
            }

            long expirationMs = tableJwtProvider.getSessionExpirationMs();
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expirationMs / 1000);

            log.debug("Configuración de sesión:");
            log.debug("  - Duración (ms): {}", expirationMs);
            log.debug("  - Expira en: {}", expiresAt);

            // Guardar con token vacío para obtener el ID generado
            TableSession session = TableSession.builder()
                    .restaurantTable(table)
                    .tableNumber(tableNumber)
                    .sessionToken("")
                    .active(true)
                    .expiresAt(expiresAt)
                    .build();

            TableSession saved = sessionRepository.save(session);
            log.debug("Sesión guardada con ID: {}", saved.getId());

            // Ahora generar el token con el ID real
            String token = tableJwtProvider.generateTableToken(tableNumber, saved.getId());
            saved.setSessionToken(token);
            sessionRepository.save(saved);
            log.debug("Token JWT generado para sesión");

            // CAMBIAR ESTADO DE LA MESA A OCCUPIED
            table.setStatus(RestaurantTable.TableStatus.OCCUPIED);
            restaurantTableRepository.save(table);
            log.info("✓ Estado de mesa cambiado a OCCUPIED");

            log.info("════════════════════════════════════════════════════════════════");
            log.info("✅ SESIÓN ABIERTA EXITOSAMENTE");
            log.info("  - Mesa: {}", tableNumber);
            log.info("  - Sesión ID: {}", saved.getId());
            log.info("  - Expira: {}", expiresAt);
            log.info("  - Estado mesa: OCCUPIED");
            log.info("════════════════════════════════════════════════════════════════");

            success = true;
            return toResponse(saved);
        } catch (AuthException e) {
            log.error("❌ ERROR EN APERTURA DE SESIÓN: {}", e.getMessage());
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Tiempo total de operación: {}ms", duration);
            tableMetricsService.recordSessionOpen(success, duration);
        }
    }

    public MessageResponse closeSession(Integer tableNumber) {
        log.info("════════════════════════════════════════════════════════════════");
        log.info("🔒 INICIANDO CIERRE DE SESIÓN DE MESA");
        log.info("════════════════════════════════════════════════════════════════");
        log.info("Número de mesa: {}", tableNumber);

        RestaurantTable table = restaurantTableRepository.findByTableNumber(tableNumber)
                .orElseThrow(() -> {
                    log.error("❌ Mesa número {} no existe", tableNumber);
                    return new AuthException("Mesa número " + tableNumber + " no existe");
                });

        log.info("✓ Mesa encontrada: ID={}, Estado actual={}", table.getId(), table.getStatus());

        TableSession session = sessionRepository.findByRestaurantTableAndActiveTrue(table)
                .orElseThrow(() -> {
                    log.error("❌ Mesa {} no tiene sesión activa", tableNumber);
                    return new AuthException("La mesa " + tableNumber + " no tiene una sesión activa");
                });

        log.info("✓ Sesión activa encontrada: ID={}", session.getId());

        session.setActive(false);
        session.setClosedAt(LocalDateTime.now());
        sessionRepository.save(session);
        log.debug("Sesión marcada como inactiva, closedAt={}", session.getClosedAt());

        // Cambiar estado de la mesa a AVAILABLE
        table.setStatus(RestaurantTable.TableStatus.AVAILABLE);
        restaurantTableRepository.save(table);
        log.info("✓ Estado de mesa cambiado a AVAILABLE");

        log.info("════════════════════════════════════════════════════════════════");
        log.info("✅ SESIÓN CERRADA EXITOSAMENTE");
        log.info("  - Mesa: {}", tableNumber);
        log.info("  - Sesión ID: {}", session.getId());
        log.info("  - Cerrada en: {}", session.getClosedAt());
        log.info("  - Estado mesa: AVAILABLE");
        log.info("════════════════════════════════════════════════════════════════");

        return new MessageResponse("Sesión de la mesa " + tableNumber + " cerrada correctamente");
    }

    @Transactional(readOnly = true)
    public boolean isTableActive(Integer tableNumber) {
        RestaurantTable table = restaurantTableRepository.findByTableNumber(tableNumber)
                .orElse(null);
        if (table == null) return false;
        return sessionRepository.existsByRestaurantTableAndActiveTrue(table);
    }

    private TableSessionResponse toResponse(TableSession s) {
        return TableSessionResponse.builder()
                .sessionToken(s.getSessionToken())
                .tableNumber(s.getRestaurantTable().getTableNumber())
                .sessionId(s.getId())
                .expiresAt(s.getExpiresAt())
                .tokenType("Bearer")
                .build();
    }
}