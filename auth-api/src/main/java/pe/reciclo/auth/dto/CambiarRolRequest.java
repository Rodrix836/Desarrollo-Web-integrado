package pe.reciclo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record CambiarRolRequest(@NotBlank String rol) {
}
