package co.edu.uniquindio.servly.DTO.Inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * DTO para representar un lote de stock.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBatchDTO {
    private Long id;
    private String batchNumber;
    private Integer quantity;
    private String supplierName;
    private LocalDate createdDate;
    private LocalDate expiryDate;
    private String status;
    private Integer daysUntilExpiry;
}

