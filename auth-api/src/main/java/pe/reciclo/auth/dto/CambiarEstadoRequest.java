package pe.reciclo.auth.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(@NotNull Boolean activo) {
}
