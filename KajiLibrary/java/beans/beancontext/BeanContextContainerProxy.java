package java.beans.beancontext;

import java.awt.Container;

/**
 * Implemented by a {@link BeanContext} that is also a visual container.
 *
 * <p>The container-side counterpart of {@link BeanContextChildComponentProxy}, and for the same
 * reason: a context does not have to be visible, so the relation to AWT is declared instead of
 * inherited.
 *
 * <p>What it is for is obvious once said: if the context is a container and its children have
 * components, the bean hierarchy and the GUI hierarchy can be kept aligned.
 *
 * @deprecated see {@link BeanContextChildComponentProxy}.
 */
@Deprecated(since = "23", forRemoval = true)
public interface BeanContextContainerProxy {

    /** The container that represents this context; never {@code null}. */
    Container getContainer();
}
