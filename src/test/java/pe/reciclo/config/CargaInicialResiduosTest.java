package pe.reciclo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import pe.reciclo.model.RegistroResiduoEntity;
import pe.reciclo.repository.RegistroResiduoJpaRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CargaInicialResiduosTest {

    @TempDir
    Path directorio;
    @Autowired
    RegistroResiduoJpaRepository repository;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    Validator validator;

    @BeforeEach
    void vaciarBase() {
        repository.deleteAllInBatch();
    }

    private void cargar(Path archivo) throws IOException {
        new CargaInicialResiduos(repository, mapper, validator, archivo.toString())
                .run(new DefaultApplicationArguments());
    }

    @Test
    void baseVaciaCargaSemillaOriginalSinCrearArchivoJson() throws IOException {
        Path archivo = directorio.resolve("residuos.json");
        cargar(archivo);
        assertThat(repository.findAll().stream().map(RegistroResiduoEntity::toRegistroResiduo))
                .extracting(registro -> registro.empresa())
                .containsExactlyInAnyOrder("EcoEnvases", "Papelera Andina", "Vidrios del Sur");
        assertThat(archivo).doesNotExist();
    }

    @Test
    void importaArchivoExistenteConIdsGeneradosPorJpaYLoConserva() throws IOException {
        Path archivo = directorio.resolve("residuos.json");
        String json = """
                [{"id":987654,"tipoResiduo":"VIDRIO","cantidad":3.0001,
                  "unidadMedida":"kg","fecha":"2026-10-14","empresa":"Empresa inicial"}]
                """;
        Files.writeString(archivo, json);
        cargar(archivo);
        var registro = repository.findAll().getFirst().toRegistroResiduo();
        assertThat(registro.id()).isPositive().isNotEqualTo(987654L);
        assertThat(registro.cantidad()).isEqualByComparingTo("3.0001");
        assertThat(Files.readString(archivo)).isEqualTo(json);
    }

    @Test
    void baseConDatosNoSeRecargaAunqueJsonCambieOSeaInvalido() throws IOException {
        Path archivo = directorio.resolve("residuos.json");
        cargar(archivo);
        var antes = repository.findAll().stream().map(RegistroResiduoEntity::toRegistroResiduo).toList();
        Files.writeString(archivo, "JSON corrupto que ya no debe leerse");
        cargar(archivo);
        assertThat(repository.findAll().stream().map(RegistroResiduoEntity::toRegistroResiduo).toList())
                .containsExactlyInAnyOrderElementsOf(antes);
    }

    @Test
    void semillaConRegistroInvalidoNoSeImportaParcialmente() throws IOException {
        Path archivo = directorio.resolve("residuos.json");
        String json = """
                [{"tipoResiduo":"VIDRIO","cantidad":3,"unidadMedida":"kg","fecha":"2026-10-14"},
                 {"tipoResiduo":"VIDRIO","cantidad":-1,"unidadMedida":"kg","fecha":"2026-10-14"}]
                """;
        Files.writeString(archivo, json);
        assertThatThrownBy(() -> cargar(archivo)).isInstanceOf(IOException.class);
        assertThat(repository.count()).isZero();
        assertThat(Files.readString(archivo)).isEqualTo(json);
    }

    @Test
    void semillaVaciaEsValida() throws IOException {
        Path archivo = directorio.resolve("residuos.json");
        Files.writeString(archivo, "[]");
        cargar(archivo);
        assertThat(repository.count()).isZero();
    }
}
