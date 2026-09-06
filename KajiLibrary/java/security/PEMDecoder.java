package java.security;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.util.Base64;

import javax.crypto.EncryptedPrivateKeyInfo;

/**
 * Lee claves y certificados escritos en PEM.
 *
 * <h2>Que devuelve</h2>
 *
 * <p>El objeto que corresponda a la etiqueta: un certificado para {@code CERTIFICATE}, una clave
 * publica para {@code PUBLIC KEY}, una clave privada cifrada para {@code ENCRYPTED PRIVATE KEY}. Una
 * etiqueta que no conoce no es un error: devuelve un {@link PEMRecord}, que guarda el texto tal
 * cual. Eso es lo que permite leer un archivo con bloques de todo tipo sin perder los que no se
 * entienden.
 *
 * <p>La version con {@code Class} es para pedir uno en particular. {@code decode(texto,
 * PEMRecord.class)} devuelve el bloque crudo aunque la etiqueta se conozca, que es como se lee un
 * archivo sin interpretarlo.
 *
 * <h2>Lo que hay antes del bloque</h2>
 *
 * <p>Se conserva, en {@link PEMRecord#leadingData}. Un archivo PEM suele traer comentarios o la
 * salida de la herramienta que lo genero antes del {@code -----BEGIN}, y a veces eso esta firmado
 * junto con el resto. Tirarlo seria perder datos.
 *
 * <h2>Es inmutable</h2>
 *
 * <p>{@link #withFactory} y {@link #withDecryption} no cambian este decodificador: devuelven otro.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Leer el formato --encontrar el bloque, separar la etiqueta, decodificar el base64-- funciona
 * entero, y con eso andan {@link PEMRecord} y {@code ENCRYPTED PRIVATE KEY}, que es la unica
 * etiqueta cuyo objeto se puede construir sin proveedores. Las demas necesitan una
 * {@link KeyFactory} o una {@link CertificateFactory}, y no hay ninguna registrada: la busqueda se
 * hace igual y lo que falla se envuelve en {@link IllegalArgumentException}, que es lo que hace el
 * JDK cuando no encuentra con que construir. {@link #withDecryption} necesita ademas cifrado
 * basado en contrasena, que tampoco hay.
 *
 * @since 25
 */
public final class PEMDecoder {

    private static final PEMDecoder UNICO = new PEMDecoder(null, null);

    /** Los OID que se saben nombrar. Uno que no este no es un error: no se lo puede construir. */
    private static final String[][] ALGORITMOS = {
        {"1.2.840.113549.1.1.1", "RSA"},
        {"1.2.840.113549.1.1.10", "RSASSA-PSS"},
        {"1.2.840.10040.4.1", "DSA"},
        {"1.2.840.10045.2.1", "EC"},
        {"1.2.840.113549.1.3.1", "DiffieHellman"},
        {"1.3.101.110", "X25519"},
        {"1.3.101.111", "X448"},
        {"1.3.101.112", "Ed25519"},
        {"1.3.101.113", "Ed448"},
    };

    private final Provider factory;
    private final char[] password;

    private PEMDecoder(Provider factory, char[] password) {
        this.factory = factory;
        this.password = password;
    }

    /**
     * El decodificador de siempre.
     *
     * <p>Devuelve siempre el mismo objeto.
     *
     * @return el decodificador
     */
    public static PEMDecoder of() {
        return UNICO;
    }

