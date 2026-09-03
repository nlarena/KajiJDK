package javax.security.cert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.security.Principal;
import java.security.Security;
import java.util.Date;

/**
 * KajiLibrary's javax.security.cert.X509Certificate -- un X.509, en el API viejo.
 *
 * <p>Es un subconjunto deliberado del X.509 de verdad: solo los campos de la <b>version 1</b>, sin
 * extensiones. Eso lo hace inutil para validar una cadena moderna --sin extensiones no hay
 * {@code basicConstraints}, y sin {@code basicConstraints} no se puede saber si un certificado tiene
 * derecho a firmar otros-- y es exactamente por eso que
 * {@link java.security.cert.X509Certificate} lo reemplazo.
 *
 * <h2>De donde sale la implementacion</h2>
 *
 * <p>Esta clase es abstracta y los {@code getInstance} tienen que fabricar algo concreto. El nombre
 * de esa clase no esta escrito aca: se lee de la propiedad de seguridad
 * {@code cert.provider.x509v1} y se instancia por reflexion. Es indireccion a proposito --deja
 * cambiar el parser sin recompilar-- y es la misma via que usa el JDK.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>KajiLibrary no trae hoy ningun parser X.509 concreto, asi que esa propiedad viene sin valor y
 * los dos {@code getInstance} lanzan {@link CertificateException}. Es una salida <b>declarada</b>
 * del metodo, no una mentira: quien llama ya tiene que manejarla, y el mensaje dice cual es la
 * propiedad que falta. Devolver un certificado a medio armar seria peor, porque los llamadores de
 * este API son justamente los que deciden si confiar en un par remoto.
 *
 * <p>Obsoleta <b>y marcada para remocion</b> desde Java 9. El reemplazo es
 * {@code java.security.cert}, que no es una version mejorada de esto sino otra cosa: soporta la
 * version 3 del formato, con extensiones, que es lo unico que sirve para validar una cadena de hoy.
 */
@Deprecated(since = "9", forRemoval = true)
public abstract class X509Certificate extends Certificate {

    /** La propiedad de seguridad que nombra la clase concreta. */
    private static final String X509_PROVIDER = "cert.provider.x509v1";

    /** Para las subclases. */
    public X509Certificate() {
    }

    /**
     * Lee un certificado de un flujo.
     *
     * <p>Consume el flujo entero y delega en {@link #getInstance(byte[])}.
     *
     * @throws CertificateException si no hay parser configurado o los bytes no cierran
     */
    public static final X509Certificate getInstance(InputStream inStream)
        throws CertificateException {
        if (inStream == null) {
            throw new CertificateException("Missing input stream");
        }
        byte[] encoded;
        try {
            encoded = inStream.readAllBytes();
        } catch (IOException e) {
            throw new CertificateException("Failed to read the certificate: " + e.getMessage());
        }
        return getInstance(encoded);
    }

    /**
     * Lee un certificado de sus bytes.
     *
     * @throws CertificateException si no hay parser configurado o los bytes no cierran
     */
    public static final X509Certificate getInstance(byte[] certData) throws CertificateException {
        if (certData == null) {
            throw new CertificateException("Missing certificate data");
        }
        String className = Security.getProperty(X509_PROVIDER);
        if (className == null || className.length() == 0) {
            throw new CertificateException(
                "No X.509 v1 certificate implementation is configured; the security property "
                    + X509_PROVIDER + " is not set");
        }
        try {
            Class<?> implementation = Class.forName(className);
            Constructor<?> ctor = implementation.getConstructor(new Class<?>[] {InputStream.class});
            Object made = ctor.newInstance(new Object[] {new ByteArrayInputStream(certData)});
            return (X509Certificate) made;
        } catch (ClassCastException e) {
            throw new CertificateException(
                className + " is not a javax.security.cert.X509Certificate");
        } catch (Exception e) {
            throw new CertificateException(
                "Could not build a certificate with " + className + ": " + e);
        }
    }

    /**
     * Comprueba que el certificado valga <b>ahora</b>.
     *
     * @throws CertificateExpiredException si ya vencio
     * @throws CertificateNotYetValidException si todavia no empezo
     */
    public abstract void checkValidity()
        throws CertificateExpiredException, CertificateNotYetValidException;

    /** Idem, contra una fecha dada. */
    public abstract void checkValidity(Date date)
        throws CertificateExpiredException, CertificateNotYetValidException;

    /** La version del formato; 1 para todo lo que este API sabe describir. */
    public abstract int getVersion();

    /** El numero de serie, unico <b>por emisor</b> y no en general. */
    public abstract BigInteger getSerialNumber();

    /** Quien lo firmo. */
    public abstract Principal getIssuerDN();

    /** Sobre quien habla. */
    public abstract Principal getSubjectDN();

    /** Desde cuando vale. */
    public abstract Date getNotBefore();

    /** Hasta cuando vale. */
    public abstract Date getNotAfter();

    /** El nombre del algoritmo de firma, si se conoce; si no, el OID. */
    public abstract String getSigAlgName();

    /** El OID del algoritmo de firma, que es el dato que de verdad esta en el certificado. */
    public abstract String getSigAlgOID();

    /** Los parametros del algoritmo, en DER, o null si no tiene. */
    public abstract byte[] getSigAlgParams();
}
