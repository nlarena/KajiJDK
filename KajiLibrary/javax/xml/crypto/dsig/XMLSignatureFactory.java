package javax.xml.crypto.dsig;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.InvalidAlgorithmParameterException;
import java.util.List;
import javax.xml.crypto.Data;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.NoSuchMechanismException;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.DigestMethodParameterSpec;
import javax.xml.crypto.dsig.spec.SignatureMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

/**
 * KajiLibrary's javax.xml.crypto.dsig.XMLSignatureFactory -- makes all the pieces of a signature.
 *
 * <p>The interfaces of this package have no constructor: they are built from here. The factory is
 * asked for by <b>mechanism</b> --the object model, typically {@code "DOM"}-- because the
 * structures it produces are tied to how the XML is represented.
 *
 * <h2>Building a signature, in order</h2>
 *
 * <p>The order of the {@code new*}s is not arbitrary: it is built from the inside out. First the
 * {@link Reference}s with their transforms, then the {@link SignedInfo} that groups them with the
 * algorithms, then the {@link XMLSignature}. Only then is {@code sign} called.
 *
 * <p>{@link #unmarshalXMLSignature} goes the other way: it reads a signature from a document to
 * validate it.
 *
 * <p>{@link #getKeyInfoFactory} returns the key information factory of the <b>same</b> mechanism.
 * It matters that it comes from here and not from {@code KeyInfoFactory.getInstance}: mixing
 * structures of two different mechanisms fails when writing.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This library comes with no XML signature mechanism --it needs canonicalization, a live DOM and
 * a transform engine-- so the four {@code getInstance}s throw {@link NoSuchMechanismException}. It
 * is the exception they already declare for "no implementation", and it is unchecked because it is
 * a deployment problem. The search among providers is implemented: registering an {@code
 * XMLSignatureFactory} service, this works unchanged.
 */
public abstract class XMLSignatureFactory {

    /** The service type a provider registers with. */
    private static final String SERVICE = "XMLSignatureFactory";

    /** The mechanism it was asked for with. */
    private String mechanismType;

    /** Where it came from. */
    private Provider provider;

    /** For the subclasses. */
    protected XMLSignatureFactory() {
    }

