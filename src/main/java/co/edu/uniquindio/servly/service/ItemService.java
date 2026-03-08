package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Inventory.ItemCreateRequest;
import co.edu.uniquindio.servly.DTO.Inventory.ItemDTO;
import co.edu.uniquindio.servly.DTO.Inventory.ItemUpdateRequest;
import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.model.entity.Item;
import co.edu.uniquindio.servly.model.entity.ItemCategory;
import co.edu.uniquindio.servly.repository.ItemCategoryRepository;
import co.edu.uniquindio.servly.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemCategoryRepository itemCategoryRepository;

    /**
     * Obtiene todos los items activos
     */
    @Transactional(readOnly = true)
    public List<ItemDTO> getAllItems() {
        log.info("Obteniendo todos los items activos");
        return itemRepository.findAllActive().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un item por ID
     */
    @Transactional(readOnly = true)
    public ItemDTO getItemById(Long id) {
        log.info("Obteniendo item con ID: {}", id);
        Item item = itemRepository.findByIdActive(id)
                .orElseThrow(() -> new AuthException("Item no encontrado"));
        return convertToDTO(item);
    }

    /**
     * Obtiene items por categoría
     */
    @Transactional(readOnly = true)
    public List<ItemDTO> getItemsByCategory(Long categoryId) {
        log.info("Obteniendo items de la categoría: {}", categoryId);
        return itemRepository.findByCategoryId(categoryId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca items por nombre
     */
    @Transactional(readOnly = true)
    public List<ItemDTO> searchItems(String name) {
        log.info("Buscando items con nombre: {}", name);
        return itemRepository.findByNameContaining(name).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Crea un nuevo item
     */
    public ItemDTO createItem(ItemCreateRequest request) {
        log.info("Creando nuevo item: {}", request.getName());

        // Validar que el nombre no esté vacío
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new AuthException("El nombre del item no puede estar vacío");
        }

        // Obtener la categoría
        ItemCategory category = itemCategoryRepository.findById(Long.parseLong(request.getCategory()))
                .orElseThrow(() -> new AuthException("Categoría no encontrada"));

        // Crear el item
        Item item = Item.builder()
                .name(request.getName())
                .description(request.getDescription())
                .unitOfMeasurement(request.getUnitOfMeasurement())
                .expirationDays(request.getExpirationDays())
                .itemCategory(category)
                .active(true)
                .build();

        Item savedItem = itemRepository.save(item);
        log.info("Item creado exitosamente con ID: {}", savedItem.getId());
        return convertToDTO(savedItem);
    }

    /**
     * Actualiza un item existente
     */
    public ItemDTO updateItem(Long id, ItemUpdateRequest request) {
        log.info("Actualizando item con ID: {}", id);

        Item item = itemRepository.findByIdActive(id)
                .orElseThrow(() -> new AuthException("Item no encontrado"));

        // Actualizar campos
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            item.setName(request.getName());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getUnitOfMeasurement() != null && !request.getUnitOfMeasurement().trim().isEmpty()) {
            item.setUnitOfMeasurement(request.getUnitOfMeasurement());
        }
        if (request.getExpirationDays() != null) {
            item.setExpirationDays(request.getExpirationDays());
        }

        // Actualizar categoría si se proporciona
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            ItemCategory category = itemCategoryRepository.findById(Long.parseLong(request.getCategory()))
                    .orElseThrow(() -> new AuthException("Categoría no encontrada"));
            item.setItemCategory(category);
        }

        Item updatedItem = itemRepository.save(item);
        log.info("Item actualizado exitosamente");
        return convertToDTO(updatedItem);
    }

    /**
     * Desactiva (soft delete) un item
     */
    public void deleteItem(Long id) {
        log.info("Desactivando item con ID: {}", id);

        Item item = itemRepository.findByIdActive(id)
                .orElseThrow(() -> new AuthException("Item no encontrado"));

        item.setActive(false);
        itemRepository.save(item);
        log.info("Item desactivado exitosamente");
    }

    /**
     * Convierte una entidad Item a ItemDTO
     */
    private ItemDTO convertToDTO(Item item) {
        return ItemDTO.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .unitOfMeasurement(item.getUnitOfMeasurement())
                .expirationDays(item.getExpirationDays())
                .category(item.getItemCategory().getName())
                .build();
    }
}

