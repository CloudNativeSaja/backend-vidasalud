package cl.duoc.vidasalud.report.dto;

import java.time.Duration;
import java.util.Arrays;

import cl.duoc.vidasalud.report.exception.InvalidRequestException;

/** Rangos de tiempo que acepta la API (los mismos que usa el frontend). */
public enum ReportRange {

    LAST_24H("last24h", Duration.ofHours(24)),
    LAST_7D("last7d", Duration.ofDays(7)),
    LAST_30D("last30d", Duration.ofDays(30));

    private final String value;
    private final Duration duration;

    ReportRange(String value, Duration duration) {
        this.value = value;
        this.duration = duration;
    }

    public String value() {
        return value;
    }

    public Duration duration() {
        return duration;
    }

    public static ReportRange from(String value) {
        return Arrays.stream(values())
                .filter(range -> range.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Rango invalido '" + value + "'. Use last24h, last7d o last30d"));
    }
}
