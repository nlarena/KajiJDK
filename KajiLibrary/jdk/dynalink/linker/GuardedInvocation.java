package jdk.dynalink.linker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SwitchPoint;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import jdk.dynalink.CallSiteDescriptor;

/**
 * Un metodo mas las condiciones bajo las cuales sigue siendo el correcto.
 *
 * <h2>Por que el enlace es esto y no un metodo a secas</h2>
 *
 * <p>Porque enlazar sale caro y llamar sale barato. Si la respuesta del enlazador fuera solo "para
 * estos argumentos, llama a esto", habria que preguntar en cada invocacion y el enlace no serviria
 * de nada. La respuesta util es "mientras se cumpla esto, llama a esto", y las tres formas de
 * decir "mientras se cumpla esto" son las tres partes opcionales de esta clase.
 *
 * <h2>Las tres condiciones, y por que son tres y no una</h2>
 *
 * <p><strong>La guarda</strong> es un metodo que devuelve {@code boolean} y recibe los mismos
 * argumentos (o un prefijo de ellos). Se evalua en <strong>cada</strong> invocacion. Es la
 * condicion que depende de los valores: "el receptor sigue siendo de esta clase".
 *
 * <p><strong>Los switch points</strong> no se evaluan nunca: son un interruptor global que alguien
 * baja una sola vez, y hasta entonces el JIT puede borrar el chequeo por completo. Es la condicion
 * que depende del mundo y no de los argumentos — "nadie redefinio este metodo todavia". Costo cero
 * mientras nada cambie, que es la razon de que existan aparte de la guarda.
 *
 * <p><strong>La excepcion</strong> es la condicion que solo se descubre intentando. Si la
 * invocacion tira esa clase de excepcion, se considera que el enlace no valia y se reintenta por
 * el camino lento. Sirve para lo que seria carisimo chequear por adelantado.
 *
 * <p>Las tres son opcionales y componen en ese orden inverso al que se enumeran: primero los
 * switch points, adentro la captura de excepcion, y mas adentro la guarda. Se ve en
 * {@link #compose}.
 *
 * <h2>Inmutable</h2>
 *
 * <p>Todos los metodos que parecen modificar —{@link #asType}, {@link #addSwitchPoint},
 * {@link #dropArguments}— devuelven una instancia nueva. Tiene que ser asi porque una misma
 * invocacion enlazada se comparte entre sitios y entre hilos.
 *
 * @since 9
 */
public class GuardedInvocation {

    private final MethodHandle invocation;
    private final MethodHandle guard;
    private final SwitchPoint[] switchPoints;
    private final Class<? extends Throwable> exception;

    /**
     * Una invocacion sin condiciones: siempre vale.
     *
     * @param invocation el metodo
     */
    public GuardedInvocation(final MethodHandle invocation) {
        this(invocation, null, (SwitchPoint[]) null, null);
    }

    /**
     * Con guarda.
     *
     * @param invocation el metodo
     * @param guard la guarda, o {@code null}
     */
    public GuardedInvocation(final MethodHandle invocation, final MethodHandle guard) {
        this(invocation, guard, (SwitchPoint[]) null, null);
    }

    /**
     * Con un switch point.
     *
     * @param invocation el metodo
     * @param switchPoint el interruptor, o {@code null}
     */
    public GuardedInvocation(final MethodHandle invocation, final SwitchPoint switchPoint) {
        this(invocation, null, switchPoint, null);
    }

    /**
     * Con guarda y un switch point.
     *
     * @param invocation el metodo
     * @param guard la guarda, o {@code null}
     * @param switchPoint el interruptor, o {@code null}
     */
    public GuardedInvocation(final MethodHandle invocation, final MethodHandle guard,
            final SwitchPoint switchPoint) {
        this(invocation, guard, switchPoint, null);
    }

    /**
     * Con las tres condiciones, en su forma de un solo switch point.
     *
     * @param invocation el metodo
     * @param guard la guarda, o {@code null}
     * @param switchPoint el interruptor, o {@code null}
     * @param exception la excepcion que invalida el enlace, o {@code null}
     */
    public GuardedInvocation(final MethodHandle invocation, final MethodHandle guard,
            final SwitchPoint switchPoint, final Class<? extends Throwable> exception) {
        this(invocation, guard,
                switchPoint == null ? null : new SwitchPoint[] { switchPoint }, exception);
    }

