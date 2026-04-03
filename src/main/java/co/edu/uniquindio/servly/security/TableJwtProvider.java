package co.edu.uniquindio.servly.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Genera y valida tokens JWT para sesiones de mesa (clientes anónimos).
 *
 * El claim "tokenType: TABLE_SESSION" lo diferencia de los tokens de usuario.
 * Contiene tableNumber y sessionId para identificar la sesión en BD.
 */
@Slf4j
@Component
public class TableJwtProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.table-session.expiration:14400000}")
    private long sessionExpiration;

    private static final String CLAIM_TABLE_NUMBER = "tableNumber";
    private static final String CLAIM_SESSION_ID   = "sessionId";
    private static final String CLAIM_TYPE         = "tokenType";
    private static final String TYPE_TABLE_SESSION = "TABLE_SESSION";

    public String generateTableToken(Integer tableNumber, String sessionId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject("table:" + tableNumber)
                .claim(CLAIM_TABLE_NUMBER, tableNumber)
                .claim(CLAIM_SESSION_ID,   sessionId)
                .claim(CLAIM_TYPE,         TYPE_TABLE_SESSION)
                .issuedAt(new Date(now))
                .expiration(new Date(now + sessionExpiration))
                .signWith(getSignKey())
                .compact();
    }

    public boolean isValidTableToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return TYPE_TABLE_SESSION.equals(claims.get(CLAIM_TYPE, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token de mesa inválido: {}", e.getMessage());
            return false;
        }
    }

    public Integer extractTableNumber(String token) {
        return parseClaims(token).get(CLAIM_TABLE_NUMBER, Integer.class);
    }

    public String extractSessionId(String token) {
        return parseClaims(token).get(CLAIM_SESSION_ID, String.class);
    }

    public long getSessionExpirationMs() {
        return sessionExpiration;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
}