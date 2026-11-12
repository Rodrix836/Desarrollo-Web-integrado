package pe.reciclo.residuos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pe.reciclo.residuos.exception.ErrorRespuesta;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    private static final String BASE = "/api/residuos";
    private static final String DETALLE = BASE + "/{id}";

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, ObjectMapper mapper,
                                  @Value("${springdoc.swagger-ui.enabled:false}") boolean swagger) throws Exception {
        AuthenticationEntryPoint sinToken = (req, res, ex) -> {
            res.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            responder(res, mapper, HttpStatus.UNAUTHORIZED, "Se requiere un token Bearer válido.");
        };
        AccessDeniedHandler sinPermiso = (req, res, ex) ->
                responder(res, mapper, HttpStatus.FORBIDDEN, "Tu rol no tiene permiso para esta operación.");

        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(permisos -> {
                    if (swagger) {
                        permisos.requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs", "/v3/api-docs/**").permitAll();
                    }
                    permisos.requestMatchers(HttpMethod.GET, BASE, DETALLE).hasAnyRole("USER", "ADMIN")
                            .requestMatchers(HttpMethod.POST, BASE).hasRole("ADMIN")
                            .requestMatchers(HttpMethod.PUT, DETALLE).hasRole("ADMIN")
                            .requestMatchers(HttpMethod.DELETE, DETALLE).hasRole("ADMIN")
                            .anyRequest().denyAll();
                })
                .exceptionHandling(errores -> errores.authenticationEntryPoint(sinToken).accessDeniedHandler(sinPermiso))
                .oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(sinToken)
                        .accessDeniedHandler(sinPermiso)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDeRoles())))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${reciclo.jwt.secret:}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (secreto.isBlank() || bytes.length < 32) {
            throw new IllegalStateException("Configura RECICLO_JWT_SECRET con la clave compartida de al menos 32 bytes.");
        }
        var clave = new SecretKeySpec(bytes, "HmacSHA256");
        var decoder = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> claimsObligatorios = jwt -> {
            if (jwt.getExpiresAt() != null && jwt.getSubject() != null && !jwt.getSubject().isBlank()
                    && jwt.getClaims().get("rol") instanceof String rol && !rol.isBlank()) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "El token debe incluir sub, exp y rol.", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO), claimsObligatorios));
        return decoder;
    }

    private JwtAuthenticationConverter conversorDeRoles() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt ->
                List.of(new SimpleGrantedAuthority("ROLE_" + jwt.getClaimAsString("rol"))));
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of("http://localhost:4200", "http://127.0.0.1:4200"));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        cors.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        cors.setExposedHeaders(List.of(HttpHeaders.LOCATION));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(BASE, cors);
        source.registerCorsConfiguration(BASE + "/**", cors);
        return source;
    }

    private void responder(HttpServletResponse response, ObjectMapper mapper, HttpStatus status, String mensaje)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ErrorRespuesta.de(status, mensaje, Map.of()));
    }
}
