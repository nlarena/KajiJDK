package java.security;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

// Un almacen de claves y certificados: alias -> material.
//
// ===============================================================================================
// LAS DOS COSAS DISTINTAS QUE GUARDA
// ===============================================================================================
//
// Un `KeyStore` mezcla dos roles que conviene no confundir, porque tienen implicancias de seguridad
// opuestas:
//
//   - **claves privadas** con su cadena: material secreto, protegido por contraseña. Se filtra y se
//     puede suplantar a su dueño.
//   - **certificados de confianza**: material publico y **sin proteger por contraseña**. No hay nada
//     secreto adentro, pero lo que entra ahi se vuelve una raiz: una CA de mas en un truststore
//     puede emitir un certificado valido para cualquier nombre. Agregar una entrada de confianza es
//     mas peligroso que filtrar una clave, aunque parezca lo contrario.
//
// Por eso `setCertificateEntry` no pide contraseña y `setKeyEntry` si: no es una asimetria de
// comodidad, es que protegen cosas distintas.
//
// `load(stream, null)` es el otro lugar donde se pierde seguridad sin darse cuenta: con contraseña
// null el almacen se lee **sin verificar su integridad**. Es legitimo cuando solo se quieren mirar
// certificados; es un agujero si de ahi salen las anclas de confianza, porque cualquiera que pueda
// escribir el archivo puede agregar una raiz.
//
// ===============================================================================================
// A KajiLibrary subset
// ===============================================================================================
//
// **No hay ningun proveedor de `KeyStore` registrado**, asi que las sobrecargas de `getInstance`
// tiran siempre `KeyStoreException`. Leer un JKS o un PKCS#12 pide descifrado —PBES2, RC2, 3DES— y
// verificacion de un MAC, y nada de eso esta implementado; un lector que ignorara el MAC estaria
// entregando material que nadie autentico. La estructura entera esta: quien traiga un `KeyStoreSpi`
// obtiene un `KeyStore` que funciona.
//
// Faltan dos tipos anidados, los dos por dependencias que esta biblioteca no tiene:
//
//   - `SecretKeyEntry`, porque `javax.crypto.SecretKey` no existe.
//   - `PasswordProtection` y `CallbackHandlerProtection`, porque necesitan
//     `javax.security.auth.Destroyable` y `javax.security.auth.callback.CallbackHandler`.
//
// La ausencia de `PasswordProtection` arrastra a los tres `Builder.newInstance` que trabajan con
// archivos: su primer paso es comprobar que la proteccion sea de una de esas dos clases, asi que sin
// ellas nunca podrian tener exito. Se dejan afuera en vez de declararlos para que siempre fallen.
public class KeyStore {

    // Una entrada del almacen. La interfaz es el modelo moderno —uniforme, tipado— frente a los
    // metodos sueltos de Java 1.2, que siguen ahi por compatibilidad.
    public interface Entry {

        // Los atributos de la entrada. Vacio por default: los atributos son cosa de PKCS#12 y
        // llegaron en Java 8, mucho despues que esta interfaz.
        default Set<Attribute> getAttributes() {
            return Collections.<Attribute>emptySet();
        }

        // Un atributo con nombre y valor. En PKCS#12 el nombre es un OID.
        interface Attribute {

            String getName();

            String getValue();
        }
    }

    // Como se protege una entrada al leerla o escribirla. Es una interfaz marcadora: cada forma de
    // proteccion —una contraseña, un dialogo que la pide— es una clase aparte.
    public interface ProtectionParameter {
    }

    // De donde cargar o hacia donde guardar el almacen entero, con su proteccion.
    public interface LoadStoreParameter {

        // La proteccion de la integridad del almacen, o null si no hay.
        ProtectionParameter getProtectionParameter();
    }