    /**
     * Con las tres condiciones y varios switch points.
     *
     * <p>Es el constructor al que llaman todos los demas.
     *
     * @param invocation el metodo; no puede ser {@code null}
     * @param guard la guarda, o {@code null}
     * @param switchPoints los interruptores, o {@code null}
     * @param exception la excepcion que invalida el enlace, o {@code null}
     */
    public GuardedInvocation(final MethodHandle invocation, final MethodHandle guard,
            final SwitchPoint[] switchPoints, final Class<? extends Throwable> exception) {
        this.invocation = Objects.requireNonNull(invocation);
        this.guard = guard;
        // Se copia al entrar y al salir: el arreglo es de quien lo paso y no queremos que
        // modificarlo despues cambie un enlace que ya se esta usando.
        this.switchPoints = switchPoints == null ? null : switchPoints.clone();
        this.exception = exception;
    }

    /** El metodo a invocar. */
    public MethodHandle getInvocation() {
        return invocation;
    }

    /** La guarda, o {@code null} si no hay. */
    public MethodHandle getGuard() {
        return guard;
    }

    /** Los interruptores, o {@code null} si no hay. Es una copia. */
    public SwitchPoint[] getSwitchPoints() {
        return switchPoints == null ? null : switchPoints.clone();
    }

    /** La excepcion que invalida el enlace, o {@code null}. */
    public Class<? extends Throwable> getException() {
        return exception;
    }

    /**
     * Si alguno de los interruptores ya se bajo.
     *
     * <p>Sirve para descartar de antemano un enlace que se sabe muerto, sin llegar a armarlo.
     */
    public boolean hasBeenInvalidated() {
        if (switchPoints == null) {
            return false;
        }
        for (final SwitchPoint sp : switchPoints) {
            if (sp.hasBeenInvalidated()) {
                return true;
            }
        }
        return false;
    }

    /**
     * La misma invocacion con otro metodo y otra guarda, conservando las demas condiciones.
     *
     * @param newInvocation el metodo nuevo
     * @param newGuard la guarda nueva, o {@code null}
     * @return la invocacion derivada
     */
    public GuardedInvocation replaceMethods(final MethodHandle newInvocation,
            final MethodHandle newGuard) {
        return new GuardedInvocation(newInvocation, newGuard, switchPoints, exception);
    }

    /**
     * Un interruptor mas.
     *
     * @param newSwitchPoint el interruptor, o {@code null} para no agregar nada
     * @return la invocacion derivada, o {@code this} si no habia nada que agregar
     */
    public GuardedInvocation addSwitchPoint(final SwitchPoint newSwitchPoint) {
        if (newSwitchPoint == null) {
            return this;
        }
        final SwitchPoint[] nuevos;
        if (switchPoints == null) {
            nuevos = new SwitchPoint[] { newSwitchPoint };
        } else {
            nuevos = Arrays.copyOf(switchPoints, switchPoints.length + 1);
            nuevos[switchPoints.length] = newSwitchPoint;
        }
        return new GuardedInvocation(invocation, guard, nuevos, exception);
    }

    /**
     * Adaptada a otra firma, con las conversiones de Java.
     *
     * @param newType la firma pedida
     * @return la invocacion adaptada
     */
    public GuardedInvocation asType(final MethodType newType) {
        return replaceMethods(invocation.asType(newType),
                guard == null ? null : guard.asType(tipoDeGuarda(guard, newType)));
    }

    /**
     * Adaptada a otra firma, con las conversiones de los lenguajes tambien.
     *
     * @param linkerServices los servicios que aportan esas conversiones
     * @param newType la firma pedida
     * @return la invocacion adaptada
     */
    public GuardedInvocation asType(final LinkerServices linkerServices, final MethodType newType) {
        return replaceMethods(linkerServices.asType(invocation, newType),
                guard == null ? null
                        : linkerServices.asType(guard, tipoDeGuarda(guard, newType)));
    }

    /**
     * Como {@link #asType(LinkerServices, MethodType)}, pero sin degradar el valor de retorno.
     *
     * @param linkerServices los servicios
     * @param newType la firma pedida
     * @return la invocacion adaptada, quiza con un retorno mas ancho que el pedido
     */
    public GuardedInvocation asTypeSafeReturn(final LinkerServices linkerServices,
            final MethodType newType) {
        return replaceMethods(linkerServices.asTypeLosslessReturn(invocation, newType),
                guard == null ? null
                        : linkerServices.asType(guard, tipoDeGuarda(guard, newType)));
    }

