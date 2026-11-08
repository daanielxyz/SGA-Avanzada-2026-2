package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Extras;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MotivoRechazo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.time.Duration;
import java.time.LocalTime;

/**
 * VO: configuración de negocio editable del alojamiento, nunca constantes del código (ALO-03). Se guarda con el
 * Alojamiento; {@code sga.*} solo aporta los valores iniciales (DEC-25). Cambiarla no altera reservas ya creadas
 * (ALO-04). Horas del día en zona horaria de Colombia (ALO-02).
 *
 * @param anticipo                   0 significa que no se exige anticipo
 * @param minimoMediosPago           mínimo de la Ficha del curso, configurable (ALO-06 · MPAG-01)
 * @param minimoServiciosAdicionales mínimo de la Ficha del curso, configurable (ALO-06 · SERV-05)
 * @param minimoTemporadas           temporadas específicas exigidas, además de la base, para activar
 *                                   apartamentos (TEM-04 · DEC-28)
 * @param minimoTramosCancelacion    tramos que debe tener cada versión de la política de cancelación; mínimo de la
 *                                   Ficha del curso, configurable (POL-01 · DEC-41)
 * @param pagoEnLinea                cuánto se paga al reservar desde el portal (DEC-69)
 * @param plazoPagoEnLinea           lo que tiene una reserva del portal para pagarse; si no, vence (DEC-69)
 * @param minimoCapacidadesDistintas capacidades distintas que deben tener los apartamentos activos; mínimo de la
 *                                   Ficha del curso, configurable (CAP-05 · DEC-51)
 * @param cupoParqueadero            vehículos que caben por noche en todo el alojamiento (RP-02)
 * @param aceptaMascotas             si el alojamiento recibe mascotas (L-16 · RP-03)
 * @param limiteMascotas             mascotas por reserva, si se aceptan (RP-03)
 * @param cargoMascota               lo que se cobra por cada mascota, una vez por estancia (RP-03)
 * @param cobroParqueadero           si los precios del parqueadero son por noche o por estadía (DEC-89)
 * @param cargoCarro                 lo que se cobra por cada carro; 0 si no se cobra (DEC-89)
 * @param cargoMoto                  lo que se cobra por cada moto; 0 si no se cobra (DEC-89)
 */
