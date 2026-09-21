package java.beans.beancontext;

import java.awt.Component;

/**
 * Implemented by a {@link BeanContextChild} that also has a visual part.
 *
 * <h2>Why the visual part is separate and not in the bean itself</h2>
 *
 * <p>Because a bean does not have to be visible. A {@link BeanContextChild} can be a service, a
 * data source or anything with no on-screen representation, and forcing it to extend {@link
 * Component} would tie it to AWT for no reason.
 *
 * <p>This interface is the way out: whoever has a component says so by implementing it, and the
 * container asks with an {@code instanceof} instead of assuming.
 *
 * @deprecated the {@code BeanContext} model has no replacement and is deprecated for removal. This
 *     tag used to end in "see the package"; this tree has no package documentation to see.
 */
@Deprecated(since = "23", forRemoval = true)
public interface BeanContextChildComponentProxy {

    /** The component that represents this bean; never {@code null}. */
    Component getComponent();
}
