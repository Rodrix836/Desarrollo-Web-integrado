package pe.reciclo.consultas;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class ConsultaService {
    private final RestClient client;

    public ConsultaService(RestClient.Builder builder, @Value("${residuos.url}") String url) {
        this.client = builder.baseUrl(url).build();
    }

    public List<Residuo> consultar(String token, String empresa, String tipo, Boolean disponible, BigDecimal minimo) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Se requiere un token Bearer válido.");
        }
        List<Residuo> registros = client.get().uri("/api/residuos")
                .header(HttpHeaders.AUTHORIZATION, token).retrieve()
                .body(new ParameterizedTypeReference<List<Residuo>>() {});
        String busqueda = empresa == null ? "" : empresa.trim().toLowerCase(Locale.ROOT);
        return registros.stream()
                .filter(r -> busqueda.isEmpty() || r.empresa().toLowerCase(Locale.ROOT).contains(busqueda))
                .filter(r -> tipo == null || tipo.isBlank() || r.tipoResiduo().equals(tipo))
                .filter(r -> disponible == null || disponible.equals(r.disponible()))
                .filter(r -> minimo == null || r.cantidad().compareTo(minimo) >= 0)
                .toList();
    }

    public record Residuo(Long id, String tipoResiduo, BigDecimal cantidad, String unidadMedida,
                          String fecha, String descripcion, String empresa, String intercambioPor,
                          Boolean disponible) {}
}
