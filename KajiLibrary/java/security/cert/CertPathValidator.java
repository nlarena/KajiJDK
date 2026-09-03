package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;

// Valida un camino de certificacion ya armado contra un conjunto de anclas.
//
// Es la operacion en la que se apoya toda la confianza de TLS y de las firmas de codigo, y su
// contrato es el que hay que tener claro: **`validate` no devuelve un boolean**. Si el camino no
// vale, lanza `CertPathValidatorException`, que ademas dice en que eslabon y por que. Si vuelve,
// devuelve datos utiles. Escribir `try { v.validate(p, ps); } catch (Exception e) {}` no es manejar
// un error: es aceptar cualquier cadena.
//
// A KajiLibrary subset: **no hay ningun proveedor registrado**, asi que las tres sobrecargas de
// `getInstance` tiran siempre `NoSuchAlgorithmException`. El motivo es el mismo que en
// `CertPathBuilder`: validar PKIX pide verificar firmas y comparar nombres X.500, y ninguna de las
// dos cosas esta implementada. Un validador que dijera que si sin verificar es el peor agujero
// posible en esta biblioteca, asi que no se registra ninguno.
public class CertPathValidator {

    private final CertPathValidatorSpi validatorSpi;
    private final Provider provider;
    private final String algorithm;

    protected CertPathValidator(CertPathValidatorSpi validatorSpi, Provider provider,
                                String algorithm) {
        this.validatorSpi = validatorSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    public static CertPathValidator getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("CertPathValidator", algorithm);
            if (s != null) {
                return armar(s, algorithm);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(algorithm + " CertPathValidator not available");
    }

    public static CertPathValidator getInstance(String algorithm, String provider)
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

    public static CertPathValidator getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider.Service s = provider.getService("CertPathValidator", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return armar(s, algorithm);
    }

    private static CertPathValidator armar(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        Object o = s.newInstance(null);
        if (!(o instanceof CertPathValidatorSpi)) {
            throw new NoSuchAlgorithmException(
                "class configured for CertPathValidator is not a CertPathValidatorSpi: "
                + s.getClassName());
        }
        return new CertPathValidator((CertPathValidatorSpi) o, s.getProvider(), algorithm);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    // Valida el camino. **Si vuelve, valio; si no, lanza.** Ver la nota de la clase.
    public final CertPathValidatorResult validate(CertPath certPath, CertPathParameters params)
            throws CertPathValidatorException, InvalidAlgorithmParameterException {
        return this.validatorSpi.engineValidate(certPath, params);
    }

    // El algoritmo por default, de la propiedad `certpathvalidator.type`. "PKIX" si no esta puesta.
    public static final String getDefaultType() {
        String t = Security.getProperty("certpathvalidator.type");
        if (t == null) {
            return "PKIX";
        }
        return t;
    }

    public final CertPathChecker getRevocationChecker() {
        return this.validatorSpi.engineGetRevocationChecker();
    }
}
