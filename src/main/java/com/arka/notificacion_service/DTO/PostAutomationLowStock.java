package com.arka.notificacion_service.DTO;

import jakarta.annotation.security.DeclareRoles;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostAutomationLowStock {
    private Integer producto_id;
    private String nombre_producto;
    private Integer stock_Actual;
    private Integer proveedor_id;
    private String proovedor_telefono;
    private String nombre_proveedor;
}

