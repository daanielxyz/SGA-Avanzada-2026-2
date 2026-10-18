package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.PagoRechazadoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoDuplicadoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.AccesoDenegadoException;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.CanalNoAutenticadoException;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.CredencialesInvalidasException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Traduce las excepciones a respuestas HTTP uniformes (DEC-33): formato → 400, credenciales → 401, rol → 403, no
 * encontrado → 404, duplicado, regla de negocio, pago rechazado o choque de concurrencia → 409. Ningún controlador
 * captura excepciones por su cuenta. El 401 y el 403 de una petición que no llega al controlador los responde
 * {@code RespuestasDeSeguridad} (DEC-67).
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

    /** Parámetros de la URL que no cumplen su validación, p. ej. una página negativa. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErrorResponse> parametroInvalido(HandlerMethodValidationException e) {
        List<String> detalles = e.getParameterValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream()
                        .map(MessageSourceResolvable::getDefaultMessage)
                        .map(mensaje -> resultado.getMethodParameter().getParameterName() + ": " + mensaje))
                .sorted()
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "VALIDACION", "La solicitud tiene datos inválidos", detalles);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> tipoInvalido(MethodArgumentTypeMismatchException e) {
        return responder(HttpStatus.BAD_REQUEST, "VALIDACION", "La solicitud tiene datos inválidos",
                List.of(e.getName() + ": tiene un formato inválido"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> parametroFaltante(MissingServletRequestParameterException e) {
        return responder(HttpStatus.BAD_REQUEST, "VALIDACION", "La solicitud tiene datos inválidos",
                List.of(e.getParameterName() + ": es obligatorio"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException e) {
        return responder(HttpStatus.BAD_REQUEST, "VALIDACION", "El cuerpo de la solicitud no es un JSON válido",
                List.of());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    ResponseEntity<ErrorResponse> credencialesInvalidas(CredencialesInvalidasException e) {
        return responder(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", e.getMessage(), List.of());
    }

    /** El mensaje de un canal externo sin su clave válida (CAN-03 · DEC-73). */
    @ExceptionHandler(CanalNoAutenticadoException.class)
    ResponseEntity<ErrorResponse> canalNoAutenticado(CanalNoAutenticadoException e) {
        return responder(HttpStatus.UNAUTHORIZED, "CANAL_NO_AUTENTICADO", e.getMessage(), List.of());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    ResponseEntity<ErrorResponse> sinPermiso(AccesoDenegadoException e) {
        return responder(HttpStatus.FORBIDDEN, "SIN_PERMISO", e.getMessage(), List.of());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", e.getMessage(), List.of());
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    ResponseEntity<ErrorResponse> duplicado(RecursoDuplicadoException e) {
        return responder(HttpStatus.CONFLICT, "DUPLICADO", e.getMessage(), List.of());
    }

    /** La pasarela no aprobó el cobro: no se registró nada y se puede intentar de nuevo (DEC-69). */
    @ExceptionHandler(PagoRechazadoException.class)
    ResponseEntity<ErrorResponse> pagoRechazado(PagoRechazadoException e) {
        return responder(HttpStatus.CONFLICT, "PAGO_RECHAZADO", e.getMessage(), List.of());
    }

    @ExceptionHandler(ReglaDominioException.class)
    ResponseEntity<ErrorResponse> reglaDeNegocio(ReglaDominioException e) {
        return responder(HttpStatus.CONFLICT, "REGLA_NEGOCIO", e.getMessage(), List.of());
    }

    /** Otro usuario guardó el mismo agregado primero: no se sobrescribe su cambio (DEC-17 · DEC-64). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ErrorResponse> concurrencia(OptimisticLockingFailureException e) {
        return responder(HttpStatus.CONFLICT, "CONCURRENCIA",
                "Otro usuario cambió este registro al mismo tiempo; vuelva a consultarlo e intente de nuevo",
                List.of());
    }

    private static ResponseEntity<ErrorResponse> responder(HttpStatus estado, String codigo, String mensaje,
                                                           List<String> detalles) {
        return ResponseEntity.status(estado).body(new ErrorResponse(estado.value(), codigo, mensaje, detalles));
    }
}