    /**
     * Lee el primer bloque de ese texto.
     *
     * @param str el texto
     * @return lo que decia el bloque
     * @throws NullPointerException si el texto es {@code null}
     * @throws IllegalArgumentException si no hay un bloque, o si no se lo puede construir
     */
    public DEREncodable decode(String str) {
        if (str == null) {
            throw new NullPointerException("str");
        }
        return construir(partir(str.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Lo mismo, leyendo de un flujo.
     *
     * @param is de donde leer
     * @return lo que decia el bloque
     * @throws IOException si falla la lectura
     * @throws NullPointerException si el flujo es {@code null}
     * @throws IllegalArgumentException si no hay un bloque, o si no se lo puede construir
     */
    public DEREncodable decode(InputStream is) throws IOException {
        if (is == null) {
            throw new NullPointerException("is");
        }
        return construir(partir(todo(is)));
    }

    /**
     * Lee el primer bloque y lo devuelve como esa clase.
     *
     * @param <S> lo que se espera
     * @param str el texto
     * @param tClass que se espera
     * @return lo que decia el bloque
     * @throws NullPointerException si alguno de los dos es {@code null}
     * @throws IllegalArgumentException si no hay un bloque, o si no se lo puede construir
     * @throws ClassCastException si lo que hay no es de esa clase
     */
    public <S extends DEREncodable> S decode(String str, Class<S> tClass) {
        if (tClass == null) {
            throw new NullPointerException("tClass");
        }
        if (str == null) {
            throw new NullPointerException("str");
        }
        return convertir(partir(str.getBytes(StandardCharsets.UTF_8)), tClass);
    }

    /**
     * Lo mismo, leyendo de un flujo.
     *
     * @param <S> lo que se espera
     * @param is de donde leer
     * @param tClass que se espera
     * @return lo que decia el bloque
     * @throws IOException si falla la lectura
     * @throws NullPointerException si alguno de los dos es {@code null}
     * @throws IllegalArgumentException si no hay un bloque, o si no se lo puede construir
     * @throws ClassCastException si lo que hay no es de esa clase
     */
    public <S extends DEREncodable> S decode(InputStream is, Class<S> tClass) throws IOException {
        if (tClass == null) {
            throw new NullPointerException("tClass");
        }
        if (is == null) {
            throw new NullPointerException("is");
        }
        return convertir(partir(todo(is)), tClass);
    }

    /**
     * Otro decodificador que construye con las fabricas de ese proveedor.
     *
     * @param provider el proveedor
     * @return el otro decodificador
     * @throws NullPointerException si el proveedor es {@code null}
     */
    public PEMDecoder withFactory(Provider provider) {
        if (provider == null) {
            throw new NullPointerException("provider");
        }
        return new PEMDecoder(provider, this.password);
    }

    /**
     * Otro decodificador que descifra las claves privadas con esa contrasena.
     *
     * @param password la contrasena
     * @return el otro decodificador
     * @throws NullPointerException si la contrasena es {@code null}
     */
    public PEMDecoder withDecryption(char[] password) {
        if (password == null) {
            throw new NullPointerException("password");
        }
        return new PEMDecoder(this.factory, password.clone());
    }

    /**
     * Encuentra el bloque y lo parte en etiqueta, contenido y lo que venia antes.
     *
     * <p>Se trabaja sobre bytes y no sobre texto porque lo que viene antes del bloque puede no ser
     * texto valido, y hay que devolverlo tal cual.
     */
    private static PEMRecord partir(byte[] datos) {
        final String texto = new String(datos, StandardCharsets.UTF_8);
        if (datos.length == 0) {
            throw new IllegalArgumentException(new EOFException("No data available"));
        }
        final int inicio = texto.indexOf("-----BEGIN ");
        if (inicio < 0) {
            throw new IllegalArgumentException(new EOFException("No PEM data found"));
        }
        final int finEtiqueta = texto.indexOf("-----", inicio + 11);
        if (finEtiqueta < 0) {
            throw new IllegalArgumentException(new EOFException("No PEM data found"));
        }
        final String tipo = texto.substring(inicio + 11, finEtiqueta);
        final String cierre = "-----END " + tipo + "-----";
        final int fin = texto.indexOf(cierre, finEtiqueta);
        if (fin < 0) {
            throw new IllegalArgumentException(new EOFException("No PEM data found"));
        }
        final StringBuilder cuerpo = new StringBuilder();
        for (int i = finEtiqueta + 5; i < fin; i++) {
            final char c = texto.charAt(i);
            if (c != '\n' && c != '\r' && c != ' ' && c != '\t') {
                cuerpo.append(c);
            }
        }
        byte[] antes = null;
        if (inicio > 0) {
            antes = new byte[inicio];
            System.arraycopy(datos, 0, antes, 0, inicio);
        }
        return new PEMRecord(tipo, cuerpo.toString(), antes);
    }

    /** El objeto que corresponde a la etiqueta, o el bloque crudo si no se la conoce. */
    private DEREncodable construir(PEMRecord r) {
        final String tipo = r.type();
        if ("ENCRYPTED PRIVATE KEY".equals(tipo)) {
            try {
                return new EncryptedPrivateKeyInfo(contenido(r));
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
        if ("CERTIFICATE".equals(tipo) || "X509 CERTIFICATE".equals(tipo)) {
            try {
                return (DEREncodable) fabricaCert().generateCertificate(
                        new java.io.ByteArrayInputStream(contenido(r)));
            } catch (java.security.cert.CertificateException e) {
                throw new IllegalArgumentException(e);
            }
        }
        if ("X509 CRL".equals(tipo)) {
            try {
                return (DEREncodable) fabricaCert().generateCRL(
                        new java.io.ByteArrayInputStream(contenido(r)));
            } catch (java.security.cert.CertificateException e) {
                throw new IllegalArgumentException(e);
            } catch (java.security.cert.CRLException e) {
                throw new IllegalArgumentException(e);
            }
        }
        if ("PUBLIC KEY".equals(tipo)) {
            return clave(r, true);
        }
        if ("PRIVATE KEY".equals(tipo)) {
            return clave(r, false);
        }
        return r;
    }

    /**
     * Arma la clave con la fabrica del algoritmo que diga la codificacion.
     *
     * <p>El algoritmo no viene en la etiqueta --el PEM dice "PUBLIC KEY" y nada mas-- asi que hay
     * que sacarlo de adentro: es el identificador de objeto del primer campo de la estructura.
     */
    private DEREncodable clave(PEMRecord r, boolean publica) {
        final byte[] datos = contenido(r);
        final String oid;
        try {
            oid = oidDe(datos, publica);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    new IOException("No recognized algorithm detected in encoding"));
        }
        final String nombre = nombreDe(oid);
        if (nombre == null) {
            throw new IllegalArgumentException(
                    new IOException("No recognized algorithm detected in encoding"));
        }
        try {
            final KeyFactory kf = this.factory == null
                    ? KeyFactory.getInstance(nombre)
                    : KeyFactory.getInstance(nombre, this.factory);
            return publica
                    ? (DEREncodable) kf.generatePublic(
                            new java.security.spec.X509EncodedKeySpec(datos))
                    : (DEREncodable) kf.generatePrivate(
                            new java.security.spec.PKCS8EncodedKeySpec(datos));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        } catch (java.security.spec.InvalidKeySpecException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private CertificateFactory fabricaCert() throws java.security.cert.CertificateException {
        return this.factory == null
                ? CertificateFactory.getInstance("X.509")
                : CertificateFactory.getInstance("X.509", this.factory);
    }

    private <S extends DEREncodable> S convertir(PEMRecord r, Class<S> tClass) {
        if (tClass == PEMRecord.class) {
            return tClass.cast(r);
        }
        return tClass.cast(construir(r));
    }

    private static byte[] contenido(PEMRecord r) {
        try {
            return Base64.getDecoder().decode(r.content());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(new IOException("Invalid Base64 data"));
        }
    }

    private static String nombreDe(String oid) {
        for (int i = 0; i < ALGORITMOS.length; i++) {
            if (ALGORITMOS[i][0].equals(oid)) {
                return ALGORITMOS[i][1];
            }
        }
        return null;
    }

    /**
     * El identificador del algoritmo que hay adentro de la codificacion.
     *
     * <p>Una clave publica es {@code SEQUENCE { AlgorithmIdentifier, BIT STRING }} y una privada es
     * {@code SEQUENCE { INTEGER, AlgorithmIdentifier, OCTET STRING }}: la unica diferencia para
     * esto es que en la privada hay que saltear el numero de version.
     */
    private static String oidDe(byte[] b, boolean publica) throws IOException {
        int p = contenidoDe(b, 0, 0x30);
        if (!publica) {
            final int[] version = tlv(b, p);
            if (version[0] != 0x02) {
                throw new IOException("falta la version");
            }
            p = version[2] + version[1];
        }
        final int alg = contenidoDe(b, p, 0x30);
        final int[] id = tlv(b, alg);
        if (id[0] != 0x06) {
            throw new IOException("falta el identificador de algoritmo");
        }
        return oid(b, id[2], id[1]);
    }

    /** La posicion del contenido de un TLV de esa etiqueta que empieza ahi. */
    private static int contenidoDe(byte[] b, int desde, int etiqueta) throws IOException {
        final int[] t = tlv(b, desde);
        if (t[0] != etiqueta) {
            throw new IOException("se esperaba " + etiqueta + " y hay " + t[0]);
        }
        return t[2];
    }

    /** La etiqueta, el largo y donde empieza el contenido del TLV que empieza ahi. */
    private static int[] tlv(byte[] b, int desde) throws IOException {
        if (desde + 1 >= b.length) {
            throw new EOFException("se termino antes de la etiqueta");
        }
        final int etiqueta = b[desde] & 0xff;
        int p = desde + 1;
        int n = b[p] & 0xff;
        p++;
        if (n >= 0x80) {
            final int octetos = n - 0x80;
            if (octetos == 0 || octetos > 4 || p + octetos > b.length) {
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
            throw new EOFException("se termino antes del contenido");
        }
        return new int[] {etiqueta, n, p};
    }

    private static String oid(byte[] b, int desde, int largo) throws IOException {
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

    private static byte[] todo(InputStream is) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buf = new byte[4096];
        int n = is.read(buf);
        while (n > 0) {
            out.write(buf, 0, n);
            n = is.read(buf);
        }
        return out.toByteArray();
    }
}
