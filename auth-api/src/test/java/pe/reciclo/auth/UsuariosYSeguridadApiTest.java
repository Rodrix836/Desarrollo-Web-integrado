package pe.reciclo.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pe.reciclo.auth.model.Rol;
import pe.reciclo.auth.model.Usuario;
import pe.reciclo.auth.repository.UsuarioRepository;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:auth-usuarios",
        "spring.jpa.hibernate.ddl-auto=create-drop", "reciclo.login.max-intentos=3"})
@AutoConfigureMockMvc
class UsuariosYSeguridadApiTest {

    private static final String SECRET = UUID.randomUUID().toString() + UUID.randomUUID();

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepository repository;
    @Autowired PasswordEncoder encoder;

    @DynamicPropertySource
    static void clave(DynamicPropertyRegistry registry) {
        registry.add("reciclo.jwt.secret", () -> SECRET);
    }

    @BeforeEach
    void cuentaDePrueba() {
        repository.findByUsername("victima").ifPresent(repository::delete);
        repository.findByUsername("cliente").ifPresent(repository::delete);
        repository.save(new Usuario("cliente", encoder.encode("cliente123"), "Cliente", Rol.USER));
    }

    private JsonNode login(String username, String password) throws Exception {
        String body = mapper.writeValueAsString(Map.of("username", username, "password", password));
        String res = mvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(res);
    }

    private String bearer(String username, String password) throws Exception {
        return "Bearer " + login(username, password).get("token").asText();
    }

    private Usuario cliente() {
        return repository.findByUsername("cliente").orElseThrow();
    }

    @Test
    void tokenIncluyeSubRolYExpiracion() throws Exception {
        JsonNode sesion = login("cliente", "cliente123");
        assertThat(sesion.get("expiraEnMs").asLong()).isPositive();
        var claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).build()
                .parseSignedClaims(sesion.get("token").asText()).getPayload();
        assertThat(claims.getSubject()).isEqualTo("cliente");
        assertThat(claims.get("rol", String.class)).isEqualTo("USER");
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    void elRolEmpresaYaNoExiste() throws Exception {
        assertThat(Rol.values()).containsExactly(Rol.ADMIN, Rol.USER);
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"username\":\"empresa\",\"password\":\"empresa123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshEmiteUnTokenNuevoConElRolActual() throws Exception {
        String token = bearer("cliente", "cliente123");
        var u = cliente();
        u.setRol(Rol.ADMIN);
        repository.save(u);
        mvc.perform(post("/api/auth/refresh").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void bloqueaTrasVariosIntentosFallidos() throws Exception {
        String malo = "{\"username\":\"victima\",\"password\":\"incorrecta\"}";
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/login").contentType("application/json").content(malo))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType("application/json").content(malo))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void loginExitosoReiniciaElContador() throws Exception {
        String malo = "{\"username\":\"cliente\",\"password\":\"incorrecta\"}";
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/auth/login").contentType("application/json").content(malo))
                    .andExpect(status().isUnauthorized());
        }
        login("cliente", "cliente123");
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/auth/login").contentType("application/json").content(malo))
                    .andExpect(status().isUnauthorized());
        }
        login("cliente", "cliente123");
    }

    @Test
    void adminDesactivaYReactivaUnaCuenta() throws Exception {
        String admin = bearer("admin", "admin123");
        long id = cliente().getId();
        mvc.perform(put("/api/usuarios/" + id + "/estado").header("Authorization", admin)
                        .contentType("application/json").content("{\"activo\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"username\":\"cliente\",\"password\":\"cliente123\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/usuarios/" + id + "/estado").header("Authorization", admin)
                        .contentType("application/json").content("{\"activo\":true}"))
                .andExpect(status().isOk());
        login("cliente", "cliente123");
    }

    @Test
    void adminEditaYEliminaCuentas() throws Exception {
        String admin = bearer("admin", "admin123");
        long id = cliente().getId();
        mvc.perform(put("/api/usuarios/" + id).header("Authorization", admin).contentType("application/json")
                        .content("{\"nombre\":\"Cliente Editado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Cliente Editado"));
        mvc.perform(put("/api/usuarios/" + id).header("Authorization", admin).contentType("application/json")
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/usuarios/" + id).header("Authorization", admin)).andExpect(status().isNoContent());
        assertThat(repository.findByUsername("cliente")).isEmpty();
        mvc.perform(delete("/api/usuarios/" + id).header("Authorization", admin)).andExpect(status().isNotFound());
    }

    @Test
    void cambiarRolSoloAceptaAdminOUser() throws Exception {
        String admin = bearer("admin", "admin123");
        long id = cliente().getId();
        mvc.perform(put("/api/usuarios/" + id + "/rol").header("Authorization", admin)
                        .contentType("application/json").content("{\"rol\":\"EMPRESA\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/usuarios/" + id + "/rol").header("Authorization", admin)
                        .contentType("application/json").content("{\"rol\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMIN"));
        mvc.perform(put("/api/usuarios/" + id + "/rol").header("Authorization", admin)
                        .contentType("application/json").content("{\"rol\":\"SUPERUSUARIO\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/usuarios/999999/rol").header("Authorization", admin)
                        .contentType("application/json").content("{\"rol\":\"USER\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminNoPuedeAutoBloquearse() throws Exception {
        String admin = bearer("admin", "admin123");
        long id = repository.findByUsername("admin").orElseThrow().getId();
        mvc.perform(put("/api/usuarios/" + id + "/rol").header("Authorization", admin)
                        .contentType("application/json").content("{\"rol\":\"USER\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/usuarios/" + id + "/estado").header("Authorization", admin)
                        .contentType("application/json").content("{\"activo\":false}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/usuarios/" + id).header("Authorization", admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void noAdminNoGestionaUsuarios() throws Exception {
        String user = bearer("cliente", "cliente123");
        long id = cliente().getId();
        mvc.perform(delete("/api/usuarios/" + id).header("Authorization", user)).andExpect(status().isForbidden());
        mvc.perform(put("/api/usuarios/" + id + "/rol").header("Authorization", user)
                        .contentType("application/json").content("{\"rol\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/usuarios/" + id + "/estado").header("Authorization", user)
                        .contentType("application/json").content("{\"activo\":false}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/usuarios/" + id).header("Authorization", user)
                        .contentType("application/json").content("{\"nombre\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioEditaSuPerfilYContrasena() throws Exception {
        String token = bearer("cliente", "cliente123");
        mvc.perform(put("/api/auth/me").header("Authorization", token).contentType("application/json")
                        .content("{\"nombre\":\"Nuevo Nombre\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Nuevo Nombre"));
        mvc.perform(put("/api/auth/me/password").header("Authorization", token).contentType("application/json")
                        .content("{\"actual\":\"equivocada\",\"nueva\":\"nueva1234\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/auth/me/password").header("Authorization", token).contentType("application/json")
                        .content("{\"actual\":\"cliente123\",\"nueva\":\"123\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/auth/me/password").header("Authorization", token).contentType("application/json")
                        .content("{\"actual\":\"cliente123\",\"nueva\":\"nueva1234\"}"))
                .andExpect(status().isNoContent());
        login("cliente", "nueva1234");
        mvc.perform(put("/api/auth/me").contentType("application/json").content("{\"nombre\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }
}
