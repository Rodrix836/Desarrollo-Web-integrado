package pe.reciclo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pe.reciclo.service.RegistroResiduoService;
import pe.reciclo.model.TipoResiduo;

import java.util.stream.Stream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RegistroResiduoControllerTest {

    @TempDir
    static Path directorio;

    @DynamicPropertySource
    static void datosDePrueba(DynamicPropertyRegistry properties) {
        properties.add("reciclo.datos.archivo", () -> directorio.resolve("residuos.json").toString());
    }

    private static final String URL = "/api/residuos";
    private static final String VALIDO = """
            {
              "tipoResiduo": "PLASTICO",
              "cantidad": 2.5,
              "unidadMedida": "kg",
              "fecha": "2026-09-09",
              "descripcion": "Botellas limpias"
            }
            """;

    private final MockMvc mvc;
    private final ObjectMapper objectMapper;
    private final RegistroResiduoService service;

    @Autowired
    RegistroResiduoControllerTest(MockMvc mvc, ObjectMapper objectMapper, RegistroResiduoService service) {
        this.mvc = mvc;
        this.objectMapper = objectMapper;
        this.service = service;
    }

    @BeforeEach
    void vaciarRegistros() {
        service.listar().forEach(registro -> service.eliminar(registro.id()));
    }

    @Test
    void listarSinDatosDevuelve200YListaVacia() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    @Test
    void crearDevuelve201LocationYPermiteConsultarYListar() throws Exception {
        var respuesta = mvc.perform(post(URL).contentType(APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.tipoResiduo").value("PLASTICO"))
                .andExpect(jsonPath("$.cantidad").value(2.5))
                .andExpect(jsonPath("$.unidadMedida").value("kg"))
                .andExpect(jsonPath("$.fecha").value("2026-09-09"))
                .andExpect(jsonPath("$.descripcion").value("Botellas limpias"))
                .andReturn().getResponse();
        long id = objectMapper.readTree(respuesta.getContentAsString()).get("id").asLong();
        assertThat(id).isPositive();
        assertThat(respuesta.getHeader("Location")).isEqualTo("http://localhost" + URL + "/" + id);

        mvc.perform(get(respuesta.getHeader("Location")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(respuesta.getContentAsString()));
        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void actualizarReemplazaDatosUsandoElIdDeLaRuta() throws Exception {
        long id = crearRegistro();
        long otroId = crearRegistro();
        var cambios = """
                {"id": %d, "tipoResiduo": "PAPEL_CARTON", "cantidad": 3,
                 "unidadMedida": "bolsas", "fecha": "2026-09-10"}
                """.formatted(otroId);

        var respuesta = mvc.perform(put(URL + "/{id}", id)
                        .contentType(APPLICATION_JSON).content(cambios))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.tipoResiduo").value("PAPEL_CARTON"))
                .andExpect(jsonPath("$.cantidad").value(3))
                .andExpect(jsonPath("$.unidadMedida").value("bolsas"))
                .andExpect(jsonPath("$.fecha").value("2026-09-10"))
                .andExpect(jsonPath("$.descripcion").value(org.hamcrest.Matchers.nullValue()))
                .andReturn().getResponse();
        mvc.perform(get(URL + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().json(respuesta.getContentAsString()));
        mvc.perform(get(URL + "/{id}", otroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoResiduo").value("PLASTICO"));
        mvc.perform(get(URL)).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void eliminarDevuelve204SinCuerpoYElRegistroDejaDeExistir() throws Exception {
        long id = crearRegistro();

        mvc.perform(delete(URL + "/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mvc.perform(get(URL + "/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.mensaje").value("No existe un registro de residuo con id " + id + "."))
                .andExpect(jsonPath("$.trace").doesNotExist());
        mvc.perform(get(URL)).andExpect(content().json("[]"));
    }

    @Test
    void consultarInexistenteDevuelve404ConErrorJson() throws Exception {
        mvc.perform(get(URL + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.mensaje").value("No existe un registro de residuo con id 999999."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void actualizarInexistenteDevuelve404SinCrearUnRegistro() throws Exception {
        mvc.perform(put(URL + "/999999").contentType(APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").isString());
        mvc.perform(get(URL)).andExpect(content().json("[]"));
    }

    @Test
    void eliminarInexistenteDevuelve404ConErrorJson() throws Exception {
        mvc.perform(delete(URL + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.mensaje").isString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"tipoResiduo", "cantidad", "unidadMedida", "fecha"})
    void rechazaCamposObligatoriosAusentesONulosEnPostYPut(String campo) throws Exception {
        var entrada = entradaJson();
        entrada.remove(campo);
        comprobarEntradaInvalida(entrada.toString(), campo);
        entrada.putNull(campo);
        comprobarEntradaInvalida(entrada.toString(), campo);
    }

    @ParameterizedTest
    @MethodSource("valoresInvalidos")
    void rechazaValoresInvalidosEnPostYPut(String campo, String valorJson) throws Exception {
        var entrada = entradaJson();
        entrada.set(campo, objectMapper.readTree(valorJson));
        comprobarEntradaInvalida(entrada.toString(), campo);
    }

    static Stream<Arguments> valoresInvalidos() {
        return Stream.of(
                Arguments.of("cantidad", "0"),
                Arguments.of("cantidad", "-0.01"),
                Arguments.of("unidadMedida", "\"   \""),
                Arguments.of("unidadMedida", "\"\""),
                Arguments.of("descripcion", "\"" + "a".repeat(501) + "\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"tipoResiduo", "fecha", "cantidad"})
    void rechazaTiposYFormatosInvalidosConErrorJson(String campo) throws Exception {
        var entrada = entradaJson();
        entrada.put(campo, switch (campo) {
            case "tipoResiduo" -> "BATERIA";
            case "fecha" -> "2026-02-30";
            default -> "mucho";
        });
        comprobarEntradaInvalida(entrada.toString(), null);
    }

    @Test
    void rechazaTipoResiduoNumericoEnLugarDeNombre() throws Exception {
        var entrada = entradaJson();
        entrada.put("tipoResiduo", 0);
        comprobarEntradaInvalida(entrada.toString(), null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "null", "", "[]"})
    void rechazaCuerpoJsonMalformadoOAusente(String cuerpo) throws Exception {
        comprobarEntradaInvalida(cuerpo, null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "99999999999999999999999999"})
    void rechazaIdNoNumericoOFueraDeRango(String id) throws Exception {
        mvc.perform(get(URL + "/" + id))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.mensaje").isString())
                .andExpect(jsonPath("$.errores.id").isString())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @ParameterizedTest
    @EnumSource(TipoResiduo.class)
    void aceptaTodosLosTiposPermitidosYDescripcionOpcional(TipoResiduo tipo) throws Exception {
        var entrada = entradaJson();
        entrada.put("tipoResiduo", tipo.name());
        entrada.remove("descripcion");
        mvc.perform(post(URL).contentType(APPLICATION_JSON).content(entrada.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoResiduo").value(tipo.name()))
                .andExpect(jsonPath("$.descripcion").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void aceptaDescripcionDe500CaracteresYCantidadPositivaPequena() throws Exception {
        var entrada = entradaJson();
        entrada.put("descripcion", "a".repeat(500));
        entrada.put("cantidad", new java.math.BigDecimal("0.001"));
        mvc.perform(post(URL).contentType(APPLICATION_JSON).content(entrada.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descripcion").value("a".repeat(500)))
                .andExpect(jsonPath("$.cantidad").value(0.001));
    }

    @Test
    void crearIgnoraIdDelCliente() throws Exception {
        long existente = crearRegistro();
        var entrada = entradaJson();
        entrada.put("id", existente);
        var respuesta = mvc.perform(post(URL).contentType(APPLICATION_JSON).content(entrada.toString()))
                .andExpect(status().isCreated()).andReturn().getResponse();
        assertThat(objectMapper.readTree(respuesta.getContentAsString()).get("id").asLong())
                .isNotEqualTo(existente);
        mvc.perform(get(URL)).andExpect(jsonPath("$.length()").value(2));
    }

    private ObjectNode entradaJson() throws Exception {
        return (ObjectNode) objectMapper.readTree(VALIDO);
    }

    private void comprobarEntradaInvalida(String cuerpo, String campo) throws Exception {
        long id = crearRegistro();
        var original = mvc.perform(get(URL + "/{id}", id)).andReturn().getResponse().getContentAsString();
        int cantidadAntes = service.listar().size();
        for (var peticion : java.util.List.of(post(URL), put(URL + "/{id}", id))) {
            var resultado = mvc.perform(peticion.contentType(APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                    .andExpect(jsonPath("$.mensaje").isString())
                    .andExpect(jsonPath("$.trace").doesNotExist())
                    .andExpect(jsonPath("$.exception").doesNotExist());
            if (campo != null) {
                resultado.andExpect(jsonPath("$.errores." + campo).isString());
            }
        }
        assertThat(service.listar()).hasSize(cantidadAntes);
        mvc.perform(get(URL + "/{id}", id)).andExpect(content().json(original));
    }

    private long crearRegistro() throws Exception {
        var respuesta = mvc.perform(post(URL).contentType(APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated()).andReturn().getResponse();
        return objectMapper.readTree(respuesta.getContentAsString()).get("id").asLong();
    }
}
