package co.edu.uniquindio.sga_avanzada_2026_2.domain.entity;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.FechaCreacion;

import java.util.List;
import java.util.Objects;

public class Reserva {

    //Atributos
    private final CodigoReserva codigo; //Identificador
    private final CanalOrigen canalOrigen;
    private final FechaCreacion fechaCreacion;
    private final Ocupante titular;

    //Elementos cambiantes
    private EstadoReserva estado;
    private Apartamento apartamento;
    private Estancia estancia;
    private List<Ocupante> ocupantes;


    private Reserva(CodigoReserva codigo, CanalOrigen canalOrigen,
                   Ocupante titular, Estancia estancia,
                   EstadoReserva estado, List<Ocupante> ocupantes,
                   FechaCreacion fechaCreacion, Apartamento apartamento) {
        this.codigo = codigo;
        this.canalOrigen = canalOrigen;
        this.titular = titular;
        this.estancia = estancia;
        this.ocupantes = ocupantes;
        this.fechaCreacion = fechaCreacion;
        this.apartamento = apartamento;

        estado = EstadoReserva.PENDIENTE;
    }

    public Reserva crearReserva(CodigoReserva codigo, CanalOrigen canalOrigen,
                                Ocupante titular, Estancia estancia,
                                EstadoReserva estado, List<Ocupante> ocupantes,
                                FechaCreacion fechaCreacion, Apartamento apartamento) {
        if(codigo==null){
            throw new ReglaDominioException("La reserva debe contener un codigo");
        }
        if(canalOrigen==null){
            throw new ReglaDominioException("La reserva debe contener un canal origen");
        }
        if(ocupantes==null || ocupantes.isEmpty()){
            throw new ReglaDominioException("La reserva debe contener un ocupante");
        }
        //terminar validaciones de negocio

        return new Reserva( codigo,  canalOrigen, titular,  estancia,
                 estado,  ocupantes, fechaCreacion,  apartamento);
    }

    //Reglas restricciones del negocio propios de reserva

    public Apartamento getApartamento() {
        return apartamento;
    }

    public CanalOrigen getCanalOrigen() {
        return canalOrigen;
    }

    public CodigoReserva getCodigo() {
        return codigo;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public Estancia getEstancia() {
        return estancia;
    }

    public FechaCreacion getFechaCreacion() {
        return fechaCreacion;
    }

    public List<Ocupante> getOcupantes() {
        return ocupantes;
    }

    public Ocupante getTitular() {
        return titular;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Reserva reserva = (Reserva) o;
        return Objects.equals(codigo, reserva.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(codigo);
    }
}
