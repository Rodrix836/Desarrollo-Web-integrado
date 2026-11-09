package pe.reciclo.residuos.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pe.reciclo.residuos.dto.ResiduoRespuesta;
import pe.reciclo.residuos.dto.ResiduoSolicitud;
import pe.reciclo.residuos.service.RegistroResiduoService;

import java.util.List;

@RestController
@RequestMapping("/api/residuos")
@SecurityRequirement(name = "bearerAuth")
public class RegistroResiduoController {

    private final RegistroResiduoService service;

    public RegistroResiduoController(RegistroResiduoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ResiduoRespuesta> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResiduoRespuesta consultar(@PathVariable @Positive long id) {
        return service.consultar(id);
    }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Residuo creado")
    public ResponseEntity<ResiduoRespuesta> crear(@Valid @RequestBody ResiduoSolicitud solicitud) {
        var creado = service.crear(solicitud);
        var ubicacion = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    public ResiduoRespuesta actualizar(@PathVariable @Positive long id,
                                        @Valid @RequestBody ResiduoSolicitud solicitud) {
        return service.actualizar(id, solicitud);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "Residuo eliminado")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
