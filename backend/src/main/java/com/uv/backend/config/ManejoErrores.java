package com.uv.backend.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Convierte errores comunes de las peticiones en respuestas claras (texto),
 * en lugar del error generico de Spring.
 */
@RestControllerAdvice
public class ManejoErrores {

    // Archivo de entrega mayor al limite (spring.servlet.multipart.max-file-size)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> archivoMuyGrande(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body("El archivo es demasiado grande (máximo 20 MB)");
    }

    // JSON mal formado o con un tipo de dato incorrecto (ej. una fecha invalida)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> jsonInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body("Los datos enviados no tienen el formato correcto");
    }

    // Falta un parametro obligatorio en la URL (ej. ?idUsuario=)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<String> faltaParametro(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest().body("Falta el parámetro " + e.getParameterName());
    }

    // Se intento borrar algo que otros registros todavia usan (ej. un usuario con cursos o entregas).
    // Antes regresaba 500.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> registroEnUso(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("No se puede completar: el registro tiene información relacionada (cursos, entregas, comentarios, etc.)");
    }

    // Un parametro de la URL tiene un tipo incorrecto (ej. /tareas/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body("El parámetro " + e.getName() + " no es válido");
    }
}
