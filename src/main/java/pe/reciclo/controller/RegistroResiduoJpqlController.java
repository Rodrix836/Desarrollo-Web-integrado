package pe.reciclo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.reciclo.model.RegistroResiduo;
import pe.reciclo.service.RegistroResiduoJpqlService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/residuos/jpql")
public class RegistroResiduoJpqlController {

    private final RegistroResiduoJpqlService service;

    public RegistroResiduoJpqlController(RegistroResiduoJpqlService service) {
        this.service = service;
    }

    @GetMapping("/empresa")
    public List<RegistroResiduo> buscarPorEmpresa(@RequestParam("empresa") String empresa) {
        return service.buscarPorEmpresa(empresa);
    }

    @GetMapping("/cantidad")
    public List<RegistroResiduo> buscarCantidadMayorQue(@RequestParam("cantidad") BigDecimal cantidad) {
        return service.buscarCantidadMayorQue(cantidad);
    }

    @GetMapping("/ordenados")
    public List<RegistroResiduo> listarOrdenadosPorCantidad() {
        return service.listarOrdenadosPorCantidad();
    }
}
