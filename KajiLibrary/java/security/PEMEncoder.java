package java.security;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.EncryptedPrivateKeyInfo;

/**
 * Escribe claves y certificados en PEM: base64 entre dos lineas de guiones.
 *
 * <h2>Para que existe un formato de texto</h2>
 *
 * <p>Porque lo binario no sobrevive al viaje. Una clave en DER pasada por un correo, un formulario o
 * un archivo de configuracion se corrompe; en base64 con dos lineas que la delimitan, no. Todo el
 * formato es eso: una etiqueta que dice que hay adentro y el contenido en base64 cortado en lineas
 * de sesenta y cuatro caracteres.
 *
 * <h2>Es inmutable</h2>
 *
 * <p>{@link #withEncryption} no cambia este codificador: devuelve otro. Es lo que permite tener uno
 * solo compartido --el de {@link #of}-- sin que nadie pueda reconfigurarlo por debajo.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Codificar funciona para todo lo que sepa dar sus bytes: {@link PEMRecord},
 * {@link EncryptedPrivateKeyInfo}, las claves y los certificados. Lo que no anda es
 * {@link #withEncryption}, que necesita cifrado basado en contrasena, y ningun proveedor registrado
 * ofrece cifrados --ver la nota de {@code javax.crypto.Cipher}--.
 *
 * @since 25
 */
public final class PEMEncoder {

    private static final PEMEncoder UNICO = new PEMEncoder(null);

    private final char[] password;

    private PEMEncoder(char[] password) {
        this.password = password;
    }

    /**
     * El codificador de siempre.
     *
     * <p>Devuelve siempre el mismo objeto: no tiene estado que valga la pena duplicar.
     *
     * @return el codificador
     */
    public static PEMEncoder of() {
        return UNICO;
    }

    /**
     * Eso en PEM.
     *
     * @param de lo que se codifica
     * @return el texto, terminado en un salto de linea
     * @throws NullPointerException si es {@code null}
     * @throws IllegalArgumentException si no se lo puede codificar
     */
    public String encodeToString(DEREncodable de) {
        if (de == null) {
            throw new NullPointerException("de");
        }
        if (de instanceof PEMRecord) {
            return de.toString();
        }
        if (de instanceof KeyPair) {
            final KeyPair par = (KeyPair) de;
            return encodeToString(par.getPublic()) + encodeToString(par.getPrivate());
        }
        if (de instanceof X509Certificate) {
            return bloque("CERTIFICATE", codificado((X509Certificate) de));
        }
        if (de instanceof X509CRL) {
            return bloque("X509 CRL", codificado((X509CRL) de));
        }
        if (de instanceof EncryptedPrivateKeyInfo) {
            return bloque("ENCRYPTED PRIVATE KEY", codificado((EncryptedPrivateKeyInfo) de));
        }
        if (de instanceof X509EncodedKeySpec) {
            return bloque("PUBLIC KEY", ((X509EncodedKeySpec) de).getEncoded());
        }
        if (de instanceof PKCS8EncodedKeySpec) {
            return bloque("PRIVATE KEY", ((PKCS8EncodedKeySpec) de).getEncoded());
        }
        if (de instanceof PublicKey) {
            return bloque("PUBLIC KEY", codificado((Key) de));
        }
        if (de instanceof PrivateKey) {
            if (this.password != null) {
                throw new IllegalArgumentException(
                        "no hay cifrado basado en contrasena: ningun proveedor registrado ofrece"
                                + " el servicio Cipher");
            }
            return bloque("PRIVATE KEY", codificado((Key) de));
        }
        throw new IllegalArgumentException("no se sabe codificar " + de.getClass().getName());
    }

    /**
     * Lo mismo, en bytes.
     *
     * @param de lo que se codifica
     * @return el texto en UTF-8
     * @throws NullPointerException si es {@code null}
     * @throws IllegalArgumentException si no se lo puede codificar
     */
    public byte[] encode(DEREncodable de) {
        return encodeToString(de).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Otro codificador que cifra las claves privadas con esa contrasena.
     *
     * <p>Este no cambia: ver la nota de la clase.
     *
     * @param password la contrasena
     * @return el otro codificador
     * @throws NullPointerException si la contrasena es {@code null}
     */
    public PEMEncoder withEncryption(char[] password) {
        if (password == null) {
            throw new NullPointerException("password");
        }
        return new PEMEncoder(password.clone());
    }

    /** El bloque armado, con el contenido en base64. */
    static String bloque(String tipo, byte[] contenido) {
        return new PEMRecord(tipo, Base64.getEncoder().encodeToString(contenido)).toString();
    }

    private static byte[] codificado(Key k) {
        final byte[] b = k.getEncoded();
        if (b == null) {
            throw new IllegalArgumentException("la clave no se deja codificar");
        }
        return b;
    }

    private static byte[] codificado(X509Certificate c) {
        try {
            return c.getEncoded();
        } catch (java.security.cert.CertificateEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static byte[] codificado(X509CRL c) {
        try {
            return c.getEncoded();
        } catch (java.security.cert.CRLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static byte[] codificado(EncryptedPrivateKeyInfo e) {
        try {
            return e.getEncoded();
        } catch (java.io.IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
