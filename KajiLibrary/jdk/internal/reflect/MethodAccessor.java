package jdk.internal.reflect;

import java.lang.reflect.InvocationTargetException;

/**
 * KajiLibrary's jdk.internal.reflect.MethodAccessor — el contrato de "invocar este metodo".
 *
 * <h2>Por que esta interfaz existe, y por que aca significa algo distinto que en HotSpot</h2>
 *
 * <p>En el JDK la flecha va {@code Method.invoke} &rarr; {@code MethodAccessor.invoke}: {@code Method}
 * hace el chequeo de acceso una vez, fabrica un accesor y le delega todas las llamadas. El accesor es
 * la maquinaria, y {@code Method} la cascara.
 *
 * <p>En esta VM la flecha va al reves. {@code Method.invoke} es un <strong>intrinseco del
 * interprete</strong> ({@code Intrinsic::MethodInvoke}): el opcode de invocacion lo reconoce por
 * {@code (clase, nombre, descriptor)} y empuja el frame del metodo destino en vez de correr el cuerpo
 * Java, que por eso tira. La maquinaria esta abajo del piso de Java y no hay accesor en el medio.
 *
 * <p>Eso no vuelve inutil a esta interfaz: la vuelve <strong>una declaracion pura</strong>, que es lo
 * que siempre fue. Una interfaz no tiene cuerpos que puedan mentir; declara una forma —"un objeto que
 * sabe invocar un metodo"— y esa forma es exactamente la del intrinseco que ya funciona. Quien quiera
 * un accesor lo pide por {@link ReflectionFactory#newMethodAccessor}, que devuelve uno enchufado a esa
 * maquinaria y no a una segunda copia de ella.
 *
 * <p>El precedente del criterio esta escrito en {@code jdk.internal.vm.VMSupport}: la interfaz anidada
 * {@code AnnotationDecoder} entro por lo mismo, "una declaracion pura cuyo contrato no depende de que
 * haya quien la use".
 */
public interface MethodAccessor {

    /**
     * Invoca el metodo sobre {@code obj} con {@code args}.
     *
     * @param obj el receptor, o {@code null} si el metodo es estatico
     * @param args los argumentos, ya en el orden de los parametros
     * @return el resultado, boxeado si el tipo de retorno es primitivo; {@code null} si es {@code void}
     * @throws IllegalArgumentException si el receptor o los argumentos no corresponden
     * @throws InvocationTargetException envolviendo lo que haya tirado el metodo destino
     */
    Object invoke(Object obj, Object[] args)
            throws IllegalArgumentException, InvocationTargetException;

    /**
     * Igual que {@link #invoke(Object, Object[])} pero diciendo quien llama.
     *
     * <p>La sobrecarga existe en el JDK por los metodos {@code @CallerSensitive}, que miran el frame
     * de quien los invoco para decidir que contestar: pasando por reflexion ese frame seria el del
     * accesor, asi que el llamador real hay que pasarlo a mano.
     *
     * <p>En esta VM no hay maquinaria sensible al llamador —no existe {@code Reflection.getCallerClass}
     * ni la anotacion que lo dispara, y los motivos estan en {@link Reflection}—, asi que no hay nada
     * que el argumento pueda cambiar. La firma se declara igual porque es parte del contrato, y su
     * implementacion en esta biblioteca lo documenta en vez de fingir que lo usa.
     *
     * @param obj el receptor, o {@code null} si el metodo es estatico
     * @param args los argumentos
     * @param caller la clase que se hace pasar por el llamador
     * @return el resultado, boxeado si el tipo de retorno es primitivo
     * @throws IllegalArgumentException si el receptor o los argumentos no corresponden
     * @throws InvocationTargetException envolviendo lo que haya tirado el metodo destino
     */
    Object invoke(Object obj, Object[] args, Class<?> caller)
            throws IllegalArgumentException, InvocationTargetException;
}
