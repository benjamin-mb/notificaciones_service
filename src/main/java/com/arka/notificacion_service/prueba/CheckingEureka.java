package com.arka.notificacion_service.prueba;

import com.netflix.discovery.DiscoveryClient;
import org.springframework.web.client.RestClient;

public class CheckingEureka {
    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private String uri;

    public CheckingEureka(DiscoveryClient discoveryClient, RestClient.Builder restClient) {
        this.discoveryClient = discoveryClient;
        this.restClient = restClient.build();
    }


}
