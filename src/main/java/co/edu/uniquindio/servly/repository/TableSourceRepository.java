package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.TableSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TableSourceRepository extends JpaRepository<TableSource, Long> {
    Optional<TableSource> findByTableNumber(Integer tableNumber);
}

