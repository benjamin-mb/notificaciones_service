package com.arka.notificacion_service.notificacion_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notificacion_abastecimiento")
@Data
@NoArgsConstructor
public class NotificacionAbastesimiento {

    @Id
    @Column(name = "id_notificacion")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "mensaje", columnDefinition = "TEXT",nullable = false)
    private String mensaje;

    @Column(name = "id_producto",nullable = false)
    private Integer id_producto;

    public NotificacionAbastesimiento(String mensaje, Integer id_producto) {
        this.mensaje = mensaje;
        this.id_producto = id_producto;
    }
}
