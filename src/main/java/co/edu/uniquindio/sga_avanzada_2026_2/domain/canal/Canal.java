package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

/**
 * Raíz del agregado Canal.
 */
public class Canal {

    private final CanalId id;
    private String nombre;
    private final CanalOrigen tipo;
    private String credencial; // externa, no versionada
    private boolean activo;

    public Canal(CanalId id, String nombre, CanalOrigen tipo, String credencial, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.credencial = credencial;
        this.activo = activo;
    }
}
