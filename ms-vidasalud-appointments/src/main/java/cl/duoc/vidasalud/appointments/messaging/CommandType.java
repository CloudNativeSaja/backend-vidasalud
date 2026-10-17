package cl.duoc.vidasalud.appointments.messaging;

/**
 * Tipos de comando que genera el ciclo de vida de una atencion.
 */
public enum CommandType {

    // q.cmd.email
    EMAIL_SOLICITUD_RECIBIDA,
    EMAIL_CITA_CONFIRMADA,
    EMAIL_LLAMADO_BOX,
    EMAIL_ATENCION_CERRADA,
    EMAIL_CITA_CANCELADA,

    // q.cmd.admission
    ADMISSION_TICKET,
    ADMISSION_LLAMADO_BOX,

    // q.cmd.record
    RECORD_COMPROBANTE,
    RECORD_RESUMEN
}
