package com.arka.notificacion_service.config;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATIONS_EXCHANGE="notifications.exchange";
    public static final String STOCK_LOW_QUEUE="notifications.stock.low.queue";
    public static final String STOCK_LOW_ROUTING_KEY = "stock.low";

    @Bean
    public TopicExchange notificactionsExchange(){
        return new TopicExchange(NOTIFICATIONS_EXCHANGE);
    }

    @Bean
    public Queue lowStockQueue(){
        return new Queue(STOCK_LOW_QUEUE);
    }

    @Bean
    public Binding bindingLowStockNoti(Queue lowStockQueue,TopicExchange notificactionsExchange){
        return BindingBuilder.bind(lowStockQueue)
                .to(notificactionsExchange)
                .with(STOCK_LOW_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // RabbitTemplate usa el conversor
    @Bean
    public AmqpTemplate rabbitConvertor(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }


}
