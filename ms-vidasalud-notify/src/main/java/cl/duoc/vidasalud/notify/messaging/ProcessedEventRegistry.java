package cl.duoc.vidasalud.notify.messaging;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import cl.duoc.vidasalud.notify.config.MessagingProperties;

/**
 * Registro de eventId ya procesados (idempotencia).
 *
 * RabbitMQ garantiza entrega "at-least-once": un mismo mensaje puede llegar
 * dos veces (ej. si el consumidor cae despues de procesar y antes del ACK).
 * Si el eventId ya fue procesado, se confirma sin repetir el efecto.
 *
 * Implementacion en memoria (LRU acotado). En produccion se usaria una tabla
 * o Redis para compartir el registro entre instancias.
 */
@Component
public class ProcessedEventRegistry {

    private final Set<String> processed;

    public ProcessedEventRegistry(MessagingProperties properties) {
        int maxSize = properties.consumer().idempotencyCacheSize();
        this.processed = Collections.newSetFromMap(Collections.synchronizedMap(
                new LinkedHashMap<String, Boolean>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                        return size() > maxSize;
                    }
                }));
    }

    public boolean alreadyProcessed(String eventId) {
        return processed.contains(eventId);
    }

    public void markProcessed(String eventId) {
        processed.add(eventId);
    }
}
