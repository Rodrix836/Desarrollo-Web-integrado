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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RegistroResiduoJpqlControllerTest {

    @TempDir
    static Path directorio;

    @DynamicPropertySource
    static void datosDePrueba(DynamicPropertyRegistry properties) {
        properties.add("reciclo.datos.archivo", () -> directorio.resolve("residuos.json").toString());
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    RegistroResiduoService service;

    @BeforeEach
    void preparar() {
        service.listar().forEach(registro -> service.eliminar(registro.id()));
        service.crear(registro("EcoEnvases", "2"));
        service.crear(registro("Papelera Andina", "5"));
        service.crear(registro("Vidrios del Sur", "3"));
    }

    private RegistroResiduo registro(String empresa, String cantidad) {
        return new RegistroResiduo(null, TipoResiduo.PLASTICO, new BigDecimal(cantidad),
                "kg", LocalDate.of(2026, 9, 10), "Residuo de prueba", empresa, "Papel", true);
    }

    @Test
    void buscaEmpresaParcialSinDistinguirMayusculas() throws Exception {
        mvc.perform(get("/api/residuos/jpql/empresa").param("empresa", "eCoEn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].empresa").value("EcoEnvases"))
                .andExpect(jsonPath("$[0].cantidad").value(2))
                .andExpect(jsonPath("$[0].tipoResiduo").value("PLASTICO"))
                .andExpect(jsonPath("$[0].fecha").value("2026-09-10"));
        mvc.perform(get("/api/residuos/jpql/empresa").param("empresa", "Inexistente"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void cantidadEsEstrictamenteMayorYConservaDecimales() throws Exception {
        service.crear(registro("Decimal", "3.0001"));
        mvc.perform(get("/api/residuos/jpql/cantidad").param("cantidad", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].cantidad", org.hamcrest.Matchers.containsInAnyOrder(5, 3.0001)));
        mvc.perform(get("/api/residuos/jpql/cantidad").param("cantidad", "5"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void ordenaCantidadDescendente() throws Exception {
        mvc.perform(get("/api/residuos/jpql/ordenados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].cantidad", org.hamcrest.Matchers.contains(5, 3, 2)));
    }

    @Test
    void consultasReflejanCreacionActualizacionYEliminacionDelCrud() throws Exception {
        var creado = service.crear(registro("Empresa Demo", "12.5"));
        mvc.perform(get("/api/residuos/jpql/empresa").param("empresa", "Demo"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(creado.id()));
        service.actualizar(creado.id(), registro("Empresa Nueva", "20"));
        mvc.perform(get("/api/residuos/jpql/empresa").param("empresa", "Demo"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/residuos/jpql/cantidad").param("cantidad", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].cantidad").value(20));
        service.eliminar(creado.id());
        mvc.perform(get("/api/residuos/jpql/cantidad").param("cantidad", "10"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void listaVaciaNoRestauraDatosAnteriores() throws Exception {
        mvc.perform(get("/api/residuos/jpql/ordenados")).andExpect(status().isOk());
        service.listar().forEach(registro -> service.eliminar(registro.id()));
        mvc.perform(get("/api/residuos/jpql/ordenados"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void parametrosAusentesONoNumericosDevuelven400() throws Exception {
        mvc.perform(get("/api/residuos/jpql/empresa")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/residuos/jpql/cantidad")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/residuos/jpql/cantidad").param("cantidad", "abc"))
                .andExpect(status().isBadRequest());
    }
}
