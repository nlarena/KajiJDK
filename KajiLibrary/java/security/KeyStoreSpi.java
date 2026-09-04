package java.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Set;

// Lo que un proveedor tiene que escribir para ofrecer un formato de almacen de claves.
//
// La mayoria de los metodos son abstractos porque el SPI es de Java 1.2; los pocos que no lo son
// llegaron despues —las entradas tipadas de Java 5, `engineProbe` de Java 9— y tienen una
// implementacion base para no romper a los proveedores que ya existian.
//
// `engineGetEntry` y `engineSetEntry` merecen atencion: su implementacion base traduce entre el
// modelo viejo —tres metodos distintos segun el tipo de entrada— y el nuevo, uniforme. Es
// compatibilidad, no una capa util: un proveedor que sepa distinguir sus tipos hace mejor
// trabajo sobreescribiendolos.
//
// A KajiLibrary subset: la sobrecarga `engineGetEntry`/`engineSetEntry` que trabajaria con
// `KeyStore.SecretKeyEntry` no puede resolver ese caso porque `javax.crypto.SecretKey` no existe en
// esta biblioteca; el codigo de traduccion cubre claves privadas y certificados de confianza, que
// es lo que si se puede representar.
public abstract class KeyStoreSpi {

    public KeyStoreSpi() {
    }

    // La clave asociada al alias, o null si no hay. La contraseña protege **esa entrada**, no el
    // almacen entero: en PKCS#12 y en JKS cada clave privada esta cifrada por separado.
    public abstract Key engineGetKey(String alias, char[] password)
        throws NoSuchAlgorithmException, UnrecoverableKeyException;

    // La cadena de certificados de esa clave, del sujeto hacia la raiz, o null.
    public abstract Certificate[] engineGetCertificateChain(String alias);

    public abstract Certificate engineGetCertificate(String alias);

    public abstract Date engineGetCreationDate(String alias);

    // Guarda una clave con su cadena. La cadena es **obligatoria** para una clave privada: una
    // clave privada sin el certificado que la publica no sirve para nada, porque nadie podria
    // verificar lo que firme.
    public abstract void engineSetKeyEntry(String alias, Key key, char[] password,
                                           Certificate[] chain) throws KeyStoreException;

