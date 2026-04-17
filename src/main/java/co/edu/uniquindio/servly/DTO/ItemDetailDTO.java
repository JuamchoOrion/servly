package co.edu.uniquindio.servly.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDetailDTO {

    private Long id;
    private Integer quantity;
    private String annotation;
    private Boolean isOptional;
    private Integer minQuantity;
    private Integer maxQuantity;

    // Item información completa
    private ItemDTO item;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemDTO {
        private Long id;
        private String name;
    }
}

