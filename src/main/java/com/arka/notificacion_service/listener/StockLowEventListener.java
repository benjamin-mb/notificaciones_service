package com.arka.notificacion_service.listener;

import com.arka.notificacion_service.DTO.PostAutomationLowStock;
import com.arka.notificacion_service.DTO.ProductRunningLowStock;
import com.arka.notificacion_service.DTO.ProveedorDto;
import com.arka.notificacion_service.config.RabbitMQConfig;
import com.arka.notificacion_service.model.NotificacionAbastesimiento;
import com.arka.notificacion_service.service.NotificacionService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Component
public class StockLowEventListener {

    private final NotificacionService service;
    private final RestTemplate restTemplate;


    public StockLowEventListener(NotificacionService service, RestTemplate restTemplate) {
        this.service = service;
        this.restTemplate = restTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.STOCK_LOW_QUEUE)
    public void handleLowStockEvent(ProductRunningLowStock event, Channel channel, Message message) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            NotificacionAbastesimiento notificacion = new NotificacionAbastesimiento();
            notificacion.setId_producto(event.getProducto_id());
            notificacion.setMensaje("El producto " + event.getNombre_producto()
                    + " tiene stock bajo (" + event.getStock_Actual() + " unidades). "
                    + "Proveedor ID: " + event.getProveedor_id());

            // Guardamos en BD
            service.create(notificacion);
            channel.basicAck(deliveryTag, false);

            ProveedorDto proveedorDto = service.getProoveedorInfo(event.getProveedor_id());

            PostAutomationLowStock postAutomationLowStock = new PostAutomationLowStock(
                    notificacion.getId_producto(),
                    event.getNombre_producto(),
                    event.getStock_Actual(),
                    event.getProveedor_id(),
                    proveedorDto.getTelefono(),
                    proveedorDto.getNombre()
            );

           /*//envio al weebhok para automation
           String webhookUrl="https://trabajobenjamin.app.n8n.cloud/webhook-test/11324961-5f7b-495c-910f-3c9aa1b5ed80t";
            try{
                restTemplate.postForEntity(webhookUrl,postAutomationLowStock, Void.class);
            }catch (Exception e){
                throw new RuntimeException("error validating wbehook:"+e.getMessage());
            }  }*/
        } catch (IllegalArgumentException nf) {
            channel.basicReject(deliveryTag, false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, true);


        }
    }
}
