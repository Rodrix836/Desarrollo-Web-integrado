package pe.reciclo.residuos;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@TestConfiguration
public class RelojPruebaConfig {
    @Bean
    @Primary
    RelojPrueba relojPrueba() { return new RelojPrueba(); }

    public static class RelojPrueba extends Clock {
        private Instant ahora = Instant.parse("2026-11-16T17:00:00Z");
        public void fijar(String instante) { ahora = Instant.parse(instante); }
        @Override public ZoneId getZone() { return ZoneId.of("America/Lima"); }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(ahora, zone); }
        @Override public Instant instant() { return ahora; }
    }
}
