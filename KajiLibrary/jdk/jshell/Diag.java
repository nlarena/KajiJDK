package jdk.jshell;

import java.util.Locale;

/**
 * Un error o un aviso sobre un fragmento.
 *
 * <h2>Las posiciones</h2>
 *
 * <p>Van referidas al texto que el usuario escribio, no al codigo que el interprete arma alrededor
 * para poder compilarlo. Traducir de uno al otro es trabajo del interprete, y es lo que hace que el
 * subrayado caiga donde el usuario espera.
 *
 * <p>{@link #getStartPosition} y {@link #getEndPosition} delimitan lo que hay que subrayar;
 * {@link #getPosition} es donde poner el cursor, que suele estar adentro pero no en el borde.
 * Cualquiera de las tres puede ser {@link #NOPOS} si el problema no es de un lugar en particular.
 *
 * <h2>{@link #getCode}</h2>
 *
 * <p>Es la clave del mensaje, no el mensaje. Sirve para reconocer un diagnostico sin depender del
 * idioma: un programa puede querer tratar distinto un error de tipos que uno de sintaxis, y comparar
 * el texto traducido seria fragil.
 *
 * @since 9
 */
public abstract class Diag {

    /** Que no hay posicion. */
    public static final long NOPOS = -1;

    Diag() {
    }

    /**
     * Si es un error y no un aviso.
     *
     * @return cierto si es un error
     */
    public abstract boolean isError();

    /**
     * Donde poner el cursor.
     *
     * @return la posicion, o {@link #NOPOS}
     */
    public abstract long getPosition();

    /**
     * Donde empieza lo que hay que subrayar.
     *
     * @return la posicion, o {@link #NOPOS}
     */
    public abstract long getStartPosition();

    /**
     * Donde termina lo que hay que subrayar.
     *
     * @return la posicion, o {@link #NOPOS}
     */
    public abstract long getEndPosition();

    /**
     * La clave del mensaje, para reconocerlo sin depender del idioma.
     *
     * @return la clave
     */
    public abstract String getCode();

    /**
     * El mensaje, para mostrar.
     *
     * @param locale en que idioma, o {@code null} para el de la maquina
     * @return el mensaje
     */
    public abstract String getMessage(Locale locale);
}
