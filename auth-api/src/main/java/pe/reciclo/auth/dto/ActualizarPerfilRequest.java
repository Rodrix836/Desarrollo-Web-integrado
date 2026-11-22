package pe.reciclo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarPerfilRequest(@NotBlank @Size(max = 100) String nombre) {
}
