package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Integer> {

    List<RestaurantTable> findByStatus(RestaurantTable.TableStatus status);

    boolean existsByTableNumber(Integer tableNumber);

    Optional<RestaurantTable> findByTableNumber(Integer tableNumber);

    @Query("SELECT rt FROM RestaurantTable rt ORDER BY rt.tableNumber ASC")
    List<RestaurantTable> findAllOrderedByTableNumber();
}

