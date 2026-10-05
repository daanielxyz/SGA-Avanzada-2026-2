package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Ocupante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Cotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.LineaCotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Tarifa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Temporada;

import java.util.Comparator;
import java.util.List;

/**
 * Servicio de dominio sin estado: calcula el valor de una estancia. Cruza CalendarioTemporadas, Tarifa y Ocupantes,
 * que recibe por parámetro.
 */
public class TarificacionDomainService {

    /**
     * Valor = Σ por noche (tarifa vigente del apartamento en la temporada de esa noche × ocupantes facturables)
     * (RN-05 · TAR-04 · NOC-04). Los facturables se cuentan a la fecha de entrada (RN-06 · OCU-03). Con tarifas
     * enteras no hay fracciones, así que no se redondea noche por noche (DIN-02). Es determinista y no reserva
     * nada (COT-01 · COT-03).
     *
     * @param tarifas tarifas vigentes del apartamento a la fecha de la operación
     *                ({@code TarifaRepository.buscarVigentesPorApartamento}); si llegan varias versiones de una
     *                temporada se usa la mayor (TAR-06)
     * @param umbral  {@code ParametrosAlojamiento.umbralEdadFacturable} (OCU-11)
     * @return desglose noche por noche y total (COT-02)
     * @throws ReglaDominioException si a alguna noche le falta tarifa para su temporada (RN-05 · TAR-03)
     */
    public Cotizacion calcularValorEstancia(ApartamentoId apartamentoId, Estancia estancia, List<Ocupante> ocupantes,
                                            int umbral, CalendarioTemporadas calendario, List<Tarifa> tarifas) {
        int facturables = (int) ocupantes.stream()
                .filter(o -> o.esFacturableA(estancia.entrada(), umbral))
                .count();
        List<LineaCotizacion> desglose = estancia.noches().stream()
                .map(noche -> linea(noche, apartamentoId, facturables, calendario, tarifas))
                .toList();
        Dinero total = desglose.stream().map(LineaCotizacion::subtotal).reduce(Dinero.CERO, Dinero::sumar);
        return new Cotizacion(desglose, total);
    }

    private static LineaCotizacion linea(Noche noche, ApartamentoId apartamentoId, int facturables,
                                         CalendarioTemporadas calendario, List<Tarifa> tarifas) {
        Temporada temporada = calendario.temporadaDe(noche);
        Tarifa tarifa = tarifas.stream()
                .filter(t -> t.aplicaA(apartamentoId, temporada.id()))
                .max(Comparator.comparingInt(Tarifa::version))
                .orElseThrow(() -> new ReglaDominioException("El apartamento " + apartamentoId.valor()
                        + " no tiene tarifa en la temporada " + temporada.nombre()));
        Dinero valor = tarifa.valorPorOcupante();
        return new LineaCotizacion(noche, temporada.id(), valor, facturables, valor.multiplicar(facturables));
    }
}
