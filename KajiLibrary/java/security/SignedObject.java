package java.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Un objeto serializado junto con su firma.
 *
 * <h2>Que garantiza y que no</h2>
 *
 * <p>Garantiza <strong>integridad y origen</strong>: quien recibe uno de estos puede comprobar que
 * el contenido no cambio y que lo firmo el dueno de una clave concreta. <strong>No</strong>
 * garantiza confidencialidad — el objeto viaja en claro, y cualquiera lo puede leer sin verificar
 * nada. Confundir las dos cosas es el error clasico con esta clase.
 *
 * <h2>Por que se guardan los bytes y no el objeto</h2>
 *
 * <p>Porque la firma es sobre <em>bytes</em>. Si esta clase guardara la referencia al objeto
 * original y lo volviera a serializar al verificar, dos serializaciones del mismo objeto podrian
 * diferir —un {@code HashMap} con otro orden, un campo que cambio— y la firma dejaria de validar sin
 * que nadie haya manipulado nada.
 *
 * <p>Guardar la copia serializada tiene ademas la consecuencia util de que {@link #getObject}
 * devuelve un objeto <strong>nuevo</strong> en cada llamada: es una copia profunda, no la instancia
 * de origen.
 *
 * <h2>La {@link Signature} la trae quien llama</h2>
 *
 * <p>Y eso es lo que hace que esta clase funcione en esta biblioteca aunque no haya proveedor
 * criptografico instalado: no pide ningun algoritmo por su cuenta. Quien construye o verifica trae
 * su motor de firma ya conseguido, y si no hay proveedor el fallo aparece alli —en
 * {@code Signature.getInstance}— y no aca.
 *
 * <h2>Sobre reusar el objeto de firma</h2>
 *
 * <p>Tanto el constructor como {@link #verify} llaman a {@code initSign}/{@code initVerify}, asi que
 * el estado previo del {@code Signature} que se les pase se pierde. Es intencional en el JDK: recibir
 * uno a medio usar y confiar en su estado seria fragil.
 *
 * @since 1.2
 */
public final class SignedObject implements Serializable {

    private static final long serialVersionUID = 720502720485447167L;

    /** El objeto, ya serializado. Ver la nota de la clase sobre por que se guarda asi. */
    private byte[] content;

    /** La firma sobre {@link #content}. */
    private byte[] signature;

    /** Con que algoritmo se firmo. */
    private String thealgorithm;

    /**
     * Serializa {@code object} y lo firma.
     *
     * @throws IOException si el objeto no se pudo serializar
     * @throws InvalidKeyException si la clave no sirve para ese motor de firma
     * @throws SignatureException si la firma fallo
     */
    public SignedObject(Serializable object, PrivateKey signingKey, Signature signingEngine)
            throws IOException, InvalidKeyException, SignatureException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        try {
            out.writeObject(object);
            out.flush();
        } finally {
            out.close();
        }
        this.content = bytes.toByteArray();
        this.thealgorithm = signingEngine.getAlgorithm();

        signingEngine.initSign(signingKey);
        signingEngine.update(this.content, 0, this.content.length);
        this.signature = signingEngine.sign().clone();
    }

    /**
     * Deserializa una copia del objeto.
     *
     * <p><strong>No verifica nada.</strong> Llamar a esto sin haber pasado antes por
     * {@link #verify} es leer datos de origen desconocido — y deserializar datos ajenos es
     * justamente el vector de los ataques de deserializacion. El orden correcto es verificar
     * primero.
     *
     * @throws IOException si los bytes no se pudieron leer
     * @throws ClassNotFoundException si la clase del objeto no esta
     */
    public Object getObject() throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(this.content));
        try {
            return in.readObject();
        } finally {
            in.close();
        }
    }

    /** Una copia de la firma; el arreglo interno no se presta. */
    public byte[] getSignature() {
        return this.signature.clone();
    }

    /** Con que algoritmo se firmo. */
    public String getAlgorithm() {
        return this.thealgorithm;
    }

    /**
     * Comprueba la firma contra esa clave publica.
     *
     * @return {@code true} si el contenido no cambio y lo firmo el dueno de la clave
     * @throws InvalidKeyException si la clave no sirve para ese motor
     * @throws SignatureException si la verificacion fallo por un motivo que no es "no coincide"
     */
    public boolean verify(PublicKey verificationKey, Signature verificationEngine)
            throws InvalidKeyException, SignatureException {
        verificationEngine.initVerify(verificationKey);
        verificationEngine.update(this.content, 0, this.content.length);
        return verificationEngine.verify(this.signature);
    }
}
