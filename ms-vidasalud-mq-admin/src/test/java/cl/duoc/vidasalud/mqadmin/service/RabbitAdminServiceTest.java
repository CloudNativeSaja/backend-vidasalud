package cl.duoc.vidasalud.mqadmin.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import cl.duoc.vidasalud.mqadmin.client.RabbitManagementClient;
import cl.duoc.vidasalud.mqadmin.config.MqAdminProperties;
import cl.duoc.vidasalud.mqadmin.dto.CreateQueueRequest;
import cl.duoc.vidasalud.mqadmin.exception.ConflictException;
import cl.duoc.vidasalud.mqadmin.exception.InvalidRequestException;
import cl.duoc.vidasalud.mqadmin.exception.ResourceNotFoundException;
import tools.jackson.databind.json.JsonMapper;

class RabbitAdminServiceTest {

    private AmqpAdmin amqpAdmin;
    private RabbitManagementClient managementClient;
    private RabbitAdminService service;

    @BeforeEach
    void setUp() {
        amqpAdmin = mock(AmqpAdmin.class);
        managementClient = mock(RabbitManagementClient.class);
        MqAdminProperties properties = new MqAdminProperties(
                "/",
                new MqAdminProperties.Management("http://localhost:15672", "guest", "guest"),
                List.of("cmd.dead.dlx", "q.cmd.email"));

        service = new RabbitAdminService(
                amqpAdmin, mock(RabbitTemplate.class), managementClient, properties, JsonMapper.builder().build());
    }

    @Test
    void crearColaExistenteDevuelveConflicto() {
        when(amqpAdmin.getQueueInfo("q.prueba")).thenReturn(new QueueInformation("q.prueba", 0, 0));

        assertThatThrownBy(() -> service.createQueue(request("q.prueba", false)))
                .isInstanceOf(ConflictException.class);
        verify(amqpAdmin, never()).declareQueue(any(Queue.class));
    }

    @Test
    void crearColaConDlqDeclaraDosColasYUnBinding() {
        when(managementClient.getExchanges()).thenReturn(List.of(
                Map.of("name", "cmd.dead.dlx", "vhost", "/", "type", "direct", "durable", true)));

        service.createQueue(request("q.prueba", true));

        verify(amqpAdmin, times(2)).declareQueue(any(Queue.class));
        verify(amqpAdmin).declareBinding(any(Binding.class));
    }

    @Test
    void crearColaConDlqSinExchangeDlxDevuelveNoEncontrado() {
        when(managementClient.getExchanges()).thenReturn(List.of());

        assertThatThrownBy(() -> service.createQueue(request("q.prueba", true)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void eliminarColaProtegidaDevuelveConflicto() {
        assertThatThrownBy(() -> service.deleteQueue("q.cmd.email", false))
                .isInstanceOf(ConflictException.class);
        verify(amqpAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void eliminarColaConNombreInvalidoDevuelveBadRequest() {
        assertThatThrownBy(() -> service.deleteQueue("amq.interna", false))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void eliminarColaInexistenteDevuelveNoEncontrado() {
        when(managementClient.getQueues()).thenReturn(List.of());

        assertThatThrownBy(() -> service.deleteQueue("q.no-existe", false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static CreateQueueRequest request(String name, boolean withDlq) {
        return new CreateQueueRequest(name, true, "classic",
                withDlq ? "cmd.dead.dlx" : null, null, withDlq, null, null);
    }
}
