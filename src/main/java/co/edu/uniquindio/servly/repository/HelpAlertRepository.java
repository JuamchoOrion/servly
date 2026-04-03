package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.HelpAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HelpAlertRepository extends JpaRepository<HelpAlert, Long> {
    List<HelpAlert> findByTableNumberAndStatusOrderByCreatedAtDesc(Integer tableNumber, HelpAlert.AlertStatus status);
    List<HelpAlert> findByStatusOrderByCreatedAtDesc(HelpAlert.AlertStatus status);
}

