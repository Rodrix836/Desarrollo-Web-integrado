package pe.reciclo.residuos;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pe.reciclo.residuos.repository.RegistroResiduoRepository;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistroResiduoApiTest {

    // Clave efímera, exclusiva de este contexto de pruebas.
    private static final String SECRETO = UUID.randomUUID() + "-" + UUID.randomUUID();
    private static final String BASE = "/api/residuos";

    private static final String REGISTRO = """
            {"tipoResiduo":"PLASTICO","cantidad":2.123456789123456789,"unidadMedida":"kg",
             "fecha":"2026-09-09","descripcion":"Botellas","empresa":"Acme",
             "intercambioPor":"Papel","disponible":true,"id":999}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RegistroResiduoRepository repository;

    @BeforeEach
    void limpiarBase() {
        repository.deleteAll();
    }

    @DynamicPropertySource
    static void configurarClave(DynamicPropertyRegistry properties) {
        properties.add("reciclo.jwt.secret", () -> SECRETO);
    }

    @Test
    void userPuedeListarUnaBaseVacia() throws Exception {
        mvc.perform(get(BASE).header("Authorization", "Bearer " + token("USER")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void adminCompletaElCrudYUserPuedeLeer() throws Exception {
        var creado = mvc.perform(json(post(BASE), REGISTRO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.tipoResiduo").value("PLASTICO"))
                .andExpect(jsonPath("$.disponible").value(true))
                .andReturn().getResponse();
        String ubicacion = creado.getHeader(HttpHeaders.LOCATION);
        assertThat(ubicacion).startsWith("http://localhost/api/residuos/");
        long id = Long.parseLong(ubicacion.substring(ubicacion.lastIndexOf('/') + 1));
        assertThat(id).isPositive().isNotEqualTo(999L);
        String detalle = BASE + "/" + id;

        mvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, "Bearer " + token("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get(detalle).header(HttpHeaders.AUTHORIZATION, "Bearer " + token("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.empresa").value("Acme"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("2.123456789123456789")));

        String cambios = REGISTRO.replace("PLASTICO", "METAL").replace("Botellas", "Latas");
        mvc.perform(json(put(detalle), cambios))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.tipoResiduo").value("METAL"));
        mvc.perform(get(detalle).header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.descripcion").value("Latas"));
        assertThat(repository.count()).isEqualTo(1);

        mvc.perform(delete(detalle).header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ADMIN")))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get(detalle).header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ADMIN")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void consultarUnRegistroInexistenteDevuelve404() throws Exception {
        mvc.perform(get(BASE + "/999").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("USER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe un registro de residuo con id 999."));
    }

    @Test
    void actualizarUnRegistroInexistenteDevuelve404SinInsertar() throws Exception {
        mvc.perform(json(put(BASE + "/999"), REGISTRO)).andExpect(status().isNotFound());
        assertThat(repository.count()).isZero();
    }

    @Test
    void eliminarUnRegistroInexistenteDevuelve404() throws Exception {
        mvc.perform(delete(BASE + "/999").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-numero", "0", "-1", "9223372036854775808"})
    void rechazaIdentificadoresInvalidos(String id) throws Exception {
        mvc.perform(get(BASE + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token("USER")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.id").isNotEmpty());
    }

    @Test
    void postInvalidoDetallaLosCamposYNoInserta() throws Exception {
        String invalido = REGISTRO.replace("2.123456789123456789", "0")
                .replace("Acme", "  ").replace("kg", " ");
        mvc.perform(json(post(BASE), invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cantidad").value("La cantidad debe ser mayor que cero."))
                .andExpect(jsonPath("$.errores.empresa").isNotEmpty())
                .andExpect(jsonPath("$.errores.unidadMedida").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400));
        assertThat(repository.count()).isZero();
    }

    @Test
    void putInvalidoDevuelveElErrorDelCampo() throws Exception {
        mvc.perform(json(put(BASE + "/999"), REGISTRO.replace("2.123456789123456789", "-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cantidad").value("La cantidad debe ser mayor que cero."));
    }

    @Test
    void rechazaCamposObligatoriosAusentes() throws Exception {
        mvc.perform(json(post(BASE), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cantidad").isNotEmpty())
                .andExpect(jsonPath("$.errores.tipoResiduo").isNotEmpty())
                .andExpect(jsonPath("$.errores.unidadMedida").isNotEmpty())
                .andExpect(jsonPath("$.errores.fecha").isNotEmpty());
    }

    @Test
    void rechazaTextosDemasiadoLargos() throws Exception {
        String invalido = REGISTRO.replace("Botellas", "d".repeat(501))
                .replace("Acme", "e".repeat(101)).replace("Papel", "p".repeat(301));
        mvc.perform(json(post(BASE), invalido)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.descripcion").isNotEmpty())
                .andExpect(jsonPath("$.errores.empresa").isNotEmpty())
                .andExpect(jsonPath("$.errores.intercambioPor").isNotEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{\"tipoResiduo\":0}", "{\"tipoResiduo\":\"MADERA\"}",
            "{\"fecha\":\"2026-02-30\"}", "{} {}"})
    void rechazaJsonMalformadoOIncompatible(String cuerpo) throws Exception {
        mvc.perform(json(post(BASE), cuerpo)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.startsWith("JSON inválido.")));
    }

    @Test
    void rechazaSolicitudSinToken() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void rechazaTokenAlterado() throws Exception {
        String firmado = token("ADMIN");
        int inicioFirma = firmado.lastIndexOf('.') + 1;
        char reemplazo = firmado.charAt(inicioFirma) == 'A' ? 'B' : 'A';
        String alterado = firmado.substring(0, inicioFirma) + reemplazo + firmado.substring(inicioFirma + 1);
        verificarTokenRechazado(alterado);
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() throws Exception {
        verificarTokenRechazado(firmar(claims("ADMIN").build(), JWSAlgorithm.HS256, SECRETO + "otra"));
    }

    @Test
    void rechazaTokenCaducado() throws Exception {
        var expirado = claims("ADMIN").expirationTime(Date.from(Instant.now().minusSeconds(1))).build();
        verificarTokenRechazado(firmar(expirado, JWSAlgorithm.HS256, SECRETO));
    }

    @Test
    void rechazaAlgoritmoDistinto() throws Exception {
        verificarTokenRechazado(firmar(claims("ADMIN").build(), JWSAlgorithm.HS512, SECRETO));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sub", "exp", "rol"})
    void rechazaTokenSinClaimObligatorio(String claim) throws Exception {
        var incompleto = claims("ADMIN").claim(claim, null).build();
        verificarTokenRechazado(firmar(incompleto, JWSAlgorithm.HS256, SECRETO));
    }

    @Test
    void userNoPuedeModificarRegistros() throws Exception {
        String user = "Bearer " + token("USER");
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(REGISTRO)
                        .header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(put(BASE + "/1").header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isForbidden());
        mvc.perform(delete(BASE + "/1").header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"EMPRESA", "USER ADMIN"})
    void rechazaRolesSinPermiso(String rol) throws Exception {
        mvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, "Bearer " + token(rol)))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:4200", "http://127.0.0.1:4200"})
    void permitePreflightDeAngular(String origen) throws Exception {
        mvc.perform(options(BASE).header(HttpHeaders.ORIGIN, origen)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origen));
    }

    @Test
    void rechazaPreflightDeOtroOrigen() throws Exception {
        mvc.perform(options(BASE).header(HttpHeaders.ORIGIN, "http://localhost:4300")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private void verificarTokenRechazado(String token) throws Exception {
        mvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder peticion, String cuerpo)
            throws JOSEException {
        return peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    private static JWTClaimsSet.Builder claims(String rol) {
        return new JWTClaimsSet.Builder().subject("cuenta-prueba").claim("rol", rol)
                .expirationTime(Date.from(Instant.now().plusSeconds(300)));
    }

    private static String token(String rol) throws JOSEException {
        return firmar(claims(rol).build(), JWSAlgorithm.HS256, SECRETO);
    }

    private static String firmar(JWTClaimsSet claims, JWSAlgorithm algoritmo, String secreto)
            throws JOSEException {
        SignedJWT jwt = new SignedJWT(new JWSHeader(algoritmo), claims);
        jwt.sign(new MACSigner(secreto));
        return jwt.serialize();
    }
}
