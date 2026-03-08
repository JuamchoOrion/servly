package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Inventory.ItemCreateRequest;
import co.edu.uniquindio.servly.DTO.Inventory.ItemDTO;
import co.edu.uniquindio.servly.DTO.Inventory.ItemUpdateRequest;
import co.edu.uniquindio.servly.DTO.Inventory.PaginatedItemResponse;
import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.model.entity.Item;
import co.edu.uniquindio.servly.model.entity.ItemCategory;
import co.edu.uniquindio.servly.model.entity.ItemStock;
import co.edu.uniquindio.servly.model.entity.Inventory;
import co.edu.uniquindio.servly.repository.ItemCategoryRepository;
import co.edu.uniquindio.servly.repository.ItemRepository;
import co.edu.uniquindio.servly.repository.ItemStockRepository;
import co.edu.uniquindio.servly.repository.InventoryRepository;
import co.edu.uniquindio.servly.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final ItemStockRepository itemStockRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryRepository inventoryRepository;


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
     * Obtiene todos los items activos con paginación
     */
    @Transactional(readOnly = true)
    public PaginatedItemResponse getAllItemsPaginated(Pageable pageable) {
        log.info("Obteniendo items paginados - página: {}, tamaño: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Item> page = itemRepository.findAllActivePaginated(pageable);

        return PaginatedItemResponse.builder()
                .content(page.getContent().stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
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
     * Obtiene items por categoría con paginación
     */
    @Transactional(readOnly = true)
    public PaginatedItemResponse getItemsByCategoryPaginated(Long categoryId, Pageable pageable) {
        log.info("Obteniendo items de categoría {} paginados", categoryId);

        Page<Item> page = itemRepository.findByCategoryIdPaginated(categoryId, pageable);

        return PaginatedItemResponse.builder()
                .content(page.getContent().stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
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
     * Busca items por nombre con paginación
     */
    @Transactional(readOnly = true)
    public PaginatedItemResponse searchItemsPaginated(String name, Pageable pageable) {
        log.info("Buscando items con nombre: {} - paginado", name);

        Page<Item> page = itemRepository.findByNameContainingPaginated(name, pageable);

        return PaginatedItemResponse.builder()
                .content(page.getContent().stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }

    /**
     * Crea un nuevo item
     */
    @Transactional
    public ItemDTO createItem(ItemCreateRequest request) {

        ItemCategory category = itemCategoryRepository
                .findById(Long.parseLong(request.getCategory()))
                .orElseThrow(() -> new AuthException("Categoría no encontrada"));

        Item item = Item.builder()
                .name(request.getName())
                .description(request.getDescription())
                .unitOfMeasurement(request.getUnitOfMeasurement())
                .expirationDays(request.getExpirationDays())
                .itemCategory(category)
                .active(true)
                .idealStock(request.getIdealStock() != null ? request.getIdealStock() : 0)
                .build();

        // Guardar y forzar flush para asegurar generación de ID antes de crear ItemStock
        Item savedItem = itemRepository.saveAndFlush(item);

        // Obtener o crear inventario principal
        Inventory inventory = inventoryRepository.findAll()
                .stream()
                .findFirst()
                .orElseGet(() -> inventoryRepository.save(Inventory.builder().build()));

        ItemStock stock = ItemStock.builder()
                .item(savedItem)
                .inventory(inventory)
                .quantity(0)
                .build();

        itemStockRepository.save(stock);

        log.info("Item creado con ID: {} y ItemStock ID: {} (cantidad: {})",
                savedItem.getId(), stock.getId(), stock.getQuantity());

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

        // Actualizar idealStock si se proporciona
        if (request.getIdealStock() != null) {
            item.setIdealStock(request.getIdealStock());
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
                .idealStock(item.getIdealStock())
                .build();
    }
}
