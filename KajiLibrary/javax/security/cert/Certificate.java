package javax.security.cert;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;

/**
 * KajiLibrary's javax.security.cert.Certificate -- un certificado, en el API viejo.
 *
 * <p>Este paquete entero existe por una sola razon: {@code javax.net.ssl.SSLSession} declara
 * {@code getPeerCertificateChain()} devolviendo estos, y esa firma no se puede cambiar sin romper
 * todo lo compilado contra ella. Para cualquier cosa nueva va
 * {@link java.security.cert.Certificate}, que es mas completa y la que el resto de la plataforma
 * usa.
 *
 * <h2>La identidad son los bytes</h2>
 *
 * <p>{@link #equals} y {@link #hashCode} miran la codificacion, no los campos. Es lo correcto para
 * algo firmado: dos certificados con el mismo emisor, el mismo sujeto y la misma clave pero
 * distintos bytes son <b>documentos distintos</b>, y solo uno de los dos tiene una firma que cierre.
 * Comparar campo por campo diria que son iguales y eso es justamente lo que un atacante querria.
 *
 * <p>Si {@code getEncoded} tira, {@link #equals} devuelve false en vez de propagar: el contrato de
 * {@code equals} no permite lanzar, y un certificado que no se puede codificar no es igual a nada.
 *
 * <p>Obsoleta <b>y marcada para remocion</b> desde Java 9. El reemplazo es
 * {@code java.security.cert}, que no es una version mejorada de esto sino otra cosa: soporta la
 * version 3 del formato, con extensiones, que es lo unico que sirve para validar una cadena de hoy.
 */
@Deprecated(since = "9", forRemoval = true)
public abstract class Certificate {

    /** Para las subclases. */
    public Certificate() {
    }

    /**
     * Igualdad por bytes codificados. Ver la nota de la clase.
     *
     * @return false si alguno de los dos no se puede codificar
     */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Certificate)) {
            return false;
        }
        try {
            byte[] mine = this.getEncoded();
            byte[] theirs = ((Certificate) other).getEncoded();
            if (mine == null || theirs == null || mine.length != theirs.length) {
                return false;
            }
            int i = 0;
            while (i < mine.length) {
                if (mine[i] != theirs[i]) {
                    return false;
                }
                i = i + 1;
            }
            return true;
        } catch (CertificateException e) {
            return false;
        }
    }

    /** Suma de los bytes codificados; 0 si no se pueden obtener, para no lanzar. */
    public int hashCode() {
        int result = 0;
        try {
            byte[] encoded = this.getEncoded();
            if (encoded == null) {
                return 0;
            }
            int i = 0;
            while (i < encoded.length) {
                result = result + (encoded[i] & 0xff) * i;
                i = i + 1;
            }
        } catch (CertificateException e) {
            return 0;
        }
        return result;
    }

    /**
     * La forma codificada, que es la que se firmo.
     *
     * @throws CertificateEncodingException si no se puede producir
     */
    public abstract byte[] getEncoded() throws CertificateEncodingException;

    /**
     * Verifica la firma con esa clave, usando el proveedor por omision.
     *
     * @throws SignatureException si la firma no cierra
     */
    public abstract void verify(PublicKey key)
        throws CertificateException, NoSuchAlgorithmException, InvalidKeyException,
               NoSuchProviderException, SignatureException;

    /**
     * Idem, pidiendole el algoritmo a un proveedor con nombre.
     *
     * @param sigProvider el nombre del proveedor
     */
    public abstract void verify(PublicKey key, String sigProvider)
        throws CertificateException, NoSuchAlgorithmException, InvalidKeyException,
               NoSuchProviderException, SignatureException;

    /** Una descripcion legible. Las subclases la deben. */
    public abstract String toString();

    /** La clave publica que el certificado ata a su sujeto. */
    public abstract PublicKey getPublicKey();
}
