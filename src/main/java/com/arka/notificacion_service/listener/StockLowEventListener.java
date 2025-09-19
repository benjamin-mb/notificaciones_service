package com.arka.notificacion_service.listener;

import com.arka.notificacion_service.DTO.PostAutomationLowStock;
import com.arka.notificacion_service.DTO.ProductRunningLowStock;
import com.arka.notificacion_service.DTO.ProveedorDto;
import com.arka.notificacion_service.config.RabbitMQConfig;
import com.arka.notificacion_service.model.NotificacionAbastesimiento;
import com.arka.notificacion_service.service.NotificacionService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class StockLowEventListener {

    private final NotificacionService service;
    private final RestTemplate restTemplate;


    public StockLowEventListener(NotificacionService service, RestTemplate restTemplate) {
        this.service = service;
        this.restTemplate = restTemplate;
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

        ProveedorDto proveedorDto=service.getProoveedorInfo(event.getProveedor_id());

        PostAutomationLowStock postAutomationLowStock= new PostAutomationLowStock(
                notificacion.getId_producto(),
                event.getNombre_producto(),
                event.getStock_Actual(),
                event.getProveedor_id(),
                proveedorDto.getTelefono(),
                proveedorDto.getNombre()
        );

        //envio al weebhok para automation
       /* String webhookUrl="test";
        try{
            restTemplate.postForEntity(webhookUrl,postAutomationLowStock, Void.class);
        }catch (Exception e){
            throw new RuntimeException("error validating wbehook:"+e.getMessage());
        }*/
    }
}
