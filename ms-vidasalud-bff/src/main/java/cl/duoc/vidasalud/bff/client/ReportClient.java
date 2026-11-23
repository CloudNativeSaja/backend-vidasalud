package cl.duoc.vidasalud.bff.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Cliente de ms-vidasalud-report (indicadores calculados desde Kafka). */
@Component
public class ReportClient extends ForwardingClient {

    public ReportClient(
            RestClient.Builder builder,
            @Value("${services.report.url}") String baseUrl) {

        super(builder, baseUrl, "ms-vidasalud-report");
    }
}
