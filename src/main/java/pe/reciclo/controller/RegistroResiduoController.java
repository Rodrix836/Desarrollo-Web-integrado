package pe.reciclo.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pe.reciclo.model.RegistroResiduo;
import pe.reciclo.service.RegistroResiduoService;

import java.util.List;

@RestController
@RequestMapping("/api/residuos")
public class RegistroResiduoController {

    private final RegistroResiduoService service;

    public RegistroResiduoController(RegistroResiduoService service) {
        this.service = service;
    }

    @GetMapping
    public List<RegistroResiduo> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public RegistroResiduo consultar(@PathVariable long id) {
        return service.consultar(id);
    }

    @PostMapping
    public ResponseEntity<RegistroResiduo> crear(@Valid @RequestBody RegistroResiduo entrada) {
        var creado = service.crear(entrada);
        var ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    public RegistroResiduo actualizar(@PathVariable long id, @Valid @RequestBody RegistroResiduo entrada) {
        return service.actualizar(id, entrada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
