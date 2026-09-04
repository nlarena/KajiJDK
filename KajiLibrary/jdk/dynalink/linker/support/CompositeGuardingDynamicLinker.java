package jdk.dynalink.linker.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;

/**
 * Varios enlazadores presentados como uno solo: se prueban en orden hasta que alguno conteste.
 *
 * <p>Es la composicion mas simple posible, y su costo es lineal: un pedido que ningun enlazador
 * sabe manejar recorre la lista entera antes de rendirse. Para el caso en que todos los
 * componentes sepan decidir por el tipo del receptor conviene
 * {@link CompositeTypeBasedGuardingDynamicLinker}, que aprovecha eso para saltear la mayoria.
 *
 * <p>La lista se copia en el constructor: la composicion es inmutable y no se puede reordenar
 * despues de armada.
 *
 * @since 9
 */
public class CompositeGuardingDynamicLinker implements GuardingDynamicLinker {

    private final GuardingDynamicLinker[] linkers;

    /**
     * Compone los enlazadores en el orden en que vienen.
     *
     * @param linkers los enlazadores
     */
    public CompositeGuardingDynamicLinker(
            final Iterable<? extends GuardingDynamicLinker> linkers) {
        final List<GuardingDynamicLinker> l = new ArrayList<GuardingDynamicLinker>();
        for (final GuardingDynamicLinker linker : linkers) {
            l.add(Objects.requireNonNull(linker));
        }
        this.linkers = l.toArray(new GuardingDynamicLinker[l.size()]);
    }

    /**
     * Lo que conteste el primer enlazador que sepa manejar el pedido.
     *
     * @return la invocacion, o {@code null} si ninguno supo
     */
    public GuardedInvocation getGuardedInvocation(final LinkRequest linkRequest,
            final LinkerServices linkerServices) throws Exception {
        for (final GuardingDynamicLinker linker : linkers) {
            final GuardedInvocation invocation =
                    linker.getGuardedInvocation(linkRequest, linkerServices);
            if (invocation != null) {
                return invocation;
            }
        }
        return null;
    }
}
