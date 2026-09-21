package javax.xml.crypto.dsig.keyinfo;

import java.math.BigInteger;
import java.security.KeyException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.List;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.NoSuchMechanismException;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.Transform;

/**
 * KajiLibrary's javax.xml.crypto.dsig.keyinfo.KeyInfoFactory -- makes the key information
 * structures.
 *
 * <p>All the interfaces of this package are built from here. It is so because they are interfaces
 * without a public implementation: whoever provides the mechanism decides which concrete class they
 * are.
 *
 * <p>It is asked for by <b>mechanism</b> --the name of the object model, typically {@code "DOM"}--
 * and not by algorithm. It is the same indirection as in {@code XMLSignatureFactory}, and both
 * factories have to be of the same mechanism for their structures to be mixable.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This library comes with no XML signature mechanism: implementing it needs canonicalization, a
 * live DOM and a transform engine, and none of the three is here. The four {@code getInstance}s
 * throw {@link NoSuchMechanismException}, which is the exception they already declare for "no
 * implementation of that mechanism" and which is <b>unchecked</b> precisely because it is a
 * deployment problem.
 *
 * <p>The search among security providers is really implemented: registering a {@code
 * KeyInfoFactory} service, this works unchanged.
 */
public abstract class KeyInfoFactory {

    /** The service type a provider registers with. */
    private static final String SERVICE = "KeyInfoFactory";

    /** The mechanism it was asked for with. */
    private String mechanismType;

    /** Where it came from. */
    private Provider provider;

    /** For the subclasses. */
    protected KeyInfoFactory() {
    }

    /**
     * The factory of that mechanism, from the first provider that has it.
     *
     * @throws NoSuchMechanismException if none has it
     * @throws NullPointerException if the mechanism is null
     */
    public static KeyInfoFactory getInstance(String mechanismType) {
        if (mechanismType == null) {
            throw new NullPointerException("mechanismType cannot be null");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService(SERVICE, mechanismType);
            if (s != null) {
                return build(s, mechanismType);
            }
            i = i + 1;
        }
        throw new NoSuchMechanismException(
            "No KeyInfoFactory implementation for mechanism type " + mechanismType);
    }

    /**
     * Likewise, from a concrete provider.
     *
     * @throws NullPointerException if either of the two is null
     */
    public static KeyInfoFactory getInstance(String mechanismType, Provider provider) {
        if (mechanismType == null) {
            throw new NullPointerException("mechanismType cannot be null");
        }
        if (provider == null) {
            throw new NullPointerException("provider cannot be null");
        }
        Provider.Service s = provider.getService(SERVICE, mechanismType);
        if (s == null) {
            throw new NoSuchMechanismException("Provider " + provider.getName()
                + " has no KeyInfoFactory for mechanism type " + mechanismType);
        }
        return build(s, mechanismType);
    }

    /**
     * Likewise, naming the provider.
     *
     * @throws NoSuchProviderException if there is no provider with that name
     * @throws IllegalArgumentException if the name is empty; JDK 25 throws {@code
     *     NoSuchProviderException} there too
     */
    public static KeyInfoFactory getInstance(String mechanismType, String provider)
        throws NoSuchProviderException {
        if (provider == null) {
            throw new NullPointerException("provider cannot be null");
        }
        if (provider.length() == 0) {
            throw new IllegalArgumentException("provider cannot be empty");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(mechanismType, p);
    }

    /** The one of the default mechanism, which is {@code "DOM"}. */
    public static KeyInfoFactory getInstance() {
        return getInstance("DOM");
    }

    /** The mechanism it was asked for with. */
    public final String getMechanismType() {
        return this.mechanismType;
    }

    /** The provider it came from. */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * A {@link KeyInfo} with that content.
     *
     * @throws NullPointerException if the list is null
     * @throws IllegalArgumentException if it is empty
     */
    public abstract KeyInfo newKeyInfo(List<? extends XMLStructure> content);

    /** Likewise, with an identifier. */
    public abstract KeyInfo newKeyInfo(List<? extends XMLStructure> content, String id);

    /** A {@link KeyName}. */
    public abstract KeyName newKeyName(String name);

    /**
     * A {@link KeyValue} with that public key.
     *
     * @throws KeyException if the key's algorithm is not supported
     */
    public abstract KeyValue newKeyValue(java.security.PublicKey key) throws KeyException;

    /** A {@link PGPData} with only the key identifier. */
    public abstract PGPData newPGPData(byte[] keyId);

    /** Likewise, with the key packet and extra content. */
    public abstract PGPData newPGPData(byte[] keyId, byte[] keyPacket,
                                       List<? extends XMLStructure> other);

    /** Likewise, with the key packet only. */
    public abstract PGPData newPGPData(byte[] keyPacket, List<? extends XMLStructure> other);

    /** A {@link RetrievalMethod} pointing to that URI. */
    public abstract RetrievalMethod newRetrievalMethod(String uri);

    /** Likewise, with type and transforms. */
    public abstract RetrievalMethod newRetrievalMethod(String uri, String type,
                                                       List<? extends Transform> transforms);

    /**
     * An {@link X509Data} with that content.
     *
     * <p>The list is heterogeneous; see {@link X509Data#getContent}.
     */
    public abstract X509Data newX509Data(List<?> content);

    /** An {@link X509IssuerSerial}. */
    public abstract X509IssuerSerial newX509IssuerSerial(String issuerName,
                                                         BigInteger serialNumber);

    /** Whether this implementation supports that feature. */
    public abstract boolean isFeatureSupported(String feature);

    /** How this factory resolves the references by default. */
    public abstract URIDereferencer getURIDereferencer();

    /**
     * Reads a {@link KeyInfo} from an already parsed structure.
     *
     * @throws MarshalException if it is not a well-formed {@code KeyInfo}
     */
    public abstract KeyInfo unmarshalKeyInfo(XMLStructure xmlStructure) throws MarshalException;

    /** What the three {@code getInstance}s with a search build in common. */
    private static KeyInfoFactory build(Provider.Service s, String mechanismType) {
        Object made;
        try {
            made = s.newInstance(null);
        } catch (Exception e) {
            throw new NoSuchMechanismException(
                "Cannot instantiate " + s.getClassName(), e);
        }
        if (!(made instanceof KeyInfoFactory)) {
            throw new NoSuchMechanismException(
                s.getClassName() + " is not a KeyInfoFactory");
        }
        KeyInfoFactory factory = (KeyInfoFactory) made;
        factory.mechanismType = mechanismType;
        factory.provider = s.getProvider();
        return factory;
    }
}
