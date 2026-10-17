package cl.duoc.vidasalud.notify.admission;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.duoc.vidasalud.notify.messaging.MessageAckHandler;

/**
 * Consumidor del dominio ADMISION: escucha q.cmd.admission.
 */
@Component
public class AdmissionCommandListener {

    private final MessageAckHandler ackHandler;
    private final AdmissionTicketService admissionService;

    public AdmissionCommandListener(MessageAckHandler ackHandler, AdmissionTicketService admissionService) {
        this.ackHandler = ackHandler;
        this.admissionService = admissionService;
    }

    @RabbitListener(queues = "${vidasalud.messaging.routes.admission.queue}")
    public void onMessage(Message message, Channel channel) {
        ackHandler.process("ADMISSION", message, channel, admissionService::process);
    }
}
