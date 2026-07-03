package com.baas.flashsale.payment.queue;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentQueueConfig {
    public static final String EXCHANGE = "flashsale.payment.exchange";
    public static final String QUEUE = "flashsale.payment.events";
    public static final String SUCCESS_ROUTING_KEY = "payment.success";
    public static final String FAILED_ROUTING_KEY = "payment.failed";

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding paymentSuccessBinding(Queue paymentQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(paymentQueue).to(paymentExchange).with(SUCCESS_ROUTING_KEY);
    }

    @Bean
    public Binding paymentFailedBinding(Queue paymentQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(paymentQueue).to(paymentExchange).with(FAILED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
