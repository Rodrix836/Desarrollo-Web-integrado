package pe.reciclo.consultas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ConsultaServiceTest {
    MockRestServiceServer server;
    ConsultaService service;
    @BeforeEach void preparar() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new ConsultaService(builder, "http://residuos");
    }
    @Test void filtraDatosDelServicioYPropagaBearer() {
        server.expect(requestTo("http://residuos/api/residuos"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer ejemplo"))
                .andRespond(withSuccess("""
                [{"id":1,"tipoResiduo":"PLASTICO","cantidad":2.75,"empresa":"Acme","disponible":true},
                 {"id":2,"tipoResiduo":"METAL","cantidad":1,"empresa":"Otra","disponible":false}]
                """, MediaType.APPLICATION_JSON));
        var result = service.consultar("Bearer ejemplo", "ACM", "PLASTICO", true, new BigDecimal("2.5"));
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(1);
        assertThat(result.getFirst().cantidad()).isEqualByComparingTo("2.75");
        server.verify();
    }
    @Test void sinTokenNoConsultaUpstream() {
        assertThatThrownBy(() -> service.consultar(null, null, null, null, null))
                .isInstanceOf(ResponseStatusException.class);
        server.verify();
    }
    @Test void tokenRechazadoPorResiduosSePropaga() {
        server.expect(requestTo("http://residuos/api/residuos")).andRespond(withUnauthorizedRequest());
        assertThatThrownBy(() -> service.consultar("Bearer malo", null, null, null, null))
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
        server.verify();
    }
}
