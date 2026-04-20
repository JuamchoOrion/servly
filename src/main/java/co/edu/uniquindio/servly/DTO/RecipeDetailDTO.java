package co.edu.uniquindio.servly.DTO;

import co.edu.uniquindio.servly.DTO.Inventory.ItemDetailDTO;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeDetailDTO {

    private Long id;
    private String name;
    private Integer quantity;
    private String description;
    private List<ItemDetailDTO> itemDetailList;
}

