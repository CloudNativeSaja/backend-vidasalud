package cl.duoc.vidasalud.notify.record;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.duoc.vidasalud.notify.messaging.MessageAckHandler;

/**
 * Consumidor del dominio DOCUMENTOS: escucha q.cmd.record.
 */
@Component
public class RecordCommandListener {

    private final MessageAckHandler ackHandler;
    private final RecordGenerationService recordService;

    public RecordCommandListener(MessageAckHandler ackHandler, RecordGenerationService recordService) {
        this.ackHandler = ackHandler;
        this.recordService = recordService;
    }

    @RabbitListener(queues = "${vidasalud.messaging.routes.record.queue}")
    public void onMessage(Message message, Channel channel) {
        ackHandler.process("RECORD", message, channel, recordService::generate);
    }
}
