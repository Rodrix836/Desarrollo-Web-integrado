package pe.reciclo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos que el ADMIN puede editar de una cuenta. */
public record ActualizarUsuarioRequest(@NotBlank @Size(max = 100) String nombre) {
}
