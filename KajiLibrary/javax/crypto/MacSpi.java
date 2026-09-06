package javax.crypto;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Lo que un proveedor tiene que escribir para ofrecer un codigo de autenticacion de mensaje.
 *
 * <h2>Que es un MAC y que no</h2>
 *
 * <p>Es un resumen con clave. Un resumen a secas prueba que el mensaje no cambio, pero cualquiera
 * puede recalcularlo despues de cambiarlo; con clave, solo puede calcularlo quien la tiene. Eso es
 * lo que lo vuelve una prueba de origen y no solo de integridad.
 *
 * <p>Lo que no es: una firma. Las dos partes comparten la misma clave, asi que ninguna de las dos
 * puede demostrarle a un tercero que la otra escribio el mensaje --podria haberlo escrito ella
 * misma--.
 *
 * <h2>{@link #clone}</h2>
 *
 * <p>Existe para lo mismo que en un resumen: poder guardar el estado despues de una parte comun y
 * seguir por dos caminos distintos sin recalcularla. Un proveedor que no lo permita hereda el
 * comportamiento de {@link Object}, que tira.
 *
 * @since 1.4
 */
public abstract class MacSpi {

    /** Uno. */
    public MacSpi() {
    }

    /**
     * Cuanto mide lo que sale.
     *
     * @return el tamano en bytes
     */
    protected abstract int engineGetMacLength();

    /**
     * Lo configura.
     *
     * @param key la clave
     * @param params los parametros, o {@code null}
     * @throws InvalidKeyException si la clave no sirve
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     */
    protected abstract void engineInit(Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException;

    /**
     * Entrega un byte.
     *
     * @param input el byte
     */
    protected abstract void engineUpdate(byte input);

    /**
     * Entrega datos.
     *
     * @param input los datos
     * @param offset desde donde
     * @param len cuantos
     */
    protected abstract void engineUpdate(byte[] input, int offset, int len);

    /**
     * Entrega lo que quede en el buffer.
     *
     * @param input los datos; queda consumido
     */
    protected void engineUpdate(ByteBuffer input) {
        if (input == null) {
            throw new NullPointerException("input");
        }
        if (!input.hasRemaining()) {
            return;
        }
        if (input.hasArray()) {
            final byte[] a = input.array();
            final int desde = input.arrayOffset() + input.position();
            final int cuantos = input.remaining();
            engineUpdate(a, desde, cuantos);
            input.position(input.limit());
            return;
        }
        final byte[] copia = new byte[input.remaining()];
        input.get(copia);
        engineUpdate(copia, 0, copia.length);
    }

    /**
     * Termina y devuelve el codigo.
     *
     * @return el codigo
     */
    protected abstract byte[] engineDoFinal();

    /** Vuelve al estado que tenia despues de configurarlo. */
    protected abstract void engineReset();

    /**
     * Una copia con el mismo estado.
     *
     * @return la copia
     * @throws CloneNotSupportedException si este no se puede copiar
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
