package com.arka.notificacion_service.notificacion_service.service;

import com.arka.notificacion_service.notificacion_service.model.NotificacionAbastesimiento;
import com.arka.notificacion_service.notificacion_service.repository.NotificacionAbastesimientoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class NotificacionService {

    private final NotificacionAbastesimientoRepository repository;
    private final RestTemplate restTemplate;

    @Value("${catalogo.service.url}")
    private String catalogServiceUrl;

    public NotificacionService(NotificacionAbastesimientoRepository repository, RestTemplate restTemplate) {
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    public NotificacionAbastesimiento create(NotificacionAbastesimiento notificacionAbastesimiento){

        String url= catalogServiceUrl+"/productos/id/"+notificacionAbastesimiento.getId_producto();

        if (notificacionAbastesimiento.getMensaje()==null || notificacionAbastesimiento.getMensaje().isBlank()){
            throw new IllegalArgumentException("mensaje can not be blank");
        }
        if (notificacionAbastesimiento.getId_producto()==null){
            throw new IllegalArgumentException("id producto can not be blank");
        }

        try{
            ResponseEntity<String>response=restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful()){
                throw new IllegalArgumentException("id "+notificacionAbastesimiento.getId()+
                        "was not found");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error by the time of consulting id"+e.getMessage());
        }

        return repository.save(notificacionAbastesimiento);
    }
}
