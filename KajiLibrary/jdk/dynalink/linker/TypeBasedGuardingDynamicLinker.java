package jdk.dynalink.linker;

/**
 * A linker that can decide by the receiver's type, before looking at anything else.
 *
 * <h2>What separating that question is for</h2>
 *
 * <p>For not walking the whole chain on every link. A chain of ordinary linkers is tried in order
 * until one does not return {@code null}; if they are all of this kind, whoever composes them can
 * instead ask {@link #canLinkType} and build a <strong>cache by class</strong> — the second time a
 * receiver of that class shows up it already knows who to send it to.
 *
 * <p>That is exactly what {@code CompositeTypeBasedGuardingDynamicLinker} does, and it is the only
 * reason this interface exists apart from {@link GuardingDynamicLinker}.
 *
 * <h2>The answer is a permission, not a promise</h2>
 *
 * <p>Answering {@code true} does not oblige it to link: {@link #getGuardedInvocation} may still
 * return {@code null} if the concrete operation is of no use to it. The other way round it is a
 * commitment — answering {@code false} means this linker is never even asked.
 *
 * @since 9
 */
public interface TypeBasedGuardingDynamicLinker extends GuardingDynamicLinker {

    /**
     * Whether this linker wants the receivers of that class offered to it.
     *
     * @param type the receiver's class
     * @return {@code true} if it is interested
     */
    boolean canLinkType(Class<?> type);
}
