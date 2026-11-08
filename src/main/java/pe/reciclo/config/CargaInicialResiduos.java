package pe.reciclo.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pe.reciclo.model.RegistroResiduo;
import pe.reciclo.model.RegistroResiduoEntity;
import pe.reciclo.repository.RegistroResiduoJpaRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** El JSON solamente se lee como semilla cuando la base de datos está vacía. */
@Component
@ConditionalOnProperty(name = "reciclo.datos.cargar-semilla", havingValue = "true", matchIfMissing = true)
public class CargaInicialResiduos implements ApplicationRunner {

    private final RegistroResiduoJpaRepository repository;
    private final ObjectMapper mapper;
    private final Validator validator;
    private final Path archivo;

    public CargaInicialResiduos(RegistroResiduoJpaRepository repository, ObjectMapper mapper,
                               Validator validator,
                               @Value("${reciclo.datos.archivo:data/residuos.json}") String archivo) {
        this.repository = repository;
        this.mapper = mapper;
        this.validator = validator;
        this.archivo = Path.of(archivo);
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (repository.count() != 0) {
            return;
        }
        try (var entrada = Files.exists(archivo) ? Files.newInputStream(archivo)
                : new ClassPathResource("residuos.json").getInputStream()) {
            List<RegistroResiduo> registros = mapper.readValue(entrada, new TypeReference<>() { });
            if (registros == null || registros.stream()
                    .anyMatch(registro -> registro == null || !validator.validate(registro).isEmpty())) {
                throw new IOException("La semilla contiene residuos inválidos; no se importó ningún registro.");
            }
            // Los identificadores de la semilla no se copian: los genera H2 mediante JPA.
            repository.saveAll(registros.stream().map(RegistroResiduoEntity::new).toList());
        }
    }
}
