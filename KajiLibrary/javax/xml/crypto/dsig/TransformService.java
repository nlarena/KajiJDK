package javax.xml.crypto.dsig;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

/**
 * KajiLibrary's javax.xml.crypto.dsig.TransformService -- the socket for writing a transform of
 * one's own.
 *
 * <p>It is the only extension XML-DSig leaves open to whoever uses the library: a new
 * {@link Transform} is implemented by extending this and registering it as a service of a security
 * provider.
 *
 * <h2>It is asked for by two things, not one</h2>
 *
 * <p>The {@code getInstance}s receive <b>algorithm and mechanism</b>: the URI of the transform and
 * the object model. Both are needed because a transform works on the concrete representation of the
 * XML, and the same transform over DOM and over another model are two different implementations.
 *
 * <p>Here the service is looked up with the type {@code TransformService} and the algorithm
 * {@code "<URI> <mechanism>"} -- both things in one string. The note said that is how the JDK
 * combines them; it is not: the JDK registers the algorithm as the bare URI and the mechanism as a
 * {@code MechanismType} attribute of the service
 * ({@code put("TransformService.<URI> MechanismType", "DOM")}), so a provider registered that way
 * is not found by this lookup.
 *
 * <h2>The two inits</h2>
 *
 * <p>{@link #init(TransformParameterSpec)} is for <b>signing</b>: the parameters are given by the
 * program. {@link #init(XMLStructure, XMLCryptoContext)} is for <b>validating</b>: the parameters
 * are read from the document. {@link #marshalParams} is the way back, when writing.
 *
 * <p>An implementation has to support all three, because a transform of one's own has to be able to
 * go to and from XML; otherwise the signature it produces cannot be validated by anybody else.
 */
public abstract class TransformService implements Transform {

    /** The service type a provider registers with. */
    private static final String SERVICE = "TransformService";

    /** The mechanism it was asked for with. */
    private String mechanismType;

    /** The URI of the algorithm. */
    private String algorithm;

    /** Where it came from. */
    private Provider provider;

    /** For the subclasses. */
    protected TransformService() {
    }

    /**
     * The service of that algorithm and that mechanism.
     *
     * @throws NoSuchAlgorithmException if no provider has it
     */
    public static TransformService getInstance(String algorithm, String mechanismType)
        throws NoSuchAlgorithmException {
        checkArgs(algorithm, mechanismType);
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService(SERVICE, algorithm + " " + mechanismType);
            if (s != null) {
                return build(s, algorithm, mechanismType);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(
            "No TransformService for algorithm " + algorithm + " and mechanism " + mechanismType);
    }

    /** Likewise, from a concrete provider. */
    public static TransformService getInstance(String algorithm, String mechanismType,
                                               Provider provider) throws NoSuchAlgorithmException {
        checkArgs(algorithm, mechanismType);
        if (provider == null) {
            throw new NullPointerException("provider cannot be null");
        }
        Provider.Service s = provider.getService(SERVICE, algorithm + " " + mechanismType);
        if (s == null) {
            throw new NoSuchAlgorithmException("Provider " + provider.getName()
                + " has no TransformService for algorithm " + algorithm);
        }
        return build(s, algorithm, mechanismType);
    }

    /**
     * Likewise, naming the provider.
     *
     * @throws NoSuchProviderException if there is no provider with that name
     */
    public static TransformService getInstance(String algorithm, String mechanismType,
                                               String provider)
        throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null) {
            throw new NullPointerException("provider cannot be null");
        }
        if (provider.length() == 0) {
            throw new NoSuchProviderException("provider cannot be empty");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(algorithm, mechanismType, p);
    }

    /** The mechanism it was asked for with. */
    public final String getMechanismType() {
        return this.mechanismType;
    }

    /** The URI of the algorithm. */
    public final String getAlgorithm() {
        return this.algorithm;
    }

    /** The provider it came from. */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * Initializes for signing, with parameters given by the program.
     *
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     */
    public abstract void init(TransformParameterSpec params)
        throws InvalidAlgorithmParameterException;

    /**
     * Writes the parameters into the XML.
     *
     * @throws MarshalException if they cannot be written there
     */
    public abstract void marshalParams(XMLStructure parent, XMLCryptoContext context)
        throws MarshalException;

    /**
     * Initializes for validating, reading the parameters from the document.
     *
     * @throws InvalidAlgorithmParameterException if what is in the document is no good
     */
    public abstract void init(XMLStructure parent, XMLCryptoContext context)
        throws InvalidAlgorithmParameterException;

    /** That both names are there. */
    private static void checkArgs(String algorithm, String mechanismType) {
        if (algorithm == null) {
            throw new NullPointerException("algorithm cannot be null");
        }
        if (mechanismType == null) {
            throw new NullPointerException("mechanismType cannot be null");
        }
    }

    /** What the three {@code getInstance}s build in common. */
    private static TransformService build(Provider.Service s, String algorithm,
                                          String mechanismType) throws NoSuchAlgorithmException {
        Object made;
        try {
            made = s.newInstance(null);
        } catch (Exception e) {
            throw new NoSuchAlgorithmException("Cannot instantiate " + s.getClassName(), e);
        }
        if (!(made instanceof TransformService)) {
            throw new NoSuchAlgorithmException(s.getClassName() + " is not a TransformService");
        }
        TransformService service = (TransformService) made;
        service.algorithm = algorithm;
        service.mechanismType = mechanismType;
        service.provider = s.getProvider();
        return service;
    }
}
