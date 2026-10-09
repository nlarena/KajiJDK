package java.security;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// The parameters for loading a **domain** of stores: several `KeyStore`s that are handled as if
// they were one.
//
// The idea is from Java 8 and solves a concrete problem: an application that needs keys from
// several places —a PKCS#12 file, a card, a store of the system— ended up opening and coordinating
// each one by hand. A domain describes them in a configuration file, each with its own password,
// and they are all opened together.
//
// The URI points at that configuration and may carry a fragment with the name of the domain:
// `file:///etc/keystores.cfg#production`.
//
// `getProtectionParameter()` always returns **null**, and it is not an oversight: the protection is
// not a single one, there is one per store, and they are in the map. Returning that of one of them
// would be choosing arbitrarily.
public final class DomainLoadStoreParameter implements KeyStore.LoadStoreParameter {

    private final URI configuration;
    private final Map<String, KeyStore.ProtectionParameter> protectionParams;

    // The keys of the map are names of stores inside the domain; the empty key gives the default
    // protection for the ones that do not appear.
    public DomainLoadStoreParameter(URI configuration,
                                    Map<String, KeyStore.ProtectionParameter> protectionParams) {
        if (configuration == null || protectionParams == null) {
            throw new NullPointerException("invalid null input");
        }
        this.configuration = configuration;
        this.protectionParams = Collections.unmodifiableMap(
            new HashMap<String, KeyStore.ProtectionParameter>(protectionParams));
    }

    // The URI of the configuration file, with the name of the domain in the fragment if it carries
    // one.
    public URI getConfiguration() {
        return this.configuration;
    }

    // An immutable copy of the map of protections.
    public Map<String, KeyStore.ProtectionParameter> getProtectionParams() {
        return this.protectionParams;
    }

    // Always null: the protection is per store, not of the domain. See the note of the class.
    public KeyStore.ProtectionParameter getProtectionParameter() {
        return null;
    }
}
