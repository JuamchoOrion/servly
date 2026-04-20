package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.Order;
import co.edu.uniquindio.servly.model.entity.TableSource;
import co.edu.uniquindio.servly.model.enums.OrderTableState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Obtiene todas las órdenes de una mesa
     */
    @Query(value = """
        SELECT o.* FROM orders o 
        INNER JOIN order_source os ON o.source_id = os.id 
        INNER JOIN table_source ts ON os.id = ts.id 
        INNER JOIN restaurant_tables rt ON ts.restaurant_table_id = rt.id 
        WHERE rt.table_number = :tableNumber
        """, nativeQuery = true)
    List<Order> findByTableNumber(@Param("tableNumber") Integer tableNumber);


    /**
     * Obtiene órdenes pendientes por estado
     */
    List<Order> findByStatus(OrderTableState status);

    /**
     * Obtiene órdenes por fecha
     */
    List<Order> findByDate(LocalDate date);

    /**
     * Obtiene una orden por ID si existe
     */
    Optional<Order> findById(Long id);
}

