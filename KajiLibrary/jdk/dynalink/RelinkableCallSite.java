package jdk.dynalink;

import java.lang.invoke.MethodHandle;
import jdk.dynalink.linker.GuardedInvocation;

/**
 * El lado del sitio de invocacion que {@link DynamicLinker} sabe manejar.
 *
 * <p>El ciclo de vida tiene un orden que la interfaz no puede imponer pero que el contrato si:
 * {@link #initialize} exactamente una vez, desde {@link DynamicLinker#link}, y despues
 * {@link #relink} o {@link #resetAndRelink} tantas veces como haga falta. La diferencia entre
 * los dos ultimos es la que define la estrategia de cache: `relink` puede acumular la nueva
 * invocacion sobre las anteriores (una cadena polimorfica), `resetAndRelink` obliga a tirarlas
 * — es lo que el enlazador pide cuando decidio que el sitio es **inestable** y encadenar solo
 * gastaria memoria.
 *
 * <p>Casi siempre conviene extender {@code jdk.dynalink.support.AbstractRelinkableCallSite} en
 * lugar de implementar esto desde cero.
 *
 * @since 9
 */
public interface RelinkableCallSite {

    /** Instala la invocacion inicial; la llama {@link DynamicLinker#link} una sola vez. */
    void initialize(MethodHandle relinkAndInvoke);

    /** El descriptor del sitio; no cambia en toda su vida. */
    CallSiteDescriptor getDescriptor();

    /** Agrega una invocacion enlazada, conservando lo que hubiera. */
    void relink(GuardedInvocation guardedInvocation, MethodHandle relinkAndInvoke);

    /** Reemplaza todo lo enlazado: el sitio se considero inestable y encadenar no rinde. */
    void resetAndRelink(GuardedInvocation guardedInvocation, MethodHandle relinkAndInvoke);
}
