package pe.reciclo.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import pe.reciclo.model.RegistroResiduo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class RegistroResiduoRepository {

    private final ObjectMapper mapper;
    private final Path archivo;
    private Map<Long, RegistroResiduo> registros = new LinkedHashMap<>();
    private long siguienteId = 1;

    public RegistroResiduoRepository(ObjectMapper mapper,
            @Value("${reciclo.datos.archivo:data/residuos.json}") String archivo, Validator validator) {
        this.mapper = mapper.copy().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.archivo = Path.of(archivo).toAbsolutePath().normalize();
        cargar(validator);
    }

    public synchronized RegistroResiduo crear(RegistroResiduo entrada) {
        var creado = guardar(siguienteId, entrada);
        siguienteId++;
        return creado;
    }

    public synchronized Optional<RegistroResiduo> buscar(long id) {
        return Optional.ofNullable(registros.get(id));
    }

    public synchronized List<RegistroResiduo> listar() {
        return List.copyOf(registros.values());
    }

    public synchronized Optional<RegistroResiduo> actualizar(long id, RegistroResiduo entrada) {
        if (!registros.containsKey(id)) {
            return Optional.empty();
        }
        return Optional.of(guardar(id, entrada));
    }

    public synchronized boolean eliminar(long id) {
        if (!registros.containsKey(id)) {
            return false;
        }
        var nuevos = new LinkedHashMap<>(registros);
        nuevos.remove(id);
        persistir(nuevos);
        registros = nuevos;
        return true;
    }

    private RegistroResiduo guardar(long id, RegistroResiduo entrada) {
        var registro = new RegistroResiduo(id, entrada.tipoResiduo(), entrada.cantidad(),
                entrada.unidadMedida(), entrada.fecha(), entrada.descripcion(), entrada.empresa(),
                entrada.intercambioPor(), entrada.disponible());
        var nuevos = new LinkedHashMap<>(registros);
        nuevos.put(id, registro);
        // La memoria cambia únicamente después de guardar el archivo correctamente.
        persistir(nuevos);
        registros = nuevos;
        return registro;
    }

    private void cargar(Validator validator) {
        boolean usarSemilla = Files.notExists(archivo);
        try (InputStream entrada = usarSemilla
                ? new ClassPathResource("residuos.json").getInputStream()
                : Files.newInputStream(archivo)) {
            List<RegistroResiduo> leidos = mapper.readValue(entrada, new TypeReference<>() { });
            if (leidos == null) {
                throw new IOException("El JSON debe contener una lista de residuos.");
            }
            var cargados = new LinkedHashMap<Long, RegistroResiduo>();
            long mayorId = 0;
            for (var registro : leidos) {
                if (registro == null || registro.id() == null || registro.id() <= 0
                        || cargados.containsKey(registro.id())) {
                    throw new IOException("Los residuos deben tener identificadores positivos y únicos.");
                }
                if (!validator.validate(registro).isEmpty()) {
                    throw new IOException("El registro " + registro.id() + " contiene campos inválidos.");
                }
                cargados.put(registro.id(), registro);
                mayorId = Math.max(mayorId, registro.id());
            }
            if (mayorId == Long.MAX_VALUE) {
                throw new IOException("Se agotó el rango de identificadores de residuos.");
            }
            if (usarSemilla) {
                persistir(cargados);
            }
            registros = cargados;
            siguienteId = mayorId + 1;
        } catch (IOException exception) {
            // Un archivo inválido se conserva para que pueda revisarse y corregirse.
            throw new UncheckedIOException("No se pudieron leer los residuos del archivo JSON.", exception);
        }
    }

    private void persistir(Map<Long, RegistroResiduo> nuevos) {
        try {
            Files.createDirectories(archivo.getParent());
            Path temporal = Files.createTempFile(archivo.getParent(), "residuos-", ".tmp");
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(temporal.toFile(), nuevos.values());
                try {
                    Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporal);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("No se pudieron guardar los residuos en el archivo JSON.", exception);
        }
    }
}
