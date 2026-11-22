package pe.reciclo.residuos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pe.reciclo.residuos.dto.AvisoIntercambio;
import pe.reciclo.residuos.dto.Estadisticas;
import pe.reciclo.residuos.dto.Pagina;
import pe.reciclo.residuos.dto.ResiduoRespuesta;
import pe.reciclo.residuos.dto.ResiduoSolicitud;
import pe.reciclo.residuos.model.EstadoPublicacion;
import pe.reciclo.residuos.model.TipoResiduo;
import pe.reciclo.residuos.security.Actor;
import pe.reciclo.residuos.service.RegistroResiduoService;
import java.util.List;

@RestController
@RequestMapping("/api/residuos")
@SecurityRequirement(name = "bearerAuth")
public class RegistroResiduoController {
    private final RegistroResiduoService service;

    public RegistroResiduoController(RegistroResiduoService service) { this.service = service; }

    @GetMapping
    public List<ResiduoRespuesta> listar(Authentication usuario,
                                         @RequestParam(required = false) EstadoPublicacion estado,
                                         @RequestParam(required = false) TipoResiduo preferencia) {
        return service.listar(Actor.de(usuario), estado, preferencia);
    }

    @GetMapping("/pagina")
    @Operation(summary = "Lista paginada (pagina empieza en 0, tamanio máximo 100)")
    public Pagina<ResiduoRespuesta> pagina(Authentication usuario,
                                           @RequestParam(required = false) EstadoPublicacion estado,
                                           @RequestParam(required = false) TipoResiduo preferencia,
                                           @RequestParam(defaultValue = "0") @Min(0) int pagina,
                                           @RequestParam(defaultValue = "10") @Min(1) @Max(100) int tamanio) {
        return service.pagina(Actor.de(usuario), estado, preferencia, pagina, tamanio);
    }

    @GetMapping("/estadisticas")
    @Operation(summary = "Kilos por tipo, empresa y mes de los registros visibles")
    public Estadisticas estadisticas(Authentication usuario) {
        return service.estadisticas(Actor.de(usuario));
    }

    @GetMapping("/avisos")
    public List<AvisoIntercambio> avisos() { return service.avisos(); }

    @GetMapping("/{id}")
    public ResiduoRespuesta consultar(@PathVariable @Positive long id, Authentication usuario) {
        return service.consultar(id, Actor.de(usuario));
    }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Residuo creado")
    public ResponseEntity<ResiduoRespuesta> crear(@Valid @RequestBody ResiduoSolicitud solicitud,
                                                  Authentication usuario) {
        var creado = service.crear(solicitud, Actor.de(usuario));
        var ubicacion = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    public ResiduoRespuesta actualizar(@PathVariable @Positive long id, @Valid @RequestBody ResiduoSolicitud solicitud,
                                       Authentication usuario) {
        return service.actualizar(id, solicitud, Actor.de(usuario));
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "Residuo eliminado")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive long id, Authentication usuario) {
        service.eliminar(id, Actor.de(usuario));
        return ResponseEntity.noContent().build();
    }
}
