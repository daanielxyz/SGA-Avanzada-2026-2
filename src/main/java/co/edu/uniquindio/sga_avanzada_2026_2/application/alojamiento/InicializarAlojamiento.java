package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.PublicarPoliticaCommand.TramoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicionalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Ubicacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: crea el único alojamiento del despliegue (ALO-01) con los valores iniciales de {@code sga.*}
 * (DEC-25), su primera política de cancelación (DEC-41) y su calendario con la temporada base (TEM-08). Lo invoca
 * el arranque de la aplicación; si el alojamiento ya existe no cambia nada (DEC-53).
 */
@Service
public class InicializarAlojamiento {

    private final AlojamientoRepository alojamientos;
    private final PoliticaCancelacionRepository politicas;
    private final CalendarioTemporadasRepository calendarios;
    private final GeneradorCodigos codigos;

    public InicializarAlojamiento(AlojamientoRepository alojamientos, PoliticaCancelacionRepository politicas,
                                  CalendarioTemporadasRepository calendarios, GeneradorCodigos codigos) {
        this.alojamientos = alojamientos;
        this.politicas = politicas;
        this.calendarios = calendarios;
        this.codigos = codigos;
    }

    /**
     * Crea el alojamiento, la política y el calendario en una sola transacción, o ninguno (DEC-53).
     *
     * @return el alojamiento: el recién creado, o el que ya existía sin cambios
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si los valores
     *         iniciales violan una regla (ALO-02 · ALO-06 · POL-01 · POL-02)
     */
    @Transactional
    public AlojamientoResult ejecutar(InicializarAlojamientoCommand comando) {
        AlojamientoId id = new AlojamientoId(comando.id());
        return AlojamientoResult.de(alojamientos.buscarPorId(id).orElseGet(() -> crear(id, comando)));
    }

    private Alojamiento crear(AlojamientoId id, InicializarAlojamientoCommand comando) {
        ParametrosAlojamiento parametros = comando.parametros().aDominio();
        Alojamiento alojamiento = Alojamiento.crear(id, comando.nombre(), comando.descripcion(), comando.ciudad(),
                comando.direccion(), new Ubicacion(comando.latitud(), comando.longitud()), comando.normas(),
                parametros,
                comando.servicios().stream()
                        .map(s -> new ServicioAdicional(
                                new ServicioAdicionalId(codigos.siguiente(SerieCodigo.SERVICIO)), s.nombre(),
                                s.generaCargo(), new Dinero(s.valor()), true))
                        .toList(),
                comando.mediosPago().stream().map(MedioPago::new).toList());
        PoliticaCancelacion politica = PoliticaCancelacion.crear(
                new PoliticaId(codigos.siguiente(SerieCodigo.POLITICA)), id,
                comando.tramosCancelacion().stream().map(TramoCommand::aDominio).toList(),
                comando.penalizacionNoShow().aDominio(), parametros.minimoTramosCancelacion());
        CalendarioTemporadas calendario = CalendarioTemporadas.crear(id,
                new TemporadaId(codigos.siguiente(SerieCodigo.TEMPORADA)), comando.nombreTemporadaBase());

        alojamientos.guardar(alojamiento);
        politicas.guardar(politica);
        calendarios.guardar(calendario);
        return alojamiento;
    }
}
