package java.lang.constant;

// Minimal java.lang.constant.ClassDesc — a *nominal* description of a class: its name,
// with no requirement that the class be loaded. The real one carries the whole
// descriptor grammar (arrays, primitives, resolution to a Class); ours carries the name,
// which is all a `switch` over enum patterns asks of it.
//
// It has to be an **interface** with a static factory, not a class: javac references
// `ClassDesc.of` as an InterfaceMethodref, so the shape is part of the contract.
public interface ClassDesc {
    // The condy behind an enum pattern label calls this to name the enum's class.
    static ClassDesc of(String name) {
        return new ConstantClassDesc(name);
    }

    String descriptorString();
}
