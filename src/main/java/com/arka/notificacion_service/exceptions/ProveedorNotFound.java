package com.arka.notificacion_service.exceptions;

import org.springframework.http.HttpStatus;

public class ProveedorNotFound extends RuntimeException {
    public ProveedorNotFound(String message) {
        super(message+ HttpStatus.NOT_FOUND);
    }
}
