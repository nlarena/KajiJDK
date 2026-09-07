package jdk.internal.event;

/**
 * La base interna de todo evento de JFR.
 *
 * <p>No es API publica —{@code jdk.internal.*} no se exporta— y existe para que las clases del JDK
 * que emiten eventos no tengan que depender del modulo {@code jdk.jfr}. {@link jdk.jfr.Event}
 * hereda de esta.
 *
 * <p>Los cuerpos estan vacios a proposito, igual que en el JDK: cuando JFR esta activo, la VM
 * <strong>reescribe</strong> estos metodos al cargar cada subclase, inyectando el codigo que
 * escribe el evento en el buffer. Sin JFR activo no hacen nada, que es exactamente lo que tienen
 * que hacer.
 */
public abstract class Event {

    /** Para las subclases. */
    protected Event() {
    }

    /** Marca el comienzo del evento. */
    public void begin() {
    }

    /** Marca el final del evento. */
    public void end() {
    }

    /** Emite el evento. */
    public void commit() {
    }

    /**
     * Si el evento esta habilitado.
     *
     * @return {@code false} mientras la VM no reescriba este metodo
     */
    public boolean isEnabled() {
        return false;
    }

    /**
     * Si el evento pasaria los filtros configurados.
     *
     * @return {@code false} mientras la VM no reescriba este metodo
     */
    public boolean shouldCommit() {
        return false;
    }

    /**
     * Fija un campo por indice.
     *
     * @param index el indice del campo
     * @param value el valor
     */
    public void set(int index, Object value) {
    }
}
