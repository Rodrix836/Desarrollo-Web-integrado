package pe.reciclo.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${reciclo.jwt.secret}") String secret,
            @Value("${reciclo.jwt.expiration-ms:3600000}") long expirationMs) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("reciclo.jwt.secret debe tener al menos 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
    }

    public String generarToken(String username, String rol) {
        return Jwts.builder()
                .subject(username)
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String extraerUsuario(String token) {
        return obtenerClaims(token).getSubject();
    }

    public String extraerRol(String token) {
        return obtenerClaims(token).get("rol", String.class);
    }

    public boolean esValido(String token) {
        try {
            obtenerClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims obtenerClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}