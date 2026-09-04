package jdk.dynalink.support;

import java.lang.invoke.MethodHandle;

import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.linker.GuardedInvocation;

/**
 * El sitio de invocacion mas simple: recuerda <strong>una</strong> invocacion por vez.
 *
 * <h2>La estrategia</h2>
 *
 * <p>Cada reenlace tira lo anterior. El destino queda siendo la invocacion nueva con su guarda, y el
 * camino de respaldo cuando la guarda falla vuelve a enlazar desde cero.
 *
 * <p>Es la cache monomorfica: rapidisima si el sitio siempre ve el mismo tipo de receptor —que es la
 * enorme mayoria de los sitios de cualquier programa— y patologica si ve dos alternandose, porque
 * entonces cada llamada tira el enlace de la anterior y vuelve a enlazar.
 *
 * <p>Para ese caso esta {@link ChainedCallSite}, que acumula. La eleccion entre uno y otro es la
 * unica decision que hay que tomar aca, y el criterio es cuantos tipos distintos se espera ver.
 *
 * <h2>Por que {@code relink} y {@code resetAndRelink} hacen lo mismo</h2>
 *
 * <p>Porque no hay nada que resetear: la diferencia entre los dos metodos es si conviene conservar
 * lo aprendido, y este sitio nunca conserva nada. En {@link ChainedCallSite} si se distinguen.
 *
 * @since 9
 */
public class SimpleRelinkableCallSite extends AbstractRelinkableCallSite {

    /**
     * Un sitio con ese descriptor.
     *
     * @param descriptor el descriptor
     */
    public SimpleRelinkableCallSite(final CallSiteDescriptor descriptor) {
        super(descriptor);
    }

    /**
     * Instala la invocacion nueva, descartando la anterior.
     *
     * @param guardedInvocation la invocacion con su guarda
     * @param relinkAndInvoke el camino de respaldo, que vuelve a enlazar
     */
    public void relink(final GuardedInvocation guardedInvocation,
            final MethodHandle relinkAndInvoke) {
        setTarget(guardedInvocation.compose(relinkAndInvoke));
    }

    /**
     * Lo mismo que {@link #relink}: este sitio no acumula nada que resetear.
     *
     * @param guardedInvocation la invocacion con su guarda
     * @param relinkAndInvoke el camino de respaldo, que vuelve a enlazar
     */
    public void resetAndRelink(final GuardedInvocation guardedInvocation,
            final MethodHandle relinkAndInvoke) {
        relink(guardedInvocation, relinkAndInvoke);
    }
}
