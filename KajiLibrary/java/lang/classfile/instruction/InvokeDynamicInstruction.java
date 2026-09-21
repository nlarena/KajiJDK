package java.lang.classfile.instruction;

import java.lang.classfile.BootstrapMethodEntry;
import java.lang.classfile.Instruction;
import java.lang.classfile.constantpool.InvokeDynamicEntry;
import java.lang.classfile.constantpool.LoadableConstantEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jdk.internal.classfile.impl.Instructions;

// `invokedynamic`. It does not name a method: it names a call site that is resolved the first time
// it runs, by calling the bootstrap method the pool entry points at. That is why the `default`
// accessors here climb down into the class's `BootstrapMethods` table, and why a
// `CONSTANT_InvokeDynamic` entry only makes sense inside the file that carries it.
public interface InvokeDynamicInstruction extends Instruction {

    /** The pool entry holding the call site. */
    InvokeDynamicEntry invokedynamic();

    /** The call site's name. */
    default Utf8Entry name() {
        return invokedynamic().nameAndType().name();
    }

    /** The call site's descriptor, as a `Utf8`. */
    default Utf8Entry type() {
        return invokedynamic().nameAndType().type();
    }

    /** The call site's descriptor. */
    default MethodTypeDesc typeSymbol() {
        return MethodTypeDesc.ofDescriptor(type().stringValue());
    }

    /** The bootstrap method. */
    default DirectMethodHandleDesc bootstrapMethod() {
        return invokedynamic().bootstrap().bootstrapMethod().asSymbol();
    }

    /** The bootstrap method's static arguments, already resolved to nominal descriptors. */
    default List<ConstantDesc> bootstrapArgs() {
        BootstrapMethodEntry bsm = invokedynamic().bootstrap();
        List<LoadableConstantEntry> raw = bsm.arguments();
        List<ConstantDesc> out = new ArrayList<ConstantDesc>();
        for (int i = 0; i < raw.size(); i++) {
            out.add(raw.get(i).constantValue());
        }
        return Collections.unmodifiableList(out);
    }

    /** This entry's `invokedynamic`. */
    public static InvokeDynamicInstruction of(InvokeDynamicEntry invokedynamic) {
        return Instructions.invokeDynamic(invokedynamic);
    }
}
