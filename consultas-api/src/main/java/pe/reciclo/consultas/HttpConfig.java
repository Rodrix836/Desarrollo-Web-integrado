package pe.reciclo.consultas;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class HttpConfig {
    @Bean
    RestClientCustomizer timeout() {
        return builder -> {
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            var factory = new JdkClientHttpRequestFactory(client);
            factory.setReadTimeout(Duration.ofSeconds(5));
            builder.requestFactory(factory);
        };
    }
}
