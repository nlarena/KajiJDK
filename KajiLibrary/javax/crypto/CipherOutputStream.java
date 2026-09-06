package javax.crypto;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Cifra --o descifra-- lo que se escribe en otro flujo.
 *
 * <h2>Cerrar es parte del mensaje</h2>
 *
 * <p>{@link #close} es lo que llama a {@code doFinal}, y {@code doFinal} es lo que escribe el ultimo
 * bloque con su relleno. Un programa que escribe todo y no cierra produce un archivo truncado: no
 * uno mas corto, uno que no se puede descifrar.
 *
 * <p>{@link #flush} no alcanza. Vacia lo que el flujo de abajo tenga pendiente, pero no puede
 * forzar al cifrador a entregar un bloque incompleto --si pudiera, no seria un cifrado por
 * bloques--.
 *
 * @since 1.4
 */
public class CipherOutputStream extends FilterOutputStream {

    private final Cipher cipher;
    private final byte[] uno = new byte[1];
    private boolean cerrado;

    /**
     * Uno que pasa lo escrito por ese cifrador.
     *
     * @param os a donde escribir
     * @param c el cifrador, ya configurado
     */
    public CipherOutputStream(OutputStream os, Cipher c) {
        super(os);
        this.cipher = c;
    }

    /**
     * Uno que no cifra nada.
     *
     * @param os a donde escribir
     */
    protected CipherOutputStream(OutputStream os) {
        this(os, new NullCipher());
    }

    /**
     * Escribe un byte.
     *
     * @param b el byte
     * @throws IOException si falla la escritura
     */
    @Override
    public void write(int b) throws IOException {
        this.uno[0] = (byte) b;
        write(this.uno, 0, 1);
    }

    /**
     * Escribe un arreglo.
     *
     * @param b los datos
     * @throws IOException si falla la escritura
     */
    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * Escribe parte de un arreglo.
     *
     * @param b los datos
     * @param off desde donde
     * @param len cuantos
     * @throws IOException si falla la escritura
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        final byte[] salieron = this.cipher.update(b, off, len);
        if (salieron != null && salieron.length > 0) {
            this.out.write(salieron);
        }
    }

    /**
     * Vacia lo pendiente del flujo de abajo.
     *
     * <p>No fuerza al cifrador a entregar un bloque incompleto: ver la nota de la clase.
     *
     * @throws IOException si falla
     */
    @Override
    public void flush() throws IOException {
        this.out.flush();
    }

    /**
     * Termina el cifrador, escribe lo ultimo, y cierra el flujo de abajo.
     *
     * @throws IOException si falla la escritura o el cierre, o si el cifrador tira
     */
    @Override
    public void close() throws IOException {
        if (this.cerrado) {
            return;
        }
        this.cerrado = true;
        try {
            final byte[] salieron = this.cipher.doFinal();
            if (salieron != null && salieron.length > 0) {
                this.out.write(salieron);
            }
        } catch (BadPaddingException e) {
            // Cifrando no pasa; descifrando si, y significa que el mensaje esta alterado.
        } catch (IllegalBlockSizeException e) {
            // Idem.
        }
        this.out.flush();
        this.out.close();
    }
}
