package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.RestaurantTable;
import co.edu.uniquindio.servly.model.entity.TableSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TableSessionRepository extends JpaRepository<TableSession, String> {

    Optional<TableSession> findByRestaurantTableAndActiveTrue(RestaurantTable restaurantTable);

    Optional<TableSession> findBySessionTokenAndActiveTrue(String sessionToken);

    boolean existsByRestaurantTableAndActiveTrue(RestaurantTable restaurantTable);

    @Modifying
    @Query("UPDATE TableSession t SET t.active = false, t.closedAt = :now " +
            "WHERE t.active = true AND t.expiresAt < :now")
    void closeExpiredSessions(LocalDateTime now);
}
