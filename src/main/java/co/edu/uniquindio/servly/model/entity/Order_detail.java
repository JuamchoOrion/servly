package co.edu.uniquindio.servly.model.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "order_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order_detail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal taxPercent;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column (precision = 10, scale = 2)
    private String annotations;

    /**
     * Almacena items opcionales elegidos por el cliente
     * Formato JSON: {"itemId": 1, "itemName": "Extra queso", "quantity": 2}, ...
     * Ejemplo: [{"itemId": 5, "itemName": "Queso extra", "quantity": 2}, {"itemId": 6, "itemName": "Salsa especial", "quantity": 1}]
     */
    @Column(columnDefinition = "TEXT")
    private String optionalItems;
}
