package pe.reciclo.service;

import org.springframework.stereotype.Service;
import pe.reciclo.model.RegistroResiduo;
import pe.reciclo.repository.RegistroResiduoRepository;

import java.util.List;
import java.util.stream.Stream;

@Service
public class RegistroResiduoService {

    private final RegistroResiduoRepository repository;

    public RegistroResiduoService(RegistroResiduoRepository repository) {
        this.repository = repository;
    }

    public RegistroResiduo crear(RegistroResiduo registro) {
        return repository.crear(registro);
    }

    public RegistroResiduo consultar(long id) {
        return repository.buscar(id).orElseThrow(() -> new RegistroNoEncontradoException(id));
    }

    public List<RegistroResiduo> listar() {
        return repository.listar();
    }

    public List<RegistroResiduo> disponiblesPara(String empresa) {
        return repository.listar().stream().filter(registro -> visiblePara(registro, empresa)).toList();
    }

    public RegistroResiduo consultarDisponible(long id, String empresa) {
        var registro = consultar(id);
        if (!visiblePara(registro, empresa)) {
            throw new RegistroNoEncontradoException(id);
        }
        return registro;
    }

    public List<String> empresasDeDemostracion() {
        return Stream.concat(Stream.of("Empresa Demo"), repository.listar().stream()
                        .map(RegistroResiduo::empresa).filter(empresa -> !empresa.isBlank()))
                .distinct().sorted().toList();
    }

    private boolean visiblePara(RegistroResiduo registro, String empresa) {
        return registro.disponible() && !registro.empresa().isBlank()
                && !registro.empresa().equalsIgnoreCase(empresa.trim());
    }

    public RegistroResiduo actualizar(long id, RegistroResiduo registro) {
        return repository.actualizar(id, registro)
                .orElseThrow(() -> new RegistroNoEncontradoException(id));
    }

    public void eliminar(long id) {
        if (!repository.eliminar(id)) {
            throw new RegistroNoEncontradoException(id);
        }
    }
}
