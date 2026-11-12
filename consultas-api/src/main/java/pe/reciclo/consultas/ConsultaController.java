package pe.reciclo.consultas;

import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/consultas")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class ConsultaController {
    private final ConsultaService service;

    public ConsultaController(ConsultaService service) {
        this.service = service;
    }

    @GetMapping("/residuos")
    public List<ConsultaService.Residuo> consultar(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) String empresa,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Boolean disponible,
            @RequestParam(required = false) BigDecimal cantidadMinima) {
        return service.consultar(token, empresa, tipo, disponible, cantidadMinima);
    }

    @GetMapping("/empresas")
    public List<String> empresas(@RequestHeader(value = "Authorization", required = false) String token) {
        return service.consultar(token, null, null, null, null).stream()
                .map(ConsultaService.Residuo::empresa).filter(e -> e != null && !e.isBlank())
                .distinct().sorted().toList();
    }
}
