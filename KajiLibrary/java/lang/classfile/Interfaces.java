package java.lang.classfile;

import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// La lista `interfaces` de una clase, como un solo elemento. Es una sola pieza y no una por interfaz
// a propósito: la lista se reemplaza entera o no se toca, porque su orden es observable.
public interface Interfaces extends ClassElement {

    /** Las interfaces directas, en el orden del archivo. */
    List<ClassEntry> interfaces();

    /** El elemento para esta lista. */
    public static Interfaces of(List<ClassEntry> interfaces) {
        return new jdk.internal.classfile.impl.InterfacesImpl(
                new ArrayList<ClassEntry>(interfaces));
    }

    /** El elemento para estas interfaces. */
    public static Interfaces of(ClassEntry... interfaces) {
        return of(Arrays.asList(interfaces));
    }

    /** El elemento para estos descriptores, resolviéndolos contra un pool nuevo. */
    public static Interfaces ofSymbols(List<ClassDesc> interfaces) {
        java.lang.classfile.constantpool.ConstantPoolBuilder cp =
                java.lang.classfile.constantpool.ConstantPoolBuilder.of();
        List<ClassEntry> entradas = new ArrayList<ClassEntry>();
        for (int i = 0; i < interfaces.size(); i++) {
            entradas.add(cp.classEntry(interfaces.get(i)));
        }
        return new jdk.internal.classfile.impl.InterfacesImpl(entradas);
    }

    /** El elemento para estos descriptores. */
    public static Interfaces ofSymbols(ClassDesc... interfaces) {
        return ofSymbols(Arrays.asList(interfaces));
    }
}
