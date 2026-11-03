package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.AgregarImagenApartamentoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.MarcarImagenPrincipalUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.QuitarImagenApartamentoUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Imágenes de un apartamento, solo del administrador (CU-31). Quitar es una acción de negocio, no un {@code DELETE}.
 */
@RestController
@RequestMapping("/api/apartamentos/{codigo}/imagenes")
public class ImagenApartamentoController {

    private final AgregarImagenApartamentoUseCase agregarImagen;
    private final QuitarImagenApartamentoUseCase quitarImagen;
    private final MarcarImagenPrincipalUseCase marcarPrincipal;

    public ImagenApartamentoController(AgregarImagenApartamentoUseCase agregarImagen,
                                       QuitarImagenApartamentoUseCase quitarImagen,
                                       MarcarImagenPrincipalUseCase marcarPrincipal) {
        this.agregarImagen = agregarImagen;
        this.quitarImagen = quitarImagen;
        this.marcarPrincipal = marcarPrincipal;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApartamentoResponse> agregar(@PathVariable String codigo,
                                                       @Valid @RequestBody ImagenApartamentoRequest solicitud) {
        return ResponseEntity.created(URI.create("/api/apartamentos/" + codigo))
                .body(ApartamentoResponse.de(agregarImagen.ejecutar(solicitud.aCommand(codigo))));
    }

    @PutMapping("/quitar")
    public ApartamentoResponse quitar(@PathVariable String codigo,
                                      @Valid @RequestBody ImagenApartamentoRequest solicitud) {
        return ApartamentoResponse.de(quitarImagen.ejecutar(solicitud.aCommand(codigo)));
    }

    @PutMapping("/principal")
    public ApartamentoResponse marcarPrincipal(@PathVariable String codigo,
                                               @Valid @RequestBody ImagenApartamentoRequest solicitud) {
        return ApartamentoResponse.de(marcarPrincipal.ejecutar(solicitud.aCommand(codigo)));
    }
}