    /**
     * The factory of that mechanism, from the first provider that has it.
     *
     * @throws NoSuchMechanismException if none has it
     */
    public static XMLSignatureFactory getInstance(String mechanismType) {
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
            "No XMLSignatureFactory implementation for mechanism type " + mechanismType);
    }

    /** Likewise, from a concrete provider. */
    public static XMLSignatureFactory getInstance(String mechanismType, Provider provider) {
        if (mechanismType == null) {
            throw new NullPointerException("mechanismType cannot be null");
        }
        if (provider == null) {
            throw new NullPointerException("provider cannot be null");
        }
        Provider.Service s = provider.getService(SERVICE, mechanismType);
        if (s == null) {
            throw new NoSuchMechanismException("Provider " + provider.getName()
                + " has no XMLSignatureFactory for mechanism type " + mechanismType);
        }
        return build(s, mechanismType);
    }

    /**
     * Likewise, naming the provider.
     *
     * @throws NoSuchProviderException if there is no provider with that name
     */
    public static XMLSignatureFactory getInstance(String mechanismType, String provider)
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
    public static XMLSignatureFactory getInstance() {
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

    /** A signature with that signed content and that key information. */
    public abstract XMLSignature newXMLSignature(SignedInfo si, KeyInfo ki);

    /** Likewise, with objects inside and identifiers. */
    public abstract XMLSignature newXMLSignature(SignedInfo si, KeyInfo ki,
                                                 List<? extends XMLObject> objects, String id,
                                                 String signatureValueId);

    /** A reference to that URI, digested with that algorithm. */
    public abstract Reference newReference(String uri, DigestMethod dm);

    /** Likewise, with transforms, type and identifier. */
    public abstract Reference newReference(String uri, DigestMethod dm,
                                           List<? extends Transform> transforms, String type,
                                           String id);

    /** Likewise, with the digest already computed: to read an existing signature. */
    public abstract Reference newReference(String uri, DigestMethod dm,
                                           List<? extends Transform> transforms, String type,
                                           String id, byte[] digestValue);

    /** Likewise, with data already resolved and application transforms. */
    public abstract Reference newReference(String uri, DigestMethod dm,
                                           List<? extends Transform> appliedTransforms,
                                           Data result, List<? extends Transform> transforms,
                                           String type, String id);

    /** The block that is really signed. */
    public abstract SignedInfo newSignedInfo(CanonicalizationMethod cm, SignatureMethod sm,
                                             List<? extends Reference> references);

    /** Likewise, with an identifier. */
    public abstract SignedInfo newSignedInfo(CanonicalizationMethod cm, SignatureMethod sm,
                                             List<? extends Reference> references, String id);

    /** A content container inside the signature. */
    public abstract XMLObject newXMLObject(List<? extends XMLStructure> content, String id,
                                           String mimeType, String encoding);

    /** A manifest. */
    public abstract Manifest newManifest(List<? extends Reference> references);

    /** Likewise, with an identifier. */
    public abstract Manifest newManifest(List<? extends Reference> references, String id);

    /** A property about the signature. */
    public abstract SignatureProperty newSignatureProperty(List<? extends XMLStructure> content,
                                                           String target, String id);

    /** A group of properties. */
    public abstract SignatureProperties newSignatureProperties(
        List<? extends SignatureProperty> properties, String id);

    /**
     * A digest algorithm.
     *
     * @throws NoSuchAlgorithmException if the mechanism does not support it
     * @throws InvalidAlgorithmParameterException if the parameters do not suit it
     */
    public abstract DigestMethod newDigestMethod(String algorithm, DigestMethodParameterSpec params)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    /** A signature algorithm. */
    public abstract SignatureMethod newSignatureMethod(String algorithm,
                                                       SignatureMethodParameterSpec params)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    /** A transform. */
    public abstract Transform newTransform(String algorithm, TransformParameterSpec params)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    /** Likewise, with the parameters as XML already built. */
    public abstract Transform newTransform(String algorithm, XMLStructure params)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    /** A canonicalization. */
    public abstract CanonicalizationMethod newCanonicalizationMethod(String algorithm,
                                                                     C14NMethodParameterSpec params)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    /** Likewise, with the parameters as XML already built. */
    public abstract CanonicalizationMethod newCanonicalizationMethod(String algorithm,
                                                                     XMLStructure params)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    /**
     * The key information factory of the <b>same</b> mechanism.
     *
     * <p>See the class note on why it should not be asked for elsewhere.
     */
    public final KeyInfoFactory getKeyInfoFactory() {
        return KeyInfoFactory.getInstance(getMechanismType(), getProvider());
    }

    /**
     * Reads the signature the context points to.
     *
     * @throws MarshalException if it is not a well-formed signature
     */
    public abstract XMLSignature unmarshalXMLSignature(XMLValidateContext context)
        throws MarshalException;

    /** Likewise, from an already parsed structure. */
    public abstract XMLSignature unmarshalXMLSignature(XMLStructure xmlStructure)
        throws MarshalException;

    /** Whether this implementation supports that feature. */
    public abstract boolean isFeatureSupported(String feature);

    /** How this factory resolves the references by default. */
    public abstract URIDereferencer getURIDereferencer();

    /** What the {@code getInstance}s with a search build in common. */
    private static XMLSignatureFactory build(Provider.Service s, String mechanismType) {
        Object made;
        try {
            made = s.newInstance(null);
        } catch (Exception e) {
            throw new NoSuchMechanismException("Cannot instantiate " + s.getClassName(), e);
        }
        if (!(made instanceof XMLSignatureFactory)) {
            throw new NoSuchMechanismException(s.getClassName() + " is not an XMLSignatureFactory");
        }
        XMLSignatureFactory factory = (XMLSignatureFactory) made;
        factory.mechanismType = mechanismType;
        factory.provider = s.getProvider();
        return factory;
    }
}
