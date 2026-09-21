package java.beans.beancontext;

/**
 * An object that **is not** a {@link BeanContextChild} but delegates to one.
 *
 * <p>It exists because of an inheritance constraint: a class that already extends another hierarchy
 * cannot also extend {@link BeanContextChildSupport}. By implementing this interface it returns the
 * child it delegates to, and the context treats that one as the real member.
 *
 * <p>An object implements either `BeanContextChild` or this interface, not both: with both, the
 * context would have no way to decide which one is the member.
 *
 * <h2>The two AWT proxies</h2>
 *
 * <p>The JDK has two more interfaces in this family: `BeanContextChildComponentProxy`, which
 * returns a `java.awt.Component`, and `BeanContextContainerProxy`, which returns a
 * `java.awt.Container`. This note said KajiLibrary leaves both out because `java.awt` here stops at
 * geometry and colour, with no component hierarchy to name. That is no longer true:
 * `java.awt.Component` and `java.awt.Container` exist in KajiLibrary, and both interfaces are in
 * this package.
 */
public interface BeanContextProxy {

    /** The child this object delegates to. */
    BeanContextChild getBeanContextProxy();
}
