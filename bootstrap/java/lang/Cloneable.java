package java.lang;

// Marker interface (JLS §10.7 / Object.clone contract): it declares NO methods.
// Implementing it is a class's opt-in to field-for-field copying — Object.clone()
// checks `instanceof Cloneable` at run time and throws CloneNotSupportedException
// for classes that didn't opt in. Arrays implement it implicitly (the VM says so;
// no class file is involved).
public interface Cloneable {
}
