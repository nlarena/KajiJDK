package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.Collection;

// Un repositorio del que sacar certificados y CRLs.
//
// Es de donde el constructor de caminos saca los eslabones que le faltan: la cadena que llega en un
// handshake suele estar incompleta, y hay que ir a buscar los certificados intermedios a algun
// lado.
//
// Hay una diferencia de contrato con `KeyStore` que vale marcar y que su nombre parecido esconde:
// **un `CertStore` no implica confianza**. Es una fuente de material, no una lista de anclas. Un
// certificado que salio de aca todavia tiene que validarse contra un `TrustAnchor`; tratar el
// contenido de un store como confiable seria darle a cualquiera que pueda escribir en el la
// capacidad de meter una raiz.
//
// Los metodos son thread-safe por contrato —varios hilos pueden consultar el mismo store a la vez—
// y devuelven colecciones **posiblemente vacias, nunca null**: "no encontre nada" es normal.
//
// A KajiLibrary subset: no hay ningun proveedor de `CertStore` registrado, asi que las tres
// sobrecargas de `getInstance` tiran siempre `NoSuchAlgorithmException`. Los dos tipos estandar
// —"Collection" y "LDAP"— no estan: el primero es facil pero necesitaria los selectores completos
// para filtrar, y el segundo pide una conexion de red. La estructura queda lista.
public class CertStore {

    private final CertStoreSpi storeSpi;
    private final Provider provider;
    private final String type;
    private final CertStoreParameters params;

    protected CertStore(CertStoreSpi storeSpi, Provider provider, String type,
                        CertStoreParameters params) {
        this.storeSpi = storeSpi;
        this.provider = provider;
        this.type = type;
        // Se copia porque los parametros son mutables: sin esto, cambiarlos despues de crear el
        // store cambiaria de donde lee.
        this.params = (params == null ? null : (CertStoreParameters) params.clone());
    }

    // Los certificados que cumplen el criterio. Coleccion vacia si no hay ninguno.
    public final Collection<? extends Certificate> getCertificates(CertSelector selector)
            throws CertStoreException {
        return this.storeSpi.engineGetCertificates(selector);
    }

    // Las CRLs que cumplen el criterio. Coleccion vacia si no hay ninguna.
    public final Collection<? extends CRL> getCRLs(CRLSelector selector)
            throws CertStoreException {
        return this.storeSpi.engineGetCRLs(selector);
    }

    public static CertStore getInstance(String type, CertStoreParameters params)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        if (type == null) {
            throw new NullPointerException("null type name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("CertStore", type);
            if (s != null) {
                return armar(s, type, params);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(type + " CertStore not available");
    }

    public static CertStore getInstance(String type, CertStoreParameters params, String provider)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException,
                   NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(type, params, p);
    }

    public static CertStore getInstance(String type, CertStoreParameters params, Provider provider)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (type == null) {
            throw new NullPointerException("null type name");
        }
        Provider.Service s = provider.getService("CertStore", type);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such type: " + type + " for provider " + provider.getName());
        }
        return armar(s, type, params);
    }

    private static CertStore armar(Provider.Service s, String type, CertStoreParameters params)
            throws NoSuchAlgorithmException {
        Object o = s.newInstance(params);
        if (!(o instanceof CertStoreSpi)) {
            throw new NoSuchAlgorithmException(
                "class configured for CertStore is not a CertStoreSpi: " + s.getClassName());
        }
        return new CertStore((CertStoreSpi) o, s.getProvider(), type, params);
    }

    // Copia de los parametros con los que se creo, o null si no habia.
    public final CertStoreParameters getCertStoreParameters() {
        return (this.params == null ? null : (CertStoreParameters) this.params.clone());
    }

    public final String getType() {
        return this.type;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    // El tipo por default, de la propiedad de seguridad `certstore.type`. "LDAP" si no esta puesta,
    // que es un default fosil: nadie publica certificados en LDAP hoy.
    public static final String getDefaultType() {
        String t = Security.getProperty("certstore.type");
        if (t == null) {
            return "LDAP";
        }
        return t;
    }
}
