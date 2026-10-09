package java.lang.runtime;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles$Lookup;
import java.lang.invoke.MethodType;

/**
 * The two bootstraps of the {@code switch} with patterns.
 *
 * <h2>What changed when the {@code switch} stopped being over integers</h2>
 *
 * <p>A classic {@code switch} compiles to a {@code tableswitch} or a {@code lookupswitch}: the value
 * is an integer and the choice is a lookup. A {@code switch} over <em>type patterns</em> cannot do
 * that -- {@code case String s} is not a number, it is an {@code instanceof} question -- and emitting
 * the chain of {@code instanceof} into the class file would freeze the strategy all over again.
 *
 * <p>The solution is the usual one here: the class file says what the labels are and an
 * {@code invokedynamic} asks for the selector. What these two methods return is a {@link CallSite}
 * whose handle answers <strong>the index of the first label that matches</strong>, or the number of
 * labels if none matches -- back to an integer, which is what the {@code tableswitch} below knows how
 * to use.
 *
 * <h2>Why there are two and not one</h2>
 *
 * <p>{@link #enumSwitch} exists because the labels of a {@code switch} over an enum are
 * <em>constant names</em>, and resolving a name to its constant needs the enum's class loaded. Doing
 * it in the bootstrap --once, at linking time-- instead of on every execution is the whole point.
 *
 * <h2>Here the VM does it</h2>
 *
 * <p>Just like {@link ObjectMethods}: implemented in Rust and recognised by name, so these bodies do
 * not run. The declaration is there so the call site resolves.
 *
 * @since 21
 */
public final class SwitchBootstraps {

    private SwitchBootstraps() {
    }

    /**
     * The selector of a {@code switch} over type patterns.
     *
     * @param lookup the access context of whoever does the {@code switch}
     * @param invocationName the call site's name, which this bootstrap ignores
     * @param invocationType the selector's shape: it takes the value and the index to resume from,
     *     and returns an {@code int}
     * @param labels the labels, in the order they are written -- and that order matters, because the
     *     first that matches wins
     */
    public static CallSite typeSwitch(MethodHandles$Lookup lookup, String invocationName,
            MethodType invocationType, Object... labels) {
        throw new UnsupportedOperationException("the switch's selector is built by the VM");
    }

    /**
     * The selector of a {@code switch} over an enum, where the labels can be constant names as well
     * as patterns.
     */
    public static CallSite enumSwitch(MethodHandles$Lookup lookup, String invocationName,
            MethodType invocationType, Object... labels) {
        throw new UnsupportedOperationException("the switch's selector is built by the VM");
    }
}
