package com.arka.notificacion_service.service;

import com.arka.notificacion_service.DTO.ProveedorDto;
import com.arka.notificacion_service.model.NotificacionAbastesimiento;
import com.arka.notificacion_service.repository.NotificacionAbastesimientoRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionAbastesimientoRepository repository;
    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;

    public NotificacionService(NotificacionAbastesimientoRepository repository,
                               RestTemplate restTemplate,
                               DiscoveryClient discoveryClient) {
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.discoveryClient = discoveryClient;
    }

    @Transactional
    public NotificacionAbastesimiento create(NotificacionAbastesimiento notificacionAbastesimiento){

        // Validaciones básicas
        if (notificacionAbastesimiento.getMensaje() == null || notificacionAbastesimiento.getMensaje().isBlank()){
            throw new IllegalArgumentException("Mensaje no puede estar vacío");
        }
        if (notificacionAbastesimiento.getId_producto() == null){
            throw new IllegalArgumentException("ID producto no puede ser null");
        }

        // ✅ Validar que el producto existe
        validarProductoExiste(notificacionAbastesimiento.getId_producto());

        // ✅ Guardar en BD
        NotificacionAbastesimiento saved = repository.save(notificacionAbastesimiento);
        log.info("✅ Notificación guardada con ID: {} para producto: {}",
                saved.getId(), saved.getId_producto());

        return saved;
    }

    // ✅ Método separado para validar producto
    private void validarProductoExiste(Integer productoId) {
        List<ServiceInstance> instances = discoveryClient.getInstances("CATALOGO-SERVICE");
        if (instances.isEmpty()) {
            throw new RuntimeException("No hay instancias disponibles de CATALOGO-SERVICE");
        }

        ServiceInstance instance = instances.get(0);
        String url = instance.getUri() + "/api/productos/id/" + productoId;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalArgumentException("Producto con ID " + productoId + " no encontrado");
            }
            log.info("✅ Producto {} validado correctamente", productoId);

        } catch (HttpClientErrorException.NotFound nf) {
            log.error("❌ Producto no encontrado: {}", productoId);
            throw new IllegalArgumentException("Producto con ID " + productoId + " no existe");

        } catch (HttpClientErrorException ce) {
            log.error("❌ Error del cliente al validar producto {}: {}", productoId, ce.getMessage());
            throw new IllegalArgumentException("Error al validar producto: " + ce.getMessage());

        } catch (HttpServerErrorException se) {
            log.error("❌ Error del servidor al validar producto {}: {}", productoId, se.getMessage());
            throw new RuntimeException("Error del servidor al validar producto: " + se.getMessage());

        } catch (RestClientException re) {
            log.error("❌ Error de conexión al validar producto {}: {}", productoId, re.getMessage());
            throw new RuntimeException("Error de conexión al validar producto: " + re.getMessage());
        }
    }


    public NotificacionAbastesimiento findById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found by id " + id));
    }


    public List<NotificacionAbastesimiento> findAll(){
        return  repository.findAll();
    }

    public ProveedorDto getProoveedorInfo(Integer proveedorId){

        List<ServiceInstance>instances=discoveryClient.getInstances("USUARIO-SERVICE");
        if (instances.isEmpty()) {
            throw new IllegalArgumentException("No instances available for USUARIO-SERVICE");
        }
        ServiceInstance instance= instances.get(0);
        String url=instance.getUri()+"/api/proveedores/"+proveedorId;

        try{
            ResponseEntity<ProveedorDto> response=restTemplate.getForEntity(url, ProveedorDto.class);
            if (!response.getStatusCode().is2xxSuccessful()){
                throw new IllegalArgumentException("id "+proveedorId+
                        "was not found");
            }
            ProveedorDto proveedorDto =new ProveedorDto();
            proveedorDto.setNombre(response.getBody().getNombre());
            proveedorDto.setTelefono(response.getBody().getTelefono());
            return proveedorDto;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error by the time of consulting id"+e.getMessage());
        }
    }
}
