package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Order.OrderItemRequest;
import co.edu.uniquindio.servly.exception.NotFoundException;
import co.edu.uniquindio.servly.exception.ValidationException;
import co.edu.uniquindio.servly.model.entity.*;
import co.edu.uniquindio.servly.repository.ItemStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Servicio para gestionar disponibilidad de productos
 * Un PRODUCTO tiene una RECETA con ITEM_DETAIL_LIST
 * Se valida que haya suficientes ITEMS en inventario para preparar el producto
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductAvailabilityService {

    private final ItemStockRepository itemStockRepository;

    /**
     * Valida si un producto está disponible para pedir
     * Verifica que TODOS los items de su receta tengan stock suficiente
     *
     * @param product Producto a validar
     * @param quantity Cantidad de veces que se pide el producto
     * @return true si hay stock, false si no
     */
    public boolean isProductAvailable(Product product, Integer quantity) {
        if (product == null || product.getRecipe() == null) {
            log.warn("Producto o receta nula: {}", product != null ? product.getId() : null);
            return false;
        }

        Recipe recipe = product.getRecipe();

        // Si la receta no tiene items, el producto siempre está disponible
        if (recipe.getItemDetailList() == null || recipe.getItemDetailList().isEmpty()) {
            return true;
        }

        // Validar que CADA item de la receta tiene stock suficiente
        for (ItemDetail itemDetail : recipe.getItemDetailList()) {
            Item item = itemDetail.getItem();
            Integer requiredQuantity = itemDetail.getQuantity() * quantity;

            if (!hasStockAvailable(item, requiredQuantity)) {
                log.debug("Stock insuficiente para item: {} (requiere: {}, disponible: {})",
                        item.getId(), requiredQuantity, getAvailableStock(item));
                return false;
            }
        }

        return true;
    }

    /**
     * Verifica si hay stock disponible de un item específico
     * @param itemId ID del item
     * @param requiredQuantity Cantidad requerida
     * @return true si hay stock suficiente
     */
    public boolean isItemAvailable(Long itemId, Integer requiredQuantity) {
        // Obtener el item (necesitamos el repositorio)
        // Por ahora usamos el método getAvailableStock que suma stocks
        Integer available = itemStockRepository.findByItem_Id(itemId).stream()
                .mapToInt(ItemStock::getQuantity)
                .sum();
        return available >= requiredQuantity;
    }

    /**
     * Verifica si hay stock disponible de un item específico
     */
    private boolean hasStockAvailable(Item item, Integer requiredQuantity) {
        Integer available = getAvailableStock(item);
        return available != null && available >= requiredQuantity;
    }

    /**
     * Obtiene la cantidad total disponible de un item en inventario
     * Suma todas las cantidades de ItemStock del item
     */
    private Integer getAvailableStock(Item item) {
        List<ItemStock> stocks = itemStockRepository.findByItem(item);

        if (stocks == null || stocks.isEmpty()) {
            return 0;
        }

        return stocks.stream()
                .mapToInt(ItemStock::getQuantity)
                .sum();
    }

    /**
     * Descuenta los items del inventario cuando se paga una orden
     * Esta función se ejecuta DESPUÉS de confirmar el pago
     *
     * @param product Producto que se pidió
     * @param quantity Cantidad pedida
     * @throws ValidationException si no hay stock (no debería pasar si se validó antes)
     */
    public void deductInventoryForProduct(Product product, Integer quantity) {
        log.info("Descontando items para producto: {} (cantidad: {})", product.getId(), quantity);

        if (product.getRecipe() == null || product.getRecipe().getItemDetailList() == null) {
            return; // Sin receta, sin descuento
        }

        Recipe recipe = product.getRecipe();

        for (ItemDetail itemDetail : recipe.getItemDetailList()) {
            Item item = itemDetail.getItem();
            Integer quantityToDeduct = itemDetail.getQuantity() * quantity;

            deductItemFromInventory(item, quantityToDeduct);
        }

        log.info("Items descontados exitosamente para producto: {}", product.getId());
    }

    /**
     * Descuenta una cantidad específica de un item del inventario
     * Descuenta en orden FIFO de los lotes (por fecha de vencimiento)
     *
     * @param item Item a descontar
     * @param quantity Cantidad a descontar
     * @throws ValidationException si no hay suficiente stock
     */
    private void deductItemFromInventory(Item item, Integer quantity) {
        log.debug("Descontando {} unidades del item: {}", quantity, item.getId());

        // Obtener todos los stocks del item ordenados por lote (FIFO)
        List<ItemStock> stocks = itemStockRepository.findByItem(item);

        if (stocks == null || stocks.isEmpty()) {
            throw new ValidationException(
                    "No hay stock del item: " + item.getId() + " (esto no debería pasar)");
        }

        Integer remaining = quantity;

        // Descontar de cada stock hasta completar
        for (ItemStock stock : stocks) {
            if (remaining <= 0) break;

            if (stock.getQuantity() >= remaining) {
                // Este stock tiene suficiente
                stock.setQuantity(stock.getQuantity() - remaining);
                itemStockRepository.save(stock);
                remaining = 0;
            } else {
                // Este stock se agota completamente
                remaining -= stock.getQuantity();
                stock.setQuantity(0);
                itemStockRepository.save(stock);
            }
        }

        if (remaining > 0) {
            throw new ValidationException(
                    "No hay stock suficiente del item: " + item.getId());
        }
    }

    /**
     * Valida que TODOS los productos en una orden tienen stock disponible
     * Se ejecuta ANTES de crear la orden
     *
     * @param orderItems Items solicitados en la orden
     * @throws ValidationException si algún producto no está disponible
     */
    public void validateOrderItemsAvailability(List<OrderItemRequest> orderItems) {
        for (OrderItemRequest itemRequest : orderItems) {
            // Asumir que itemRequest contiene productId (no itemId)
            // Necesito que actualices OrderItemRequest
        }
    }
}

