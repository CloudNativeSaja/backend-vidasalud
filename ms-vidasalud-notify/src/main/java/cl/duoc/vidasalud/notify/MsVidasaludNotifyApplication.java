package cl.duoc.vidasalud.notify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio consumidor de comandos asincronos de VidaSalud.
 *
 * Escucha las colas q.cmd.email, q.cmd.admission y q.cmd.record,
 * confirma (ACK) cada mensaje procesado y envia a la DLQ los que fallan.
 */
@SpringBootApplication
public class MsVidasaludNotifyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsVidasaludNotifyApplication.class, args);
	}

}