    // Una clave privada con su cadena de certificados.
    //
    // La cadena no es opcional y el constructor lo hace cumplir: una clave privada sin el
    // certificado que publica su clave publica no le sirve a nadie, porque no habria forma de
    // verificar lo que firme. Ademas se comprueba que la clave publica del primer certificado sea
    // del mismo algoritmo que la privada —no que sean el par, que eso costaria una operacion de
    // clave publica, pero si que no sean de familias distintas—.
    public static final class PrivateKeyEntry implements Entry {

        private final PrivateKey privKey;
        private final Certificate[] chain;
        private final Set<Attribute> attributes;

        public PrivateKeyEntry(PrivateKey privateKey, Certificate[] chain) {
            this(privateKey, chain, Collections.<Attribute>emptySet());
        }

        public PrivateKeyEntry(PrivateKey privateKey, Certificate[] chain,
                               Set<Attribute> attributes) {
            if (privateKey == null || chain == null || attributes == null) {
                throw new NullPointerException("invalid null input");
            }
            if (chain.length == 0) {
                throw new IllegalArgumentException("invalid zero-length input chain");
            }
            Certificate[] copia = new Certificate[chain.length];
            System.arraycopy(chain, 0, copia, 0, chain.length);
            // Todos los certificados de la cadena tienen que ser del mismo tipo: una cadena que
            // mezcle formatos no se puede validar de punta a punta.
            String tipo = copia[0].getType();
            int i = 1;
            while (i < copia.length) {
                if (!tipo.equals(copia[i].getType())) {
                    throw new IllegalArgumentException(
                        "chain does not contain certificates of the same type");
                }
                i = i + 1;
            }
            if (!privateKey.getAlgorithm().equals(copia[0].getPublicKey().getAlgorithm())) {
                throw new IllegalArgumentException(
                    "private key algorithm does not match algorithm of public key in end entity "
                    + "certificate (at index 0)");
            }
            this.privKey = privateKey;
            this.chain = copia;
            this.attributes = Collections.unmodifiableSet(new HashSet<Attribute>(attributes));
        }

        public PrivateKey getPrivateKey() {
            return this.privKey;
        }

        // Copia de la cadena, del sujeto hacia la raiz.
        public Certificate[] getCertificateChain() {
            Certificate[] c = new Certificate[this.chain.length];
            System.arraycopy(this.chain, 0, c, 0, this.chain.length);
            return c;
        }

        // El certificado de la propia clave: el primero de la cadena.
        public Certificate getCertificate() {
            return this.chain[0];
        }

