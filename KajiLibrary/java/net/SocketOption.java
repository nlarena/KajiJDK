package java.net;

/**
 * KajiLibrary's java.net.SocketOption — una opción de socket, con su nombre y el tipo de su valor.
 *
 * <p>Las dos preguntas que declara son justo lo que hace falta para que las opciones sean
 * **extensibles y seguras a la vez**: el nombre identifica la opción, y `type()` dice de qué tipo es
 * su valor, de modo que `setOption(opción, valor)` puede verificar en compilación —por la variable de
 * tipo— y también en ejecución que el valor corresponde.
 *
 * <p>Sin esto habría que tener un método por opción, o pasar `Object` y descubrir el error tarde.
 *
 * @param <T> el tipo del valor de la opción
 */
public interface SocketOption<T> {

    /** El nombre de la opción. */
    String name();

    /** El tipo de su valor. */
    Class<T> type();
}
