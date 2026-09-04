package javax.accessibility;

/**
 * Un proveedor de tecnología de asistencia que se enchufa por servicio.
 *
 * <p>Se descubre con el mecanismo de servicios de la plataforma, así que una ayuda técnica se instala
 * poniéndose en el camino de clases y no tocando la aplicación.
 *
 * <p>El constructor es protegido a propósito: la clase se instancia por el cargador de servicios, no
 * por quien la use.
 */
public abstract class AccessibilityProvider {

    /** Para las subclases. */
    protected AccessibilityProvider() {
    }

    /** Cómo se llama este proveedor. */
    public abstract String getName();

    /** Lo pone en funcionamiento. */
    public abstract void activate();
}
