package pe.reciclo.residuos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.reciclo.residuos.dto.SolicitudCrear;
import pe.reciclo.residuos.dto.SolicitudDecision;
import pe.reciclo.residuos.dto.SolicitudRespuesta;
import pe.reciclo.residuos.security.Actor;
import pe.reciclo.residuos.service.SolicitudIntercambioService;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
@SecurityRequirement(name = "bearerAuth")
public class SolicitudIntercambioController {

    private final SolicitudIntercambioService service;

    public SolicitudIntercambioController(SolicitudIntercambioService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "USER solicita un material disponible")
    public SolicitudRespuesta crear(@Valid @RequestBody SolicitudCrear datos, Authentication usuario) {
        return service.crear(datos, Actor.de(usuario));
    }

    @GetMapping("/mias")
    @Operation(summary = "Solicitudes enviadas por el USER autenticado")
    public List<SolicitudRespuesta> mias(Authentication usuario) {
        return service.mias(Actor.de(usuario));
    }

    @GetMapping
    @Operation(summary = "Todas las solicitudes (ADMIN)")
    public List<SolicitudRespuesta> todas() {
        return service.todas();
    }

    @PutMapping("/{id}/decision")
    @Operation(summary = "ADMIN acepta o rechaza una solicitud pendiente")
    public SolicitudRespuesta decidir(@PathVariable @Positive long id, @Valid @RequestBody SolicitudDecision decision,
                                      Authentication usuario) {
        return service.decidir(id, decision, Actor.de(usuario));
    }

    @PutMapping("/{id}/cancelar")
    public SolicitudRespuesta cancelar(@PathVariable @Positive long id, Authentication usuario) {
        return service.cancelar(id, Actor.de(usuario));
    }
}
