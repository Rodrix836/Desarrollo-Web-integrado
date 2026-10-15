package pe.reciclo.service;

import org.springframework.stereotype.Service;
import pe.reciclo.model.RegistroResiduo;
import pe.reciclo.model.RegistroResiduoEntity;
import pe.reciclo.repository.RegistroResiduoJpaRepository;

import java.util.List;
import java.util.stream.Stream;

@Service
public class RegistroResiduoService {

    private final RegistroResiduoJpaRepository repository;

    public RegistroResiduoService(RegistroResiduoJpaRepository repository) {
        this.repository = repository;
    }

    public RegistroResiduo crear(RegistroResiduo registro) {
        return repository.save(new RegistroResiduoEntity(registro)).toRegistroResiduo();
    }

    public RegistroResiduo consultar(long id) {
        return repository.findById(id).orElseThrow(() -> new RegistroNoEncontradoException(id))
                .toRegistroResiduo();
    }

    public List<RegistroResiduo> listar() {
        return repository.findAll().stream().map(RegistroResiduoEntity::toRegistroResiduo).toList();
    }

    public List<RegistroResiduo> disponiblesPara(String empresa) {
        return listar().stream().filter(registro -> visiblePara(registro, empresa)).toList();
    }

    public RegistroResiduo consultarDisponible(long id, String empresa) {
        var registro = consultar(id);
        if (!visiblePara(registro, empresa)) {
            throw new RegistroNoEncontradoException(id);
        }
        return registro;
    }

    public List<String> empresasDeDemostracion() {
        return Stream.concat(Stream.of("Empresa Demo"), listar().stream()
                        .map(RegistroResiduo::empresa).filter(empresa -> !empresa.isBlank()))
                .distinct().sorted().toList();
    }

    private boolean visiblePara(RegistroResiduo registro, String empresa) {
        return registro.disponible() && !registro.empresa().isBlank()
                && !registro.empresa().equalsIgnoreCase(empresa.trim());
    }

    public RegistroResiduo actualizar(long id, RegistroResiduo registro) {
        var existente = repository.findById(id)
                .orElseThrow(() -> new RegistroNoEncontradoException(id));
        existente.actualizarDesde(registro);
        return repository.save(existente).toRegistroResiduo();
    }

    public void eliminar(long id) {
        if (!repository.existsById(id)) {
            throw new RegistroNoEncontradoException(id);
        }
        repository.deleteById(id);
    }
}
