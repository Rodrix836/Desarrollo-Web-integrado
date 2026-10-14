package pe.reciclo.service;

import org.springframework.stereotype.Service;
import pe.reciclo.model.RegistroResiduo;
import pe.reciclo.model.RegistroResiduoEntity;
import pe.reciclo.repository.RegistroResiduoJpqlRepository;
import pe.reciclo.repository.RegistroResiduoRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RegistroResiduoJpqlService {

    private final RegistroResiduoRepository origen;
    private final RegistroResiduoJpqlRepository repository;

    public RegistroResiduoJpqlService(RegistroResiduoRepository origen,
                                    RegistroResiduoJpqlRepository repository) {
        this.origen = origen;
        this.repository = repository;
    }

    public synchronized List<RegistroResiduo> buscarPorEmpresa(String empresa) {
        prepararDatos();
        return convertir(repository.buscarPorEmpresa(empresa));
    }

    public synchronized List<RegistroResiduo> buscarCantidadMayorQue(BigDecimal cantidad) {
        prepararDatos();
        return convertir(repository.buscarCantidadMayorQue(cantidad));
    }

    public synchronized List<RegistroResiduo> listarOrdenadosPorCantidad() {
        prepararDatos();
        return convertir(repository.listarOrdenadosPorCantidad());
    }

    // El JSON sigue siendo el origen. Se refresca la copia para reflejar también el CRUD.
    // Los métodos sincronizados evitan que dos consultas mezclen sus copias temporales.
    private void prepararDatos() {
        var registros = origen.listar();
        repository.deleteAllInBatch();
        repository.saveAll(registros.stream().map(RegistroResiduoEntity::new).toList());
    }

    private List<RegistroResiduo> convertir(List<RegistroResiduoEntity> registros) {
        return registros.stream().map(RegistroResiduoEntity::toRegistroResiduo).toList();
    }
}
