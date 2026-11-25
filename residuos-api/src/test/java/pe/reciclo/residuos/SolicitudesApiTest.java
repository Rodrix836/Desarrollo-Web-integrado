package pe.reciclo.residuos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pe.reciclo.residuos.repository.RegistroResiduoRepository;
import pe.reciclo.residuos.repository.SolicitudIntercambioRepository;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:solicitudes")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelojPruebaConfig.class)
class SolicitudesApiTest {
    private static final String SECRET = UUID.randomUUID() + "-" + UUID.randomUUID();
    private static final String BASE = "/api/solicitudes";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired RegistroResiduoRepository residuos;
    @Autowired SolicitudIntercambioRepository solicitudes;
    @Autowired RelojPruebaConfig.RelojPrueba reloj;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry propiedades) {
        propiedades.add("reciclo.jwt.secret", () -> SECRET);
    }
    @BeforeEach void preparar() {
        solicitudes.deleteAll();
        residuos.deleteAll();
        reloj.fijar("2026-11-16T17:00:00Z");
    }
    private String bearer(String usuario, String rol) throws Exception {
        var claims = new JWTClaimsSet.Builder().subject(usuario).claim("rol", rol)
                .expirationTime(Date.from(Instant.now().plusSeconds(300))).build();
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRET));
        return "Bearer " + jwt.serialize();
    }
    private ObjectNode registro() {
        return mapper.createObjectNode().put("tipoResiduo", "PLASTICO").put("cantidad", 2.75)
                .put("unidadMedida", "kg").put("descripcion", "Botellas")
                .put("empresa", "Acme").put("intercambioPor", "PAPEL_CARTON")
                .put("disponible", true).put("fechaPublicacion", "2026-11-16");
    }
    private long crear(ObjectNode datos) throws Exception {
        var resultado = mvc.perform(post("/api/residuos").header("Authorization", bearer("administrador", "ADMIN"))
                .contentType("application/json").content(datos.toString()))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }
    private long solicitar(long residuoId, String usuario) throws Exception {
        var resultado = mvc.perform(post(BASE).header("Authorization", bearer(usuario, "USER"))
                .contentType("application/json").content(mapper.createObjectNode().put("residuoId", residuoId)
                        .put("ofrece", "METAL").put("cantidadOfrecida", 1.5).put("mensaje", "Ofrezco latas").toString()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.solicitante").value(usuario))
                .andExpect(jsonPath("$.estado").value("PENDIENTE")).andReturn();
        return mapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }
    @Test void unaPropuestaNuevaNoPuedeOfrecerOtros() throws Exception {
        long material = crear(registro());
        mvc.perform(post(BASE).header("Authorization", bearer("ana", "USER"))
                        .contentType("application/json").content(mapper.createObjectNode()
                                .put("residuoId", material).put("ofrece", "OTROS").toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.ofrece").isNotEmpty());
        assertThat(solicitudes.count()).isZero();
    }

    @Test void aceptarRetiraMaterialYRechazaLasOtrasSolicitudes() throws Exception {
        long material = crear(registro());
        long primera = solicitar(material, "ana");
        long segunda = solicitar(material, "luis");
        mvc.perform(put(BASE + "/" + primera + "/decision").header("Authorization", bearer("administrador", "ADMIN"))
                .contentType("application/json").content("{\"estado\":\"ACEPTADA\",\"respuesta\":\"Coordinemos\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ACEPTADA"))
                .andExpect(jsonPath("$.respondidoPor").value("administrador"));
        mvc.perform(get(BASE + "/mias").header("Authorization", bearer("luis", "USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(segunda))
                .andExpect(jsonPath("$[0].estado").value("RECHAZADA"));
        mvc.perform(get(BASE + "/mias").header("Authorization", bearer("ana", "USER")))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].estado").value("ACEPTADA"));
        mvc.perform(get("/api/residuos").header("Authorization", bearer("ana", "USER")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/residuos/" + material).header("Authorization", bearer("ana", "USER")))
                .andExpect(status().isNotFound());
    }
    @Test void permisosYPrivacidadDeSolicitudes() throws Exception {
        long material = crear(registro());
        long solicitud = solicitar(material, "ana");
        mvc.perform(post(BASE).header("Authorization", bearer("administrador", "ADMIN"))
                .contentType("application/json").content("{\"residuoId\":" + material + "}"))
                .andExpect(status().isForbidden());
        mvc.perform(get(BASE).header("Authorization", bearer("ana", "USER"))).andExpect(status().isForbidden());
        mvc.perform(put(BASE + "/" + solicitud + "/decision").header("Authorization", bearer("ana", "USER"))
                .contentType("application/json").content("{\"estado\":\"ACEPTADA\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(BASE + "/" + solicitud + "/cancelar").header("Authorization", bearer("luis", "USER")))
                .andExpect(status().isNotFound());
        mvc.perform(put(BASE + "/" + solicitud + "/cancelar").header("Authorization", bearer("ana", "USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADA"));
        mvc.perform(put(BASE + "/" + solicitud + "/cancelar").header("Authorization", bearer("ana", "USER")))
                .andExpect(status().isConflict());
    }
    @Test void duplicadosYDecisionesRepetidasSonConflictos() throws Exception {
        long material = crear(registro());
        long solicitud = solicitar(material, "ana");
        mvc.perform(post(BASE).header("Authorization", bearer("ana", "USER"))
                .contentType("application/json").content("{\"residuoId\":" + material + "}"))
                .andExpect(status().isConflict());
        String ruta = BASE + "/" + solicitud + "/decision";
        mvc.perform(put(ruta).header("Authorization", bearer("administrador", "ADMIN"))
                .contentType("application/json").content("{\"estado\":\"PENDIENTE\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put(ruta).header("Authorization", bearer("administrador", "ADMIN"))
                .contentType("application/json").content("{\"estado\":\"RECHAZADA\"}"))
                .andExpect(status().isOk());
        mvc.perform(put(ruta).header("Authorization", bearer("administrador", "ADMIN"))
                .contentType("application/json").content("{\"estado\":\"ACEPTADA\"}"))
                .andExpect(status().isConflict());
    }
    @Test void validaMaterialYDatosObligatorios() throws Exception {
        long programado = crear(registro().put("fechaPublicacion", "2026-11-17"));
        for (long id : new long[]{ programado, 999999 }) {
            mvc.perform(post(BASE).header("Authorization", bearer("ana", "USER"))
                    .contentType("application/json").content("{\"residuoId\":" + id + "}"))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(post(BASE).header("Authorization", bearer("ana", "USER"))
                .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.residuoId").isNotEmpty());
    }
    @Test void eliminarMaterialCancelaSusPendientes() throws Exception {
        long material = crear(registro());
        solicitar(material, "ana");
        solicitar(material, "luis");
        mvc.perform(delete("/api/residuos/" + material).header("Authorization", bearer("administrador", "ADMIN")))
                .andExpect(status().isNoContent());
        mvc.perform(get(BASE).header("Authorization", bearer("administrador", "ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].estado").value("CANCELADA"))
                .andExpect(jsonPath("$[1].estado").value("CANCELADA"));
    }
    @Test void paginacionRespetaMetadatosLimitesYVisibilidad() throws Exception {
        crear(registro());
        crear(registro());
        crear(registro().put("fechaPublicacion", "2026-11-17"));
        mvc.perform(get("/api/residuos/pagina").param("pagina", "1").param("tamanio", "2")
                .header("Authorization", bearer("administrador", "ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.pagina").value(1)).andExpect(jsonPath("$.tamanio").value(2))
                .andExpect(jsonPath("$.totalElementos").value(3)).andExpect(jsonPath("$.totalPaginas").value(2));
        mvc.perform(get("/api/residuos/pagina").header("Authorization", bearer("ana", "USER")))
                .andExpect(jsonPath("$.totalElementos").value(2));
        mvc.perform(get("/api/residuos/pagina").param("tamanio", "500")
                .header("Authorization", bearer("administrador", "ADMIN"))).andExpect(status().isBadRequest());
    }
    private JsonNode estadisticas(String rol) throws Exception {
        var resultado = mvc.perform(get("/api/residuos/estadisticas").header("Authorization", bearer("prueba", rol)))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(resultado.getResponse().getContentAsString());
    }
    @Test void estadisticasAgrupanKilosPorTipoEmpresaYMesSegunVisibilidad() throws Exception {
        long material = crear(registro());
        crear(registro().put("tipoResiduo", "METAL").put("cantidad", 1.25));
        crear(registro().put("empresa", "Otra").put("cantidad", 6).put("fechaPublicacion", "2026-11-17"));
        solicitar(material, "ana");
        var admin = estadisticas("ADMIN");
        assertThat(admin.path("registros").asInt()).isEqualTo(3);
        assertThat(admin.path("disponibles").asInt()).isEqualTo(2);
        assertThat(admin.path("totalKg").asDouble()).isEqualTo(10);
        assertThat(admin.path("solicitudesPendientes").asInt()).isEqualTo(1);
        assertThat(admin.path("porTipo").get(0).path("kg").asDouble()).isEqualTo(8.75);
        assertThat(admin.path("porEmpresa").get(0).path("etiqueta").asText()).isEqualTo("Otra");
        assertThat(admin.path("porEmpresa").get(1).path("kg").asDouble()).isEqualTo(4);
        assertThat(admin.path("porMes").get(0).path("kg").asDouble()).isEqualTo(10);
        var user = estadisticas("USER");
        assertThat(user.path("registros").asInt()).isEqualTo(2);
        assertThat(user.path("totalKg").asDouble()).isEqualTo(4);
        assertThat(user.path("solicitudesPendientes").asInt()).isZero();
        assertThat(user.path("porTipo").get(0).path("kg").asDouble()).isEqualTo(2.75);
        assertThat(user.path("porEmpresa").size()).isEqualTo(1);
        assertThat(user.path("porEmpresa").get(0).path("kg").asDouble()).isEqualTo(4);
    }
    @Test void auditoriaUsaElSubDeQuienCreaYEdita() throws Exception {
        var datos = registro();
        long id = crear(datos);
        mvc.perform(get("/api/residuos/" + id).header("Authorization", bearer("administrador", "ADMIN")))
                .andExpect(jsonPath("$.creadoPor").value("administrador"))
                .andExpect(jsonPath("$.creadoEn").isNotEmpty());
        mvc.perform(put("/api/residuos/" + id).header("Authorization", bearer("editora", "ADMIN"))
                .contentType("application/json").content(datos.put("cantidad", 4).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.creadoPor").value("administrador"))
                .andExpect(jsonPath("$.actualizadoPor").value("editora"))
                .andExpect(jsonPath("$.actualizadoEn").isNotEmpty());
    }
}
