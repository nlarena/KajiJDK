package jdk.dynalink.linker.support;

import java.util.Objects;

import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.linker.LinkRequest;

/**
 * La implementacion obvia de {@link LinkRequest}: guarda lo que le pasan.
 *
 * <p>Es inmutable, y por eso {@link #getArguments} copia el arreglo al salir igual que el
 * constructor lo copia al entrar. Sin las dos copias la inmutabilidad seria de mentira: quien
 * construyo el pedido, o quien leyo los argumentos, podria cambiarlos despues por debajo del
 * enlazador que los esta mirando.
 *
 * @since 9
 */
public class SimpleLinkRequest implements LinkRequest {

    private final CallSiteDescriptor callSiteDescriptor;
    private final Object[] arguments;
    private final boolean callSiteUnstable;

    /**
     * Un pedido nuevo.
     *
     * @param callSiteDescriptor el descriptor del sitio
     * @param callSiteUnstable si el sitio ya se reenlazo demasiadas veces
     * @param arguments los argumentos de la invocacion
     */
    public SimpleLinkRequest(final CallSiteDescriptor callSiteDescriptor,
            final boolean callSiteUnstable, final Object... arguments) {
        this.callSiteDescriptor = Objects.requireNonNull(callSiteDescriptor);
        this.callSiteUnstable = callSiteUnstable;
        this.arguments = Objects.requireNonNull(arguments).clone();
    }

    /** {@inheritDoc} */
    public Object[] getArguments() {
        return arguments.clone();
    }

    /** {@inheritDoc} */
    public Object getReceiver() {
        return arguments.length > 0 ? arguments[0] : null;
    }

    /** {@inheritDoc} */
    public CallSiteDescriptor getCallSiteDescriptor() {
        return callSiteDescriptor;
    }

    /** {@inheritDoc} */
    public boolean isCallSiteUnstable() {
        return callSiteUnstable;
    }

    /** {@inheritDoc} */
    public LinkRequest replaceArguments(final CallSiteDescriptor newCallSiteDescriptor,
            final Object... newArguments) {
        return new SimpleLinkRequest(newCallSiteDescriptor, callSiteUnstable, newArguments);
    }
}
