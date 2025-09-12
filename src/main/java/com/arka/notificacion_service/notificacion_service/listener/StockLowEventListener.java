package com.arka.notificacion_service.notificacion_service.listener;

import com.arka.notificacion_service.notificacion_service.DTO.ProductRunningLowStock;
import com.arka.notificacion_service.notificacion_service.config.RabbitMQConfig;
import com.arka.notificacion_service.notificacion_service.model.NotificacionAbastesimiento;
import com.arka.notificacion_service.notificacion_service.service.NotificacionService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class StockLowEventListener {

    private final NotificacionService service;


    public StockLowEventListener(NotificacionService service, RestTemplate restTemplate) {
        this.service = service;

    }



    @RabbitListener(queues = RabbitMQConfig.STOCK_LOW_QUEUE)
    public void handleLowStockEvent( ProductRunningLowStock event) {

        NotificacionAbastesimiento notificacion = new NotificacionAbastesimiento();
        notificacion.setId_producto(event.getProducto_id());
        notificacion.setMensaje("El producto " + event.getNombre_producto()
                + " tiene stock bajo (" + event.getStock_Actual() + " unidades). "
                + "Proveedor ID: " + event.getProveedor_id());

        // Guardamos en BD
        service.create(notificacion);

    }
}
