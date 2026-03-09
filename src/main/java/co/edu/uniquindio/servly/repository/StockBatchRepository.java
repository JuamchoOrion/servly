package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.StockBatch;
import co.edu.uniquindio.servly.model.entity.ItemStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

    // 🆕 Obtener todos los lotes de un ItemStock (no eliminados), ordenados por fecha de vencimiento (FIFO)
    @Query("SELECT sb FROM StockBatch sb WHERE sb.itemStock = :itemStock AND sb.deletedAt IS NULL ORDER BY sb.expiryDate ASC")
    List<StockBatch> findByItemStockOrderByExpiryDateAsc(ItemStock itemStock);

    // 🆕 Obtener todos los lotes de un ItemStock (no eliminados)
    @Query("SELECT sb FROM StockBatch sb WHERE sb.itemStock = :itemStock AND sb.deletedAt IS NULL")
    List<StockBatch> findByItemStock(ItemStock itemStock);

    // 🆕 Obtener lotes expirados (no eliminados)
    @Query("SELECT sb FROM StockBatch sb WHERE sb.expiryDate < CURRENT_DATE AND sb.deletedAt IS NULL")
    List<StockBatch> findExpiredBatches();

    // 🆕 Obtener lotes próximos a expirar (menos de 7 días, no eliminados)
    @Query(value = "SELECT * FROM stock_batch sb WHERE sb.expiry_date > CURRENT_DATE AND sb.expiry_date <= CURRENT_DATE + INTERVAL '7 days' AND sb.deleted_at IS NULL", nativeQuery = true)
    List<StockBatch> findBatchesCloseTExpiry();

    // 🆕 Obtener lotes vigentes de un ItemStock (no eliminados)
    @Query("SELECT sb FROM StockBatch sb WHERE sb.itemStock = :itemStock AND sb.expiryDate > CURRENT_DATE AND sb.deletedAt IS NULL ORDER BY sb.expiryDate ASC")
    List<StockBatch> findActiveBatchesByItemStock(ItemStock itemStock);

    // 🆕 Contar lotes por ItemStock (no eliminados)
    @Query("SELECT COUNT(sb) FROM StockBatch sb WHERE sb.itemStock = :itemStock AND sb.deletedAt IS NULL")
    long countByItemStock(ItemStock itemStock);

    // 🆕 Obtener lotes eliminados de un ItemStock (para auditoría)
    @Query("SELECT sb FROM StockBatch sb WHERE sb.itemStock = :itemStock AND sb.deletedAt IS NOT NULL ORDER BY sb.deletedAt DESC")
    List<StockBatch> findDeletedBatchesByItemStock(ItemStock itemStock);
}

