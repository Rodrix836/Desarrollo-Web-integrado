package pe.reciclo.residuos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.reciclo.residuos.dto.ResiduoSolicitud;
import pe.reciclo.residuos.model.RegistroResiduo;
import pe.reciclo.residuos.model.TipoResiduo;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pe.reciclo.residuos.repository.RegistroResiduoRepository;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:publicacion")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelojPruebaConfig.class)
class PublicacionApiTest {
    private static final String BASE = "/api/residuos";
    private static final String SECRET = UUID.randomUUID() + "-" + UUID.randomUUID();
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired RegistroResiduoRepository repository;
    @Autowired RelojPruebaConfig.RelojPrueba reloj;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry properties) {
        properties.add("reciclo.jwt.secret", () -> SECRET);
    }
    @BeforeEach void preparar() {
        repository.deleteAll();
        reloj.fijar("2026-11-16T17:00:00Z");
    }
    private String bearer(String rol) throws Exception {
        var claims = new JWTClaimsSet.Builder().subject("prueba").claim("rol", rol)
                .expirationTime(Date.from(Instant.now().plusSeconds(300))).build();
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRET));
        return "Bearer " + jwt.serialize();
    }
    private ObjectNode registro() {
        return mapper.createObjectNode().put("tipoResiduo", "PLASTICO").put("cantidad", 2.75)
                .put("unidadMedida", "kg").put("descripcion", "Botellas")
                .put("empresa", "Acme").put("intercambioPor", "PAPEL_CARTON")
                .put("disponible", true).put("fechaPublicacion", "2026-11-17");
    }
    private long crear(ObjectNode body) throws Exception {
        var response = mvc.perform(post(BASE).header("Authorization", bearer("ADMIN"))
                        .contentType("application/json").content(body.toString()))
                .andExpect(status().isCreated()).andReturn().getResponse();
        return mapper.readTree(response.getContentAsString()).get("id").asLong();
    }

    @Test void futuroSoloSeAnunciaYSePublicaAlComenzarElDiaEnLima() throws Exception {
        long id = crear(registro());
        mvc.perform(get(BASE + "/" + id).header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estadoPublicacion").value("PROGRAMADO"));
        mvc.perform(get(BASE).header("Authorization", bearer("USER")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get(BASE + "/" + id).header("Authorization", bearer("USER")))
                .andExpect(status().isNotFound());
        mvc.perform(get(BASE + "/avisos").header("Authorization", bearer("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].fechaPublicacion").value("2026-11-17"));
        reloj.fijar("2026-11-17T04:59:59Z");
        mvc.perform(get(BASE + "/" + id).header("Authorization", bearer("USER")))
                .andExpect(status().isNotFound());
        reloj.fijar("2026-11-17T05:00:00Z");
        mvc.perform(get(BASE + "/" + id).header("Authorization", bearer("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estadoPublicacion").value("DISPONIBLE"));
    }

    @Test void desmarcadoSeGuardaSinIntercambioYQuedaOculto() throws Exception {
        var body = registro().put("disponible", false);
        long id = crear(body);
        mvc.perform(get(BASE + "/" + id).header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estadoPublicacion").value("NO_PUBLICADO"))
                .andExpect(jsonPath("$.intercambioPor").value(""))
                .andExpect(jsonPath("$.fechaPublicacion").isEmpty());
        mvc.perform(get(BASE).header("Authorization", bearer("USER")))
                .andExpect(content().json("[]"));
        mvc.perform(get(BASE + "/" + id).header("Authorization", bearer("USER")))
                .andExpect(status().isNotFound());
        mvc.perform(get(BASE + "/avisos").header("Authorization", bearer("USER")))
                .andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fechaPublicacion", "intercambioPor", "empresa"})
    void marcadoExigeCamposDeIntercambio(String campo) throws Exception {
        var body = registro();
        body.remove(campo);
        mvc.perform(post(BASE).header("Authorization", bearer("ADMIN")).contentType("application/json")
                        .content(body.toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores." + campo).isNotEmpty());
        assertThat(repository.count()).isZero();
    }

    @Test void rechazaFechaNuevaPasadaYPreferenciaFueraDelCatalogo() throws Exception {
        var body = registro().put("fechaPublicacion", "2026-11-15").put("intercambioPor", "DINERO");
        mvc.perform(post(BASE).header("Authorization", bearer("ADMIN")).contentType("application/json")
                        .content(body.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fechaPublicacion").isNotEmpty())
                .andExpect(jsonPath("$.errores.intercambioPor").isNotEmpty());
    }

    @Test void unaFechaPublicadaPasadaSeConservaPeroNoSeReprogramaAlPasado() throws Exception {
        var body = registro().put("fechaPublicacion", "2026-11-16");
        long id = crear(body);
        reloj.fijar("2026-11-20T17:00:00Z");
        body.put("cantidad", 3.125);
        mvc.perform(put(BASE + "/" + id).header("Authorization", bearer("ADMIN"))
                        .contentType("application/json").content(body.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cantidad").value(3.125))
                .andExpect(jsonPath("$.fecha").value("2026-11-16"))
                .andExpect(jsonPath("$.fechaPublicacion").value("2026-11-16"));
        body.put("fechaPublicacion", "2026-11-19");
        mvc.perform(put(BASE + "/" + id).header("Authorization", bearer("ADMIN"))
                        .contentType("application/json").content(body.toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get(BASE + "/avisos").header("Authorization", bearer("USER")))
                .andExpect(content().json("[]"));
    }

    @Test void reprogramarCancelarYEliminarActualizanElMismoAviso() throws Exception {
        var body = registro();
        long id = crear(body);
        body.put("fechaPublicacion", "2026-11-19");
        mvc.perform(put(BASE + "/" + id).header("Authorization", bearer("ADMIN"))
                        .contentType("application/json").content(body.toString())).andExpect(status().isOk());
        mvc.perform(get(BASE + "/avisos").header("Authorization", bearer("USER")))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].fechaPublicacion").value("2026-11-19"))
                .andExpect(jsonPath("$[0].cantidad").doesNotExist())
                .andExpect(jsonPath("$[0].descripcion").doesNotExist());
        body.put("disponible", false).put("empresa", "");
        mvc.perform(put(BASE + "/" + id).header("Authorization", bearer("ADMIN"))
                        .contentType("application/json").content(body.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.empresa").value("Acme"))
                .andExpect(jsonPath("$.intercambioPor").value(""))
                .andExpect(jsonPath("$.fechaPublicacion").isEmpty());
        mvc.perform(get(BASE + "/avisos").header("Authorization", bearer("USER"))).andExpect(content().json("[]"));
        body.put("disponible", true).put("empresa", "Acme").put("fechaPublicacion", "2026-11-18");
        mvc.perform(put(BASE + "/" + id).header("Authorization", bearer("ADMIN"))
                        .contentType("application/json").content(body.toString())).andExpect(status().isOk());
        mvc.perform(delete(BASE + "/" + id).header("Authorization", bearer("ADMIN"))).andExpect(status().isNoContent());
        mvc.perform(get(BASE + "/avisos").header("Authorization", bearer("USER"))).andExpect(content().json("[]"));
    }

    @Test void conservaLegadosSinProgramacionYSuUnidadAlEditar() throws Exception {
        var anterior = new RegistroResiduo(new ResiduoSolicitud(TipoResiduo.METAL, new BigDecimal("1.123456"),
                "toneladas", LocalDate.of(2020, 1, 2), "Legado", "Anterior", "Cobre limpio", true, null));
        long id = repository.save(anterior).aRespuesta(LocalDate.of(2026, 11, 16)).id();
        mvc.perform(get(BASE + "/" + id).header("Authorization", bearer("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estadoPublicacion").value("DISPONIBLE"))
                .andExpect(jsonPath("$.unidadMedida").value("toneladas"))
                .andExpect(jsonPath("$.intercambioPor").value("Cobre limpio"));
        var body = registro().put("unidadMedida", "toneladas").put("cantidad", 1.123456)
                .put("fechaPublicacion", "2026-11-16").put("intercambioPor", "Cobre limpio");
        mvc.perform(put(BASE + "/" + id).header("Authorization", bearer("ADMIN"))
                        .contentType("application/json").content(body.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unidadMedida").value("toneladas"))
                .andExpect(jsonPath("$.fecha").value("2020-01-02"));
    }

    @Test void nuevasAltasExigenKgYLaProgramacionSePersiste() throws Exception {
        mvc.perform(post(BASE).header("Authorization", bearer("ADMIN")).contentType("application/json")
                        .content(registro().put("unidadMedida", "toneladas").toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.unidadMedida").isNotEmpty());
        long id = crear(registro());
        assertThat(jdbc.queryForObject("select fecha_publicacion from registro_residuo where id = ?",
                LocalDate.class, id)).isEqualTo(LocalDate.of(2026, 11, 17));
    }

    @Test void filtrosDelAdminNoAmplianLaVisibilidadDeUser() throws Exception {
        crear(registro());
        mvc.perform(get(BASE).param("estado", "PROGRAMADO").param("preferencia", "PAPEL_CARTON")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(BASE).param("estado", "PROGRAMADO").param("preferencia", "PAPEL_CARTON")
                        .header("Authorization", bearer("USER")))
                .andExpect(content().json("[]"));
        mvc.perform(get(BASE + "/avisos")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/avisos").header("Authorization", bearer("EMPRESA"))).andExpect(status().isForbidden());
    }
}
