// ...existing code...
package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
}
