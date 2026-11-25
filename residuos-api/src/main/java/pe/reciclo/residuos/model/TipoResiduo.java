package pe.reciclo.residuos.model;

public enum TipoResiduo {
    PLASTICO("Plástico"), PAPEL_CARTON("Papel y cartón"), VIDRIO("Vidrio"),
    METAL("Metal"), ORGANICO("Orgánico"),
    // Sólo para leer datos históricos de H2; no se admite en altas ni modificaciones.
    OTROS("Categoría retirada");

    private final String etiqueta;

    TipoResiduo(String etiqueta) { this.etiqueta = etiqueta; }
    public String etiqueta() { return etiqueta; }
    public boolean vigente() { return this != OTROS; }
}
