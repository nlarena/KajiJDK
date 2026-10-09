package java.lang.classfile;

// The root of everything that can be part of a `.class` file seen as a sequence of pieces:
// attributes, flags, member models and instructions. It declares nothing; its job is to give a common
// type to what a {@link ClassFileBuilder} accepts and a {@link CompoundElement} emits.
//
// In the JDK this interface is `sealed`. Here it is not, for the reason {@code PoolEntry} explains:
// sealing would force the public package to name its internal implementations.
public interface ClassFileElement {
}
