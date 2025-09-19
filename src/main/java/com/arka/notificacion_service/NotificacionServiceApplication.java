package com.arka.notificacion_service;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NotificacionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificacionServiceApplication.class, args);
	}

}
