package cl.duoc.vidasalud.mqadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio administrador de RabbitMQ.
 * Expone una API REST para crear, listar y eliminar colas, exchanges y bindings.
 */
@SpringBootApplication
public class MsVidasaludMqAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsVidasaludMqAdminApplication.class, args);
	}

}
