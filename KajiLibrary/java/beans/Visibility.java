package java.beans;

// How a bean negotiates with its environment whether it can count on a graphical interface. A bean
// running on a server with no screen needs to hear about it so as not to try to draw itself.
//
// The four methods are two questions and two notices: needsGui/avoidingGui are answered by the bean,
// dontUseGui/okToUseGui are told to it by the environment.
public interface Visibility {

    // Whether the bean CANNOT work without a graphical interface.
    boolean needsGui();

    // The environment tells it not to use one, even if there is one.
    void dontUseGui();

    // The environment tells it that it may use one.
    void okToUseGui();

    // Whether the bean is avoiding using one right now.
    boolean avoidingGui();
}
