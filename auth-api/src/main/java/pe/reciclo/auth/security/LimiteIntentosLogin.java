package pe.reciclo.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloquea temporalmente un usuario tras varios intentos fallidos para frenar ataques de fuerza bruta.
 * Vive en memoria: basta para un único nodo de auth-api.
 */
@Component
public class LimiteIntentosLogin {

    private final int maxIntentos;
    private final Duration ventana;
    private final Map<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();
    private Clock clock = Clock.systemUTC();

    public LimiteIntentosLogin(@Value("${reciclo.login.max-intentos:5}") int maxIntentos,
                               @Value("${reciclo.login.bloqueo-minutos:15}") long minutos) {
        this.maxIntentos = maxIntentos;
        this.ventana = Duration.ofMinutes(minutos);
    }

    void usarReloj(Clock clock) {
        this.clock = clock;
    }

    public boolean bloqueado(String username) {
        var lista = fallos.get(clave(username));
        if (lista == null) return false;
        synchronized (lista) {
            limpiar(lista);
            return lista.size() >= maxIntentos;
        }
    }

    public void registrarFallo(String username) {
        var lista = fallos.computeIfAbsent(clave(username), k -> new ArrayDeque<>());
        synchronized (lista) {
            limpiar(lista);
            lista.addLast(clock.instant());
        }
    }

    public void reiniciar(String username) {
        fallos.remove(clave(username));
    }

    public long minutosDeBloqueo() {
        return ventana.toMinutes();
    }

    private void limpiar(Deque<Instant> lista) {
        Instant limite = clock.instant().minus(ventana);
        while (!lista.isEmpty() && lista.peekFirst().isBefore(limite)) {
            lista.removeFirst();
        }
    }

    private String clave(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
