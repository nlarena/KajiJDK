package java.nio.file.attribute;

// The root of the attribute views: all it knows is how to say what it is called.
//
// It is an empty interface but for `name()`, and that is all that is needed for the rest of the
// package to be writable: the name is the key `Files.getAttribute("posix:owner")` chooses the view
// with. KajiJDK provides no implementation --there is nowhere to take the attributes from-- but the
// type has to exist all the same, because it is the parameter of methods that do exist.
public interface AttributeView {

    /** The view's name, the prefix that identifies it in `"view:attribute"`. */
    String name();
}
