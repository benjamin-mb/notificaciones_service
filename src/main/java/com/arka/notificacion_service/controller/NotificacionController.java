package com.arka.notificacion_service.controller;

import com.arka.notificacion_service.model.NotificacionAbastesimiento;
import com.arka.notificacion_service.service.NotificacionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService service;

    public NotificacionController(NotificacionService service) {
        this.service = service;
    }


    @GetMapping
    public ResponseEntity<List<NotificacionAbastesimiento>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

}
