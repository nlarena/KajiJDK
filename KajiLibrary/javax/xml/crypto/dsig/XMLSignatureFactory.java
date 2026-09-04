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
 * KajiLibrary's javax.xml.crypto.dsig.XMLSignatureFactory -- fabrica todas las piezas de una firma.
 *
 * <p>Las interfaces de este paquete no tienen constructor: se arman desde aca. La fabrica se pide por
 * <b>mecanismo</b> --el modelo de objetos, tipicamente {@code "DOM"}-- porque las estructuras que
 * produce estan atadas a como se representa el XML.
 *
 * <h2>Armar una firma, en orden</h2>
 *
 * <p>El orden de los {@code new*} no es arbitrario: se construye de adentro hacia afuera. Primero las
 * {@link Reference} con sus transformaciones, despues el {@link SignedInfo} que las agrupa con los
 * algoritmos, despues la {@link XMLSignature}. Recien ahi se llama a {@code sign}.
 *
 * <p>{@link #unmarshalXMLSignature} hace el camino inverso: lee una firma de un documento para
 * validarla.
 *
 * <p>{@link #getKeyInfoFactory} devuelve la fabrica de informacion de clave del <b>mismo</b>
 * mecanismo. Es importante que salga de aca y no de {@code KeyInfoFactory.getInstance}: mezclar
 * estructuras de dos mecanismos distintos falla al escribir.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta biblioteca no trae un mecanismo de firma XML --pide canonicalizacion, un DOM vivo y un
 * motor de transformaciones-- asi que los cuatro {@code getInstance} lanzan
 * {@link NoSuchMechanismException}. Es la excepcion que ya declaran para "no hay implementacion", y no
 * es comprobada porque es un problema de despliegue. La busqueda entre proveedores esta implementada:
 * registrando un servicio {@code XMLSignatureFactory}, esto funciona sin cambios.
 */
public abstract class XMLSignatureFactory {

    /** El tipo de servicio con el que se registra un proveedor. */
    private static final String SERVICE = "XMLSignatureFactory";

    /** El mecanismo con el que se pidio. */
    private String mechanismType;

    /** De donde salio. */
    private Provider provider;

    /** Para las subclases. */
    protected XMLSignatureFactory() {
    }

    /**
     * La fabrica de ese mecanismo, del primer proveedor que la tenga.
     *
     * @throws NoSuchMechanismException si ninguno la tiene
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

    /** Idem, de un proveedor concreto. */
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
     * Idem, nombrando el proveedor.
     *
     * @throws NoSuchProviderException si no hay proveedor con ese nombre
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

    /** La del mecanismo por omision, que es {@code "DOM"}. */
    public static XMLSignatureFactory getInstance() {
        return getInstance("DOM");
    }

    /** El mecanismo con el que se pidio. */
    public final String getMechanismType() {
        return this.mechanismType;
    }

    /** El proveedor de donde salio. */
    public final Provider getProvider() {
        return this.provider;
    }

    /** Una firma con ese contenido firmado y esa informacion de clave. */
    public abstract XMLSignature newXMLSignature(SignedInfo si, KeyInfo ki);

    /** Idem, con objetos adentro e identificadores. */
    public abstract XMLSignature newXMLSignature(SignedInfo si, KeyInfo ki,
                                                 List<? extends XMLObject> objects, String id,
                                                 String signatureValueId);

    /** Una referencia a ese URI, resumida con ese algoritmo. */
    public abstract Reference newReference(String uri, DigestMethod dm);

    /** Idem, con transformaciones, tipo e identificador. */
    public abstract Reference newReference(String uri, DigestMethod dm,
                                           List<? extends Transform> transforms, String type,
                                           String id);

    /** Idem, con el resumen ya calculado: para leer una firma existente. */
    public abstract Reference newReference(String uri, DigestMethod dm,
                                           List<? extends Transform> transforms, String type,
                                           String id, byte[] digestValue);

    /** Idem, con datos ya resueltos y transformaciones de aplicacion. */
    public abstract Reference newReference(String uri, DigestMethod dm,
                                           List<? extends Transform> appliedTransforms,
                                           Data result, List<? extends Transform> transforms,
                                           String type, String id);

    /** El bloque que de verdad se firma. */
    public abstract SignedInfo newSignedInfo(CanonicalizationMethod cm, SignatureMethod sm,
                                             List<? extends Reference> references);

    /** Idem, con identificador. */
    public abstract SignedInfo newSignedInfo(CanonicalizationMethod cm, SignatureMethod sm,
                                             List<? extends Reference> references, String id);

    /** Un contenedor de contenido dentro de la firma. */
    public abstract XMLObject newXMLObject(List<? extends XMLStructure> content, String id,
                                           String mimeType, String encoding);

    /** Un manifiesto. */
    public abstract Manifest newManifest(List<? extends Reference> references);

    /** Idem, con identificador. */
    public abstract Manifest newManifest(List<? extends Reference> references, String id);

    /** Una propiedad sobre la firma. */
    public abstract SignatureProperty newSignatureProperty(List<? extends XMLStructure> content,
                                                           String target, String id);

    /** Un grupo de propiedades. */
    public abstract SignatureProperties newSignatureProperties(
        List<? extends SignatureProperty> properties, String id);

    /**
     * Un algoritmo de resumen.
     *
     * @throws NoSuchAlgorithmException si el mecanismo no lo soporta
     * @throws InvalidAlgorithmParameterException si los parametros no le sirven
     */
    public abstract DigestMethod newDigestMethod(String algorithm, DigestMethodParameterSpec params)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    /** Un algoritmo de firma. */
    public abstract SignatureMethod newSignatureMethod(String algorithm,
                                                       SignatureMethodParameterSpec params)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    /** Una transformacion. */
    public abstract Transform newTransform(String algorithm, TransformParameterSpec params)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    /** Idem, con los parametros como XML ya armado. */
    public abstract Transform newTransform(String algorithm, XMLStructure params)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    /** Una canonicalizacion. */
    public abstract CanonicalizationMethod newCanonicalizationMethod(String algorithm,
                                                                     C14NMethodParameterSpec params)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    /** Idem, con los parametros como XML ya armado. */
    public abstract CanonicalizationMethod newCanonicalizationMethod(String algorithm,
                                                                     XMLStructure params)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    /**
     * La fabrica de informacion de clave del <b>mismo</b> mecanismo.
     *
     * <p>Ver la nota de la clase sobre por que no hay que pedirla por otro lado.
     */
    public final KeyInfoFactory getKeyInfoFactory() {
        return KeyInfoFactory.getInstance(getMechanismType(), getProvider());
    }

    /**
     * Lee la firma que el contexto apunta.
     *
     * @throws MarshalException si no es una firma bien formada
     */
    public abstract XMLSignature unmarshalXMLSignature(XMLValidateContext context)
        throws MarshalException;

    /** Idem, desde una estructura ya analizada. */
    public abstract XMLSignature unmarshalXMLSignature(XMLStructure xmlStructure)
        throws MarshalException;

    /** Si esta implementacion soporta esa caracteristica. */
    public abstract boolean isFeatureSupported(String feature);

    /** Como esta fabrica resuelve las referencias por omision. */
    public abstract URIDereferencer getURIDereferencer();

    /** El armado comun de los {@code getInstance} con busqueda. */
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
