package co.edu.uniquindio.servly.repository;

import co.edu.uniquindio.servly.model.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para gestionar tokens revocados (blacklist).
 */
@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {

    /**
     * Busca un token revocado por su valor.
     */
    Optional<RevokedToken> findByToken(String token);

    /**
     * Verifica si un token está en la blacklist.
     */
    boolean existsByToken(String token);

    /**
     * Elimina todos los tokens expirados (limpieza).
     */
    void deleteByExpiresAtBefore(LocalDateTime date);

    /**
     * Busca todos los tokens revocados de un usuario.
     */
    List<RevokedToken> findByUserEmail(String userEmail);

    /**
     * Elimina todos los tokens revocados de un usuario (útil al cambiar password).
     */
    void deleteByUserEmail(String userEmail);
}
