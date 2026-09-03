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

// `invokedynamic`. No nombra un método: nombra un sitio de llamada que se resuelve la primera vez
// que se ejecuta, llamando al método de arranque que la entrada del pool señala. Por eso los
// accesores `default` de acá bajan a la tabla `BootstrapMethods` de la clase, y por eso una entrada
// `CONSTANT_InvokeDynamic` sólo tiene sentido dentro del archivo que la lleva.
public interface InvokeDynamicInstruction extends Instruction {

    /** La entrada del pool con el sitio de llamada. */
    InvokeDynamicEntry invokedynamic();

    /** El nombre del sitio de llamada. */
    default Utf8Entry name() {
        return invokedynamic().nameAndType().name();
    }

    /** El descriptor del sitio de llamada, como `Utf8`. */
    default Utf8Entry type() {
        return invokedynamic().nameAndType().type();
    }

    /** El descriptor del sitio de llamada. */
    default MethodTypeDesc typeSymbol() {
        return MethodTypeDesc.ofDescriptor(type().stringValue());
    }

    /** El método de arranque. */
    default DirectMethodHandleDesc bootstrapMethod() {
        return invokedynamic().bootstrap().bootstrapMethod().asSymbol();
    }

    /** Los argumentos estáticos del método de arranque, ya resueltos a descriptores nominales. */
    default List<ConstantDesc> bootstrapArgs() {
        BootstrapMethodEntry bsm = invokedynamic().bootstrap();
        List<LoadableConstantEntry> crudos = bsm.arguments();
        List<ConstantDesc> salida = new ArrayList<ConstantDesc>();
        for (int i = 0; i < crudos.size(); i++) {
            salida.add(crudos.get(i).constantValue());
        }
        return Collections.unmodifiableList(salida);
    }

    /** El `invokedynamic` de esta entrada. */
    public static InvokeDynamicInstruction of(InvokeDynamicEntry invokedynamic) {
        return Instructions.invokeDynamic(invokedynamic);
    }
}
