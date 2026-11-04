package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ApartamentoDisponibleResult;

/**
 * JSON de un apartamento libre para la estancia buscada (CU-07). El precio se pide con {@code POST
 * /api/cotizaciones}.
 */
public record ApartamentoDisponibleResponse(String codigo, String nombre, int capacidad, int dormitorios) {

    public static ApartamentoDisponibleResponse de(ApartamentoDisponibleResult r) {
        return new ApartamentoDisponibleResponse(r.codigo(), r.nombre(), r.capacidad(), r.dormitorios());
    }
}
