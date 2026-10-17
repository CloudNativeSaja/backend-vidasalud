package cl.duoc.vidasalud.notify;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifica que el contexto (topologia, propiedades y listeners) se construye.
 * Los listeners no se inician para no requerir un RabbitMQ en el build.
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class MsVidasaludNotifyApplicationTests {

	@Test
	void contextLoads() {
	}

}
