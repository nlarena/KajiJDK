package jdk.dynalink;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Objects;

/**
 * Portador de un {@link Lookup}, con la distincion entre entregarlo afuera y usarlo adentro.
 *
 * <p>Un `Lookup` es una credencial: quien lo tiene puede alcanzar los miembros privados de la
 * clase que lo creo. Por eso hay dos accesores para el mismo campo. {@link #getLookup()} es el
 * publico, el que un enlazador de terceros llama; {@link #getLookupPrivileged()} es `protected`
 * y lo usan las subclases que ya estan del lado de adentro — {@link CallSiteDescriptor} lo
 * llama en `equals`, `hashCode` y `toString`, donde no tendria sentido pasar por el control.
 *
 * <p>Los dos son `final`: la separacion no serviria de nada si una subclase pudiera redefinir
 * cual de los dos devuelve que.
 *
 * @since 9
 */
public class SecureLookupSupplier {

    /**
     * Nombre del permiso `RuntimePermission` que historicamente guardaba {@link #getLookup()}.
     *
     * <p>Se conserva porque es API publica y `final`, aunque el `SecurityManager` este
     * deshabilitado permanentemente desde JDK 24 y el chequeo ya no ocurra.
     */
    public static final String GET_LOOKUP_PERMISSION_NAME = "dynalink.getLookup";

    private final MethodHandles.Lookup lookup;

    public SecureLookupSupplier(final MethodHandles.Lookup lookup) {
        this.lookup = Objects.requireNonNull(lookup, "lookup");
    }

    /** El lookup, para el llamador externo. */
    public final Lookup getLookup() {
        return lookup;
    }

    /** El mismo lookup, para las subclases que ya operan del lado de adentro. */
    protected final Lookup getLookupPrivileged() {
        return lookup;
    }
}
