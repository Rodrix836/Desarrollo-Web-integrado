package pe.reciclo.model;

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
}
