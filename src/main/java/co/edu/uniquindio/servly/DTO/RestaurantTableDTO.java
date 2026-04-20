package co.edu.uniquindio.servly.DTO;

import co.edu.uniquindio.servly.model.entity.RestaurantTable;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantTableDTO {

    private Integer id;

    @JsonProperty("table_number")
    private Integer tableNumber;

    private Integer capacity;

    private RestaurantTable.TableStatus status;

    private String location;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public static RestaurantTableDTO fromEntity(RestaurantTable table) {
        return RestaurantTableDTO.builder()
                .id(table.getId())
                .tableNumber(table.getTableNumber())
                .capacity(table.getCapacity())
                .status(table.getStatus())
                .location(table.getLocation())
                .createdAt(table.getCreatedAt())
                .updatedAt(table.getUpdatedAt())
                .build();
    }
}

