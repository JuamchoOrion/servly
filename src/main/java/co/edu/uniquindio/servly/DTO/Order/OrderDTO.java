package co.edu.uniquindio.servly.DTO.Order;

import co.edu.uniquindio.servly.model.enums.OrderTableState;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private Long id;

    @JsonProperty("table_number")
    private Integer tableNumber;

    private List<OrderDetailDTO> items;

    private OrderTableState status;

    private BigDecimal total;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("order_type")
    private String orderType;
}

