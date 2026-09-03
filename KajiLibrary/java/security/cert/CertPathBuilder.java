package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;

// Construye un camino de certificacion: dado un certificado y un conjunto de anclas, busca la
// cadena que los une.
//
// Es la pieza que hace falta cuando la cadena **no** viene completa, que es lo normal: un servidor
// TLS suele mandar su certificado y algunos intermedios, pero no todos, y el resto hay que ir a
// buscarlo a los `CertStore` configurados. Construir es una busqueda con vuelta atras, y por eso
// `PKIXBuilderParameters` tiene un largo maximo.
//
// El camino que devuelve ya esta validado. No es un atajo del API: buscar una cadena implica
// verificar cada eslabon para saber si sirve, asi que separarlo en dos pasos duplicaria el trabajo.
//
// A KajiLibrary subset: **no hay ningun proveedor registrado**, asi que las tres sobrecargas de
// `getInstance` tiran siempre `NoSuchAlgorithmException`. Implementar PKIX honestamente pide
// verificar firmas —RSA, ECDSA— y comparar nombres X.500, y ninguna de las dos cosas esta escrita
// en esta biblioteca. Un constructor que devolviera cadenas sin verificar seria exactamente el
// agujero que este paquete tiene que evitar.
public class CertPathBuilder {

    private final CertPathBuilderSpi builderSpi;
    private final Provider provider;
    private final String algorithm;

    protected CertPathBuilder(CertPathBuilderSpi builderSpi, Provider provider, String algorithm) {
        this.builderSpi = builderSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    public static CertPathBuilder getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("CertPathBuilder", algorithm);
            if (s != null) {
                return armar(s, algorithm);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(algorithm + " CertPathBuilder not available");
    }

    public static CertPathBuilder getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(algorithm, p);
    }

    public static CertPathBuilder getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider.Service s = provider.getService("CertPathBuilder", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return armar(s, algorithm);
    }

    private static CertPathBuilder armar(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        Object o = s.newInstance(null);
        if (!(o instanceof CertPathBuilderSpi)) {
            throw new NoSuchAlgorithmException(
                "class configured for CertPathBuilder is not a CertPathBuilderSpi: "
                + s.getClassName());
        }
        return new CertPathBuilder((CertPathBuilderSpi) o, s.getProvider(), algorithm);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    // Busca y valida un camino. Si no hay ninguno, lanza: la ausencia de camino no se devuelve como
    // null.
    public final CertPathBuilderResult build(CertPathParameters params)
            throws CertPathBuilderException, InvalidAlgorithmParameterException {
        return this.builderSpi.engineBuild(params);
    }

    // El algoritmo por default, de la propiedad `certpathbuilder.type`. "PKIX" si no esta puesta.
    public static final String getDefaultType() {
        String t = Security.getProperty("certpathbuilder.type");
        if (t == null) {
            return "PKIX";
        }
        return t;
    }

    // El chequeador de revocacion de este proveedor, para configurarlo antes de construir. Tira
    // `UnsupportedOperationException` si el proveedor no lo ofrece.
    public final CertPathChecker getRevocationChecker() {
        return this.builderSpi.engineGetRevocationChecker();
    }
}
