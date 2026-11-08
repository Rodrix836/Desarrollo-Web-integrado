package pe.reciclo.service;

import org.springframework.stereotype.Service;
import pe.reciclo.model.RegistroResiduo;
import pe.reciclo.model.RegistroResiduoEntity;
import pe.reciclo.repository.RegistroResiduoJpqlRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RegistroResiduoJpqlService {

    private final RegistroResiduoJpqlRepository repository;

    public RegistroResiduoJpqlService(RegistroResiduoJpqlRepository repository) {
        this.repository = repository;
    }

    public synchronized List<RegistroResiduo> buscarPorEmpresa(String empresa) {
        return convertir(repository.buscarPorEmpresa(empresa));
    }

    public synchronized List<RegistroResiduo> buscarCantidadMayorQue(BigDecimal cantidad) {
        return convertir(repository.buscarCantidadMayorQue(cantidad));
    }

    public synchronized List<RegistroResiduo> listarOrdenadosPorCantidad() {
        return convertir(repository.listarOrdenadosPorCantidad());
    }

    private List<RegistroResiduo> convertir(List<RegistroResiduoEntity> registros) {
        return registros.stream().map(RegistroResiduoEntity::toRegistroResiduo).toList();
    }
}
