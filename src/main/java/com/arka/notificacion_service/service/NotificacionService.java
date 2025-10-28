package com.arka.notificacion_service.service;

import com.arka.notificacion_service.DTO.ProveedorDto;
import com.arka.notificacion_service.exceptions.ProductNotFound;
import com.arka.notificacion_service.exceptions.ProveedorNotFound;
import com.arka.notificacion_service.model.NotificacionAbastesimiento;
import com.arka.notificacion_service.repository.NotificacionAbastesimientoRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import java.util.List;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionAbastesimientoRepository repository;
    private final RestClient catalogRestClient;
    private final RestClient usuarioRestClient;

    public NotificacionService(
            NotificacionAbastesimientoRepository repository,
            RestClient.Builder restClientBuilder) {
        this.repository = repository;
        this.catalogRestClient = restClientBuilder
                .baseUrl("http://CATALOG-SERVICE")
                .build();
        this.usuarioRestClient = restClientBuilder
                .baseUrl("http://USUARIO-SERVICE")
                .build();
    }


    @Transactional
    public NotificacionAbastesimiento create(NotificacionAbastesimiento notificacionAbastesimiento){

        if (notificacionAbastesimiento.getMensaje() == null || notificacionAbastesimiento.getMensaje().isBlank()){
            throw new IllegalArgumentException("Mensaje no puede estar vacío");
        }
        if (notificacionAbastesimiento.getId_producto() == null){
            throw new IllegalArgumentException("ID producto no puede ser null");
        }

        validarProductoExiste(notificacionAbastesimiento.getId_producto());

        NotificacionAbastesimiento saved = repository.save(notificacionAbastesimiento);
        log.info("Notificación guardada con ID: {} para producto: {}",
                saved.getId(), saved.getId_producto());

        return saved;
    }


    private void validarProductoExiste(Integer productoId) {
        try{
            catalogRestClient.get()
                    .uri("CATALOG-SERVICE")
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw new ProductNotFound("ProductNotFound");
        }
    }


    public List<NotificacionAbastesimiento> findAll(){
        return  repository.findAll();
    }

    public ProveedorDto getProoveedorInfo(Integer proveedorId){



        try{
           return usuarioRestClient.get()
                    .uri("http://USUARIO-SERVICE")
                    .retrieve()
                    .body(ProveedorDto.class);
        } catch (Exception e) {
            throw new ProveedorNotFound("Error by the time of consulting id"+e.getMessage());
        }
    }
}
