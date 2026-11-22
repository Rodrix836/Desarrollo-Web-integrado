package pe.reciclo.consultas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Combinaciones de filtros sobre un mismo inventario. */
class ConsultaFiltrosTest {

    private static final String INVENTARIO = """
            [
             {"id":1,"tipoResiduo":"PLASTICO","cantidad":2.75,"empresa":"Acme SAC","disponible":true,
              "estadoPublicacion":"DISPONIBLE","intercambioPor":"PAPEL_CARTON"},
             {"id":2,"tipoResiduo":"PLASTICO","cantidad":10,"empresa":"Beta","disponible":true,
              "estadoPublicacion":"PROGRAMADO","intercambioPor":"VIDRIO"},
             {"id":3,"tipoResiduo":"VIDRIO","cantidad":5,"empresa":"acme sac","disponible":false,
              "estadoPublicacion":"NO_PUBLICADO","intercambioPor":""},
             {"id":4,"tipoResiduo":"METAL","cantidad":2.5,"empresa":"Gamma","disponible":true,
              "estadoPublicacion":"DISPONIBLE","intercambioPor":"PAPEL_CARTON"}
            ]
            """;

    MockRestServiceServer server;
    ConsultaService service;

    @BeforeEach
    void preparar() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        server.expect(manyTimes(), requestTo("http://residuos/api/residuos"))
                .andRespond(withSuccess(INVENTARIO, MediaType.APPLICATION_JSON));
        service = new ConsultaService(builder, "http://residuos");
    }

    static Stream<Arguments> combinaciones() {
        return Stream.of(
                Arguments.of("sin filtros", null, null, null, null, null, null, List.of(1L, 2L, 3L, 4L)),
                Arguments.of("filtros en blanco se ignoran", "  ", "", null, null, "", "", List.of(1L, 2L, 3L, 4L)),
                Arguments.of("empresa sin distinguir mayúsculas", "ACME", null, null, null, null, null, List.of(1L, 3L)),
                Arguments.of("empresa con espacios", "  beta ", null, null, null, null, null, List.of(2L)),
                Arguments.of("tipo", null, "PLASTICO", null, null, null, null, List.of(1L, 2L)),
                Arguments.of("disponible=false", null, null, false, null, null, null, List.of(3L)),
                Arguments.of("cantidad mínima inclusiva", null, null, null, "2.75", null, null, List.of(1L, 2L, 3L)),
                Arguments.of("estado", null, null, null, null, "DISPONIBLE", null, List.of(1L, 4L)),
                Arguments.of("preferencia", null, null, null, null, null, "PAPEL_CARTON", List.of(1L, 4L)),
                Arguments.of("tipo + disponible", null, "PLASTICO", true, null, null, null, List.of(1L, 2L)),
                Arguments.of("empresa + tipo", "acme", "VIDRIO", null, null, null, null, List.of(3L)),
                Arguments.of("estado + preferencia + mínimo", null, null, null, "2.6", "DISPONIBLE", "PAPEL_CARTON", List.of(1L)),
                Arguments.of("combinación sin resultados", "Gamma", "PLASTICO", null, null, null, null, List.of()),
                Arguments.of("todos los filtros", "acme", "PLASTICO", true, "1", "DISPONIBLE", "PAPEL_CARTON", List.of(1L))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("combinaciones")
    void aplicaLosFiltrosCombinados(String caso, String empresa, String tipo, Boolean disponible, String minimo,
                                    String estado, String preferencia, List<Long> esperados) {
        var resultado = service.consultar("Bearer t", empresa, tipo, disponible,
                minimo == null ? null : new BigDecimal(minimo), estado, preferencia);
        assertThat(resultado).extracting(ConsultaService.Residuo::id).containsExactlyElementsOf(esperados);
    }

    @Test
    void empresasDelControladorSonUnicasYOrdenadas() {
        var controller = new ConsultaController(service);
        assertThat(controller.empresas("Bearer t")).containsExactly("Acme SAC", "Beta", "Gamma", "acme sac");
    }

    @Test
    void ignoraCamposNuevosQueAgregueResiduos() {
        var builder = RestClient.builder();
        var otro = MockRestServiceServer.bindTo(builder).build();
        otro.expect(requestTo("http://residuos/api/residuos")).andRespond(withSuccess("""
                [{"id":9,"tipoResiduo":"METAL","cantidad":1,"empresa":"X","disponible":true,
                  "estadoPublicacion":"DISPONIBLE","intercambioPor":"VIDRIO","propietario":"x","creadoEn":"2026-11-16T10:00:00Z"}]
                """, MediaType.APPLICATION_JSON));
        var resultado = new ConsultaService(builder, "http://residuos").consultar("Bearer t", null, null, null, null, null, null);
        assertThat(resultado).extracting(ConsultaService.Residuo::id).containsExactly(9L);
    }
}
