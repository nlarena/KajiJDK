package javax.net.ssl;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Varios almacenes de claves, cada uno con su forma de conseguir la contrasena.
 *
 * <h2>Que resuelve un {@code KeyStore.Builder}</h2>
 *
 * <p>Que la contrasena no siempre esta disponible cuando se configura. Un
 * {@link KeyStore.Builder} difiere la apertura: la fabrica lo abre cuando de verdad necesita la
 * clave, y recien ahi se pide la contrasena — a una tarjeta, a un usuario, a un servicio.
 *
 * <p>Que sean <em>varios</em> permite lo otro: presentar certificados de origenes distintos en la
 * misma conexion, uno por hardware y otro por archivo.
 */
public class KeyStoreBuilderParameters implements ManagerFactoryParameters {

    private final List<KeyStore.Builder> parameters;

    /** Con un solo almacen. */
    public KeyStoreBuilderParameters(KeyStore.Builder builder) {
        List<KeyStore.Builder> uno = new ArrayList<KeyStore.Builder>();
        uno.add(builder);
        this.parameters = Collections.unmodifiableList(uno);
    }

    /**
     * Con varios, en orden de preferencia.
     *
     * @throws IllegalArgumentException si la lista viene vacia
     */
    public KeyStoreBuilderParameters(List<KeyStore.Builder> parameters) {
        if (parameters.isEmpty()) {
            throw new IllegalArgumentException("la lista de builders esta vacia");
        }
        this.parameters =
                Collections.unmodifiableList(new ArrayList<KeyStore.Builder>(parameters));
    }

    /** Los almacenes, en una lista inmodificable. */
    public List<KeyStore.Builder> getParameters() {
        return this.parameters;
    }
}
