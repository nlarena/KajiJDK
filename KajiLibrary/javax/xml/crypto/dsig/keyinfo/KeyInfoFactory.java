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
 * KajiLibrary's javax.xml.crypto.dsig.keyinfo.KeyInfoFactory -- fabrica las estructuras de
 * informacion de clave.
 *
 * <p>Todas las interfaces de este paquete se construyen desde aca. Es asi porque son interfaces sin
 * implementacion publica: quien provee el mecanismo decide de que clase concreta son.
 *
 * <p>Se pide por <b>mecanismo</b> --el nombre del modelo de objetos, tipicamente {@code "DOM"}-- y no
 * por algoritmo. Es la misma indireccion que en {@code XMLSignatureFactory}, y las dos fabricas
 * tienen que ser del mismo mecanismo para que sus estructuras se puedan mezclar.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta biblioteca no trae un mecanismo de firma XML: implementarlo pide canonicalizacion, un DOM
 * vivo y un motor de transformaciones, y ninguno de los tres esta. Los cuatro {@code getInstance}
 * lanzan {@link NoSuchMechanismException}, que es la excepcion que ya declaran para "no hay
 * implementacion de ese mecanismo" y que <b>no es comprobada</b> justamente porque es un problema de
 * despliegue.
 *
 * <p>La busqueda entre proveedores de seguridad esta implementada de verdad: registrando un servicio
 * {@code KeyInfoFactory}, esto funciona sin cambios.
 */
public abstract class KeyInfoFactory {

    /** El tipo de servicio con el que se registra un proveedor. */
    private static final String SERVICE = "KeyInfoFactory";

    /** El mecanismo con el que se pidio. */
    private String mechanismType;

    /** De donde salio. */
    private Provider provider;

    /** Para las subclases. */
    protected KeyInfoFactory() {
    }

    /**
     * La fabrica de ese mecanismo, del primer proveedor que la tenga.
     *
     * @throws NoSuchMechanismException si ninguno la tiene
     * @throws NullPointerException si el mecanismo es null
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
     * Idem, de un proveedor concreto.
     *
     * @throws NullPointerException si alguno de los dos es null
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
     * Idem, nombrando el proveedor.
     *
     * @throws NoSuchProviderException si no hay proveedor con ese nombre
     * @throws IllegalArgumentException si el nombre esta vacio
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

    /** La del mecanismo por omision, que es {@code "DOM"}. */
    public static KeyInfoFactory getInstance() {
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

    /**
     * Un {@link KeyInfo} con ese contenido.
     *
     * @throws NullPointerException si la lista es null
     * @throws IllegalArgumentException si esta vacia
     */
    public abstract KeyInfo newKeyInfo(List<? extends XMLStructure> content);

    /** Idem, con identificador. */
    public abstract KeyInfo newKeyInfo(List<? extends XMLStructure> content, String id);

    /** Un {@link KeyName}. */
    public abstract KeyName newKeyName(String name);

    /**
     * Un {@link KeyValue} con esa clave publica.
     *
     * @throws KeyException si el algoritmo de la clave no esta soportado
     */
    public abstract KeyValue newKeyValue(java.security.PublicKey key) throws KeyException;

    /** Un {@link PGPData} con solo el identificador de clave. */
    public abstract PGPData newPGPData(byte[] keyId);

    /** Idem, con el paquete de clave y contenido extra. */
    public abstract PGPData newPGPData(byte[] keyId, byte[] keyPacket,
                                       List<? extends XMLStructure> other);

    /** Idem, con el paquete de clave solo. */
    public abstract PGPData newPGPData(byte[] keyPacket, List<? extends XMLStructure> other);

    /** Un {@link RetrievalMethod} que apunta a ese URI. */
    public abstract RetrievalMethod newRetrievalMethod(String uri);

    /** Idem, con tipo y transformaciones. */
    public abstract RetrievalMethod newRetrievalMethod(String uri, String type,
                                                       List<? extends Transform> transforms);

    /**
     * Un {@link X509Data} con ese contenido.
     *
     * <p>La lista es heterogenea; ver {@link X509Data#getContent}.
     */
    public abstract X509Data newX509Data(List<?> content);

    /** Un {@link X509IssuerSerial}. */
    public abstract X509IssuerSerial newX509IssuerSerial(String issuerName,
                                                         BigInteger serialNumber);

    /** Si esta implementacion soporta esa caracteristica. */
    public abstract boolean isFeatureSupported(String feature);

    /** Como esta fabrica resuelve las referencias por omision. */
    public abstract URIDereferencer getURIDereferencer();

    /**
     * Lee un {@link KeyInfo} de una estructura ya analizada.
     *
     * @throws MarshalException si no es un {@code KeyInfo} bien formado
     */
    public abstract KeyInfo unmarshalKeyInfo(XMLStructure xmlStructure) throws MarshalException;

    /** El armado comun de los tres {@code getInstance} con busqueda. */
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
