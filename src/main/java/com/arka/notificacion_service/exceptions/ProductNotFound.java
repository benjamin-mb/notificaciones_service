package com.arka.notificacion_service.exceptions;

import org.springframework.http.HttpStatus;

public class ProductNotFound extends RuntimeException {
  public ProductNotFound(String message) {
    super(message+ HttpStatus.NOT_FOUND);
  }
}
