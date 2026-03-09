package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.StockBatch;
import co.edu.uniquindio.servly.model.entity.ItemStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

    // Obtener todos los lotes de un ItemStock, ordenados por fecha de vencimiento (FIFO)
    List<StockBatch> findByItemStockOrderByExpiryDateAsc(ItemStock itemStock);

    // Obtener todos los lotes de un ItemStock
    List<StockBatch> findByItemStock(ItemStock itemStock);

    // Obtener lotes expirados
    @Query("SELECT sb FROM StockBatch sb WHERE sb.expiryDate < CURRENT_DATE")
    List<StockBatch> findExpiredBatches();

    // Obtener lotes próximos a expirar (menos de 7 días)
    @Query(value = "SELECT * FROM stock_batch sb WHERE sb.expiry_date > CURRENT_DATE AND sb.expiry_date <= CURRENT_DATE + INTERVAL '7 days'", nativeQuery = true)
    List<StockBatch> findBatchesCloseTExpiry();

    // Obtener lotes vigentes de un ItemStock
    @Query("SELECT sb FROM StockBatch sb WHERE sb.itemStock = :itemStock AND sb.expiryDate > CURRENT_DATE ORDER BY sb.expiryDate ASC")
    List<StockBatch> findActiveBatchesByItemStock(ItemStock itemStock);

    // Contar lotes por ItemStock
    long countByItemStock(ItemStock itemStock);
}

