package pe.reciclo.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:auth-test", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class AuthApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @DynamicPropertySource
    static void clave(DynamicPropertyRegistry registry) {
        String secret = java.util.UUID.randomUUID().toString() + java.util.UUID.randomUUID();
        registry.add("reciclo.jwt.secret", () -> secret);
    }

    private String token(String username, String password) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(mapper.writeValueAsString(java.util.Map.of("username", username, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("token").asText();
    }

    @Test void usuarioNoPuedeAdministrar() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + token("user", "user123")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + token("admin", "admin123")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].username").exists());
    }

    @Test void loginIncorrectoYMe() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"admin\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token("admin", "admin123")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test void registroCreaUserYEvitaDuplicados() throws Exception {
        String body = "{\"username\":\"nuevo-test\",\"password\":\"clave123\",\"nombre\":\"Prueba\"}";
        mvc.perform(post("/api/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("USER"));
        mvc.perform(post("/api/auth/register").contentType("application/json").content(body))
                .andExpect(status().isConflict());
    }
}
