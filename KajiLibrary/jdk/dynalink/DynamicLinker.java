package jdk.dynalink;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.GuardedInvocationTransformer;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.support.SimpleLinkRequest;

/**
 * El enlazador: lo que decide, en cada invocacion, a que metodo va.
 *
 * <h2>El problema que resuelve</h2>
 *
 * <p>En un lenguaje dinamico, {@code a.b(c)} no se puede compilar a una llamada: no se sabe que es
 * {@code a} hasta que el programa corre. La salida es dejar el lugar vacio --un sitio de
 * invocacion-- y llenarlo la primera vez que pasa por ahi, con una comprobacion barata que dice
 * "mientras {@code a} siga siendo de esta clase, esto sirve". Eso es una invocacion con guarda.
 *
 * <h2>Por que sirve enlazar y no simplemente buscar cada vez</h2>
 *
 * <p>Porque casi siempre el mismo sitio ve siempre el mismo tipo. Un bucle que llama
 * {@code lista.tamano()} un millon de veces lo llama sobre la misma clase el millon de veces, asi
 * que la busqueda se hace una y las otras 999.999 son una comparacion de clase y un salto. Esa
 * apuesta es todo el rendimiento de un lenguaje dinamico sobre la maquina virtual.
 *
 * <h2>Cuando la apuesta falla</h2>
 *
 * <p>Un sitio que ve muchos tipos distintos --{@code megamorfico}-- acumula guardas que fallan una
 * tras otra, y encadenar mas lo empeora. Por eso hay un umbral: pasadas tantas reenlazadas, el sitio
 * se declara inestable y a partir de ahi se lo reemplaza entero en vez de encadenar.
 * {@link RelinkableCallSite#resetAndRelink} es como se le pide eso.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>{@link #getLinkerServices} funciona y con el la mitad del sistema que decide sobre tipos: si
 * una conversion es posible, cual de dos destinos conviene, que enlazador atiende que clase.
 *
 * <p>{@link #link} no puede. Enlazar es armar una manija de metodo, y esta maquina virtual no tiene
 * manijas de metodo --ver {@code java.lang.invoke.MethodHandles}, que lo dice en su nota--. La
 * construccion esta escrita tal como va, asi que lo que sale es la
 * {@link UnsupportedOperationException} de la fabrica de manijas y no un error inventado aca.
 *
 * @since 9
 */
public final class DynamicLinker {

    private final LinkerServices servicios;
    private final GuardedInvocationTransformer preenlace;
    private final boolean sincronizar;
    private final int umbralInestable;

    DynamicLinker(final LinkerServices servicios, final GuardedInvocationTransformer preenlace,
            final boolean sincronizar, final int umbralInestable) {
        this.servicios = servicios;
        this.preenlace = preenlace;
        this.sincronizar = sincronizar;
        this.umbralInestable = umbralInestable;
    }

    /**
     * Enlaza ese sitio de invocacion.
     *
     * <p>Le instala una manija que, la primera vez que se la invoque, busca a donde va, deja
     * instalado el resultado, y sigue con la invocacion. De ahi en mas el sitio va directo mientras
     * la guarda siga valiendo.
     *
     * @param <T> el tipo del sitio
     * @param callSite el sitio
     * @return el mismo sitio, para poder encadenar
     * @throws NullPointerException si el sitio es {@code null}
     * @throws UnsupportedOperationException si la maquina virtual no tiene manijas de metodo, que
     *     es el caso de esta
     */
    public <T extends RelinkableCallSite> T link(final T callSite) {
        final MethodType tipo = callSite.getDescriptor().getMethodType();
        callSite.initialize(reenlazarEInvocar(callSite, tipo, 0));
        return callSite;
    }

    /**
     * Lo que el enlazador les presta a los enlazadores.
     *
     * @return los servicios
     */
    public LinkerServices getLinkerServices() {
        return this.servicios;
    }

