package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.RestaurantTableDTO;
import co.edu.uniquindio.servly.model.entity.RestaurantTable;
import co.edu.uniquindio.servly.repository.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para operaciones de staff sobre mesas
 * Centraliza la lógica de obtención y transformación de mesas
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantTableStaffService {

    private final RestaurantTableRepository restaurantTableRepository;

    /**
     * Obtener todas las mesas del restaurante
     */
    @Transactional(readOnly = true)
    public List<RestaurantTableDTO> getAllTablesForStaff() {
        log.info("Staff obteniendo todas las mesas");
        return restaurantTableRepository.findAll().stream()
                .map(RestaurantTableDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Obtener una mesa específica por número
     */
    @Transactional(readOnly = true)
    public RestaurantTableDTO getTableByNumberForStaff(Integer tableNumber) {
        log.info("Staff obteniendo mesa: {}", tableNumber);
        RestaurantTable table = restaurantTableRepository.findByTableNumber(tableNumber)
                .orElseThrow(() -> new co.edu.uniquindio.servly.exception.NotFoundException(
                        "Mesa número " + tableNumber + " no encontrada"));
        return RestaurantTableDTO.fromEntity(table);
    }
}