    // Guarda una clave que ya viene protegida en su formato final. El almacen no la interpreta: es
    // el camino para mover una clave entre almacenes sin descifrarla en el medio.
    public abstract void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain)
        throws KeyStoreException;

    // Guarda un certificado **de confianza**. Es la operacion mas delicada del almacen: lo que entra
    // por aca se convierte en una raiz, y una raiz de mas puede firmar un certificado para cualquier
    // nombre.
    public abstract void engineSetCertificateEntry(String alias, Certificate cert)
        throws KeyStoreException;

    public abstract void engineDeleteEntry(String alias) throws KeyStoreException;

    public abstract Enumeration<String> engineAliases();

    public abstract boolean engineContainsAlias(String alias);

    public abstract int engineSize();

    public abstract boolean engineIsKeyEntry(String alias);

    public abstract boolean engineIsCertificateEntry(String alias);

    // El alias del primer certificado que coincida, o null. La comparacion es por codificacion.
    public abstract String engineGetCertificateAlias(Certificate cert);

    public abstract void engineStore(OutputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException;

    // Guardar con parametros en vez de con una contraseña suelta. Base que lanza, porque llego
    // despues.
    public void engineStore(KeyStore.LoadStoreParameter param)
            throws IOException, NoSuchAlgorithmException, CertificateException {
        throw new UnsupportedOperationException();
    }

    // Carga el almacen. La contraseña puede ser null: en ese caso **no se verifica la integridad**
    // del archivo, solo se lee. Es legitimo cuando solo interesan los certificados publicos, y es un
    // error cuando de ahi van a salir anclas de confianza.
    public abstract void engineLoad(InputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException;

    public void engineLoad(KeyStore.LoadStoreParameter param)
            throws IOException, NoSuchAlgorithmException, CertificateException {
        throw new UnsupportedOperationException();
    }

    // Los atributos de la entrada, o un conjunto vacio. Los atributos son cosa de PKCS#12: nombre
    // amigable, identificador local.
    public Set<KeyStore.Entry.Attribute> engineGetAttributes(String alias) {
        return Collections.<KeyStore.Entry.Attribute>emptySet();
    }

    // Traduce el modelo viejo al de entradas tipadas. Un proveedor que distinga sus tipos mejor
    // que esto lo sobreescribe.
    public KeyStore.Entry engineGetEntry(String alias, KeyStore.ProtectionParameter protParam)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableEntryException {
        if (!this.engineContainsAlias(alias)) {
            return null;
        }
        if (protParam == null) {
            if (this.engineIsCertificateEntry(alias)) {
                return new KeyStore.TrustedCertificateEntry(
                    this.engineGetCertificate(alias), this.engineGetAttributes(alias));
            }
            throw new UnrecoverableKeyException(
                "requested entry requires a password");
        }
        // A KajiLibrary subset: el JDK acepta aca una `KeyStore.PasswordProtection`, que no existe
        // en esta biblioteca porque necesita `javax.security.auth.Destroyable`. Sin ella no hay
        // forma de sacar la contraseña de un `ProtectionParameter`, asi que el unico caso que se
        // puede atender es el de un certificado de confianza, que no lleva contraseña.
        if (this.engineIsCertificateEntry(alias)) {
            return new KeyStore.TrustedCertificateEntry(
                this.engineGetCertificate(alias), this.engineGetAttributes(alias));
        }
        throw new UnsupportedOperationException(
            "protection parameters are not supported by this KeyStoreSpi");
    }

    public void engineSetEntry(String alias, KeyStore.Entry entry,
                               KeyStore.ProtectionParameter protParam) throws KeyStoreException {
        if (entry == null) {
            throw new KeyStoreException("invalid null input");
        }
        // A KajiLibrary subset: el JDK acepta aca una `KeyStore.PasswordProtection` y le saca la
        // contraseña. Esa clase no existe en esta biblioteca (ver `KeyStore`), asi que cualquier
        // proteccion que no sea null se rechaza — que es lo mismo que hace el JDK con una
        // proteccion de un tipo que no conoce.
        if (protParam != null) {
            throw new KeyStoreException("unsupported protection parameter");
        }
        if (entry instanceof KeyStore.TrustedCertificateEntry) {
            KeyStore.TrustedCertificateEntry tce = (KeyStore.TrustedCertificateEntry) entry;
            this.engineSetCertificateEntry(alias, tce.getTrustedCertificate());
            return;
        }
        if (entry instanceof KeyStore.PrivateKeyEntry) {
            // Sin contraseña no se puede guardar una clave privada: quedaria en claro dentro del
            // almacen. Fallar es lo unico correcto.
            throw new KeyStoreException("non-null password required to create PrivateKeyEntry");
        }
        throw new KeyStoreException(
            "unsupported entry type: " + entry.getClass().getName());
    }

    public boolean engineEntryInstanceOf(String alias,
                                         Class<? extends KeyStore.Entry> entryClass) {
        if (entryClass == KeyStore.TrustedCertificateEntry.class) {
            return this.engineIsCertificateEntry(alias);
        }
        if (entryClass == KeyStore.PrivateKeyEntry.class) {
            return this.engineIsKeyEntry(alias) && this.engineGetCertificate(alias) != null;
        }
        return false;
    }

    // Si este stream parece ser de este formato. Sirve para que `KeyStore.getInstance(File, ...)`
    // adivine el tipo sin que se lo digan.
    //
    // Tiene que dejar el stream **como lo encontro**: se lo van a pasar a otro proveedor si este
    // dice que no.
    public boolean engineProbe(InputStream stream) throws IOException {
        return false;
    }
}
