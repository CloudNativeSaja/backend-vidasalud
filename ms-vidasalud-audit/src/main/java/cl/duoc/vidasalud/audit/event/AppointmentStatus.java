package cl.duoc.vidasalud.audit.event;

/** Estados de una cita, en el orden del flujo de atencion. */
public enum AppointmentStatus {
    SOLICITADA,
    CONFIRMADA,
    EN_ESPERA,
    EN_ATENCION,
    CERRADA,
    CANCELADA
}
