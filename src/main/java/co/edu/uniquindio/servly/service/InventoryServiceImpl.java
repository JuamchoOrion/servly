package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Inventory.ItemStockDTO;
import co.edu.uniquindio.servly.DTO.Inventory.PaginatedInventoryResponse;
import co.edu.uniquindio.servly.exception.NotFoundException;
import co.edu.uniquindio.servly.model.entity.Inventory;
import co.edu.uniquindio.servly.model.entity.ItemStock;
import co.edu.uniquindio.servly.repository.InventoryRepository;
import co.edu.uniquindio.servly.repository.ItemStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ItemStockRepository itemStockRepository;

    @Transactional(readOnly = true)
    @Override
    public List<ItemStockDTO> getInventory() {
        Inventory inventory = inventoryRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No inventory found"));

        List<ItemStock> stocks = itemStockRepository.findByInventoryId(inventory.getId());

        return stocks.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public PaginatedInventoryResponse getInventoryPaginated(Pageable pageable) {
        Inventory inventory = inventoryRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No inventory found"));

        Page<ItemStock> page = itemStockRepository.findByInventoryId(inventory.getId(), pageable);

        List<ItemStockDTO> content = page.getContent().stream().map(this::toDTO).collect(Collectors.toList());

        return PaginatedInventoryResponse.builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }

    @Transactional
    @Override
    public void increaseStock(Long itemStockId, Integer quantity) {
        if (quantity == null || quantity <= 0) return;
        ItemStock stock = itemStockRepository.findById(itemStockId)
                .orElseThrow(() -> new NotFoundException("ItemStock not found with id: " + itemStockId));
        stock.setQuantity(stock.getQuantity() + quantity);
        itemStockRepository.save(stock);
    }

    @Transactional
    @Override
    public void decreaseStock(Long itemStockId, Integer quantity) {
        if (quantity == null || quantity <= 0) return;
        ItemStock stock = itemStockRepository.findById(itemStockId)
                .orElseThrow(() -> new NotFoundException("ItemStock not found with id: " + itemStockId));

        int newQty = stock.getQuantity() - quantity;
        if (newQty < 0) {
            stock.setQuantity(0);
        } else {
            stock.setQuantity(newQty);
        }
        itemStockRepository.save(stock);
    }

    private ItemStockDTO toDTO(ItemStock s) {
        return ItemStockDTO.builder()
                .itemStockId(s.getId())
                .name(s.getItem() != null ? s.getItem().getName() : null)
                .description(s.getItem() != null ? s.getItem().getDescription() : null)
                .category(s.getItem() != null && s.getItem().getItemCategory() != null ? s.getItem().getItemCategory().getName() : null)
                .quantity(s.getQuantity())
                .unitOfMeasurement(s.getItem() != null ? s.getItem().getUnitOfMeasurement() : null)
                .supplierName(s.getSupplier() != null ? s.getSupplier().getName() : null)
                .expirationDays(s.getItem() != null ? s.getItem().getExpirationDays() : null)
                .stockPercent(null)
                .build();
    }
}
