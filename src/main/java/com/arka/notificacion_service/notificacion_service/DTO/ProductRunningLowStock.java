package com.arka.notificacion_service.notificacion_service.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProductRunningLowStock {
    private Integer producto_id;
    private String nombre_producto;
    private Integer stock_Actual;
    private Integer proveedor_id;

}
