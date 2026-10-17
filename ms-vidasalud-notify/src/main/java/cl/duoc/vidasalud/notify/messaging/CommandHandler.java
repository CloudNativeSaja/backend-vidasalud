package cl.duoc.vidasalud.notify.messaging;

/**
 * Logica de negocio que procesa un comando ya validado.
 *
 * Debe lanzar {@link RecoverableProcessingException} ante fallas temporales
 * o {@link InvalidCommandException} ante datos invalidos.
 */
@FunctionalInterface
public interface CommandHandler {

    void handle(CommandEnvelope command);
}
