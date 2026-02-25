package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.DTO.TableSessionResponse;
import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.model.entity.TableSession;
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
    private final TableJwtProvider       tableJwtProvider;

    public TableSessionResponse openSession(Integer tableNumber) {
        if (tableNumber == null || tableNumber < 1) {
            throw new AuthException("Número de mesa inválido");
        }

        Optional<TableSession> existing =
                sessionRepository.findByTableNumberAndActiveTrue(tableNumber);

        if (existing.isPresent()) {
            log.debug("Mesa {} ya tiene sesión activa, reutilizando", tableNumber);
            return toResponse(existing.get());
        }

        long expirationMs = tableJwtProvider.getSessionExpirationMs();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expirationMs / 1000);

        // Guardar con token vacío para obtener el ID generado
        TableSession session = TableSession.builder()
                .tableNumber(tableNumber)
                .sessionToken("")
                .active(true)
                .expiresAt(expiresAt)
                .build();

        TableSession saved = sessionRepository.save(session);

        // Ahora generar el token con el ID real
        String token = tableJwtProvider.generateTableToken(tableNumber, saved.getId());
        saved.setSessionToken(token);
        sessionRepository.save(saved);

        log.info("Sesión abierta para mesa {}, expira: {}", tableNumber, expiresAt);
        return toResponse(saved);
    }

    public MessageResponse closeSession(Integer tableNumber) {
        TableSession session = sessionRepository
                .findByTableNumberAndActiveTrue(tableNumber)
                .orElseThrow(() -> new AuthException(
                        "La mesa " + tableNumber + " no tiene una sesión activa"));

        session.setActive(false);
        session.setClosedAt(LocalDateTime.now());
        sessionRepository.save(session);

        log.info("Sesión cerrada para mesa {}", tableNumber);
        return new MessageResponse("Sesión de la mesa " + tableNumber + " cerrada correctamente");
    }

    @Transactional(readOnly = true)
    public boolean isTableActive(Integer tableNumber) {
        return sessionRepository.existsByTableNumberAndActiveTrue(tableNumber);
    }

    private TableSessionResponse toResponse(TableSession s) {
        return TableSessionResponse.builder()
                .sessionToken(s.getSessionToken())
                .tableNumber(s.getTableNumber())
                .sessionId(s.getId())
                .expiresAt(s.getExpiresAt())
                .tokenType("Bearer")
                .build();
    }
}