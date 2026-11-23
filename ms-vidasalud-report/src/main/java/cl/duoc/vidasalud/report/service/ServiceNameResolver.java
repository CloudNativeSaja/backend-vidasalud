package cl.duoc.vidasalud.report.service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Obtiene el nombre de una prestacion desde ms-vidasalud-catalog.
 *
 * Los eventos solo traen el serviceId; el nombre se consulta una vez y se
 * guarda en cache. Si el catalogo no responde, se usa un nombre generico y
 * se vuelve a intentar en la siguiente consulta.
 */
@Component
public class ServiceNameResolver {

    private static final Logger log = LoggerFactory.getLogger(ServiceNameResolver.class);

    private final RestClient restClient;
    private final Map<Long, String> cache = new ConcurrentHashMap<>();

    public ServiceNameResolver(@Value("${vidasalud.catalog.url}") String catalogUrl) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(2));

        this.restClient = RestClient.builder()
                .baseUrl(catalogUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public String nameOf(Long serviceId) {
        String cached = cache.get(serviceId);
        if (cached != null) {
            return cached;
        }
        try {
            ServiceResponse service = restClient.get()
                    .uri("/api/catalog/services/{id}", serviceId)
                    .retrieve()
                    .body(ServiceResponse.class);
            if (service != null && service.name() != null) {
                cache.put(serviceId, service.name());
                return service.name();
            }
        } catch (RestClientException e) {
            log.debug("No se pudo obtener el nombre de la prestacion {}: {}", serviceId, e.getMessage());
        }
        return "Prestacion #" + serviceId;
    }

    private record ServiceResponse(Long id, String name) {
    }
}
