package java.lang.constant;

import java.util.Optional;

// Implemented by a type whose values can be described *nominally* — `String`, the box types,
// `Class`, and the descriptors in this package. The `Optional` is not decoration: a value of a
// constable type is not always constable (a `Class` for a hidden class has no nominal form), so
// the method has to be able to say "not this one".
public interface Constable {

    // A nominal descriptor for this value, if it has one.
    Optional<? extends ConstantDesc> describeConstable();
}
