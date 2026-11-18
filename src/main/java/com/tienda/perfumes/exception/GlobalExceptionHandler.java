package com.tienda.perfumes.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Manejo centralizado de errores para todos los controladores.
 *
 * Antes cada controlador tenía su propio try/catch y armaba el
 * Map.of("error", ...) a mano. Ahora los servicios solo lanzan
 * una excepción y esta clase la transforma en la respuesta HTTP
 * correcta, siempre con el mismo formato (ErrorResponse).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- 400: datos inválidos ----------

    // Falla de @Valid en el cuerpo JSON (@NotBlank, @Size, @Min, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError campo : ex.getBindingResult().getFieldErrors()) {
            // Si un campo tiene varios errores se conserva el primero
            errores.putIfAbsent(campo.getField(), campo.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors().forEach(global ->
                errores.putIfAbsent(global.getObjectName(), global.getDefaultMessage()));

        String primero = errores.values().stream()
                .findFirst()
                .orElse("Los datos enviados no son válidos.");

        return responder(HttpStatus.BAD_REQUEST,
                ErrorResponse.deCampos(400, primero, errores, request.getRequestURI()));
    }

    // Falla de validación en parámetros (@PathVariable, @RequestParam)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> restriccion(
            ConstraintViolationException ex, HttpServletRequest request) {

        String mensaje = ex.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .findFirst()
                .orElse("Parámetro no válido.");

        return error(HttpStatus.BAD_REQUEST, mensaje, request);
    }

    // JSON mal escrito o con tipos incorrectos (ej. "precio": "abc")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> jsonInvalido(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        return error(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es un JSON válido o tiene tipos incorrectos.",
                request);
    }

    // Parámetro de la URL con tipo incorrecto (ej. /productos/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> tipoParametro(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        return error(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' no tiene un formato válido.",
                request);
    }

    // Reglas de negocio que no se cumplen
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> argumentoInvalido(
            IllegalArgumentException ex, HttpServletRequest request) {

        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // Archivo mayor al límite de application.properties
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> archivoGrande(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {

        return error(HttpStatus.BAD_REQUEST, "La imagen supera el tamaño máximo de 5 MB.", request);
    }

    // ---------- 403: sin permisos ----------

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado(
            AccessDeniedException ex, HttpServletRequest request) {

        return error(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    // ---------- 404: no encontrado ----------

    @ExceptionHandler({RecursoNoEncontradoException.class, NoSuchElementException.class})
    public ResponseEntity<ErrorResponse> noEncontrado(
            RuntimeException ex, HttpServletRequest request) {

        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaInexistente(
            NoResourceFoundException ex, HttpServletRequest request) {

        return error(HttpStatus.NOT_FOUND, "La ruta solicitada no existe.", request);
    }

    // ---------- 405: método no permitido ----------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoPermitido(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        return error(HttpStatus.METHOD_NOT_ALLOWED,
                "El método " + ex.getMethod() + " no está permitido en esta ruta.",
                request);
    }

    // ---------- 409: conflicto con los datos ----------

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> conflicto(
            ConflictoException ex, HttpServletRequest request) {

        return error(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // Restricciones de MySQL (unique, foreign key, not null)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.warn("Violación de integridad en {}: {}",
                request.getRequestURI(), ex.getMostSpecificCause().getMessage());

        return error(HttpStatus.CONFLICT,
                "La operación no se pudo completar porque entra en conflicto con datos existentes.",
                request);
    }

    // ---------- 500: error inesperado ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(
            Exception ex, HttpServletRequest request) {

        // Otras excepciones estándar de Spring MVC ya traen su código HTTP
        // (ej. 415 tipo de contenido no soportado): se respeta ese código.
        if (ex instanceof org.springframework.web.ErrorResponse errorSpring) {
            int codigo = errorSpring.getStatusCode().value();
            String detalle = errorSpring.getBody().getDetail();
            return responder(HttpStatus.valueOf(codigo),
                    ErrorResponse.de(codigo,
                            detalle != null ? detalle : "Solicitud no válida.",
                            request.getRequestURI()));
        }

        // Se registra el detalle en la consola, pero no se expone al cliente
        log.error("Error inesperado en {}", request.getRequestURI(), ex);

        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado en el servidor.", request);
    }

    // ---------- Utilidades ----------

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status, String mensaje, HttpServletRequest request) {

        return responder(status,
                ErrorResponse.de(status.value(), mensaje, request.getRequestURI()));
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus status, ErrorResponse cuerpo) {
        return ResponseEntity.status(status).body(cuerpo);
    }
}
