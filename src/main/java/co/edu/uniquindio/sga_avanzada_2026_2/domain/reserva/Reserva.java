package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.LineaCotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;

import java.time.LocalTime;
import java.util.List;

/**
 * Raíz del agregado Reserva. Nace PENDIENTE; valor, desglose y política se congelan al crear (RN-22).
 */
public class Reserva {

    private final ReservaId codigo;
    private ApartamentoId apartamentoId;
    private final TitularId titularId;
    private Estancia estancia;
    private EstadoReserva estado;
    private final CanalOrigen canalOrigen;
    private final CanalId canalId;     // opcional: solo si EXTERNO
    private final String idExterno;    // opcional: solo si EXTERNO
    private List<Ocupante> ocupantes;
    private Registro registro;         // opcional
    private Salida salida;             // opcional
    private LocalTime horaEstimadaLlegada;
    private Dinero valorTotal;               // congelado
    private List<LineaCotizacion> desglose;  // congelado, por noche
    private final PoliticaId politicaVersionId; // congelada

    public Reserva(ReservaId codigo, ApartamentoId apartamentoId, TitularId titularId, Estancia estancia,
                   EstadoReserva estado, CanalOrigen canalOrigen, CanalId canalId, String idExterno,
                   List<Ocupante> ocupantes, Registro registro, Salida salida, LocalTime horaEstimadaLlegada,
                   Dinero valorTotal, List<LineaCotizacion> desglose, PoliticaId politicaVersionId) {
        this.codigo = codigo;
        this.apartamentoId = apartamentoId;
        this.titularId = titularId;
        this.estancia = estancia;
        this.estado = estado;
        this.canalOrigen = canalOrigen;
        this.canalId = canalId;
        this.idExterno = idExterno;
        this.ocupantes = List.copyOf(ocupantes);
        this.registro = registro;
        this.salida = salida;
        this.horaEstimadaLlegada = horaEstimadaLlegada;
        this.valorTotal = valorTotal;
        this.desglose = List.copyOf(desglose);
        this.politicaVersionId = politicaVersionId;
    }
}
