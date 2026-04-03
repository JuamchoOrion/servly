package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.ItemStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemStockRepository extends JpaRepository<ItemStock, Long> {
    List<ItemStock> findByInventoryId(Long inventoryId);
    Page<ItemStock> findByInventoryId(Long inventoryId, Pageable pageable);

    /**
     * Obtiene todos los stocks de un item específico
     */
    List<ItemStock> findByItem(co.edu.uniquindio.servly.model.entity.Item item);

    /**
     * Obtiene todos los stocks de un item por su ID
     */
    List<ItemStock> findByItem_Id(Long itemId);
}
