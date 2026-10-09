package jdk.dynalink;

/**
 * The namespace an {@link Operation} acts on.
 *
 * <p>The interface is **empty on purpose**: a namespace is an identity, not a behaviour. The only
 * thing an implementation is asked for is that it compare with `equals` —
 * {@link NamespaceOperation#contains} does nothing else. The language's three are in
 * {@link StandardNamespace}; a dynamic language that needs more (a "global variables" namespace,
 * say) declares its own.
 *
 * @since 9
 */
public interface Namespace {
}
