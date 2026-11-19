package pe.reciclo.residuos.model;

public enum TipoResiduo {
    PLASTICO("Plástico"), PAPEL_CARTON("Papel y cartón"), VIDRIO("Vidrio"),
    METAL("Metal"), ORGANICO("Orgánico"), OTROS("Otros");

    private final String etiqueta;

    TipoResiduo(String etiqueta) { this.etiqueta = etiqueta; }
    public String etiqueta() { return etiqueta; }
}
