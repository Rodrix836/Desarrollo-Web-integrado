package pe.reciclo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pe.reciclo.model.RegistroResiduo;
import pe.reciclo.model.RegistroResiduoEntity;
import pe.reciclo.model.TipoResiduo;
import pe.reciclo.repository.RegistroResiduoJpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RegistroResiduoServiceTest {

    @Autowired
    private RegistroResiduoService service;
    @Autowired
    private RegistroResiduoJpaRepository repository;

    @BeforeEach
    void preparar() {
        repository.deleteAllInBatch();
    }

    @Test
    void crearAsignaIdDelServidorYPermiteConsultar() {
        var entrada = new RegistroResiduo(999L, TipoResiduo.PLASTICO,
                new BigDecimal("2.5"), "kg", LocalDate.of(2026, 9, 9), null);

        var creado = service.crear(entrada);

        assertThat(creado).isNotNull();
        assertThat(creado.id()).isPositive().isNotEqualTo(999L);
        assertThat(creado.tipoResiduo()).isEqualTo(TipoResiduo.PLASTICO);
        assertThat(creado.cantidad()).isEqualByComparingTo("2.5");
        assertThat(creado.unidadMedida()).isEqualTo("kg");
        assertThat(creado.fecha()).isEqualTo(LocalDate.of(2026, 9, 9));
        assertThat(creado.descripcion()).isNull();
        assertThat(service.consultar(creado.id())).isEqualTo(creado);
    }

    @Test
    void listarSinRegistrosDevuelveListaVacia() {
        assertThat(service.listar()).isEmpty();
    }

    @Test
    void listarIncluyeLosCreadosConIdentificadoresDistintos() {
        var primero = service.crear(entrada());
        var segundo = service.crear(entrada());

        assertThat(primero.id()).isNotEqualTo(segundo.id());
        assertThat(service.listar()).containsExactly(primero, segundo);
    }

    @Test
    void consultarInexistenteInformaLaAusencia() {
        assertThatThrownBy(() -> service.consultar(999))
                .isInstanceOf(RegistroNoEncontradoException.class)
                .hasMessage("No existe un registro de residuo con id 999.");
    }

    @Test
    void actualizarReemplazaCamposYConservaIdSinModificarOtroRegistro() {
        var creado = service.crear(entrada());
        var otro = service.crear(entrada());
        var cambios = new RegistroResiduo(otro.id(), TipoResiduo.PAPEL_CARTON,
                new BigDecimal("3"), "bolsas", LocalDate.of(2026, 9, 10), null);

        var actualizado = service.actualizar(creado.id(), cambios);

        assertThat(actualizado).isEqualTo(new RegistroResiduo(creado.id(),
                TipoResiduo.PAPEL_CARTON, new BigDecimal("3"), "bolsas",
                LocalDate.of(2026, 9, 10), null));
        assertThat(service.consultar(creado.id())).isEqualTo(actualizado);
        assertThat(service.consultar(otro.id())).isEqualTo(otro);
        assertThat(service.listar()).hasSize(2);
    }

    @Test
    void eliminarRetiraSoloElRegistroSolicitadoYNoReutilizaSuId() {
        var creado = service.crear(entrada());
        var otro = service.crear(entrada());

        service.eliminar(creado.id());

        assertThat(service.listar()).containsExactly(otro);
        assertThatThrownBy(() -> service.consultar(creado.id()))
                .isInstanceOf(RegistroNoEncontradoException.class);
        assertThat(service.crear(entrada()).id()).isGreaterThan(otro.id());
    }

    @Test
    void actualizarInexistenteInformaLaAusenciaSinCrearRegistros() {
        assertThatThrownBy(() -> service.actualizar(999, entrada()))
                .isInstanceOf(RegistroNoEncontradoException.class);
        assertThat(service.listar()).isEmpty();
    }

    @Test
    void eliminarInexistenteInformaLaAusencia() {
        assertThatThrownBy(() -> service.eliminar(999))
                .isInstanceOf(RegistroNoEncontradoException.class);
    }

    @Test
    void crudLeeYEscribeLaMismaBaseDelRepositorioJpa() {
        var insertado = repository.save(new RegistroResiduoEntity(entrada())).toRegistroResiduo();
        assertThat(service.consultar(insertado.id())).isEqualTo(insertado);
        assertThat(service.listar()).containsExactly(insertado);

        var actualizado = service.actualizar(insertado.id(), oferta("Empresa en H2", true));
        assertThat(repository.findById(insertado.id()).orElseThrow().toRegistroResiduo())
                .isEqualTo(actualizado);

        service.eliminar(insertado.id());
        assertThat(repository.existsById(insertado.id())).isFalse();
        assertThat(repository.count()).isZero();
    }

    @Test
    void empresaConsultaSoloMaterialesDisponiblesDeOtrasEmpresas() {
        service.crear(oferta("Empresa Demo", true));
        var ajeno = service.crear(oferta("Papelera Andina", true));
        service.crear(oferta("Vidrios del Sur", false));
        service.crear(entrada());

        assertThat(service.disponiblesPara("Empresa Demo")).containsExactly(ajeno);
        assertThat(ajeno.empresa()).isEqualTo("Papelera Andina");
        assertThat(ajeno.intercambioPor()).isEqualTo("Cartón limpio");
    }

    @Test
    void detalleEmpresaNoExponeMaterialPropioONoDisponible() {
        var propio = service.crear(oferta("Empresa Demo", true));
        var oculto = service.crear(oferta("Otra empresa", false));

        assertThatThrownBy(() -> service.consultarDisponible(propio.id(), "empresa demo"))
                .isInstanceOf(RegistroNoEncontradoException.class);
        assertThatThrownBy(() -> service.consultarDisponible(oculto.id(), "Empresa Demo"))
                .isInstanceOf(RegistroNoEncontradoException.class);
    }

    private RegistroResiduo oferta(String empresa, boolean disponible) {
        return new RegistroResiduo(null, TipoResiduo.PLASTICO, new BigDecimal("2"),
                "kg", LocalDate.of(2026, 9, 10), "Botellas limpias", empresa, "Cartón limpio", disponible);
    }

    private RegistroResiduo entrada() {
        return new RegistroResiduo(null, TipoResiduo.PLASTICO,
                new BigDecimal("2.5"), "kg", LocalDate.of(2026, 9, 9), "Botellas limpias");
    }
}
