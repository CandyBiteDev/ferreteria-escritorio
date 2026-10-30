package com.umg.ferreteria.api;

import com.umg.ferreteria.excepcion.FerreteriaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Convierte las excepciones propias en respuestas JSON {"error": "..."} con su código HTTP. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(FerreteriaException.class)
    public ResponseEntity<Map<String, String>> propia(FerreteriaException e) {
        return ResponseEntity.status(e.getCodigoHttp()).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> general(Exception e) {
        log.error("Error no controlado en la API", e);
        return ResponseEntity.status(500).body(Map.of("error", "Error interno: " + e.getMessage()));
    }
}
