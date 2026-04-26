package co.edu.uniquindio.servly.DTO.Order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailDTO {

    private Long id;

    @JsonProperty("item_id")
    private Long itemId;

    @JsonProperty("item_name")
    private String itemName;

    private Integer quantity;

    @JsonProperty("unit_price")
    private BigDecimal unitPrice;

    @JsonProperty("tax_percent")
    private BigDecimal taxPercent;

    private BigDecimal subtotal;

    private String annotations;

    /**
     * Items opcionales elegidos por el cliente
     * Formato: [{"itemId": 5, "itemName": "Queso extra", "quantity": 2}, ...]
     */
    @JsonProperty("optional_items")
    private String optionalItems;
}

