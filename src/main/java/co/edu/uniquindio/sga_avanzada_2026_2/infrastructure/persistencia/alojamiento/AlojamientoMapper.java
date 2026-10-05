package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicionalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Ubicacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;

import java.time.Duration;
import java.util.List;

/**
 * Convierte Alojamiento ⇄ AlojamientoJpa. Al cargar usa el constructor de reconstrucción del dominio, que
 * vuelve a validar las invariantes (DEC-06). Las duraciones se guardan en minutos (DEC-16).
 */
final class AlojamientoMapper {

    private AlojamientoMapper() {
    }

    static Alojamiento aDominio(AlojamientoJpa jpa) {
        ParametrosAlojamiento parametros = new ParametrosAlojamiento(
                jpa.getUmbralEdadFacturable(),
                jpa.getHoraEntrada(),
                jpa.getHoraSalida(),
                Duration.ofMinutes(jpa.getTiempoPreparacionMin()),
                Duration.ofMinutes(jpa.getPlazoConfirmacionMin()),
                jpa.getHoraLimiteNoShow(),
                new Porcentaje(jpa.getAnticipoPct()),
                jpa.getMinimoMediosPago(),
                jpa.getMinimoServiciosAdicionales(),
                jpa.getMinimoTemporadas());
        return new Alojamiento(
                new AlojamientoId(jpa.getId()),
                jpa.getNombre(),
                jpa.getDescripcion(),
                jpa.getCiudad(),
                jpa.getDireccion(),
                new Ubicacion(jpa.getLatitud(), jpa.getLongitud()),
                jpa.getNormas(),
                parametros,
                jpa.getServiciosAdicionales().stream()
                        .map(s -> new ServicioAdicional(new ServicioAdicionalId(s.getId()), s.getNombre(),
                                s.isGeneraCargo(), new Dinero(s.getValor()), s.isActivo()))
                        .toList(),
                jpa.getMediosPago().stream().map(MedioPago::new).toList());
    }

    /**
     * Copia el estado del dominio sobre una fila nueva o ya cargada; no toca {@code version} (DEC-17).
     */
    static void copiar(Alojamiento alojamiento, AlojamientoJpa jpa) {
        ParametrosAlojamiento p = alojamiento.parametros();
        jpa.setId(alojamiento.id().valor());
        jpa.setNombre(alojamiento.nombre());
        jpa.setDescripcion(alojamiento.descripcion());
        jpa.setCiudad(alojamiento.ciudad());
        jpa.setDireccion(alojamiento.direccion());
        jpa.setLatitud(alojamiento.ubicacion().latitud());
        jpa.setLongitud(alojamiento.ubicacion().longitud());
        jpa.setNormas(alojamiento.normas());
        jpa.setUmbralEdadFacturable(p.umbralEdadFacturable());
        jpa.setHoraEntrada(p.horaEntrada());
        jpa.setHoraSalida(p.horaSalida());
        jpa.setTiempoPreparacionMin((int) p.tiempoPreparacion().toMinutes());
        jpa.setPlazoConfirmacionMin((int) p.plazoConfirmacion().toMinutes());
        jpa.setHoraLimiteNoShow(p.horaLimiteNoShow());
        jpa.setAnticipoPct(p.anticipo().valor());
        jpa.setMinimoMediosPago(p.minimoMediosPago());
        jpa.setMinimoServiciosAdicionales(p.minimoServiciosAdicionales());
        jpa.setMinimoTemporadas(p.minimoTemporadas());
        jpa.getServiciosAdicionales().clear();
        alojamiento.serviciosAdicionales().forEach(s -> jpa.getServiciosAdicionales().add(new ServicioAdicionalJpa(
                s.id().valor(), s.nombre(), s.generaCargo(), s.valor().monto(), s.activo())));
        // conjunto: se quitan solo los deshabilitados y se agregan los nuevos, sin reescribir los demás
        List<String> medios = alojamiento.mediosPago().stream().map(MedioPago::nombre).toList();
        jpa.getMediosPago().retainAll(medios);
        jpa.getMediosPago().addAll(medios);
    }
}
