package pe.reciclo.residuos.dto;

import java.util.List;

public record Pagina<T>(List<T> contenido, int pagina, int tamanio, long totalElementos, int totalPaginas) {

    public static <T> Pagina<T> de(List<T> todos, int pagina, int tamanio) {
        int total = todos.size();
        int paginas = total == 0 ? 0 : (total + tamanio - 1) / tamanio;
        int desde = Math.min(pagina * tamanio, total);
        int hasta = Math.min(desde + tamanio, total);
        return new Pagina<>(List.copyOf(todos.subList(desde, hasta)), pagina, tamanio, total, paginas);
    }
}
