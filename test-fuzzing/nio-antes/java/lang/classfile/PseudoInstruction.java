package java.lang.classfile;

// A piece of a method's body that is NOT an instruction: a label, a line number mark, a local
// variable's scope or an exception handler. It takes no bytes in the `code` array; it comes out of
// the tables of the attributes accompanying it.
public interface PseudoInstruction extends CodeElement {
}
