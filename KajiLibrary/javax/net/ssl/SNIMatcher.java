package javax.net.ssl;

/**
 * El criterio con el que un servidor acepta o rechaza un nombre SNI que le mandaron.
 *
 * <p>Es una clase y no un predicado suelto porque lleva el <strong>tipo</strong> que sabe examinar:
 * un matcher solo se consulta para nombres de su mismo tipo, y sin ese dato el servidor tendria que
 * pasarle todo a todos.
 *
 * <p>Rechazar no es un detalle de configuracion: si ningun matcher acepta, el servidor corta el
 * handshake. Es la forma de que un servidor solo atienda los nombres que realmente sirve.
 */
public abstract class SNIMatcher {

    private final int type;

    /**
     * @throws IllegalArgumentException si el tipo no entra en un byte sin signo
     */
    protected SNIMatcher(int type) {
        if (type < 0 || type > 255) {
            throw new IllegalArgumentException("tipo fuera de rango: " + String.valueOf(type));
        }
        this.type = type;
    }

    /** El tipo de nombre que este matcher examina. */
    public final int getType() {
        return this.type;
    }

    /** Si el nombre es aceptable. */
    public abstract boolean matches(SNIServerName serverName);
}
