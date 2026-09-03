package java.security.cert;

import java.io.InputStream;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

// La fabrica que convierte bytes en certificados, CRLs y caminos.
//
// Es el unico camino oficial para construir un `Certificate` desde su codificacion, y por eso es
// la pieza que falta para que el resto del paquete sirva de algo: sin ella, `X509Certificate` es un
// contrato que nadie puede instanciar.
//
// Vale ser explicito sobre lo que **no** hace, porque el nombre invita a confundirse: leer un
// certificado no lo valida. `generateCertificate` devuelve el objeto y nada mas —no chequea la
// fecha, no verifica la firma, no mira quien lo emitio—. El certificado que sale de aca es un dato,
// no una afirmacion. Quien crea que "parseo bien, entonces sirve" tiene un agujero.
//
// ===============================================================================================
// A KajiLibrary subset
// ===============================================================================================
//
// **No hay ninguna fabrica registrada**, asi que las tres sobrecargas de `getInstance` tiran
// siempre `CertificateException`. No se registra una fabrica X.509 porque leer un certificado es
// parsear la estructura entera de ASN.1 —nombres X.500, GeneralName, todas las extensiones— y esta
// biblioteca solo tiene el lector de DER minimo que explica `DerReader`. Una fabrica que parsee
// **a medias** es peor que no tenerla: devolveria un objeto que parece un certificado y miente
// sobre lo que dice.
//
// La estructura entera esta, y `CertificateFactorySpi` es la interfaz completa: el dia que haya un
// parser, se registra y todo lo demas funciona.
public class CertificateFactory {

    private final CertificateFactorySpi certFacSpi;
    private final Provider provider;
    private final String type;

    protected CertificateFactory(CertificateFactorySpi certFacSpi, Provider provider, String type) {
        this.certFacSpi = certFacSpi;
        this.provider = provider;
        this.type = type;
    }

    public static final CertificateFactory getInstance(String type) throws CertificateException {
        if (type == null) {
            throw new NullPointerException("null type name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("CertificateFactory", type);
            if (s != null) {
                return armar(s, type);
            }
            i = i + 1;
        }
        throw new CertificateException(type + " not found");
    }

    public static final CertificateFactory getInstance(String type, String provider)
            throws CertificateException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(type, p);
    }

    public static final CertificateFactory getInstance(String type, Provider provider)
            throws CertificateException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (type == null) {
            throw new NullPointerException("null type name");
        }
        Provider.Service s = provider.getService("CertificateFactory", type);
        if (s == null) {
            throw new CertificateException(
                "no such type: " + type + " for provider " + provider.getName());
        }
        return armar(s, type);
    }

    private static CertificateFactory armar(Provider.Service s, String type)
            throws CertificateException {
        Object o;
        try {
            o = s.newInstance(null);
        } catch (java.security.NoSuchAlgorithmException e) {
            // A diferencia del resto de las fabricas del area, esta no puede propagar
            // `NoSuchAlgorithmException`: su `getInstance` solo declara `CertificateException`. Se
            // envuelve conservando la causa en vez de perderla.
            throw new CertificateException(e.getMessage(), e);
        }
        if (!(o instanceof CertificateFactorySpi)) {
            throw new CertificateException(
                "class configured for CertificateFactory is not a CertificateFactorySpi: "
                + s.getClassName());
        }
        return new CertificateFactory((CertificateFactorySpi) o, s.getProvider(), type);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    // El tipo de certificado: "X.509".
    public final String getType() {
        return this.type;
    }

    // Lee **un** certificado del stream. No lo valida; ver la nota de la clase.
    public final Certificate generateCertificate(InputStream inStream)
            throws CertificateException {
        return this.certFacSpi.engineGenerateCertificate(inStream);
    }

    // Las codificaciones de camino que soporta, con la preferida primero.
    public final Iterator<String> getCertPathEncodings() {
        return this.certFacSpi.engineGetCertPathEncodings();
    }

    public final CertPath generateCertPath(InputStream inStream) throws CertificateException {
        return this.certFacSpi.engineGenerateCertPath(inStream);
    }

    public final CertPath generateCertPath(InputStream inStream, String encoding)
            throws CertificateException {
        return this.certFacSpi.engineGenerateCertPath(inStream, encoding);
    }

    // Arma un camino a partir de una lista ya en memoria. El orden de la lista **es** el del camino
    // y no se reordena: el primero es el sujeto y cada uno esta firmado por el siguiente.
    public final CertPath generateCertPath(List<? extends Certificate> certificates)
            throws CertificateException {
        return this.certFacSpi.engineGenerateCertPath(certificates);
    }

    // Lee todos los certificados del stream.
    public final Collection<? extends Certificate> generateCertificates(InputStream inStream)
            throws CertificateException {
        return this.certFacSpi.engineGenerateCertificates(inStream);
    }

    public final CRL generateCRL(InputStream inStream) throws CRLException {
        return this.certFacSpi.engineGenerateCRL(inStream);
    }

    public final Collection<? extends CRL> generateCRLs(InputStream inStream) throws CRLException {
        return this.certFacSpi.engineGenerateCRLs(inStream);
    }
}
