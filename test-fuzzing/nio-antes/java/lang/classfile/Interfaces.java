package java.lang.classfile;

import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// A class's `interfaces` list, as a single element. It is one piece and not one per interface on
// purpose: the list is replaced whole or left alone, because its order is observable.
public interface Interfaces extends ClassElement {

    /** The direct interfaces, in file order. */
    List<ClassEntry> interfaces();

    /** The element for this list. */
    public static Interfaces of(List<ClassEntry> interfaces) {
        return new jdk.internal.classfile.impl.InterfacesImpl(
                new ArrayList<ClassEntry>(interfaces));
    }

    /** The element for these interfaces. */
    public static Interfaces of(ClassEntry... interfaces) {
        return of(Arrays.asList(interfaces));
    }

    /** The element for these descriptors, resolving them against a new pool. */
    public static Interfaces ofSymbols(List<ClassDesc> interfaces) {
        java.lang.classfile.constantpool.ConstantPoolBuilder cp =
                java.lang.classfile.constantpool.ConstantPoolBuilder.of();
        List<ClassEntry> entries = new ArrayList<ClassEntry>();
        for (int i = 0; i < interfaces.size(); i++) {
            entries.add(cp.classEntry(interfaces.get(i)));
        }
        return new jdk.internal.classfile.impl.InterfacesImpl(entries);
    }

    /** The element for these descriptors. */
    public static Interfaces ofSymbols(ClassDesc... interfaces) {
        return ofSymbols(Arrays.asList(interfaces));
    }
}
