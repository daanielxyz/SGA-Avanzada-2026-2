package co.edu.uniquindio.sga_avanzada_2026_2.domain.service;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Bloqueo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.repository.ReservaRepository;

import java.util.List;

public class DisponibilidadApartamentoService {


    private final ReservaRepository reservaRepository;
    private final BloqueoRepository bloqueoRepository;

    public DisponibilidadApartamentoService(ReservaRepository reservaRepository, BloqueoRepository bloqueoRepository) {
        this.reservaRepository = reservaRepository;
        this.bloqueoRepository = bloqueoRepository;
    }

    public void verificarDisponibilidad(Apartamento apartamento, Estancia estancia,int totalOcupantes) {

        //RN-02
        if(!apartamento.admite(totalOcupantes)){
            throw new ReglaDominioException("El numero de ocupantes excede a la capacidad del apartaento");
        }
        if(!apartamento.estaActivo())throw new ReglaDominioException("El apartamento no esta disponible para una reserva");

        List<Reserva> reservasActivas=reservaRepository.buscarActivasByApartamento(apartamento.getIdentificacion());

        for(Reserva otraReserva:reservasActivas){

            //RN-01
            if(otraReserva.seSolapaCon(estancia)){
                    throw new ReglaDominioException("El apartamento ya esta reservado para esas fechas");
            }
            //RN-20

        }

        //RN-07
        List<Bloqueo> bloqueos=bloqueoRepository.buscarVigentesByApartamento(apartamento.getIdentificacion());
        for(Bloqueo otroBloqueo:bloqueos){
            if(bloqueo.impide(estancia)) throw new ReglaDominioException("El apartamento tiene un bloqueo en esas fechas");
        }
    }
}
