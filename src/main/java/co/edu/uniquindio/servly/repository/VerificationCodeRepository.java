package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.VerificationCode;
import co.edu.uniquindio.servly.model.enums.CodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, String> {

    Optional<VerificationCode> findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(
            String email, CodeType type);

    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.email = :email AND v.type = :type")
    void deleteByEmailAndType(String email, CodeType type);

    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.expiresAt < :now OR v.used = true")
    void deleteExpiredAndUsed(LocalDateTime now);
}