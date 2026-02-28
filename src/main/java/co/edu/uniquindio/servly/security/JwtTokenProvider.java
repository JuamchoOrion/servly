package co.edu.uniquindio.servly.security;

import co.edu.uniquindio.servly.model.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Genera y valida tokens JWT para usuarios autenticados (staff).
 * Produce dos tipos: accessToken (24h) y refreshToken (7 días).
 * Incluye passwordVersion para invalidar tokens al cambiar contraseña.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), jwtExpiration, getPasswordVersion(userDetails));
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), refreshExpiration, getPasswordVersion(userDetails));
    }

    private Integer getPasswordVersion(UserDetails userDetails) {
        if (userDetails instanceof User user) {
            return user.getPasswordVersion();
        }
        return 0; // Default para otros tipos de UserDetails
    }

    private String buildToken(String subject, long expiration, Integer passwordVersion) {
        long now = System.currentTimeMillis();
        Map<String, Object> claims = new HashMap<>();
        if (passwordVersion != null) {
            claims.put("passwordVersion", passwordVersion);
        }
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(getSignKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            Integer tokenPasswordVersion = extractPasswordVersion(token);
            Integer currentPasswordVersion = getPasswordVersion(userDetails);
            
            boolean usernameMatches = username.equals(userDetails.getUsername());
            boolean notExpired = !isTokenExpired(token);
            boolean versionMatches = tokenPasswordVersion == null || 
                                     tokenPasswordVersion.equals(currentPasswordVersion);
            
            if (!versionMatches) {
                log.debug("Token JWT con versión de contraseña obsoleta para: {}", username);
            }
            
            return usernameMatches && notExpired && versionMatches;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Integer extractPasswordVersion(String token) {
        try {
            return extractClaim(token, claims -> claims.get("passwordVersion", Integer.class));
        } catch (Exception e) {
            log.debug("No se pudo extraer passwordVersion del token: {}", e.getMessage());
            return null;
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }

    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
}