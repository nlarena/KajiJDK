package javax.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Un objeto guardado cifrado.
 *
 * <h2>Que hace</h2>
 *
 * <p>Serializa el objeto y cifra los bytes. Lo que queda es un objeto que se puede seguir tratando
 * como cualquier otro --guardar, mandar, serializar de nuevo-- pero cuyo contenido no se puede leer
 * sin la clave.
 *
 * <h2>Que no hace</h2>
 *
 * <p>No autentica. Si el cifrado no es autenticado, alguien puede cambiar los bytes cifrados y
 * {@link #getObject} va a deserializar lo que salga. Y deserializar datos que uno no controla es
 * peligroso por si mismo: la deserializacion construye objetos arbitrarios antes de que el programa
 * pueda mirarlos.
 *
 * <p>Por eso conviene sellar con un cifrado autenticado, o guardar aparte un
 * {@link Mac} de lo sellado.
 *
 * <h2>{@link #getAlgorithm}</h2>
 *
 * <p>Guarda con que se sello, para que el que abre pueda armar el cifrador correspondiente. No
 * guarda la clave, obviamente; los parametros si, en {@link #encodedParams}, porque sin ellos
 * --sin el vector de inicializacion, por ejemplo-- no se podria descifrar.
 *
 * @since 1.4
 */
public class SealedObject implements Serializable {

    private static final long serialVersionUID = 4482838265551344752L;

    /** Los parametros con que se cifro, codificados, o {@code null} si no hubo. */
    protected byte[] encodedParams;

    private final byte[] encryptedContent;
    private final String sealAlg;

    /**
     * Sella ese objeto con ese cifrador.
     *
     * @param object lo que se guarda
     * @param c el cifrador, ya configurado para cifrar
     * @throws IOException si el objeto no se puede serializar
     * @throws IllegalBlockSizeException si el cifrador no puede con lo serializado
     * @throws NullPointerException si el cifrador es {@code null}
     */
    public SealedObject(Serializable object, Cipher c)
            throws IOException, IllegalBlockSizeException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bytes);
        oos.writeObject(object);
        oos.flush();
        oos.close();
        try {
            this.encryptedContent = c.doFinal(bytes.toByteArray());
        } catch (BadPaddingException e) {
            // Cifrando no puede pasar: el relleno lo pone el cifrador.
            throw new RuntimeException(e.getMessage());
        }
        this.sealAlg = c.getAlgorithm();
        final java.security.AlgorithmParameters p = c.getParameters();
        this.encodedParams = p == null ? null : p.getEncoded();
    }

    /**
     * Una copia de otro.
     *
     * @param so el original
     */
    protected SealedObject(SealedObject so) {
        this.encryptedContent = so.encryptedContent == null ? null
                : so.encryptedContent.clone();
        this.sealAlg = so.sealAlg;
        this.encodedParams = so.encodedParams == null ? null : so.encodedParams.clone();
    }

    /**
     * Con que se sello.
     *
     * @return el algoritmo
     */
    public final String getAlgorithm() {
        return this.sealAlg;
    }

    /**
     * Lo abre con esa clave.
     *
     * <p>Arma el cifrador solo, con el algoritmo y los parametros guardados. Es la version comoda; la
     * de {@link #getObject(Cipher)} sirve cuando el cifrador ya esta armado.
     *
     * @param key la clave
     * @return el objeto
     * @throws IOException si no se puede deserializar
     * @throws ClassNotFoundException si la clase del objeto no esta
     * @throws NoSuchAlgorithmException si no hay un proveedor con ese algoritmo
     * @throws InvalidKeyException si la clave no sirve
     */
    public final Object getObject(Key key)
            throws IOException, ClassNotFoundException, NoSuchAlgorithmException,
            InvalidKeyException {
        try {
            final Cipher c = Cipher.getInstance(this.sealAlg);
            return abrir(c, key);
        } catch (NoSuchPaddingException e) {
            throw new NoSuchAlgorithmException(e.getMessage());
        }
    }

    /**
     * Lo abre con ese cifrador.
     *
     * @param c el cifrador, ya configurado para descifrar
     * @return el objeto
     * @throws IOException si no se puede deserializar
     * @throws ClassNotFoundException si la clase del objeto no esta
     * @throws IllegalBlockSizeException si lo guardado no mide un multiplo del bloque
     * @throws BadPaddingException si el relleno no cierra, casi siempre porque la clave esta mal
     * @throws NullPointerException si el cifrador es {@code null}
     */
    public final Object getObject(Cipher c)
            throws IOException, ClassNotFoundException, IllegalBlockSizeException,
            BadPaddingException {
        return leer(c.doFinal(this.encryptedContent));
    }

    /**
     * Lo abre con esa clave, usando ese proveedor.
     *
     * @param key la clave
     * @param provider el nombre del proveedor
     * @return el objeto
     * @throws IOException si no se puede deserializar
     * @throws ClassNotFoundException si la clase del objeto no esta
     * @throws NoSuchAlgorithmException si ese proveedor no tiene ese algoritmo
     * @throws NoSuchProviderException si no hay un proveedor con ese nombre
     * @throws InvalidKeyException si la clave no sirve
     * @throws IllegalArgumentException si el nombre del proveedor es {@code null} o vacio
     */
    public final Object getObject(Key key, String provider)
            throws IOException, ClassNotFoundException, NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        try {
            final Cipher c = Cipher.getInstance(this.sealAlg, provider);
            return abrir(c, key);
        } catch (NoSuchPaddingException e) {
            throw new NoSuchAlgorithmException(e.getMessage());
        }
    }

    /** Configura el cifrador con lo guardado y lo abre. */
    private Object abrir(Cipher c, Key key)
            throws IOException, ClassNotFoundException, InvalidKeyException,
            NoSuchAlgorithmException {
        try {
            if (this.encodedParams == null) {
                c.init(Cipher.DECRYPT_MODE, key);
            } else {
                final java.security.AlgorithmParameters p =
                        java.security.AlgorithmParameters.getInstance(sinModo(this.sealAlg));
                p.init(this.encodedParams);
                c.init(Cipher.DECRYPT_MODE, key, p);
            }
            return leer(c.doFinal(this.encryptedContent));
        } catch (java.security.InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e.getMessage());
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e.getMessage());
        } catch (BadPaddingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /** El algoritmo sin el modo ni el relleno, que es como se nombran los parametros. */
    private static String sinModo(String alg) {
        if (alg == null) {
            return null;
        }
        final int barra = alg.indexOf('/');
        return barra < 0 ? alg : alg.substring(0, barra);
    }

    private static Object leer(byte[] datos) throws IOException, ClassNotFoundException {
        final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(datos));
        try {
            return ois.readObject();
        } finally {
            ois.close();
        }
    }
}
