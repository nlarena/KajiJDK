package java.lang.reflect;

/**
 * The behaviour behind a dynamic proxy: what happens when a method is called on one.
 *
 * <p>A {@link Proxy} instance implements a set of interfaces without any of them being written down.
 * Every call it receives — {@code equals} and {@code hashCode} and {@code toString} included — is
 * turned into a call to {@link #invoke} on this handler, with the {@link Method} that was called
 * reified as an argument. That single indirection is what every mocking library, every JDK-proxy
 * AOP interceptor and every {@code java.rmi} stub in the world is built on.
 *
 * <p>{@code invoke} declares {@code throws Throwable} because a handler must be able to rethrow
 * whatever the proxied method declares, and the handler cannot know statically what that is. If the
 * handler throws something the proxied method does <em>not</em> declare, the proxy wraps it in an
 * {@link UndeclaredThrowableException} — which is the entire reason that class exists.
 *
 * <h2>KajiLibrary status</h2>
 *
 * <p>The interface is complete, and {@link Proxy} calls it for real: a proxy instance routes every
 * interface method — plus {@code equals}, {@code hashCode} and {@code toString} — to {@link #invoke}.
 *
 * <p>{@link #invokeDefault} is the one gap, and it is a gap for a reason that is not laziness. Its
 * job is to run an interface's {@code default} body <em>as the proxy would have</em>, which at the
 * bytecode level is an {@code invokespecial} into a superinterface — an instruction only the proxy
 * class itself is allowed to execute (JVMS §6.5, {@code invokespecial}). Reaching it needs either
 * {@code MethodHandles.Lookup.findSpecial} with the proxy class as the special caller, or an extra
 * generated method per default method. Neither is wired, so this throws
 * {@link UnsupportedOperationException} rather than invent a semantics — running the body on some
 * other receiver, or calling back into the handler and looping, would both be wrong in ways the
 * caller could not see.
 */
public interface InvocationHandler {

    /**
     * Handles a method call made on a proxy instance.
     *
     * @param proxy the proxy the call was made on
     * @param method the method that was called, resolved on the interface that declared it
     * @param args the arguments, boxed, or {@code null} if the method takes none
     * @return the value to return from the call, unboxed if the return type is primitive
     * @throws Throwable anything the proxied method declares; anything else is wrapped in an
     *         {@link UndeclaredThrowableException}
     */
    Object invoke(Object proxy, Method method, Object[] args) throws Throwable;

    /**
     * Invokes the {@code default} implementation of {@code method} on {@code proxy}.
     *
     * <p>The way for a handler to say "do whatever the interface would have done", without which
     * intercepting one method of an interface means reimplementing its default methods too.
     *
     * <p>Not supported in KajiLibrary — see the interface notes.
     *
     * @param proxy the proxy instance
     * @param method the default method to invoke
     * @param args the arguments
     * @return the result of the default implementation
     * @throws Throwable whatever the default implementation throws
     */
    static Object invokeDefault(Object proxy, Method method, Object... args) throws Throwable {
        throw new UnsupportedOperationException(
                "invokeDefault requires invokespecial into a superinterface");
    }
}
