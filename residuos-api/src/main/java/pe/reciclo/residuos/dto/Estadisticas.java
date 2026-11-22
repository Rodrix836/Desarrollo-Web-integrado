package pe.reciclo.residuos.dto;

import java.math.BigDecimal;
import java.util.List;

public record Estadisticas(long registros, long disponibles, BigDecimal totalKg,
                           List<Grupo> porTipo, List<Grupo> porEmpresa, List<Grupo> porMes,
                           long solicitudesPendientes) {

    public record Grupo(String clave, String etiqueta, long registros, BigDecimal kg) {
    }
}
