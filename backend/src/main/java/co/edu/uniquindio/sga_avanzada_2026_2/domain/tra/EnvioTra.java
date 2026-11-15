package co.edu.uniquindio.sga_avanzada_2026_2.domain.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Raíz del agregado EnvioTra: el envío del TRA de una reserva al Ministerio (DEC-130). Uno por reserva; su id es el
 * código de la reserva. Recuerda qué ya entró (el código del huésped principal y los acompañantes enviados) para que un
 * reintento no repita nada. No guarda una copia de los datos personales enviados.
 */
public class EnvioTra {

    private final ReservaId reserva;
    private EstadoEnvioTra estado;
    private String codigoPrincipal;          // el que devolvió el Ministerio
    private final Set<OcupanteId> acompanantesEnviados;
    private int intentos;
    private LocalDateTime ultimoIntento;
    private String ultimoError;              // solo CON_ERROR
    private LocalDateTime enviadoEn;         // solo ENVIADO

    // DEC-130
    public EnvioTra(ReservaId reserva, EstadoEnvioTra estado, String codigoPrincipal,
                    Set<OcupanteId> acompanantesEnviados, int intentos, LocalDateTime ultimoIntento,
                    String ultimoError, LocalDateTime enviadoEn) {
        if (reserva == null || estado == null || acompanantesEnviados == null || intentos < 0) {
            throw new ReglaDominioException("Faltan datos del envío del TRA");
        }
        if ((estado == EstadoEnvioTra.ENVIADO) != (enviadoEn != null && codigoPrincipal != null)) {
            throw new ReglaDominioException("Solo un envío ENVIADO tiene fecha de envío y código del Ministerio");
        }
        if ((estado == EstadoEnvioTra.CON_ERROR) != (ultimoError != null)) {
            throw new ReglaDominioException("Solo un envío CON_ERROR tiene un error");
        }
        this.reserva = reserva;
        this.estado = estado;
        this.codigoPrincipal = codigoPrincipal;
        this.acompanantesEnviados = new LinkedHashSet<>(acompanantesEnviados);
        this.intentos = intentos;
        this.ultimoIntento = ultimoIntento;
        this.ultimoError = ultimoError;
        this.enviadoEn = enviadoEn;
    }

    /** El envío que deja la llegada: todavía no ha salido nada (DEC-130). */
    public static EnvioTra pendiente(ReservaId reserva) {
        return new EnvioTra(reserva, EstadoEnvioTra.PENDIENTE, null, Set.of(), 0, null, null, null);
    }

    /**
     * Empieza un intento. Lo que ya entró en uno anterior no se vuelve a enviar.
     *
     * @throws ReglaDominioException si ya se envió completo
     */
    public void iniciarIntento(LocalDateTime ahora) {
        Objects.requireNonNull(ahora, "ahora");
        exigirSinEnviar();
        intentos++;
        ultimoIntento = ahora;
    }

    /** El Ministerio recibió al huésped principal y le dio este código. */
    public void registrarPrincipal(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ReglaDominioException("El Ministerio no devolvió el código del huésped principal");
        }
        if (codigoPrincipal != null) {
            throw new ReglaDominioException("El huésped principal ya se había enviado");
        }
        codigoPrincipal = codigo.trim();
    }

    /** El Ministerio recibió a este acompañante. */
    public void registrarAcompanante(OcupanteId ocupante) {
        if (codigoPrincipal == null) {
            throw new ReglaDominioException("Un acompañante se envía después del huésped principal");
        }
        acompanantesEnviados.add(Objects.requireNonNull(ocupante, "ocupante"));
    }

    /** Ya entraron todos: queda ENVIADO. */
    public void terminar(LocalDateTime ahora) {
        if (codigoPrincipal == null) {
            throw new ReglaDominioException("No se puede terminar un envío sin el huésped principal");
        }
        estado = EstadoEnvioTra.ENVIADO;
        enviadoEn = Objects.requireNonNull(ahora, "ahora");
        ultimoError = null;
    }

    /** El intento falló: queda CON_ERROR con el motivo, y lo que ya entró se conserva. */
    public void fallar(String motivo) {
        exigirSinEnviar();
        estado = EstadoEnvioTra.CON_ERROR;
        ultimoError = motivo == null || motivo.isBlank() ? "No se pudo enviar" : recortar(motivo.trim());
    }

    public boolean principalEnviado() {
        return codigoPrincipal != null;
    }

    public boolean yaEnviado(OcupanteId ocupante) {
        return acompanantesEnviados.contains(ocupante);
    }

    /** Los acompañantes del reporte que todavía no entraron: un reintento solo envía estos. */
    public List<HuespedTra> acompanantesPorEnviar(ReporteTra reporte) {
        return reporte.acompanantes().stream().filter(a -> !yaEnviado(a.ocupante())).toList();
    }

    /**
     * @throws ReglaDominioException si ya se envió completo
     */
    public void exigirSinEnviar() {
        if (estado == EstadoEnvioTra.ENVIADO) {
            throw new ReglaDominioException("El TRA de la reserva " + reserva.valor() + " ya se envió");
        }
    }

    private static String recortar(String texto) {
        return texto.length() <= 500 ? texto : texto.substring(0, 500);
    }

    public ReservaId reserva() {
        return reserva;
    }

    public EstadoEnvioTra estado() {
        return estado;
    }

    public String codigoPrincipal() {
        return codigoPrincipal;
    }

    public Set<OcupanteId> acompanantesEnviados() {
        return Set.copyOf(acompanantesEnviados);
    }

    public int intentos() {
        return intentos;
    }

    public LocalDateTime ultimoIntento() {
        return ultimoIntento;
    }

    public String ultimoError() {
        return ultimoError;
    }

    public LocalDateTime enviadoEn() {
        return enviadoEn;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EnvioTra otro && reserva.equals(otro.reserva);
    }

    @Override
    public int hashCode() {
        return reserva.hashCode();
    }
}