    /**
     * Donde esta el sitio que se esta reenlazando ahora.
     *
     * <p>Sirve para que un lenguaje pueda decir "no encuentro el metodo {@code b}" nombrando la
     * linea del programa del usuario y no la del enlazador.
     *
     * @return siempre {@code null}: no puede haber un reenlace en curso, porque {@link #link} no
     *     llega a instalar nada. El JDK devuelve {@code null} en la misma situacion
     */
    public static StackTraceElement getLinkedCallSiteLocation() {
        return null;
    }

    /**
     * La manija que reenlaza el sitio y despues invoca lo que quedo enlazado.
     *
     * <p>Son cuatro combinadores y el orden importa: una que junta todos los argumentos en un
     * arreglo y llama a {@link #reenlazar}, y {@code foldArguments} para que lo que esa devuelva
     * --otra manija-- se invoque con los argumentos originales. Asi la primera invocacion paga la
     * busqueda y las siguientes no.
     */
    private MethodHandle reenlazarEInvocar(final RelinkableCallSite callSite, final MethodType tipo,
            final int cuenta) {
        final MethodHandle reenlazar;
        try {
            reenlazar = MethodHandles.lookup().findVirtual(DynamicLinker.class, "reenlazar",
                    MethodType.methodType(MethodHandle.class, RelinkableCallSite.class, int.class,
                            Object[].class));
        } catch (final NoSuchMethodException e) {
            // El metodo esta en esta misma clase: si no se lo encuentra, el archivo se corrompio.
            throw new AssertionError(e);
        } catch (final IllegalAccessException e) {
            throw new AssertionError(e);
        }
        final MethodHandle atado =
                MethodHandles.insertArguments(reenlazar, 0, this, callSite, Integer.valueOf(cuenta));
        final MethodHandle juntando = atado.asCollector(Object[].class, tipo.parameterCount());
        return MethodHandles.foldArguments(MethodHandles.exactInvoker(tipo),
                juntando.asType(tipo.changeReturnType(MethodHandle.class)));
    }

    /**
     * Busca a donde va el sitio para estos argumentos, lo deja instalado, y devuelve que invocar.
     *
     * <p>Es lo que corre la manija de {@link #reenlazarEInvocar}. Pasado el umbral el sitio se
     * considera inestable: se le pide que reemplace todo en vez de encadenar, y ademas se le avisa
     * al enlazador --por {@link LinkRequest#isCallSiteUnstable}-- porque un enlazador que sabe que
     * el sitio es inestable puede devolver una invocacion mas general y sin guarda.
     *
     * @param callSite el sitio
     * @param cuenta cuantas veces se lo reenlazo
     * @param argumentos los argumentos de esta invocacion
     * @return la manija a invocar
     * @throws Exception lo que tire el enlazador
     * @throws NoSuchDynamicMethodException si ningun enlazador sabe atender la operacion
     */
    MethodHandle reenlazar(final RelinkableCallSite callSite, final int cuenta,
            final Object[] argumentos) throws Exception {
        final CallSiteDescriptor descriptor = callSite.getDescriptor();
        final boolean inestable = cuenta >= this.umbralInestable;
        final LinkRequest pedido = new SimpleLinkRequest(descriptor, inestable, argumentos);
        GuardedInvocation invocacion = this.servicios.getGuardedInvocation(pedido);
        if (invocacion == null) {
            throw new NoSuchDynamicMethodException(descriptor.toString());
        }
        if (this.preenlace != null) {
            invocacion = this.preenlace.filter(invocacion, pedido, this.servicios);
        }
        final MethodHandle siguiente = reenlazarEInvocar(callSite, descriptor.getMethodType(),
                cuenta + 1);
        if (this.sincronizar) {
            synchronized (callSite) {
                instalar(callSite, invocacion, siguiente, inestable);
            }
        } else {
            instalar(callSite, invocacion, siguiente, inestable);
        }
        return invocacion.getInvocation();
    }

    private static void instalar(final RelinkableCallSite callSite,
            final GuardedInvocation invocacion, final MethodHandle siguiente,
            final boolean inestable) {
        if (inestable) {
            callSite.resetAndRelink(invocacion, siguiente);
        } else {
            callSite.relink(invocacion, siguiente);
        }
    }
}
