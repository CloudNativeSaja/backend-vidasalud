package cl.duoc.vidasalud.notify.email;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.duoc.vidasalud.notify.messaging.MessageAckHandler;

/**
 * Consumidor del dominio EMAIL: escucha q.cmd.email (nombre desde application.yaml).
 * La confirmacion (ACK/NACK) la decide {@link MessageAckHandler}.
 */
@Component
public class EmailCommandListener {

    private final MessageAckHandler ackHandler;
    private final EmailNotificationService emailService;

    public EmailCommandListener(MessageAckHandler ackHandler, EmailNotificationService emailService) {
        this.ackHandler = ackHandler;
        this.emailService = emailService;
    }

    @RabbitListener(queues = "${vidasalud.messaging.routes.email.queue}")
    public void onMessage(Message message, Channel channel) {
        ackHandler.process("EMAIL", message, channel, emailService::send);
    }
}
