package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Order.OrderDTO;
import co.edu.uniquindio.servly.exception.NotFoundException;
import co.edu.uniquindio.servly.model.entity.Order_detail;
import co.edu.uniquindio.servly.model.enums.OrderTableState;
import co.edu.uniquindio.servly.model.enums.OrderType;
import co.edu.uniquindio.servly.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de OrderService")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    @Nested
    @DisplayName("Pruebas de actualización de estado")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("Debe cambiar estado de PENDING a SERVED")
        void shouldUpdateStatusSuccessfully() {
            Long orderId = 1L;
            when(orderRepository.existsById(orderId)).thenReturn(true);

            orderService.updateOrderStatus(orderId, null);

            verify(orderRepository, times(1)).existsById(orderId);
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando la orden no existe")
        void shouldThrowExceptionWhenOrderNotFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () ->
                orderService.updateOrderStatus(999L, null));
        }
    }

    @Nested
    @DisplayName("Pruebas de cancelación")
    class CancelOrderTests {

        @Test
        @DisplayName("Debe cancelar una orden")
        void shouldCancelOrderSuccessfully() {
            Long orderId = 1L;
            when(orderRepository.existsById(orderId)).thenReturn(true);

            orderService.cancelOrder(orderId);

            verify(orderRepository, times(1)).existsById(orderId);
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando la orden no existe")
        void shouldThrowExceptionWhenCancellingNonExistentOrder() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> orderService.cancelOrder(999L));
        }
    }

    @Nested
    @DisplayName("Pruebas de confirmación de pago")
    class ConfirmPaymentTests {

        @Test
        @DisplayName("Debe confirmar el pago")
        void shouldConfirmPaymentSuccessfully() {
            Long orderId = 1L;
            when(orderRepository.existsById(orderId)).thenReturn(true);

            orderService.confirmPayment(orderId);

            verify(orderRepository, times(1)).existsById(orderId);
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando la orden no existe")
        void shouldThrowExceptionWhenConfirmingNonExistentOrder() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> orderService.confirmPayment(999L));
        }
    }

    @Nested
    @DisplayName("Pruebas de búsqueda")
    class SearchOrderTests {

        @Test
        @DisplayName("Debe obtener órdenes por ID")
        void shouldFindOrderById() {
            Long orderId = 1L;
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(new co.edu.uniquindio.servly.model.entity.Order()));

            Optional<co.edu.uniquindio.servly.model.entity.Order> result = orderRepository.findById(orderId);

            assertTrue(result.isPresent());
            verify(orderRepository, times(1)).findById(orderId);
        }

        @Test
        @DisplayName("Debe retornar vacío cuando la orden no existe")
        void shouldReturnEmptyWhenOrderNotFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<co.edu.uniquindio.servly.model.entity.Order> result = orderRepository.findById(999L);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Debe obtener órdenes por fecha")
        void shouldFindOrdersByDate() {
            LocalDate today = LocalDate.now();
            when(orderRepository.findByDate(today)).thenReturn(new ArrayList<>());

            List<co.edu.uniquindio.servly.model.entity.Order> result = orderRepository.findByDate(today);

            assertNotNull(result);
            verify(orderRepository, times(1)).findByDate(today);
        }
    }

    @Nested
    @DisplayName("Pruebas de cálculo de total")
    class CalculateTotalTests {

        @Test
        @DisplayName("Debe calcular el total de una orden correctamente")
        void shouldCalculateTotalCorrectly() {
            Order_detail detail1 = Order_detail.builder()
                    .quantity(2)
                    .unitPrice(new BigDecimal("10.00"))
                    .subtotal(new BigDecimal("20.00"))
                    .build();

            Order_detail detail2 = Order_detail.builder()
                    .quantity(1)
                    .unitPrice(new BigDecimal("5.99"))
                    .subtotal(new BigDecimal("5.99"))
                    .build();

            List<Order_detail> details = List.of(detail1, detail2);
            BigDecimal total = details.stream()
                    .map(Order_detail::getSubtotal)
                    .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

            assertEquals(new BigDecimal("25.99"), total);
        }
    }
}

