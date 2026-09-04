package jdk.internal.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * El {@link MethodAccessor} que fabrica {@link ReflectionFactory#newMethodAccessor}.
 *
 * <p>Delega en {@link Method#invoke}, que en esta VM es el intrinseco {@code Intrinsic::MethodInvoke}
 * del interprete. Eso invierte la flecha respecto de HotSpot —alla {@code Method.invoke} llama al
 * accesor, aca el accesor llama a {@code Method.invoke}— y por eso no hay ciclo: el intrinseco no
 * vuelve a Java, empuja el frame del metodo destino y listo.
 *
 * <p>Que la delegacion sea de una linea es la garantia, no la sospecha: significa que no hay una
 * segunda implementacion de la invocacion reflexiva que pueda desincronizarse de la primera. El
 * desempaquetado de argumentos, el ensanchado de primitivos, el reboxeo del retorno y el envoltorio
 * de la excepcion del destino en {@link InvocationTargetException} pasan una sola vez, adentro del
 * interprete ({@code reflective_call.rs}).
 */
final class AccesorDeMetodo implements MethodAccessor {

    private final Method metodo;

    AccesorDeMetodo(Method metodo) {
        this.metodo = metodo;
    }

    public Object invoke(Object obj, Object[] args)
            throws IllegalArgumentException, InvocationTargetException {
        // `null` significa "sin argumentos", no "un argumento nulo": es lo que promete el contrato
        // del JDK y lo que el intrinseco espera recibir como arreglo vacio.
        if (args == null) {
            return this.metodo.invoke(obj, new Object[0]);
        }
        return this.metodo.invoke(obj, args);
    }

    /**
     * {@inheritDoc}
     *
     * <p>El {@code caller} se ignora, y no por comodidad: en esta VM ningun metodo es sensible al
     * llamador. No existe {@code @CallerSensitive} en esta biblioteca ni el gancho del VM que la lee
     * ({@code Reflection.getCallerClass}), asi que no hay un solo destino cuyo resultado pueda
     * depender de quien lo invoque. El JDK hace exactamente esto mismo —delegar en la forma de dos
     * argumentos— para todo metodo que no este anotado, que aca son todos.
     *
     * <p>Con una diferencia observable que conviene dejar anotada, porque se la midio: en el JDK 25
     * esta sobrecarga <strong>no</strong> se puede llamar sobre un accesor que no sea sensible al
     * llamador. Su {@code DirectMethodHandleAccessor} arma dos handles distintos segun el caso, y
     * llamar la forma de tres sobre el handle del caso comun tira
     * {@code IllegalArgumentException: argument type mismatch}. Es un artefacto de como esta armado
     * el handle y no algo que la interfaz prometa; aca no hay dos handles, asi que no hay nada que
     * pueda no coincidir, y la llamada anda. Por eso la prueba de comportamiento no la compara entre
     * las dos VMs: la unica manera de igualarlas seria tirar a proposito.
     */
    public Object invoke(Object obj, Object[] args, Class<?> caller)
            throws IllegalArgumentException, InvocationTargetException {
        return this.invoke(obj, args);
    }
}
