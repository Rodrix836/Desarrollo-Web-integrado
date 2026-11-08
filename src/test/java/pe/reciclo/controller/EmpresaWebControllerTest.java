package pe.reciclo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import pe.reciclo.model.Rol;
import pe.reciclo.model.UsuarioSesion;
import pe.reciclo.model.RegistroResiduo;
import pe.reciclo.model.TipoResiduo;
import pe.reciclo.service.RegistroResiduoService;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EmpresaWebControllerTest {

    @TempDir
    static Path directorio;

    @DynamicPropertySource
    static void datosDePrueba(DynamicPropertyRegistry properties) {
        properties.add("reciclo.datos.archivo", () -> directorio.resolve("residuos.json").toString());
    }

    private final MockMvc mvc;
    private final RegistroResiduoService service;

    @Autowired
    EmpresaWebControllerTest(MockMvc mvc, RegistroResiduoService service) {
        this.mvc = mvc;
        this.service = service;
    }

    @BeforeEach
    void preparar() {
        service.listar().forEach(registro -> service.eliminar(registro.id()));
    }

    @Test
    void empresaVeCatalogoAjenoSinAccionesDeAdministracion() throws Exception {
        crear("Empresa Demo", true, "Material propio");
        crear("Papelera Andina", true, "Cartón de otra empresa");
        crear("Vidrios del Sur", false, "Material retirado");

        mvc.perform(get("/empresa").session(empresaSession()))
                .andExpect(status().isOk()).andExpect(view().name("empresa/catalogo"))
                .andExpect(content().string(containsString("Cartón de otra empresa")))
                .andExpect(content().string(not(containsString("Material propio"))))
                .andExpect(content().string(not(containsString("Material retirado"))))
                .andExpect(content().string(not(containsString("Editar residuo"))))
                .andExpect(content().string(not(containsString("Eliminar residuo"))));
    }

    @Test
    void empresaConsultaDetalleYCambiaLaEmpresaDeDemostracion() throws Exception {
        long id = crear("Papelera Andina", true, "Cajas limpias").id();
        mvc.perform(get("/empresa/materiales/{id}", id).session(empresaSession("Empresa Demo")))
                .andExpect(status().isOk()).andExpect(view().name("empresa/detalle"))
                .andExpect(content().string(containsString("Papelera Andina")))
                .andExpect(content().string(containsString("Envases de vidrio")));
        mvc.perform(get("/empresa/materiales/{id}", id).session(empresaSession("Papelera Andina")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Vista empresa")))
                .andExpect(content().string(not(containsString("Administración"))))
                .andExpect(model().attribute("empresaActual", "Papelera Andina"));
        mvc.perform(get("/empresa/materiales/abc").session(empresaSession("Papelera Andina")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Vista empresa")));
        mvc.perform(get("/empresa").session(empresaSession("Papelera Andina")))
                .andExpect(status().isOk()).andExpect(model().attribute("ofertas", java.util.List.of()));
    }

    @Test
    void administradorConservaLaGestionYPublicaMaterialesParaEmpresas() throws Exception {
        mvc.perform(get("/admin").session(adminSession())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Administración")));
        mvc.perform(post("/residuos").session(adminSession()).param("tipoResiduo", "PAPEL_CARTON").param("cantidad", "5")
                        .param("unidadMedida", "kg").param("fecha", "2026-09-10")
                        .param("descripcion", "Cajas disponibles").param("empresa", "Papelera Andina")
                        .param("intercambioPor", "Envases de vidrio").param("disponible", "true"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/empresa").session(empresaSession())).andExpect(content().string(containsString("Cajas disponibles")));
    }

    @Test
    void noSePublicaComoDisponibleSinIndicarLaEmpresa() throws Exception {
        mvc.perform(post("/residuos").session(adminSession()).param("tipoResiduo", "PAPEL_CARTON").param("cantidad", "5")
                        .param("unidadMedida", "kg").param("fecha", "2026-09-10").param("disponible", "true"))
                .andExpect(status().isBadRequest()).andExpect(view().name("residuos/formulario"))
                .andExpect(content().string(containsString("Indica la empresa propietaria")));
    }


    private MockHttpSession adminSession() {
        var session = new MockHttpSession();
        session.setAttribute("usuario", new UsuarioSesion("admin", "Administrador ReCiclo", Rol.ADMIN, ""));
        return session;
    }

    private MockHttpSession empresaSession() {
        return empresaSession("Empresa Demo");
    }

    private MockHttpSession empresaSession(String empresa) {
        var session = new MockHttpSession();
        session.setAttribute("usuario", new UsuarioSesion("empresa", empresa, Rol.EMPRESA, empresa));
        return session;
    }

    private RegistroResiduo crear(String empresa, boolean disponible, String descripcion) {
        return service.crear(new RegistroResiduo(null, TipoResiduo.PAPEL_CARTON,
                new BigDecimal("5"), "kg", LocalDate.of(2026, 9, 10), descripcion,
                empresa, "Envases de vidrio", disponible));
    }
}
