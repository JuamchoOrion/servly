package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.model.entity.RestaurantTable;
import co.edu.uniquindio.servly.repository.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio para gestionar las mesas del restaurante.
 *
 * Responsabilidades:
 *  - CRUD de mesas (crear, actualizar, listar)
 *  - Cambiar estado de mesas (AVAILABLE, OCCUPIED, MAINTENANCE, etc.)
 *  - Validar que el número de mesa sea único
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantTableService {

    private final RestaurantTableRepository tableRepository;

    public RestaurantTable createTable(Integer tableNumber, Integer capacity, String location) {
        if (tableNumber == null || tableNumber < 1) {
            throw new AuthException("Número de mesa inválido");
        }
        if (capacity == null || capacity < 1) {
            throw new AuthException("Capacidad inválida");
        }

        if (tableRepository.existsByTableNumber(tableNumber)) {
            throw new AuthException("La mesa número " + tableNumber + " ya existe");
        }

        RestaurantTable table = RestaurantTable.builder()
                .tableNumber(tableNumber)
                .capacity(capacity)
                .location(location)
                .status(RestaurantTable.TableStatus.AVAILABLE)
                .build();

        RestaurantTable saved = tableRepository.save(table);
        log.info("Mesa creada: número={}, capacidad={}, ubicación={}", tableNumber, capacity, location);
        return saved;
    }

    public RestaurantTable getTableByNumber(Integer tableNumber) {
        log.debug("Buscando mesa número: {}", tableNumber);
        RestaurantTable table = tableRepository.findByTableNumber(tableNumber)
                .orElseThrow(() -> {
                    log.error("❌ Mesa número {} no encontrada", tableNumber);
                    return new AuthException("Mesa número " + tableNumber + " no existe");
                });
        log.debug("✓ Mesa encontrada: ID={}, Status={}", table.getId(), table.getStatus());
        return table;
    }

    public RestaurantTable updateTableStatus(Integer tableNumber, RestaurantTable.TableStatus newStatus) {
        log.info("════════════════════════════════════════════════════════════════");
        log.info("📍 ACTUALIZANDO ESTADO DE MESA");
        log.info("════════════════════════════════════════════════════════════════");
        log.info("Mesa: {}", tableNumber);

        RestaurantTable table = getTableByNumber(tableNumber);

        log.info("Estado anterior: {}", table.getStatus());
        log.info("Nuevo estado: {}", newStatus);

        table.setStatus(newStatus);
        tableRepository.save(table);

        log.info("════════════════════════════════════════════════════════════════");
        log.info("✅ ESTADO ACTUALIZADO EXITOSAMENTE");
        log.info("  - Mesa: {}", tableNumber);
        log.info("  - Nuevo estado: {}", newStatus);
        log.info("════════════════════════════════════════════════════════════════");

        return table;
    }

    @Transactional(readOnly = true)
    public List<RestaurantTable> getAllTables() {
        return tableRepository.findAllOrderedByTableNumber();
    }

    @Transactional(readOnly = true)
    public List<RestaurantTable> getTablesByStatus(RestaurantTable.TableStatus status) {
        return tableRepository.findByStatus(status);
    }

    public void deleteTable(Integer tableNumber) {
        RestaurantTable table = getTableByNumber(tableNumber);
        tableRepository.delete(table);
        log.info("Mesa {} eliminada", tableNumber);
    }
}

