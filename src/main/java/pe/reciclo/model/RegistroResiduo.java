package pe.reciclo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistroResiduo(
        Long id,
        @NotNull(message = "El tipo de residuo es obligatorio.")
        TipoResiduo tipoResiduo,
        @NotNull(message = "La cantidad es obligatoria.")
        @Positive(message = "La cantidad debe ser mayor que cero.")
        BigDecimal cantidad,
        @NotBlank(message = "La unidad de medida es obligatoria.")
        String unidadMedida,
        @NotNull(message = "La fecha es obligatoria.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fecha,
        @Size(max = 500, message = "La descripción admite hasta 500 caracteres.")
        String descripcion,
        @Size(max = 100, message = "La empresa admite hasta 100 caracteres.")
        String empresa,
        @Size(max = 300, message = "La preferencia de intercambio admite hasta 300 caracteres.")
        String intercambioPor,
        Boolean disponible
) {
    public RegistroResiduo {
        empresa = empresa == null ? "" : empresa.trim();
        intercambioPor = intercambioPor == null ? "" : intercambioPor.trim();
        disponible = Boolean.TRUE.equals(disponible);
    }

    // Mantiene compatibles los registros y las pruebas de la primera base.
    public RegistroResiduo(Long id, TipoResiduo tipoResiduo, BigDecimal cantidad,
                           String unidadMedida, LocalDate fecha, String descripcion) {
        this(id, tipoResiduo, cantidad, unidadMedida, fecha, descripcion, "", "", false);
    }

    @JsonIgnore
    @AssertTrue(message = "Indica la empresa propietaria para publicar el material como disponible.")
    public boolean isEmpresaValida() {
        return !disponible || !empresa.isBlank();
    }
}
