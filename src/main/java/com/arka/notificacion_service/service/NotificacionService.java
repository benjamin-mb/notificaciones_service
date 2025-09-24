package com.arka.notificacion_service.service;

import com.arka.notificacion_service.DTO.ProveedorDto;
import com.arka.notificacion_service.model.NotificacionAbastesimiento;
import com.arka.notificacion_service.repository.NotificacionAbastesimientoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class NotificacionService {

    private final NotificacionAbastesimientoRepository repository;
    private final RestTemplate restTemplate;

    @Value("${catalogo.service.url}")
    private String catalogServiceUrl;

    @Value("${usuario.service.url}")
    private String userServiceUrl;

    public NotificacionService(NotificacionAbastesimientoRepository repository, RestTemplate restTemplate) {
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    public NotificacionAbastesimiento create(NotificacionAbastesimiento notificacionAbastesimiento){

        String url= catalogServiceUrl+"/api/productos/id/"+notificacionAbastesimiento.getId_producto();

        if (notificacionAbastesimiento.getMensaje()==null || notificacionAbastesimiento.getMensaje().isBlank()){
            throw new IllegalArgumentException("mensaje can not be blank");
        }
        if (notificacionAbastesimiento.getId_producto()==null){
            throw new IllegalArgumentException("id producto can not be blank");
        }

        try{
            ResponseEntity<String>response=restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful()){
                return null;
            }
        } catch (HttpServerErrorException se) {
            throw se;
        } catch (HttpClientErrorException.NotFound nf){
            return null;
        } catch (HttpClientErrorException ce){
            return null;
        } catch (RestClientException re){
            throw re;
        }

        return repository.save(notificacionAbastesimiento);
    }


    public NotificacionAbastesimiento findById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found by id " + id));
    }


    public List<NotificacionAbastesimiento> findAll(){
        return  repository.findAll();
    }

    public ProveedorDto getProoveedorInfo(Integer proveedorId){
        String url=userServiceUrl+"/api/proovedores/"+proveedorId;

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
            throw new RuntimeException("Error by the time of consulting id"+e.getMessage());
        }
    }
}
