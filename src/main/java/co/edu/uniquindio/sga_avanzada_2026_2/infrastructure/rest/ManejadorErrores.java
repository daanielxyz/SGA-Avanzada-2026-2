package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoDuplicadoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Traduce las excepciones a respuestas HTTP uniformes (DEC-33): formato → 400, no encontrado → 404,
 * duplicado o regla de negocio → 409. Ningún controlador captura excepciones por su cuenta.
 */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException e) {
        List<String> detalles = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "VALIDACION", "La solicitud tiene datos inválidos", detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException e) {
        return responder(HttpStatus.BAD_REQUEST, "VALIDACION", "El cuerpo de la solicitud no es un JSON válido",
                List.of());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", e.getMessage(), List.of());
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    ResponseEntity<ErrorResponse> duplicado(RecursoDuplicadoException e) {
        return responder(HttpStatus.CONFLICT, "DUPLICADO", e.getMessage(), List.of());
    }

    @ExceptionHandler(ReglaDominioException.class)
    ResponseEntity<ErrorResponse> reglaDeNegocio(ReglaDominioException e) {
        return responder(HttpStatus.CONFLICT, "REGLA_NEGOCIO", e.getMessage(), List.of());
    }

    private static ResponseEntity<ErrorResponse> responder(HttpStatus estado, String codigo, String mensaje,
                                                           List<String> detalles) {
        return ResponseEntity.status(estado).body(new ErrorResponse(estado.value(), codigo, mensaje, detalles));
    }
}