public record ParametrosAlojamiento(int umbralEdadFacturable, LocalTime horaEntrada, LocalTime horaSalida,
                                    Duration tiempoPreparacion, Duration plazoConfirmacion,
                                    LocalTime horaLimiteNoShow, Porcentaje anticipo, int minimoMediosPago,
                                    int minimoServiciosAdicionales, int minimoTemporadas,
                                    int minimoTramosCancelacion, int minimoCapacidadesDistintas,
                                    ModoPagoEnLinea pagoEnLinea, Duration plazoPagoEnLinea, int cupoParqueadero,
                                    boolean aceptaMascotas, int limiteMascotas, Dinero cargoMascota,
                                    CobroParqueadero cobroParqueadero, Dinero cargoCarro, Dinero cargoMoto) {

    // ALO-02 · ALO-03 · ALO-06 · TPRE-01 · TEM-04 · POL-01 · CAP-05
    public ParametrosAlojamiento {
        if (horaEntrada == null || horaSalida == null || horaLimiteNoShow == null) {
            throw new ReglaDominioException("Las horas de entrada, salida y límite de no-show son obligatorias");
        }
        if (umbralEdadFacturable < 0) {
            throw new ReglaDominioException("El umbral de edad facturable no puede ser negativo");
        }
        if (tiempoPreparacion == null || tiempoPreparacion.isNegative() || tiempoPreparacion.toSeconds() % 3600 != 0) {
            throw new ReglaDominioException("El tiempo de preparación debe ser un número entero de horas ≥ 0");
        }
        if (plazoConfirmacion == null || plazoConfirmacion.isNegative() || plazoConfirmacion.isZero()) {
            throw new ReglaDominioException("El plazo de confirmación debe ser mayor que cero");
        }
        if (anticipo == null) {
            throw new ReglaDominioException("El anticipo es obligatorio (0 si no se exige)");
        }
        if (minimoMediosPago < 0 || minimoServiciosAdicionales < 0 || minimoTemporadas < 0
                || minimoCapacidadesDistintas < 0) {
            throw new ReglaDominioException(
                    "Los mínimos de medios de pago, servicios, temporadas y capacidades no pueden ser negativos");
        }
        if (minimoTramosCancelacion < 1) {
            throw new ReglaDominioException("La política de cancelación necesita al menos un tramo");
        }
        if (pagoEnLinea == null) {
            throw new ReglaDominioException("Falta cuánto se paga en línea: el anticipo o el total");
        }
        if (plazoPagoEnLinea == null || plazoPagoEnLinea.isNegative() || plazoPagoEnLinea.isZero()) {
            throw new ReglaDominioException("El plazo para pagar en línea debe ser mayor que cero");
        }
        if (cupoParqueadero < 0 || limiteMascotas < 0) {
            throw new ReglaDominioException("El cupo de parqueadero y el límite de mascotas no pueden ser negativos");
        }
        if (cargoMascota == null) {
            throw new ReglaDominioException("El cargo por mascota es obligatorio (0 si no se cobra)");
        }
        if (cobroParqueadero == null || cargoCarro == null || cargoMoto == null) {
            throw new ReglaDominioException("Falta cómo se cobra el parqueadero y su precio (0 si no se cobra)");
        }
    }

    /** Con parqueadero gratis, para lo que no usa los precios del parqueadero. */
    public ParametrosAlojamiento(int umbralEdadFacturable, LocalTime horaEntrada, LocalTime horaSalida,
                                 Duration tiempoPreparacion, Duration plazoConfirmacion, LocalTime horaLimiteNoShow,
                                 Porcentaje anticipo, int minimoMediosPago, int minimoServiciosAdicionales,
                                 int minimoTemporadas, int minimoTramosCancelacion, int minimoCapacidadesDistintas,
                                 ModoPagoEnLinea pagoEnLinea, Duration plazoPagoEnLinea, int cupoParqueadero,
                                 boolean aceptaMascotas, int limiteMascotas, Dinero cargoMascota) {
        this(umbralEdadFacturable, horaEntrada, horaSalida, tiempoPreparacion, plazoConfirmacion, horaLimiteNoShow,
                anticipo, minimoMediosPago, minimoServiciosAdicionales, minimoTemporadas, minimoTramosCancelacion,
                minimoCapacidadesDistintas, pagoEnLinea, plazoPagoEnLinea, cupoParqueadero, aceptaMascotas,
                limiteMascotas, cargoMascota, CobroParqueadero.POR_NOCHE, Dinero.CERO, Dinero.CERO);
    }

    /** Sin parqueadero ni mascotas, para lo que no usa esas reglas. */
    public ParametrosAlojamiento(int umbralEdadFacturable, LocalTime horaEntrada, LocalTime horaSalida,
                                 Duration tiempoPreparacion, Duration plazoConfirmacion, LocalTime horaLimiteNoShow,
                                 Porcentaje anticipo, int minimoMediosPago, int minimoServiciosAdicionales,
                                 int minimoTemporadas, int minimoTramosCancelacion, int minimoCapacidadesDistintas,
                                 ModoPagoEnLinea pagoEnLinea, Duration plazoPagoEnLinea) {
        this(umbralEdadFacturable, horaEntrada, horaSalida, tiempoPreparacion, plazoConfirmacion, horaLimiteNoShow,
                anticipo, minimoMediosPago, minimoServiciosAdicionales, minimoTemporadas, minimoTramosCancelacion,
                minimoCapacidadesDistintas, pagoEnLinea, plazoPagoEnLinea, 0, false, 0, Dinero.CERO);
    }

    /**
     * RP-03 · L-16: se rechaza declarar mascotas si no se aceptan o si superan el límite.
     *
     * @throws ReglaDominioException si la cantidad es negativa, no se aceptan o supera el límite
     */
    public void exigirMascotas(int mascotas) {
        exigirMascotas(mascotas, limiteMascotas, "El alojamiento");
    }

    /**
     * RP-03 · DEC-93: como {@link #exigirMascotas(int)}, con el límite del apartamento si tiene uno propio.
     *
     * @throws ReglaDominioException si la cantidad es negativa, el alojamiento no las acepta o supera el límite
     */
    public void exigirMascotas(int mascotas, Apartamento apartamento) {
        exigirMascotas(mascotas, limiteMascotasPara(apartamento), "El apartamento " + apartamento.codigo().valor());
    }

    /**
     * RP-03 · DEC-93: las mascotas que admite una reserva de ese apartamento: ninguna si el alojamiento no las acepta;
     * si las acepta, el límite propio del apartamento o, si no tiene, el del alojamiento.
     */
    public int limiteMascotasPara(Apartamento apartamento) {
        return aceptaMascotas ? apartamento.limiteMascotas().orElse(limiteMascotas) : 0;
    }

    private void exigirMascotas(int mascotas, int limite, String quien) {
        if (mascotas < 0) {
            throw new ReglaDominioException("Las mascotas de la reserva no pueden ser negativas");
        }
        if (mascotas > 0 && !aceptaMascotas) {
            throw new ReglaDominioException(MotivoRechazo.MASCOTAS_NO_ADMITIDAS, "El alojamiento no acepta mascotas");
        }
        if (mascotas > limite) {
            throw new ReglaDominioException(MotivoRechazo.MASCOTAS_NO_ADMITIDAS,
                    quien + " admite máximo " + limite + " mascotas por reserva");
        }
    }

    /** RP-02 · RP-03 · DEC-89: lo que suman a la estancia los vehículos y las mascotas de una reserva. */
    public Extras calcularExtras(int carros, int motos, int mascotas, int noches) {
        Dinero porCarros = cobroParqueadero.cargoDeUnVehiculo(cargoCarro, noches).multiplicar(carros);
        Dinero porMotos = cobroParqueadero.cargoDeUnVehiculo(cargoMoto, noches).multiplicar(motos);
        return new Extras(porCarros.sumar(porMotos), cargoMascota.multiplicar(mascotas));
    }

    /**
     * El plazo para confirmar una reserva PENDIENTE antes de que venza (RN-21): la del portal se paga en línea
     * y tiene el plazo del pago; las demás, el de confirmación (DEC-69).
     */
    public Duration plazoParaConfirmar(CanalOrigen canal) {
        return canal == CanalOrigen.PORTAL ? plazoPagoEnLinea : plazoConfirmacion;
    }

    /** El más corto de los dos plazos: antes de él no vence ninguna reserva. */
    public Duration plazoMasCorto() {
        return plazoPagoEnLinea.compareTo(plazoConfirmacion) < 0 ? plazoPagoEnLinea : plazoConfirmacion;
    }
}
