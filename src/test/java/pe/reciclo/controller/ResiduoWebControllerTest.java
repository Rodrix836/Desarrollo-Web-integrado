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
import pe.reciclo.model.RegistroResiduo;
import pe.reciclo.model.TipoResiduo;
import pe.reciclo.service.RegistroResiduoService;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ResiduoWebControllerTest {

    @TempDir
    static Path directorio;

    @DynamicPropertySource
    static void datosDePrueba(DynamicPropertyRegistry properties) {
        properties.add("reciclo.datos.archivo", () -> directorio.resolve("residuos.json").toString());
    }

    private final MockMvc mvc;
    private final RegistroResiduoService service;

    @Autowired
    ResiduoWebControllerTest(MockMvc mvc, RegistroResiduoService service) {
        this.mvc = mvc;
        this.service = service;
    }

    @BeforeEach
    void preparar() {
        service.listar().forEach(registro -> service.eliminar(registro.id()));
    }

    @Test
    void contextoIniciaYLaPaginaPrincipalOfreceLasDosAcciones() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("inicio"))
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("Plataforma web para promover y gestionar el reciclaje de residuos.")))
                .andExpect(content().string(containsString("Ver residuos")))
                .andExpect(content().string(containsString("Registrar residuo")));
    }

    @Test
    void paginasDeListadoFormularioDetalleEdicionYEliminacionSeRenderizan() throws Exception {
        mvc.perform(get("/residuos")).andExpect(status().isOk()).andExpect(view().name("residuos/lista"));
        mvc.perform(get("/residuos/nuevo")).andExpect(status().isOk()).andExpect(view().name("residuos/formulario"));
        var registro = service.crear(new RegistroResiduo(null, TipoResiduo.PLASTICO,
                new BigDecimal("2"), "kg", LocalDate.of(2026, 9, 10), "Botellas de muestra"));
        mvc.perform(get("/residuos")).andExpect(content().string(containsString("Plástico")));
        mvc.perform(get("/residuos/{id}", registro.id()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Botellas de muestra")));
        mvc.perform(get("/residuos/{id}/editar", registro.id()))
                .andExpect(status().isOk()).andExpect(view().name("residuos/formulario"));
        mvc.perform(get("/residuos/{id}/eliminar", registro.id()))
                .andExpect(status().isOk()).andExpect(view().name("residuos/eliminar"));
        assertThat(service.listar()).containsExactly(registro);
    }

    @Test
    void formulariosRegistranEditanYEliminanSinJavascript() throws Exception {
        mvc.perform(post("/residuos").param("tipoResiduo", "VIDRIO").param("cantidad", "3")
                        .param("unidadMedida", "kg").param("fecha", "2026-09-10").param("descripcion", "Frascos"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/residuos"));
        var creado = service.listar().getFirst();
        assertThat(creado.tipoResiduo()).isEqualTo(TipoResiduo.VIDRIO);
        assertThat(creado.fecha()).isEqualTo(LocalDate.of(2026, 9, 10));

        mvc.perform(post("/residuos/{id}/editar", creado.id()).param("tipoResiduo", "METAL")
                        .param("cantidad", "4.5").param("unidadMedida", "kg").param("fecha", "2026-09-11"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/residuos"));
        assertThat(service.consultar(creado.id()).cantidad()).isEqualByComparingTo("4.5");
        assertThat(service.consultar(creado.id()).tipoResiduo()).isEqualTo(TipoResiduo.METAL);

        mvc.perform(post("/residuos/{id}/eliminar", creado.id()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/residuos"));
        assertThat(service.listar()).isEmpty();
    }

    @Test
    void formularioInvalidoMuestraErroresYConservaLosValores() throws Exception {
        mvc.perform(post("/residuos").param("tipoResiduo", "VIDRIO").param("cantidad", "0")
                        .param("unidadMedida", "kg").param("fecha", "2026-09-10").param("descripcion", "Conservar este texto"))
                .andExpect(status().isBadRequest()).andExpect(view().name("residuos/formulario"))
                .andExpect(model().attributeHasFieldErrors("residuo", "cantidad"))
                .andExpect(content().string(containsString("La cantidad debe ser mayor que cero.")))
                .andExpect(content().string(containsString("Conservar este texto")));
        assertThat(service.listar()).isEmpty();
    }

    @Test
    void fechaImposibleSeMuestraComoErrorDeFormulario() throws Exception {
        mvc.perform(post("/residuos").param("tipoResiduo", "VIDRIO").param("cantidad", "2")
                        .param("unidadMedida", "kg").param("fecha", "2026-02-30"))
                .andExpect(status().isBadRequest()).andExpect(view().name("residuos/formulario"))
                .andExpect(model().attributeHasFieldErrors("residuo", "fecha"));
        assertThat(service.listar()).isEmpty();
    }

    @Test
    void idInexistenteONoNumericoMuestraErrorHtml() throws Exception {
        mvc.perform(get("/residuos/999999"))
                .andExpect(status().isNotFound()).andExpect(view().name("error"))
                .andExpect(content().contentTypeCompatibleWith("text/html"));
        mvc.perform(get("/residuos/abc"))
                .andExpect(status().isBadRequest()).andExpect(view().name("error"));
    }
}
