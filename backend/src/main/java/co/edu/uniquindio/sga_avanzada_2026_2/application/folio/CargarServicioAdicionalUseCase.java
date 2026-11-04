package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CargadorReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicionalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CargoServicioDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Caso de uso: recepción cobra en el folio un servicio adicional que consumió el grupo alojado (CAR-05 · DEC-60).
 * Solo cambia el Folio; la reserva y el alojamiento se leen.
 */
@Service
public class CargarServicioAdicionalUseCase {

    private final CargadorReserva cargador;
    private final FolioRepository folios;
    private final CargoServicioDomainService cargoServicio;
    private final Clock reloj;

    public CargarServicioAdicionalUseCase(CargadorReserva cargador, FolioRepository folios,
                                          CargoServicioDomainService cargoServicio, Clock reloj) {
        this.cargador = cargador;
        this.folios = folios;
        this.cargoServicio = cargoServicio;
        this.reloj = reloj;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva, su folio, su apartamento o su alojamiento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la reserva no está
     *         EN_CURSO, el servicio no existe, está inactivo o no genera cargo, o el folio está cerrado (CAR-07)
     */
    @Transactional
    public FolioResult ejecutar(CargarServicioAdicionalCommand comando) {
        Reserva reserva = cargador.reserva(comando.codigoReserva());
        Folio folio = cargador.folioDe(reserva);
        Alojamiento alojamiento = cargador.alojamientoDe(reserva);

        cargoServicio.cargar(reserva, folio, alojamiento, new ServicioAdicionalId(comando.servicioId()),
                LocalDate.now(reloj));

        folios.guardar(folio);
        return FolioResult.de(folio);
    }
}
