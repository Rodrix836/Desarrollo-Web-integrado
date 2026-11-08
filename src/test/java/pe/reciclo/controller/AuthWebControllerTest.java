package pe.reciclo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "reciclo.seguridad.activa=true")
class AuthWebControllerTest {

    private final MockMvc mvc;

    @Autowired
    AuthWebControllerTest(MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    void loginCorrectoDeAdministradorRedirigeAlModuloAdmin() throws Exception {
        mvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void loginCorrectoDeEmpresaRedirigeAlCatalogo() throws Exception {
        mvc.perform(post("/login")
                        .param("username", "empresa")
                        .param("password", "empresa123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/empresa"));
    }

    @Test
    void loginIncorrectoMuestraError() throws Exception {
        mvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "incorrecta"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Usuario o contraseña incorrectos."));
    }

    @Test
    void empresaNoPuedeEntrarAlModuloAdministrativo() throws Exception {
        var login = mvc.perform(post("/login")
                        .param("username", "empresa")
                        .param("password", "empresa123"))
                .andReturn().getRequest().getSession();

        mvc.perform(get("/admin").session((org.springframework.mock.web.MockHttpSession) login))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/acceso-denegado"));
    }

    @Test
    void usuarioSinSesionNoPuedeEntrarAlModuloEmpresa() throws Exception {
        mvc.perform(get("/empresa"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void logoutInvalidaLaSesion() throws Exception {
        var result = mvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "admin123"))
                .andReturn();

        var session = result.getRequest().getSession();
        mvc.perform(get("/logout").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }
}
