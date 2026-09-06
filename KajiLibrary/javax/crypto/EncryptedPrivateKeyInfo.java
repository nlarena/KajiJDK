package javax.crypto;

import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.DEREncodable;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * Una clave privada guardada cifrada, con la etiqueta de como se la cifro.
 *
 * <h2>Que es</h2>
 *
 * <p>La estructura de PKCS#8: una identificacion de algoritmo y un bloque de bytes cifrados. Es lo
 * que hay adentro de un archivo de clave privada protegido por contrasena --lo que se ve como
 * {@code -----BEGIN ENCRYPTED PRIVATE KEY-----}--.
 *
 * <p>La identificacion viaja en claro y tiene que viajar: sin ella no habria forma de saber con que
 * descifrar. Incluye los parametros --la sal y la cantidad de vueltas, si es una contrasena-- que
 * son publicos por diseno.
 *
 * <h2>Por que el algoritmo se guarda como texto</h2>
 *
 * <p>Porque en el archivo es un identificador de objeto, una lista de numeros. {@link #getAlgName}
 * lo traduce al nombre si lo conoce, y si no devuelve los numeros con puntos. Devolver los numeros
 * es mejor que fallar: el archivo se puede seguir leyendo y guardando aunque esta biblioteca no
 * conozca ese algoritmo.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Leer, escribir y traducir identificadores funciona de verdad, y es lo que hace falta para
 * manejar el archivo. Descifrar no: {@link #getKeySpec(Cipher)} necesita un cifrador ya armado, y
 * los demas necesitan armarlo, para lo cual hace falta un proveedor que ofrezca cifrados --ver la
 * nota de {@link Cipher}--. La tabla de nombres tiene los identificadores de PKCS#5 y PKCS#12, que
 * son los que aparecen en estos archivos; un nombre que no este es un
 * {@link NoSuchAlgorithmException} y no un identificador inventado.
 *
 * @since 1.4
 */
public class EncryptedPrivateKeyInfo implements DEREncodable {

    private static final Map<String, String> NOMBRE_A_OID = new HashMap<String, String>();
    private static final Map<String, String> OID_A_NOMBRE = new HashMap<String, String>();

    static {
        // Los nombres y los identificadores estan tomados del JDK 25, uno por uno. Donde el JDK
        // acepta un nombre pero devuelve otro al releer --DES da DES/CBC-- se anota igual, porque
        // esa asimetria tambien es parte del comportamiento.
        registrar("PBEWithMD5AndDES", "1.2.840.113549.1.5.3", true);
        registrar("PBEWithSHA1AndDESede", "1.2.840.113549.1.12.1.3", true);
        registrar("PBEWithSHA1AndRC2_40", "1.2.840.113549.1.12.1.6", true);
        registrar("PBEWithSHA1AndRC2_128", "1.2.840.113549.1.12.1.5", true);
        registrar("PBEWithSHA1AndRC4_40", "1.2.840.113549.1.12.1.2", true);
        registrar("PBEWithSHA1AndRC4_128", "1.2.840.113549.1.12.1.1", true);
        registrar("PBES2", "1.2.840.113549.1.5.13", true);
        registrar("PBKDF2WithHmacSHA1", "1.2.840.113549.1.5.12", true);
        registrar("AES", "2.16.840.1.101.3.4.1", true);
        registrar("AES_128/CBC/NoPadding", "2.16.840.1.101.3.4.1.2", true);
        registrar("AES_256/CBC/NoPadding", "2.16.840.1.101.3.4.1.42", true);
        registrar("DESede", "1.3.14.3.2.17", true);
        registrar("DES/CBC", "1.3.14.3.2.7", true);
        registrar("DES", "1.3.14.3.2.7", false);
        registrar("RC2/CBC/PKCS5Padding", "1.2.840.113549.3.2", true);
        registrar("RC2", "1.2.840.113549.3.2", false);
        registrar("Blowfish", "1.3.6.1.4.1.3029.1.1.2", true);
        registrar("HmacSHA256", "1.2.840.113549.2.9", true);
        registrar("EC", "1.2.840.10045.2.1", true);
        registrar("DiffieHellman", "1.2.840.113549.1.3.1", true);
    }

    private final String oid;
    private final byte[] paramsDer;
    private final byte[] encryptedData;

    /**
     * Uno leido de su codificacion.
     *
     * @param encoded los bytes
     * @throws IOException si no es una estructura valida
     * @throws NullPointerException si los bytes son {@code null}
     */
    public EncryptedPrivateKeyInfo(byte[] encoded) throws IOException {
        if (encoded == null) {
            throw new NullPointerException("the encoded parameter must be non-null");
        }
        final byte[] copia = encoded.clone();
        final Der raiz = Der.leer(copia, 0);
        if (raiz.etiqueta != 0x30) {
            throw new IOException("no es una SEQUENCE");
        }
        final Der alg = Der.leer(copia, raiz.contenido);
        if (alg.etiqueta != 0x30) {
            throw new IOException("el AlgorithmIdentifier no es una SEQUENCE");
        }
        final Der id = Der.leer(copia, alg.contenido);
        if (id.etiqueta != 0x06) {
            throw new IOException("falta el identificador de algoritmo");
        }
        this.oid = Der.oid(copia, id.contenido, id.largo);
        final int finAlg = alg.contenido + alg.largo;
        final int desdeParams = id.contenido + id.largo;
        if (desdeParams < finAlg) {
            this.paramsDer = new byte[finAlg - desdeParams];
            System.arraycopy(copia, desdeParams, this.paramsDer, 0, this.paramsDer.length);
        } else {
            this.paramsDer = null;
        }
        final Der datos = Der.leer(copia, finAlg);
        if (datos.etiqueta != 0x04) {
            throw new IOException("los datos cifrados no son una OCTET STRING");
        }
        this.encryptedData = new byte[datos.largo];
        System.arraycopy(copia, datos.contenido, this.encryptedData, 0, datos.largo);
    }

    /**
     * Uno con ese algoritmo y esos datos.
     *
     * @param algName el nombre del algoritmo, o su identificador con puntos
     * @param encryptedData los datos cifrados
     * @throws NoSuchAlgorithmException si el nombre no esta en la tabla y no es un identificador
     * @throws NullPointerException si alguno de los dos es {@code null}
     * @throws IllegalArgumentException si los datos estan vacios
     */
    public EncryptedPrivateKeyInfo(String algName, byte[] encryptedData)
            throws NoSuchAlgorithmException {
        if (algName == null) {
            throw new NullPointerException("the algName parameter must be non-null");
        }
        if (encryptedData == null) {
            throw new NullPointerException("the encryptedData parameter must be non-null");
        }
        if (encryptedData.length == 0) {
            throw new IllegalArgumentException("the encryptedData parameter must not be empty");
        }
        this.oid = aOid(algName);
        this.paramsDer = null;
        this.encryptedData = encryptedData.clone();
    }

    /**
     * Uno con esos parametros y esos datos.
     *
     * @param algParams los parametros, de donde salen el algoritmo y su configuracion
     * @param encryptedData los datos cifrados
     * @throws NoSuchAlgorithmException si el algoritmo de los parametros no esta en la tabla
     * @throws NullPointerException si alguno de los dos es {@code null}
     * @throws IllegalArgumentException si los datos estan vacios
     */
    public EncryptedPrivateKeyInfo(AlgorithmParameters algParams, byte[] encryptedData)
            throws NoSuchAlgorithmException {
        if (algParams == null) {
            throw new NullPointerException("algParams must be non-null");
        }
        if (encryptedData == null) {
            throw new NullPointerException("encryptedData must be non-null");
        }
        if (encryptedData.length == 0) {
            throw new IllegalArgumentException("the encryptedData parameter must not be empty");
        }
        this.oid = aOid(algParams.getAlgorithm());
        byte[] p;
        try {
            p = algParams.getEncoded();
        } catch (IOException e) {
            p = null;
        }
        this.paramsDer = p;
        this.encryptedData = encryptedData.clone();
    }

    /**
     * Con que algoritmo se cifro.
     *
     * @return el nombre si esta en la tabla, o el identificador con puntos
     */
    public String getAlgName() {
        final String n = OID_A_NOMBRE.get(this.oid);
        return n == null ? this.oid : n;
    }

    /**
     * Los parametros del algoritmo.
     *
     * @return los parametros, o {@code null} si no hay ninguno o si no se los pudo interpretar
     */
    public AlgorithmParameters getAlgParameters() {
        if (this.paramsDer == null) {
            return null;
        }
        try {
            final AlgorithmParameters p = AlgorithmParameters.getInstance(getAlgName());
            p.init(this.paramsDer);
            return p;
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Los bytes cifrados.
     *
     * @return una copia
     */
    public byte[] getEncryptedData() {
        return this.encryptedData.clone();
    }

    /**
     * Descifra y devuelve la clave privada en su forma codificada.
     *
     * @param cipher el cifrador, ya configurado para descifrar
     * @return la clave privada codificada
     * @throws InvalidKeySpecException si lo descifrado no es una clave privada codificada, que es lo
     *     que pasa cuando la clave de descifrado esta mal
     * @throws NullPointerException si el cifrador es {@code null}
     */
    public PKCS8EncodedKeySpec getKeySpec(Cipher cipher) throws InvalidKeySpecException {
        if (cipher == null) {
            throw new NullPointerException("cipher must be non-null");
        }
        final byte[] claro;
        try {
            claro = cipher.doFinal(this.encryptedData);
        } catch (GeneralSecurityException e) {
            throw new InvalidKeySpecException("Cannot retrieve the PKCS8EncodedKeySpec", e);
        }
        return aKeySpec(claro);
    }

    /**
     * Cifra una clave privada con una contrasena.
     *
     * @param key la clave a cifrar
     * @param password la contrasena
     * @param algorithm con que algoritmo, o {@code null} para el de omision
     * @param spec los parametros, o {@code null}
     * @param provider que proveedor usar, o {@code null} para buscar entre todos
     * @return la clave cifrada
     * @throws IllegalArgumentException si la clave o la contrasena son {@code null}
     * @throws UnsupportedOperationException siempre: hace falta un cifrado basado en contrasena, y
     *     ningun proveedor registrado ofrece cifrados
     */
    public static EncryptedPrivateKeyInfo encryptKey(PrivateKey key, char[] password,
            String algorithm, AlgorithmParameterSpec spec, Provider provider) {
        if (key == null || password == null) {
            throw new IllegalArgumentException("key and password must be non-null");
        }
        throw new UnsupportedOperationException(SIN_CIFRADO);
    }

    /**
     * Lo mismo, con el algoritmo y los parametros de omision.
     *
     * @param key la clave a cifrar
     * @param password la contrasena
     * @return la clave cifrada
     * @throws IllegalArgumentException si la clave o la contrasena son {@code null}
     * @throws UnsupportedOperationException siempre: ver la otra version
     */
    public static EncryptedPrivateKeyInfo encryptKey(PrivateKey key, char[] password) {
        return encryptKey(key, password, null, null, null);
    }

    /**
     * Cifra una clave privada con otra clave.
     *
     * @param key la clave a cifrar
     * @param encKey con que clave cifrarla
     * @param algorithm con que algoritmo, o {@code null} para el de omision
     * @param spec los parametros, o {@code null}
     * @param provider que proveedor usar, o {@code null} para buscar entre todos
     * @param random de donde sacar el azar, o {@code null}
     * @return la clave cifrada
     * @throws IllegalArgumentException si alguna de las dos claves es {@code null}
     * @throws UnsupportedOperationException siempre: ningun proveedor registrado ofrece cifrados
     */
    public static EncryptedPrivateKeyInfo encryptKey(PrivateKey key, Key encKey, String algorithm,
            AlgorithmParameterSpec spec, Provider provider, SecureRandom random) {
        if (key == null || encKey == null) {
            throw new IllegalArgumentException("key and encKey must be non-null");
        }
        throw new UnsupportedOperationException(SIN_CIFRADO);
    }

    /**
     * Descifra la clave privada con una contrasena.
     *
     * @param password la contrasena
     * @return la clave privada
     * @throws GeneralSecurityException si no se la puede descifrar; en esta biblioteca, siempre,
     *     porque no hay proveedor que ofrezca el cifrado basado en contrasena
     * @throws NullPointerException si la contrasena es {@code null}
     */
    public PrivateKey getKey(char[] password) throws GeneralSecurityException {
        if (password == null) {
            throw new NullPointerException("password must be non-null");
        }
        throw new NoSuchAlgorithmException(getAlgName() + " Cipher not available");
    }

    /**
     * Descifra la clave privada con otra clave.
     *
     * @param decryptKey con que clave descifrarla
     * @param provider que proveedor usar, o {@code null} para buscar entre todos
     * @return la clave privada
     * @throws GeneralSecurityException si no se la puede descifrar
     * @throws NullPointerException si la clave es {@code null}
     */
    public PrivateKey getKey(Key decryptKey, Provider provider) throws GeneralSecurityException {
        if (decryptKey == null) {
            throw new NullPointerException("decryptKey must be non-null");
        }
        throw new NoSuchAlgorithmException(getAlgName() + " Cipher not available");
    }

    /**
     * Descifra y devuelve la clave privada codificada, armando el cifrador solo.
     *
     * @param decryptKey con que clave descifrarla
     * @return la clave privada codificada
     * @throws NoSuchAlgorithmException si ningun proveedor tiene ese cifrado
     * @throws InvalidKeyException si la clave no sirve
     * @throws NullPointerException si la clave es {@code null}
     */
    public PKCS8EncodedKeySpec getKeySpec(Key decryptKey)
            throws NoSuchAlgorithmException, InvalidKeyException {
        return conCifrador(cifrador(decryptKey, (Provider) null), decryptKey);
    }

    /**
     * Lo mismo, con un proveedor nombrado.
     *
     * @param decryptKey con que clave descifrarla
     * @param providerName el nombre del proveedor
     * @return la clave privada codificada
     * @throws NoSuchProviderException si no hay un proveedor con ese nombre
     * @throws NoSuchAlgorithmException si ese proveedor no tiene ese cifrado
     * @throws InvalidKeyException si la clave no sirve
     * @throws NullPointerException si la clave o el nombre son {@code null}
     */
    public PKCS8EncodedKeySpec getKeySpec(Key decryptKey, String providerName)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException {
        if (providerName == null) {
            throw new NullPointerException("providerName must be non-null");
        }
        final Provider p = java.security.Security.getProvider(providerName);
        if (p == null) {
            throw new NoSuchProviderException("No such provider: " + providerName);
        }
        return getKeySpec(decryptKey, p);
    }

    /**
     * Lo mismo, con un proveedor.
     *
     * @param decryptKey con que clave descifrarla
     * @param provider el proveedor
     * @return la clave privada codificada
     * @throws NoSuchAlgorithmException si ese proveedor no tiene ese cifrado
     * @throws InvalidKeyException si la clave no sirve
     * @throws NullPointerException si la clave o el proveedor son {@code null}
     */
    public PKCS8EncodedKeySpec getKeySpec(Key decryptKey, Provider provider)
            throws NoSuchAlgorithmException, InvalidKeyException {
        if (provider == null) {
            throw new NullPointerException("provider must be non-null");
        }
        return conCifrador(cifrador(decryptKey, provider), decryptKey);
    }

    /**
     * La codificacion de esta estructura.
     *
     * <p>Los parametros del algoritmo se vuelven a escribir tal como llegaron, byte por byte. No es
     * pereza: interpretarlos y volver a armarlos podria cambiar la codificacion, y esta estructura
     * suele ir firmada.
     *
     * @return los bytes
     * @throws IOException si no se la puede codificar
     */
    public byte[] getEncoded() throws IOException {
        final byte[] idDer = Der.escribirOid(this.oid);
        final int largoAlg = idDer.length + (this.paramsDer == null ? 0 : this.paramsDer.length);
        final byte[] alg = Der.envolver(0x30, unir(idDer, this.paramsDer), largoAlg);
        final byte[] datos = Der.envolver(0x04, this.encryptedData, this.encryptedData.length);
        return Der.envolver(0x30, unir(alg, datos), alg.length + datos.length);
    }

    private static final String SIN_CIFRADO =
            "no hay cifrado: ningun proveedor registrado ofrece el servicio Cipher";

    private PKCS8EncodedKeySpec conCifrador(Cipher c, Key decryptKey)
            throws NoSuchAlgorithmException, InvalidKeyException {
        c.init(Cipher.DECRYPT_MODE, decryptKey);
        try {
            return aKeySpec(c.doFinal(this.encryptedData));
        } catch (GeneralSecurityException e) {
            throw new InvalidKeyException("Cannot retrieve the PKCS8EncodedKeySpec", e);
        } catch (InvalidKeySpecException e) {
            throw new InvalidKeyException("Cannot retrieve the PKCS8EncodedKeySpec", e);
        }
    }

    private Cipher cifrador(Key decryptKey, Provider provider)
            throws NoSuchAlgorithmException, InvalidKeyException {
        if (decryptKey == null) {
            throw new NullPointerException("decryptKey must be non-null");
        }
        try {
            return provider == null
                    ? Cipher.getInstance(getAlgName())
                    : Cipher.getInstance(getAlgName(), provider);
        } catch (NoSuchPaddingException e) {
            throw new NoSuchAlgorithmException(e.getMessage());
        }
    }

    /** Lo descifrado tiene que ser una clave privada codificada, o la clave estaba mal. */
    private static PKCS8EncodedKeySpec aKeySpec(byte[] claro) throws InvalidKeySpecException {
        try {
            final Der d = Der.leer(claro, 0);
            if (d.etiqueta != 0x30 || d.contenido + d.largo != claro.length) {
                throw new InvalidKeySpecException("Cannot retrieve the PKCS8EncodedKeySpec");
            }
        } catch (IOException e) {
            throw new InvalidKeySpecException("Cannot retrieve the PKCS8EncodedKeySpec", e);
        }
        return new PKCS8EncodedKeySpec(claro);
    }

    private static void registrar(String nombre, String oid, boolean canonico) {
        NOMBRE_A_OID.put(nombre, oid);
        if (canonico) {
            OID_A_NOMBRE.put(oid, nombre);
        }
    }

    /** El identificador de ese nombre, o el nombre mismo si ya es un identificador. */
    private static String aOid(String algName) throws NoSuchAlgorithmException {
        final String oid = NOMBRE_A_OID.get(algName);
        if (oid != null) {
            return oid;
        }
        if (esOid(algName)) {
            return algName;
        }
        throw new NoSuchAlgorithmException("unrecognized algorithm name: " + algName);
    }

    private static boolean esOid(String s) {
        if (s.isEmpty()) {
            return false;
        }
        final String[] partes = s.split("[.]", -1);
        if (partes.length < 2) {
            return false;
        }
        for (int i = 0; i < partes.length; i++) {
            if (partes[i].isEmpty()) {
                return false;
            }
            for (int j = 0; j < partes[i].length(); j++) {
                final char c = partes[i].charAt(j);
                if (c < '0' || c > '9') {
                    return false;
                }
            }
        }
        return true;
    }

    private static byte[] unir(byte[] a, byte[] b) {
        if (b == null) {
            return a;
        }
        final byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    /**
     * Lo minimo de DER que hace falta para esta estructura.
     *
     * <p>Solo formas definidas y solo tres etiquetas: SEQUENCE, OBJECT IDENTIFIER y OCTET STRING.
     * Los parametros del algoritmo no se interpretan --se copian-- asi que no hace falta mas.
     */
    private static final class Der {

        final int etiqueta;
        final int largo;
        final int contenido;

        private Der(int etiqueta, int largo, int contenido) {
            this.etiqueta = etiqueta;
            this.largo = largo;
            this.contenido = contenido;
        }

        /** Lee la etiqueta y el largo que empiezan en esa posicion. */
        static Der leer(byte[] b, int desde) throws IOException {
            if (desde + 1 >= b.length) {
                throw new java.io.EOFException("se termino antes de la etiqueta");
            }
            final int etiqueta = b[desde] & 0xff;
            int p = desde + 1;
            int n = b[p] & 0xff;
            p++;
            if (n == 0x80) {
                throw new IOException("forma indefinida: no es DER");
            }
            if (n > 0x80) {
                final int octetos = n - 0x80;
                if (octetos > 4 || p + octetos > b.length) {
                    throw new IOException("largo fuera de rango");
                }
                n = 0;
                for (int i = 0; i < octetos; i++) {
                    n = (n << 8) | (b[p] & 0xff);
                    p++;
                }
                if (n < 0) {
                    throw new IOException("largo fuera de rango");
                }
            }
            if (p + n > b.length) {
                throw new java.io.EOFException("se termino antes del contenido");
            }
            return new Der(etiqueta, n, p);
        }

        /** El identificador de objeto que hay en esa porcion, con puntos. */
        static String oid(byte[] b, int desde, int largo) throws IOException {
            if (largo == 0) {
                throw new IOException("identificador vacio");
            }
            final StringBuilder s = new StringBuilder();
            final int primero = b[desde] & 0xff;
            s.append(primero / 40).append('.').append(primero % 40);
            long v = 0;
            for (int i = desde + 1; i < desde + largo; i++) {
                final int c = b[i] & 0xff;
                v = (v << 7) | (c & 0x7f);
                if ((c & 0x80) == 0) {
                    s.append('.').append(v);
                    v = 0;
                }
            }
            return s.toString();
        }

        /** Escribe un identificador de objeto dado con puntos. */
        static byte[] escribirOid(String oid) {
            final String[] partes = oid.split("[.]", -1);
            final java.io.ByteArrayOutputStream cuerpo = new java.io.ByteArrayOutputStream();
            cuerpo.write(Integer.parseInt(partes[0]) * 40 + Integer.parseInt(partes[1]));
            for (int i = 2; i < partes.length; i++) {
                escribirBase128(cuerpo, Long.parseLong(partes[i]));
            }
            final byte[] c = cuerpo.toByteArray();
            return envolver(0x06, c, c.length);
        }

        /** Un valor en base 128, con el bit de arriba prendido salvo en el ultimo octeto. */
        private static void escribirBase128(java.io.ByteArrayOutputStream out, long v) {
            int octetos = 1;
            long t = v >>> 7;
            while (t != 0) {
                octetos++;
                t = t >>> 7;
            }
            for (int i = octetos - 1; i >= 0; i--) {
                int c = (int) ((v >>> (7 * i)) & 0x7f);
                if (i != 0) {
                    c = c | 0x80;
                }
                out.write(c);
            }
        }

        /** Envuelve los primeros bytes de un arreglo con esa etiqueta y su largo. */
        static byte[] envolver(int etiqueta, byte[] contenido, int largo) {
            final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            out.write(etiqueta);
            if (largo < 128) {
                out.write(largo);
            } else {
                int octetos = 1;
                int t = largo >>> 8;
                while (t != 0) {
                    octetos++;
                    t = t >>> 8;
                }
                out.write(0x80 | octetos);
                for (int i = octetos - 1; i >= 0; i--) {
                    out.write((largo >>> (8 * i)) & 0xff);
                }
            }
            out.write(contenido, 0, largo);
            return out.toByteArray();
        }
    }
}