    /**
     * Adaptada a la firma de un sitio de invocacion.
     *
     * @param desc el descriptor del sitio
     * @return la invocacion adaptada
     */
    public GuardedInvocation asType(final CallSiteDescriptor desc) {
        return asType(desc.getMethodType());
    }

    /**
     * La firma que le corresponde a la guarda dentro de una invocacion de firma {@code tipo}.
     *
     * <p>Es la de la invocacion recortada a los parametros que la guarda mira, y devolviendo
     * {@code boolean}. La guarda puede tomar menos parametros que la invocacion —lo habitual es que
     * mire solo el receptor— y no tendria sentido obligarla a declarar los que ignora.
     */
    private static MethodType tipoDeGuarda(final MethodHandle guarda, final MethodType tipo) {
        return tipo.dropParameterTypes(guarda.type().parameterCount(), tipo.parameterCount())
                .changeReturnType(boolean.class);
    }

    /**
     * Con filtros aplicados a algunos argumentos.
     *
     * <p>Los mismos filtros van al metodo y a la guarda: si el argumento que llega esta filtrado,
     * la guarda tiene que opinar sobre el valor filtrado y no sobre el original.
     *
     * @param pos la posicion del primer argumento a filtrar
     * @param filters los filtros
     * @return la invocacion derivada
     */
    public GuardedInvocation filterArguments(final int pos, final MethodHandle... filters) {
        return replaceMethods(MethodHandles.filterArguments(invocation, pos, filters),
                guard == null ? null : MethodHandles.filterArguments(guard, pos, filters));
    }

    /**
     * Con argumentos extra que se ignoran.
     *
     * @param pos donde insertarlos
     * @param valueTypes los tipos de los argumentos ignorados
     * @return la invocacion derivada
     */
    public GuardedInvocation dropArguments(final int pos, final List<Class<?>> valueTypes) {
        return replaceMethods(MethodHandles.dropArguments(invocation, pos, valueTypes),
                guard == null ? null : MethodHandles.dropArguments(guard, pos, valueTypes));
    }

    /**
     * Con argumentos extra que se ignoran.
     *
     * @param pos donde insertarlos
     * @param valueTypes los tipos de los argumentos ignorados
     * @return la invocacion derivada
     */
    public GuardedInvocation dropArguments(final int pos, final Class<?>... valueTypes) {
        return replaceMethods(MethodHandles.dropArguments(invocation, pos, valueTypes),
                guard == null ? null : MethodHandles.dropArguments(guard, pos, valueTypes));
    }

    /**
     * El metodo final: la invocacion con sus tres condiciones puestas, y un mismo camino de
     * respaldo para las tres.
     *
     * @param fallback a donde ir cuando alguna condicion no se cumple
     * @return el metodo compuesto
     */
    public MethodHandle compose(final MethodHandle fallback) {
        return compose(fallback, fallback, fallback);
    }

    /**
     * El metodo final, con un respaldo distinto para cada condicion.
     *
     * <p>El armado va de adentro hacia afuera, y el orden no es arbitrario. La guarda queda mas
     * adentro porque es la que se evalua siempre y tiene que ser lo primero. Despues la captura de
     * excepcion, que envuelve tanto a la invocacion como a la guarda. Los switch points quedan
     * afuera de todo porque son los mas baratos: si el interruptor esta bajado no hay que entrar a
     * nada.
     *
     * @param switchpointFallback a donde ir si un interruptor se bajo
     * @param guardFallback a donde ir si la guarda dio {@code false}
     * @param catchFallback a donde ir si salto la excepcion
     * @return el metodo compuesto
     */
    public MethodHandle compose(final MethodHandle switchpointFallback,
            final MethodHandle guardFallback, final MethodHandle catchFallback) {
        final MethodHandle conGuarda = guard == null ? invocation
                : MethodHandles.guardWithTest(guard, invocation, guardFallback);
        // El manejador de catchException recibe la excepcion como primer argumento, y el respaldo
        // no la espera: dropArguments le agrega ese parametro adelante para que la descarte.
        final MethodHandle conCaptura = exception == null ? conGuarda
                : MethodHandles.catchException(conGuarda, exception,
                        MethodHandles.dropArguments(catchFallback, 0, exception));
        if (switchPoints == null) {
            return conCaptura;
        }
        MethodHandle salida = conCaptura;
        for (final SwitchPoint sp : switchPoints) {
            salida = sp.guardWithTest(salida, switchpointFallback);
        }
        return salida;
    }
}
