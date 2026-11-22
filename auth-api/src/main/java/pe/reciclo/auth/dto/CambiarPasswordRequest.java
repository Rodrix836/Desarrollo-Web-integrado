package pe.reciclo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequest(
        @NotBlank String actual,
        @NotBlank @Size(min = 6, max = 100) String nueva
) {
}
