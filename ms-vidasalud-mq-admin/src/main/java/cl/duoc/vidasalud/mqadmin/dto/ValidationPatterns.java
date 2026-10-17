package cl.duoc.vidasalud.mqadmin.dto;

/**
 * Reglas de nombres compartidas por los DTO y el servicio.
 */
public final class ValidationPatterns {

    /**
     * Nombre de cola/exchange: 3-120 caracteres, letras, numeros, '.', '_' o '-',
     * debe empezar con letra o numero y no puede usar el prefijo reservado "amq.".
     */
    public static final String RESOURCE_NAME = "^(?!amq\\.)[A-Za-z0-9][A-Za-z0-9._-]{2,119}$";

    public static final String RESOURCE_NAME_MESSAGE =
            "debe tener 3 a 120 caracteres (letras, numeros, '.', '_' o '-'), "
            + "comenzar con letra o numero y no usar el prefijo reservado 'amq.'";

    /** Routing key / patron: letras, numeros, '.', '_', '-', '*' y '#'. Vacia permitida (fanout). */
    public static final String ROUTING_KEY = "^[A-Za-z0-9._*#-]{0,255}$";

    public static final String ROUTING_KEY_MESSAGE =
            "solo admite letras, numeros, '.', '_', '-', '*' y '#' (maximo 255)";

    private ValidationPatterns() {
    }
}