        @Override
        public Set<Attribute> getAttributes() {
            return this.attributes;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Private key entry and certificate chain with "
                + this.chain.length + " elements:\r\n");
            int i = 0;
            while (i < this.chain.length) {
                sb.append(this.chain[i].toString());
                sb.append("\r\n");
                i = i + 1;
            }
            return sb.toString();
        }
    }

    // Un certificado en el que se confia. Sin contraseña, porque no hay nada secreto que proteger
    // —lo que hay que proteger es la **integridad** del almacen, para que nadie agregue uno—.
    public static final class TrustedCertificateEntry implements Entry {

        private final Certificate cert;
        private final Set<Attribute> attributes;

        public TrustedCertificateEntry(Certificate trustedCert) {
            this(trustedCert, Collections.<Attribute>emptySet());
        }

        public TrustedCertificateEntry(Certificate trustedCert, Set<Attribute> attributes) {
            if (trustedCert == null || attributes == null) {
                throw new NullPointerException("invalid null input");
            }
            this.cert = trustedCert;
            this.attributes = Collections.unmodifiableSet(new HashSet<Attribute>(attributes));
        }

        public Certificate getTrustedCertificate() {
            return this.cert;
        }

        @Override
        public Set<Attribute> getAttributes() {
            return this.attributes;
        }

        @Override
        public String toString() {
            return "Trusted certificate entry:\r\n" + this.cert.toString();
        }
    }

    // Una fabrica perezosa de almacenes: no abre nada hasta que se lo piden.
    //
    // Existe para el caso en que la contraseña todavia no se conoce cuando se configura el sistema
    // —hay que preguntarsela a alguien— y para no tener el almacen abierto mas tiempo del necesario.
    public abstract static class Builder {

        protected Builder() {
        }

        // El almacen, cargado. Cada llamada puede devolver el mismo objeto.
        public abstract KeyStore getKeyStore() throws KeyStoreException;

        // Con que proteger la entrada de ese alias.
        public abstract ProtectionParameter getProtectionParameter(String alias)
            throws KeyStoreException;

        // Un builder sobre un almacen **ya cargado**: la proteccion es la misma para todos los
        // alias.
        public static Builder newInstance(final KeyStore keyStore,
                                          final ProtectionParameter protectionParameter) {
            if (keyStore == null || protectionParameter == null) {
                throw new NullPointerException();
            }
            return new Builder() {

                @Override
                public KeyStore getKeyStore() {
                    return keyStore;
                }

                @Override
                public ProtectionParameter getProtectionParameter(String alias) {
                    if (alias == null) {
                        throw new NullPointerException();
                    }
                    return protectionParameter;
                }
            };
        }

        // Un builder que crea el almacen recien cuando se lo piden. Sirve para almacenes que no
        // vienen de un archivo —una tarjeta, un HSM—, donde `load` no lee nada pero puede necesitar
        // una credencial.
        public static Builder newInstance(final String type, final Provider provider,
                                          final ProtectionParameter protection) {
            if (type == null || protection == null) {
                throw new NullPointerException();
            }
            return new Builder() {

                private KeyStore ks;

                @Override
                public synchronized KeyStore getKeyStore() throws KeyStoreException {
                    if (this.ks != null) {
                        return this.ks;
                    }
                    KeyStore nuevo;
                    if (provider == null) {
                        nuevo = KeyStore.getInstance(type);
                    } else {
                        nuevo = KeyStore.getInstance(type, provider);
                    }
                    try {
                        nuevo.load(new ParametrosSimples(protection));
                    } catch (Exception e) {
                        throw new KeyStoreException("KeyStore instantiation failed", e);
                    }
                    this.ks = nuevo;
                    return nuevo;
                }

                @Override
                public ProtectionParameter getProtectionParameter(String alias)
                        throws KeyStoreException {
                    if (alias == null) {
                        throw new NullPointerException();
                    }
                    // Se fuerza la creacion primero para que un error de carga salga aca y no
                    // despues, con una proteccion ya en la mano y un almacen que nunca se abrio.
                    this.getKeyStore();
                    return protection;
                }
            };
        }
    }

    // El `LoadStoreParameter` minimo: solo lleva la proteccion. Package-private porque en el JDK
    // tambien es interno.
    static final class ParametrosSimples implements LoadStoreParameter {

        private final ProtectionParameter protection;

        ParametrosSimples(ProtectionParameter protection) {
            this.protection = protection;
        }

        public ProtectionParameter getProtectionParameter() {
            return this.protection;
        }
    }

    private final KeyStoreSpi keyStoreSpi;
    private final Provider provider;
    private final String type;
    // Un almacen sin cargar no responde nada: es lo que separa "recien creado" de "vacio".
    private boolean initialized = false;

    protected KeyStore(KeyStoreSpi keyStoreSpi, Provider provider, String type) {
        this.keyStoreSpi = keyStoreSpi;
        this.provider = provider;
        this.type = type;
    }

    public static KeyStore getInstance(String type) throws KeyStoreException {
        if (type == null) {
            throw new NullPointerException("null type name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("KeyStore", type);
            if (s != null) {
                return armar(s, type);
            }
            i = i + 1;
        }
        throw new KeyStoreException(type + " not found");
    }

    public static KeyStore getInstance(String type, String provider)
            throws KeyStoreException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(type, p);
    }

    public static KeyStore getInstance(String type, Provider provider) throws KeyStoreException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (type == null) {
            throw new NullPointerException("null type name");
        }
        Provider.Service s = provider.getService("KeyStore", type);
        if (s == null) {
            throw new KeyStoreException(
                "no such type: " + type + " for provider " + provider.getName());
        }
        return armar(s, type);
    }

    private static KeyStore armar(Provider.Service s, String type) throws KeyStoreException {
        Object o;
        try {
            o = s.newInstance(null);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException(e.getMessage(), e);
        }
        if (!(o instanceof KeyStoreSpi)) {
            throw new KeyStoreException(
                "class configured for KeyStore is not a KeyStoreSpi: " + s.getClassName());
        }
        return new KeyStore((KeyStoreSpi) o, s.getProvider(), type);
    }

    // Abre un almacen adivinando su tipo a partir del contenido del archivo.
    //
    // A KajiLibrary subset: la deteccion se hace preguntandole a cada proveedor con `engineProbe`, y
    // como no hay ninguno registrado, esto siempre termina en `KeyStoreException`. La logica queda
    // escrita porque es la unica parte no trivial y no depende de saber ningun formato.
    public static final KeyStore getInstance(File file, char[] password)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        if (file == null) {
            throw new NullPointerException();
        }
        throw new KeyStoreException("Unable to determine KeyStore type: no KeyStore provider "
            + "is registered in this library");
    }

    public static final KeyStore getInstance(File file, LoadStoreParameter param)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        if (file == null) {
            throw new NullPointerException();
        }
        throw new KeyStoreException("Unable to determine KeyStore type: no KeyStore provider "
            + "is registered in this library");
    }

    // El tipo por default, de la propiedad de seguridad `keystore.type`. "pkcs12" si no esta puesta.
    public static final String getDefaultType() {
        String t = Security.getProperty("keystore.type");
        if (t == null) {
            return "pkcs12";
        }
        return t;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final String getType() {
        return this.type;
    }

    // Los atributos de esa entrada, o un conjunto vacio.
    public final Set<Entry.Attribute> getAttributes(String alias) throws KeyStoreException {
        this.exigirCargado();
        return this.keyStoreSpi.engineGetAttributes(alias);
    }

    // La clave del alias, o null si no hay ninguna con ese nombre.
    public final Key getKey(String alias, char[] password)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        this.exigirCargado();
        return this.keyStoreSpi.engineGetKey(alias, password);
    }

    // La cadena de esa clave, o null. Del sujeto hacia la raiz.
    public final Certificate[] getCertificateChain(String alias) throws KeyStoreException {
        this.exigirCargado();
        return this.keyStoreSpi.engineGetCertificateChain(alias);
    }

    // El certificado del alias. Si el alias es de una clave, devuelve el primero de su cadena.
    public final Certificate getCertificate(String alias) throws KeyStoreException {
        this.exigirCargado();
        return this.keyStoreSpi.engineGetCertificate(alias);
    }

    public final Date getCreationDate(String alias) throws KeyStoreException {
        this.exigirCargado();
        return this.keyStoreSpi.engineGetCreationDate(alias);
    }

    // Guarda una clave. Si es privada, la cadena es obligatoria.
    //
    // La cadena faltante es `IllegalArgumentException` y no `KeyStoreException`, aunque el metodo
    // declare la segunda: es un error del llamador —le falto un argumento— y no un problema del
    // almacen. Sorprende, pero es lo que hace el JDK.
    public final void setKeyEntry(String alias, Key key, char[] password, Certificate[] chain)
            throws KeyStoreException {
        this.exigirCargado();
        if (key instanceof PrivateKey && (chain == null || chain.length == 0)) {
            throw new IllegalArgumentException(
                "Private key must be accompanied by certificate chain");
        }
        this.keyStoreSpi.engineSetKeyEntry(alias, key, password, chain);
    }

    // Guarda una clave ya protegida en su formato final, sin descifrarla.
    public final void setKeyEntry(String alias, byte[] key, Certificate[] chain)
            throws KeyStoreException {
        this.exigirCargado();
        this.keyStoreSpi.engineSetKeyEntry(alias, key, chain);
    }

    // Marca un certificado como de confianza. Ver la nota de la clase: esto crea una raiz.
    //
    // No valida que el alias ni el certificado sean no nulos: la fachada solo comprueba que el
    // almacen este cargado y delega. Es lo que hace el JDK, y tiene sentido porque que sea legal
    // guardar un null depende del formato de abajo, no de esta clase.
    public final void setCertificateEntry(String alias, Certificate cert)
            throws KeyStoreException {
        this.exigirCargado();
        this.keyStoreSpi.engineSetCertificateEntry(alias, cert);
    }

    public final void deleteEntry(String alias) throws KeyStoreException {
        this.exigirCargado();
        this.keyStoreSpi.engineDeleteEntry(alias);
    }

    public final Enumeration<String> aliases() throws KeyStoreException {
        this.exigirCargado();
        return this.keyStoreSpi.engineAliases();
    }

    public final boolean containsAlias(String alias) throws KeyStoreException {
        this.exigirCargado();
        return this.keyStoreSpi.engineContainsAlias(alias);
    }

    public final int size() throws KeyStoreException {
        this.exigirCargado();
        return this.keyStoreSpi.engineSize();
    }

    public final boolean isKeyEntry(String alias) throws KeyStoreException {
        this.exigirCargado();
        return this.keyStoreSpi.engineIsKeyEntry(alias);
    }

    public final boolean isCertificateEntry(String alias) throws KeyStoreException {
        this.exigirCargado();
        return this.keyStoreSpi.engineIsCertificateEntry(alias);
    }

    // El alias del primer certificado que coincida, o null. La comparacion es por codificacion, no
    // por identidad.
    public final String getCertificateAlias(Certificate cert) throws KeyStoreException {
        this.exigirCargado();
        return this.keyStoreSpi.engineGetCertificateAlias(cert);
    }

    // Escribe el almacen y **protege su integridad** con la contraseña.
    public final void store(OutputStream stream, char[] password)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        this.exigirCargado();
        this.keyStoreSpi.engineStore(stream, password);
    }

    public final void store(LoadStoreParameter param)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        this.exigirCargado();
        this.keyStoreSpi.engineStore(param);
    }

    // Carga el almacen. **Hay que llamarlo antes que cualquier otra cosa**, incluso para crear uno
    // vacio: ahi se pasa un stream null.
    //
    // Con contraseña null no se verifica la integridad. Ver la nota de la clase.
    public final void load(InputStream stream, char[] password)
            throws IOException, NoSuchAlgorithmException, CertificateException {
        this.keyStoreSpi.engineLoad(stream, password);
        this.initialized = true;
    }

    public final void load(LoadStoreParameter param)
            throws IOException, NoSuchAlgorithmException, CertificateException {
        this.keyStoreSpi.engineLoad(param);
        this.initialized = true;
    }

    // La entrada como objeto tipado, que es el modelo moderno.
    public final Entry getEntry(String alias, ProtectionParameter protParam)
            throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException {
        if (alias == null) {
            throw new NullPointerException("invalid null input");
        }
        this.exigirCargado();
        return this.keyStoreSpi.engineGetEntry(alias, protParam);
    }

    public final void setEntry(String alias, Entry entry, ProtectionParameter protParam)
            throws KeyStoreException {
        if (alias == null || entry == null) {
            throw new NullPointerException("invalid null input");
        }
        this.exigirCargado();
        this.keyStoreSpi.engineSetEntry(alias, entry, protParam);
    }

    // Si la entrada es de ese tipo. Preguntar esto es mas barato que sacarla, porque no hace falta
    // la contraseña.
    public final boolean entryInstanceOf(String alias, Class<? extends Entry> entryClass)
            throws KeyStoreException {
        if (alias == null || entryClass == null) {
            throw new NullPointerException("invalid null input");
        }
        this.exigirCargado();
        return this.keyStoreSpi.engineEntryInstanceOf(alias, entryClass);
    }

    // Un almacen sin cargar no puede responder: no es que este vacio, es que no se sabe. Fallar aca
    // es lo unico correcto —devolver null o cero haria pasar por "no esta" a algo que si podria
    // estar—.
    private void exigirCargado() throws KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
    }
}
