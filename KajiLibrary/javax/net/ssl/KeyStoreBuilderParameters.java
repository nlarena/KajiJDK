package javax.net.ssl;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Several key stores, each with its own way of getting the password.
 *
 * <h2>What a {@code KeyStore.Builder} solves</h2>
 *
 * <p>That the password is not always available when configuring. A {@link KeyStore.Builder} defers
 * the opening: the factory opens it when it really needs the key, and only then is the password
 * asked for — to a card, to a user, to a service.
 *
 * <p>Their being <em>several</em> allows the other thing: presenting certificates from different
 * origins on the same connection, one from hardware and another from a file.
 */
public class KeyStoreBuilderParameters implements ManagerFactoryParameters {

    private final List<KeyStore.Builder> parameters;

    /** With a single store. */
    public KeyStoreBuilderParameters(KeyStore.Builder builder) {
        List<KeyStore.Builder> single = new ArrayList<KeyStore.Builder>();
        single.add(builder);
        this.parameters = Collections.unmodifiableList(single);
    }

    /**
     * With several, in order of preference.
     *
     * @throws IllegalArgumentException if the list comes empty
     */
    public KeyStoreBuilderParameters(List<KeyStore.Builder> parameters) {
        if (parameters.isEmpty()) {
            throw new IllegalArgumentException("the list of builders is empty");
        }
        this.parameters =
                Collections.unmodifiableList(new ArrayList<KeyStore.Builder>(parameters));
    }

    /** The stores, in an unmodifiable list. */
    public List<KeyStore.Builder> getParameters() {
        return this.parameters;
    }
}
