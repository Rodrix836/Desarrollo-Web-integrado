package pe.reciclo.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TipoResiduo {

    PLASTICO("Plástico"),
    PAPEL_CARTON("Papel / Cartón"),
    VIDRIO("Vidrio"),
    METAL("Metal"),
    ORGANICO("Orgánico"),
    OTROS("Otros");

    private final String etiqueta;

    TipoResiduo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @JsonCreator
    public static TipoResiduo desdeJson(String valor) {
        if (valor == null) {
            return null;
        }

        for (TipoResiduo tipo : values()) {
            if (tipo.name().equals(valor)) {
                return tipo;
            }
        }

        throw new IllegalArgumentException(
                "Tipo de residuo no válido: " + valor
        );
    }
}