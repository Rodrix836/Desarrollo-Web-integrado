package pe.reciclo.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.reciclo.model.RegistroResiduo;
import pe.reciclo.model.TipoResiduo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistroResiduoRepositoryTest {

    @TempDir
    Path directorio;

    @Test
    void primerInicioCopiaTresEjemplosAlArchivoExterno() throws IOException {
        Path archivo = directorio.resolve("data/residuos.json");

        var repository = abrir(archivo);

        assertThat(repository.listar()).extracting(RegistroResiduo::tipoResiduo)
                .containsExactly(TipoResiduo.PLASTICO, TipoResiduo.PAPEL_CARTON, TipoResiduo.VIDRIO);
        assertThat(repository.listar()).extracting(RegistroResiduo::cantidad)
                .containsExactly(new BigDecimal("2"), new BigDecimal("5"), new BigDecimal("3"));
        assertThat(Files.isRegularFile(archivo)).isTrue();
        assertThat(abrir(archivo).listar()).containsExactlyElementsOf(repository.listar());
    }

    @Test
    void crearActualizarYEliminarPersistenTrasVolverACargar() throws IOException {
        Path archivo = archivoVacio();
        var repository = abrir(archivo);
        var primero = repository.crear(entrada("2"));
        var segundo = repository.crear(entrada("3"));

        var reiniciado = abrir(archivo);
        assertThat(reiniciado.listar()).containsExactly(primero, segundo);
        assertThat(reiniciado.crear(entrada("4")).id()).isEqualTo(3L);
        var actualizado = reiniciado.actualizar(primero.id(), entrada("5")).orElseThrow();
        assertThat(reiniciado.eliminar(segundo.id())).isTrue();

        var ultimaCarga = abrir(archivo);
        assertThat(ultimaCarga.buscar(primero.id())).contains(actualizado);
        assertThat(ultimaCarga.buscar(segundo.id())).isEmpty();
        assertThat(ultimaCarga.listar()).hasSize(2);
        var json = new ObjectMapper().readTree(Files.readString(archivo));
        assertThat(json.isArray()).isTrue();
        assertThat(json.get(0).get("fecha").asText()).isEqualTo("2026-09-10");
        assertThat(json.get(0).get("empresa").asText()).isEqualTo("Papelera Andina");
        assertThat(json.get(0).get("intercambioPor").asText()).isEqualTo("Vidrio limpio");
        assertThat(json.get(0).get("disponible").asBoolean()).isTrue();
    }

    @Test
    void archivoAnteriorSinEmpresaSeConservaSinPublicarlo() throws IOException {
        Path archivo = directorio.resolve("residuos.json");
        String contenido = """
                [{"id":7,"tipoResiduo":"PLASTICO","cantidad":2,
                  "unidadMedida":"kg","fecha":"2026-09-10","descripcion":"Registro anterior"}]
                """;
        Files.writeString(archivo, contenido);

        var registro = abrir(archivo).buscar(7L).orElseThrow();

        assertThat(registro.empresa()).isEmpty();
        assertThat(registro.intercambioPor()).isEmpty();
        assertThat(registro.disponible()).isFalse();
        assertThat(registro.descripcion()).isEqualTo("Registro anterior");
        assertThat(Files.readString(archivo)).isEqualTo(contenido);
    }

    @Test
    void eliminarTodosNoReinstalaLaSemillaAlReiniciar() throws IOException {
        Path archivo = archivoVacio();
        var repository = abrir(archivo);
        var creado = repository.crear(entrada("2"));

        repository.eliminar(creado.id());

        assertThat(abrir(archivo).listar()).isEmpty();
        assertThat(new ObjectMapper().readTree(Files.readString(archivo)).isEmpty()).isTrue();
    }

    @Test
    void jsonInvalidoImpideIniciarYSeConservaSinSobrescribir() throws IOException {
        Path archivo = directorio.resolve("residuos.json");
        String contenido = "{ json incompleto";
        Files.writeString(archivo, contenido);

        assertThatThrownBy(() -> abrir(archivo)).isInstanceOf(UncheckedIOException.class);

        assertThat(Files.readString(archivo)).isEqualTo(contenido);
    }

    @Test
    void idsNulosODuplicadosImpidenDescartarRegistrosSilenciosamente() throws IOException {
        Path archivo = directorio.resolve("residuos.json");
        for (String contenido : new String[]{"[{\"id\":null}]", "[{\"id\":1},{\"id\":1}]"}) {
            Files.writeString(archivo, contenido);

            assertThatThrownBy(() -> abrir(archivo)).isInstanceOf(UncheckedIOException.class);
            assertThat(Files.readString(archivo)).isEqualTo(contenido);
        }
    }

    @Test
    void camposInvalidosEnElArchivoImpidenIniciarSinSobrescribirlo() throws IOException {
        Path archivo = directorio.resolve("residuos.json");
        for (String contenido : new String[]{"[{\"id\":1}]", """
                [{"id":1,"tipoResiduo":"PLASTICO","cantidad":-2,
                  "unidadMedida":"kg","fecha":"2026-09-10"}]
                """}) {
            Files.writeString(archivo, contenido);

            assertThatThrownBy(() -> abrir(archivo)).isInstanceOf(UncheckedIOException.class);
            assertThat(Files.readString(archivo)).isEqualTo(contenido);
        }
    }

    @Test
    void falloAlGuardarNoPublicaCambiosEnMemoriaNiConsumeId() throws IOException {
        Path archivo = archivoVacio();
        var repository = abrir(archivo);
        var primero = repository.crear(entrada("2"));
        var segundo = repository.crear(entrada("3"));
        // Un directorio no vacío en lugar del archivo simula un destino no escribible.
        Files.delete(archivo);
        Files.createDirectory(archivo);
        Path bloqueo = archivo.resolve("bloqueo.txt");
        Files.writeString(bloqueo, "impide reemplazar este directorio");

        assertThatThrownBy(() -> repository.crear(entrada("4")))
                .isInstanceOf(UncheckedIOException.class);
        assertThatThrownBy(() -> repository.actualizar(primero.id(), entrada("5")))
                .isInstanceOf(UncheckedIOException.class);
        assertThatThrownBy(() -> repository.eliminar(segundo.id()))
                .isInstanceOf(UncheckedIOException.class);
        assertThat(repository.listar()).containsExactly(primero, segundo);

        Files.delete(bloqueo);
        Files.delete(archivo);
        assertThat(repository.crear(entrada("4")).id()).isEqualTo(3L);
        assertThat(abrir(archivo).listar()).hasSize(3);
    }

    private Path archivoVacio() throws IOException {
        Path archivo = directorio.resolve("residuos.json");
        Files.writeString(archivo, "[]");
        return archivo;
    }

    private RegistroResiduoRepository abrir(Path archivo) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            return new RegistroResiduoRepository(new ObjectMapper().findAndRegisterModules(),
                    archivo.toString(), factory.getValidator());
        }
    }

    private RegistroResiduo entrada(String cantidad) {
        return new RegistroResiduo(null, TipoResiduo.PLASTICO, new BigDecimal(cantidad),
                "kg", LocalDate.of(2026, 9, 10), "Botellas limpias",
                "Papelera Andina", "Vidrio limpio", true);
    }
}
