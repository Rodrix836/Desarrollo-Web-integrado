package pe.reciclo.residuos.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import pe.reciclo.residuos.dto.ResiduoSolicitud;
import pe.reciclo.residuos.exception.RegistroNoEncontradoException;
import pe.reciclo.residuos.model.RegistroResiduo;
import pe.reciclo.residuos.model.TipoResiduo;
import pe.reciclo.residuos.repository.RegistroResiduoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroResiduoServiceTest {

    private static final ResiduoSolicitud SOLICITUD = new ResiduoSolicitud(
            TipoResiduo.PLASTICO, new BigDecimal("2.123456789"), "kg",
            LocalDate.of(2026, 9, 9), "Botellas", "Acme", "Papel", true);

    @Mock
    private RegistroResiduoRepository repository;

    @InjectMocks
    private RegistroResiduoService service;

    @Test
    void listarOrdenaPorIdYDevuelveLasRespuestas() {
        Sort orden = Sort.by("id");
        when(repository.findAll(orden)).thenReturn(List.of(registro(8L), registro(9L)));
        assertThat(service.listar()).containsExactly(registro(8L).aRespuesta(), registro(9L).aRespuesta());
        verify(repository).findAll(orden);
    }

    @Test
    void consultarBuscaPorId() {
        RegistroResiduo guardado = registro(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(guardado));
        assertThat(service.consultar(7L)).isEqualTo(guardado.aRespuesta());
        verify(repository).findById(7L);
    }

    @Test
    void crearEntregaUnaEntidadNuevaYDevuelveElIdGenerado() {
        when(repository.save(any(RegistroResiduo.class))).thenAnswer(llamada -> {
            RegistroResiduo nuevo = llamada.getArgument(0);
            assertThat(nuevo.aRespuesta().id()).isNull();
            assertThat(nuevo.aRespuesta().cantidad()).isEqualTo(SOLICITUD.cantidad());
            ReflectionTestUtils.setField(nuevo, "id", 7L);
            return nuevo;
        });
        assertThat(service.crear(SOLICITUD)).isEqualTo(registro(7L).aRespuesta());
        verify(repository).save(any(RegistroResiduo.class));
    }

    @Test
    void actualizarModificaLaEntidadGestionadaYConservaSuId() {
        RegistroResiduo guardado = registro(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(guardado));
        var cambios = new ResiduoSolicitud(TipoResiduo.METAL, new BigDecimal("10.002"), "kg",
                LocalDate.of(2026, 9, 10), "Latas", null, null, null);
        var actualizado = service.actualizar(7L, cambios);
        assertThat(actualizado).isEqualTo(guardado.aRespuesta());
        assertThat(actualizado.id()).isEqualTo(7L);
        assertThat(actualizado.tipoResiduo()).isEqualTo(TipoResiduo.METAL);
        assertThat(actualizado.cantidad()).isEqualByComparingTo("10.002");
        assertThat(actualizado.disponible()).isFalse();
        verify(repository).findById(7L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void eliminarBorraLaEntidadEncontrada() {
        RegistroResiduo guardado = registro(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(guardado));
        service.eliminar(7L);
        verify(repository).findById(7L);
        verify(repository).delete(guardado);
    }

    @ParameterizedTest
    @EnumSource(Operacion.class)
    void idInexistenteNoCausaEscrituras(Operacion operacion) {
        when(repository.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> {
            switch (operacion) {
                case CONSULTAR -> service.consultar(7L);
                case ACTUALIZAR -> service.actualizar(7L, SOLICITUD);
                case ELIMINAR -> service.eliminar(7L);
            }
        }).isInstanceOf(RegistroNoEncontradoException.class).hasMessageContaining("7");
        verify(repository).findById(7L);
        verifyNoMoreInteractions(repository);
    }

    private static RegistroResiduo registro(long id) {
        RegistroResiduo entidad = new RegistroResiduo(SOLICITUD);
        ReflectionTestUtils.setField(entidad, "id", id);
        return entidad;
    }

    private enum Operacion {
        CONSULTAR, ACTUALIZAR, ELIMINAR
    }
}
