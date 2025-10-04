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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class StockLowEventListener {
    private static final Logger log = LoggerFactory.getLogger(StockLowEventListener.class);
    private static final int MAX_RETRIES = 3;

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
            log.info("Procesando evento de stock bajo para producto ID: {}", event.getProducto_id());

            ProveedorDto proveedorDto = service.getProoveedorInfo(event.getProveedor_id());

            NotificacionAbastesimiento notificacion = new NotificacionAbastesimiento();
            notificacion.setId_producto(event.getProducto_id());
            notificacion.setMensaje("El producto " + event.getNombre_producto()
                    + " tiene stock bajo (" + event.getStock_Actual() + " unidades). "
                    + "Proveedor: " + proveedorDto.getNombre()
                    + " (ID: " + event.getProveedor_id() + ")");

            NotificacionAbastesimiento notificacionGuardada = service.create(notificacion);
            log.info("Notificación guardada con ID: {}", notificacionGuardada.getId());


            PostAutomationLowStock postAutomationLowStock = new PostAutomationLowStock(
                    notificacionGuardada.getId_producto(),
                    event.getNombre_producto(),
                    event.getStock_Actual(),
                    event.getProveedor_id(),
                    proveedorDto.getTelefono(),
                    proveedorDto.getNombre()
            );


        String webhookUrl = "https://trabajobenjamin.app.n8n.cloud/webhook-test/a39f01e8-c55b-41f6-a018-b256df004f2d";
        try {
            restTemplate.postForEntity(webhookUrl, postAutomationLowStock, Void.class);
            log.info("Webhook enviado exitosamente");
        } catch (Exception e) {
            log.error("Error enviando webhook: {}", e.getMessage());
            throw new RuntimeException("Error al enviar webhook", e);
        }



            channel.basicAck(deliveryTag, false);
            log.info("Mensaje procesado exitosamente");

        } catch (IllegalArgumentException e) {
            // Errores de validación (producto/proveedor no existe)
            log.error("Error de validación: {}", e.getMessage());
            channel.basicReject(deliveryTag, false); // No requeue

        } catch (Exception e) {
            log.error("Error inesperado: {}", e.getMessage(), e);

            Integer retryCount = (Integer) message.getMessageProperties().getHeaders().get("x-retry-count");
            if (retryCount == null) retryCount = 0;

            if (retryCount < MAX_RETRIES) {
                log.warn("Reencolando mensaje. Intento {} de {}", retryCount + 1, MAX_RETRIES);
                channel.basicNack(deliveryTag, false, true);
            } else {
                log.error("Máximo de reintentos alcanzado");
                channel.basicReject(deliveryTag, false);
            }
        }
    }
}

