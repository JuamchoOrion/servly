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
        return tableRepository.findByTableNumber(tableNumber)
                .orElseThrow(() -> new AuthException("Mesa número " + tableNumber + " no existe"));
    }

    public RestaurantTable updateTableStatus(Integer tableNumber, RestaurantTable.TableStatus newStatus) {
        RestaurantTable table = getTableByNumber(tableNumber);
        table.setStatus(newStatus);
        tableRepository.save(table);
        log.info("Estado de mesa {} actualizado a {}", tableNumber, newStatus);
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

