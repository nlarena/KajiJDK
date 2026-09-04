package javax.management.remote;

import java.io.Serializable;
import java.security.Principal;

/**
 * KajiLibrary's javax.management.remote.JMXPrincipal -- la identidad de un cliente JMX.
 *
 * <p>Un nombre y nada mas. Lo produce un {@link JMXAuthenticator} y va adentro del {@code Subject} que
 * este devuelve; de ahi lo saca el servidor para decidir que puede hacer ese cliente.
 *
 * <p>Es inmutable, y {@link #equals} compara solo el nombre: dos instancias con el mismo nombre son la
 * misma identidad, sin importar quien las creo.
 */
public class JMXPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = -4184480100214577411L;

    /** El nombre. */
    private String name;

    /**
     * @throws NullPointerException si el nombre es null
     */
    public JMXPrincipal(String name) {
        if (name == null) {
            throw new NullPointerException("illegal null input");
        }
        this.name = name;
    }

    /** El nombre. */
    public String getName() {
        return this.name;
    }

    /** {@code "JMXPrincipal:  "} y el nombre; los dos espacios son del JDK. */
    @Override
    public String toString() {
        return "JMXPrincipal:  " + this.name;
    }

    /** Solo el nombre. */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (!(o instanceof JMXPrincipal)) {
            return false;
        }
        return this.name.equals(((JMXPrincipal) o).getName());
    }

    /** El del nombre. */
    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
