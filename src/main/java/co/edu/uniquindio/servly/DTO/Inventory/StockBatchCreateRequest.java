package co.edu.uniquindio.servly.DTO.Inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * Request DTO para crear un lote de stock.
 * Un lote es una entrada específica de un item con cantidad, proveedor y fecha de vencimiento.
 *
 * Si expiryDate no se proporciona, se calcula automáticamente como:
 * expiryDate = hoy + Item.expirationDays
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBatchCreateRequest {
    private Long itemStockId;
    private String batchNumber;
    private Integer quantity;
    private Long supplierId;
    private LocalDate expiryDate; // Opcional - se calcula automáticamente si no se proporciona
}

