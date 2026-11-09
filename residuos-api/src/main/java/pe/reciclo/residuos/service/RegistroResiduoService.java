package pe.reciclo.residuos.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.reciclo.residuos.dto.ResiduoRespuesta;
import pe.reciclo.residuos.dto.ResiduoSolicitud;
import pe.reciclo.residuos.exception.RegistroNoEncontradoException;
import pe.reciclo.residuos.model.RegistroResiduo;
import pe.reciclo.residuos.repository.RegistroResiduoRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RegistroResiduoService {

    private final RegistroResiduoRepository repository;

    public RegistroResiduoService(RegistroResiduoRepository repository) {
        this.repository = repository;
    }

    public List<ResiduoRespuesta> listar() {
        return repository.findAll(Sort.by("id")).stream().map(RegistroResiduo::aRespuesta).toList();
    }

    public ResiduoRespuesta consultar(long id) {
        return buscar(id).aRespuesta();
    }

    @Transactional
    public ResiduoRespuesta crear(ResiduoSolicitud solicitud) {
        return repository.save(new RegistroResiduo(solicitud)).aRespuesta();
    }

    @Transactional
    public ResiduoRespuesta actualizar(long id, ResiduoSolicitud solicitud) {
        RegistroResiduo existente = buscar(id);
        existente.actualizarDesde(solicitud);
        return existente.aRespuesta();
    }

    @Transactional
    public void eliminar(long id) {
        repository.delete(buscar(id));
    }

    private RegistroResiduo buscar(long id) {
        return repository.findById(id).orElseThrow(() -> new RegistroNoEncontradoException(id));
    }
}
