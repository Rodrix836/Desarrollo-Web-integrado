package pe.reciclo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pe.reciclo.security.JwtAuthenticationFilter;

/**
 * Seguridad del proyecto (Integrante 1 lidera Auth/JWT/Roles).
 * Usuarios se cargan desde BD vía CustomUserDetailsService.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFiltro)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // API Auth pública
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        // Rutas web (Thymeleaf) — controladas por AccesoInterceptor
                        .requestMatchers("/", "/login", "/logout", "/error", "/acceso-denegado",
                                "/css/**", "/js/**", "/admin/**", "/empresa/**", "/residuos/**")
                        .permitAll()

                        // Gestión de usuarios: solo ADMIN
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")

                        // Residuos API (como estaba)
                        .requestMatchers(HttpMethod.GET, "/api/residuos/**").hasAnyRole("USER", "ADMIN", "EMPRESA")
                        .requestMatchers(HttpMethod.POST, "/api/residuos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/residuos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/residuos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/residuos/**").hasRole("ADMIN")

                        .requestMatchers("/api/auth/me").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFiltro, UsernamePasswordAuthenticationFilter.class)
                .headers(h -> h.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }
}
