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
 * KajiLibrary's javax.xml.crypto.dsig.TransformService -- el enchufe para escribir una
 * transformacion propia.
 *
 * <p>Es la unica extension que XML-DSig deja abierta a quien usa la biblioteca: una
 * {@link Transform} nueva se implementa extendiendo esto y registrandola como servicio de un
 * proveedor de seguridad.
 *
 * <h2>Se pide por dos cosas, no por una</h2>
 *
 * <p>Los {@code getInstance} reciben <b>algoritmo y mecanismo</b>: el URI de la transformacion y el
 * modelo de objetos. Hacen falta los dos porque una transformacion trabaja sobre la representacion
 * concreta del XML, y la misma transformacion sobre DOM y sobre otro modelo son dos implementaciones
 * distintas.
 *
 * <p>Adentro, el servicio se registra con el tipo {@code TransformService} y el algoritmo
 * {@code "<URI> MechanismType"} -- las dos cosas en una cadena, que es como el JDK las combina.
 *
 * <h2>Los dos init</h2>
 *
 * <p>{@link #init(TransformParameterSpec)} es para <b>firmar</b>: los parametros los da el programa.
 * {@link #init(XMLStructure, XMLCryptoContext)} es para <b>validar</b>: los parametros se leen del
 * documento. {@link #marshalParams} es el camino de vuelta, al escribir.
 *
 * <p>Una implementacion tiene que soportar los tres, porque una transformacion propia tiene que poder
 * ir y volver del XML; si no, la firma que produce no la puede validar nadie mas.
 */
public abstract class TransformService implements Transform {

    /** El tipo de servicio con el que se registra un proveedor. */
    private static final String SERVICE = "TransformService";

    /** El mecanismo con el que se pidio. */
    private String mechanismType;

    /** El URI del algoritmo. */
    private String algorithm;

    /** De donde salio. */
    private Provider provider;

    /** Para las subclases. */
    protected TransformService() {
    }

    /**
     * El servicio de ese algoritmo y ese mecanismo.
     *
     * @throws NoSuchAlgorithmException si ningun proveedor lo tiene
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

    /** Idem, de un proveedor concreto. */
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
     * Idem, nombrando el proveedor.
     *
     * @throws NoSuchProviderException si no hay proveedor con ese nombre
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

    /** El mecanismo con el que se pidio. */
    public final String getMechanismType() {
        return this.mechanismType;
    }

    /** El URI del algoritmo. */
    public final String getAlgorithm() {
        return this.algorithm;
    }

    /** El proveedor de donde salio. */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * Inicializa para firmar, con parametros dados por el programa.
     *
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     */
    public abstract void init(TransformParameterSpec params)
        throws InvalidAlgorithmParameterException;

    /**
     * Escribe los parametros en el XML.
     *
     * @throws MarshalException si no se pueden escribir ahi
     */
    public abstract void marshalParams(XMLStructure parent, XMLCryptoContext context)
        throws MarshalException;

    /**
     * Inicializa para validar, leyendo los parametros del documento.
     *
     * @throws InvalidAlgorithmParameterException si lo que hay en el documento no sirve
     */
    public abstract void init(XMLStructure parent, XMLCryptoContext context)
        throws InvalidAlgorithmParameterException;

    /** Que los dos nombres esten. */
    private static void checkArgs(String algorithm, String mechanismType) {
        if (algorithm == null) {
            throw new NullPointerException("algorithm cannot be null");
        }
        if (mechanismType == null) {
            throw new NullPointerException("mechanismType cannot be null");
        }
    }

    /** El armado comun de los tres {@code getInstance}. */
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
