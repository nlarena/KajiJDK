package javax.management;

/**
 * La raiz **verificada** de JMX: todo lo que una operacion de gestion puede fallar y el que llama
 * tiene que atender cuelga de aca.
 *
 * <p>JMX parte sus errores en dos arboles que **no** se tocan: este, que hereda de `Exception`, y
 * {@link JMRuntimeException}, que hereda de `RuntimeException`. La division no es de comodidad sino
 * de responsabilidad -- el verificado dice "el pedido no se pudo cumplir" (no existe el MBean, el
 * atributo no esta) y el no verificado dice "el MBean se rompio o lo llamaron mal".
 *
 * <p>Sin constructor con causa, y no es olvido: JMX es de 1999 y anterior al encadenado de
 * `Throwable`. Las dos subclases que si envuelven algo --{@link MBeanException} y
 * {@link ReflectionException}-- se lo guardan en un campo propio y lo publican pisando
 * `getCause()`.
 */
public class JMException extends Exception {

    private static final long serialVersionUID = 350520924977331825L;

    /** Sin mensaje. */
    public JMException() {
        super();
    }

    /** Con el mensaje que explica que fallo. */
    public JMException(String msg) {
        super(msg);
    }
}
