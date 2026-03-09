package co.edu.uniquindio.servly.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "stock_batch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con ItemStock (uno a muchos)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_stock_id", nullable = false)
    private ItemStock itemStock;

    @Column(name = "batch_number", nullable = false)
    private String batchNumber;

    @Column(nullable = false)
    private Integer quantity;

    // Proveedor específico del lote
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "VIGENTE"; // VIGENTE, PROXIMO_A_EXPIRAR, EXPIRADO

    // 🆕 SOFT DELETE - Campo para marcar como eliminado
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Métodos auxiliares para manejo de vencimiento
    @Transient
    public boolean isExpired() {
        if (expiryDate == null) return false;
        return expiryDate.isBefore(LocalDate.now());
    }

    @Transient
    public int getDaysUntilExpiry() {
        if (expiryDate == null) return Integer.MAX_VALUE;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
        return (int) days;
    }

    @Transient
    public boolean isCloseTExpiry(int daysThreshold) {
        if (expiryDate == null) return false;
        return getDaysUntilExpiry() <= daysThreshold && getDaysUntilExpiry() > 0;
    }

    @Transient
    public String getExpiryStatus() {
        if (isExpired()) return "EXPIRADO";
        if (isCloseTExpiry(7)) return "PROXIMO_A_EXPIRAR";
        return "VIGENTE";
    }

    // Actualizar estado basado en expiración
    public void updateStatus() {
        this.status = getExpiryStatus();
    }

    // 🆕 Método para soft delete
    @Transient
    public boolean isDeleted() {
        return deletedAt != null;
    }
}



