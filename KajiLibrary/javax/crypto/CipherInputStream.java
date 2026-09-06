package javax.crypto;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Descifra --o cifra-- lo que se lee de otro flujo.
 *
 * <h2>Por que el desfasaje</h2>
 *
 * <p>Un cifrado por bloques no entrega nada hasta tener un bloque entero, asi que este flujo tiene
 * que leer de mas para poder entregar algo. Lee de a 512 bytes, se los da al cifrador, y guarda lo
 * que salga hasta que alguien lo pida. Es la razon de que {@link #available} casi nunca coincida con
 * lo que queda en el flujo de abajo.
 *
 * <h2>Cerrar no es optativo</h2>
 *
 * <p>{@link #close} es lo que llama a {@code doFinal}, y {@code doFinal} es donde se comprueba el
 * relleno --y, en un cifrado autenticado, la etiqueta--. Un programa que lee hasta el final y no
 * cierra no se entera de que el mensaje estaba alterado.
 *
 * <p>Y hay algo peor, que es propio de esta clase: si {@code doFinal} falla, {@link #close} se come
 * la excepcion. Es lo que hace el JDK y no se puede cambiar sin romper a quien dependa de ello, pero
 * significa que para datos autenticados esta clase no sirve: hay que usar {@link Cipher}
 * directamente y mirar lo que tira.
 *
 * <h2>Sin marcas</h2>
 *
 * <p>{@link #markSupported} da falso siempre. Volver atras obligaria a rebobinar el estado del
 * cifrador, que en un modo encadenado depende de todo lo que paso antes.
 *
 * @since 1.4
 */
public class CipherInputStream extends FilterInputStream {

    private static final int TAM = 512;

    private final Cipher cipher;
    private final byte[] entrada = new byte[TAM];

    private byte[] salida;
    private int desde;
    private int hasta;
    private boolean terminado;
    private boolean cerrado;

    /**
     * Uno que pasa lo leido por ese cifrador.
     *
     * @param is de donde leer
     * @param c el cifrador, ya configurado
     */
    public CipherInputStream(InputStream is, Cipher c) {
        super(is);
        this.cipher = c;
    }

    /**
     * Uno que no cifra nada.
     *
     * <p>Es protegido porque solo tiene sentido para una subclase que quiera el comportamiento de
     * flujo sin la transformacion.
     *
     * @param is de donde leer
     */
    protected CipherInputStream(InputStream is) {
        this(is, new NullCipher());
    }

    /**
     * El byte siguiente.
     *
     * @return el byte, entre 0 y 255, o -1 si se termino
     * @throws IOException si falla la lectura
     */
    @Override
    public int read() throws IOException {
        if (this.desde >= this.hasta && !llenar()) {
            return -1;
        }
        final int b = this.salida[this.desde] & 0xff;
        this.desde++;
        return b;
    }

    /**
     * Llena el arreglo.
     *
     * @param b donde escribir
     * @return cuantos bytes se leyeron, o -1 si se termino
     * @throws IOException si falla la lectura
     */
    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Llena parte del arreglo.
     *
     * @param b donde escribir
     * @param off desde donde
     * @param len cuantos como mucho
     * @return cuantos bytes se leyeron, o -1 si se termino
     * @throws IOException si falla la lectura
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (this.desde >= this.hasta && !llenar()) {
            return -1;
        }
        if (len <= 0) {
            return 0;
        }
        final int cuantos = Math.min(len, this.hasta - this.desde);
        System.arraycopy(this.salida, this.desde, b, off, cuantos);
        this.desde += cuantos;
        return cuantos;
    }

    /**
     * Descarta bytes.
     *
     * <p>Solo salta lo que ya esta descifrado y esperando: no tiene sentido leer y descifrar de mas
     * para tirarlo.
     *
     * @param n cuantos
     * @return cuantos se saltaron
     * @throws IOException si falla
     */
    @Override
    public long skip(long n) throws IOException {
        final long disponible = this.hasta - this.desde;
        final long cuantos = n > disponible ? disponible : n;
        if (cuantos <= 0) {
            return 0;
        }
        this.desde += (int) cuantos;
        return cuantos;
    }

    /**
     * Cuanto se puede leer sin bloquear.
     *
     * @return lo que ya esta descifrado y esperando
     * @throws IOException si falla
     */
    @Override
    public int available() throws IOException {
        return this.hasta - this.desde;
    }

    /**
     * Cierra el flujo de abajo y termina el cifrador.
     *
     * <p>Lo que {@code doFinal} tire se descarta: ver la nota de la clase.
     *
     * @throws IOException si falla el cierre del flujo de abajo
     */
    @Override
    public void close() throws IOException {
        if (this.cerrado) {
            return;
        }
        this.cerrado = true;
        this.in.close();
        try {
            this.cipher.doFinal();
        } catch (BadPaddingException e) {
            // El JDK se la come, y cambiarlo romperia a quien dependa de eso.
        } catch (IllegalBlockSizeException e) {
            // Idem.
        }
        this.desde = 0;
        this.hasta = 0;
    }

    /**
     * Si se puede volver atras.
     *
     * @return falso: rebobinar el cifrador no se puede
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    /** Lee del flujo de abajo y pasa lo leido por el cifrador; falso si ya no queda nada. */
    private boolean llenar() throws IOException {
        while (true) {
            if (this.terminado) {
                return false;
            }
            final int leidos = this.in.read(this.entrada, 0, TAM);
            byte[] salieron;
            if (leidos == -1) {
                this.terminado = true;
                try {
                    salieron = this.cipher.doFinal();
                } catch (BadPaddingException e) {
                    salieron = null;
                } catch (IllegalBlockSizeException e) {
                    salieron = null;
                }
            } else {
                salieron = this.cipher.update(this.entrada, 0, leidos);
            }
            if (salieron != null && salieron.length > 0) {
                this.salida = salieron;
                this.desde = 0;
                this.hasta = salieron.length;
                return true;
            }
            if (this.terminado) {
                return false;
            }
        }
    }
}
