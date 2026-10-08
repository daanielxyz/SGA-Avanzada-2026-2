package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Raíz del agregado Canal de venta. Uno externo se autentica con su propia credencial (CAN-01 · CAN-03), que nunca
 * se guarda aquí: solo su referencia en la configuración externa (CAN-02 · DEC-47).
 */
public class Canal {

    private final CanalId id;
    private final AlojamientoId alojamientoId; // DEC-26
    private final String nombre;
    private final CanalOrigen tipo;
    private final String referenciaCredencial; // solo EXTERNO: nombre del secreto en la configuración, no el secreto
    private boolean activo;

    // CAN-01 · CAN-02
    public Canal(CanalId id, AlojamientoId alojamientoId, String nombre, CanalOrigen tipo,
                 String referenciaCredencial, boolean activo) {
        if (id == null || alojamientoId == null || tipo == null) {
            throw new ReglaDominioException("El canal requiere id, alojamiento y tipo");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre del canal es obligatorio");
        }
        boolean tieneReferencia = referenciaCredencial != null && !referenciaCredencial.isBlank();
        if ((tipo == CanalOrigen.EXTERNO) != tieneReferencia) {
            throw new ReglaDominioException("Solo un canal EXTERNO tiene credencial, y siempre la tiene");
        }
        this.id = id;
        this.alojamientoId = alojamientoId;
        this.nombre = nombre.trim();
        this.tipo = tipo;
        this.referenciaCredencial = tieneReferencia ? referenciaCredencial.trim() : null;
        this.activo = activo;
    }

    /**
     * Desactiva el canal: deja de crear reservas nuevas y sus reservas históricas se conservan y siguen en los
     * reportes (CAN-05 · CU-54). Que solo lo haga el administrador (CAN-06) se controla en la aplicación (DEC-22).
     *
     * @throws ReglaDominioException si ya estaba inactivo
     */
    public void desactivar() {
        if (!activo) {
            throw new ReglaDominioException("El canal " + id.valor() + " ya está inactivo");
        }
        activo = false;
    }

    /**
     * Exige que el canal pueda traer reservas externas: debe ser EXTERNO y estar activo (CAN-05 · CORI-03).
     *
     * @throws ReglaDominioException si no es externo o está inactivo
     */
    public void exigirReservasExternas() {
        if (tipo != CanalOrigen.EXTERNO) {
            throw new ReglaDominioException("El canal " + id.valor() + " no es un canal externo");
        }
        if (!activo) {
            throw new ReglaDominioException("El canal " + id.valor() + " está inactivo y no crea reservas nuevas");
        }
    }

    public CanalId id() {
        return id;
    }

    public AlojamientoId alojamientoId() {
        return alojamientoId;
    }

    public String nombre() {
        return nombre;
    }

    public CanalOrigen tipo() {
        return tipo;
    }

    public String referenciaCredencial() {
        return referenciaCredencial;
    }

    public boolean activo() {
        return activo;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Canal otro && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
