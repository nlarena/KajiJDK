package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.BootstrapMethodEntry;
import java.util.List;

// `BootstrapMethods` (JVMS §4.7.23): la tabla que resuelven `invokedynamic` y `CONSTANT_Dynamic`.
// Es medio pool y medio atributo: las entradas dinámicas del pool la indexan, así que sin ella el
// pool no se puede resolver del todo.
//
// No tiene fábrica, y en el JDK tampoco: la tabla la administra el
// {@link java.lang.classfile.constantpool.ConstantPoolBuilder} —cada `bsmEntry` le agrega una fila y
// devuelve su índice—, así que armar una a mano dejaría al pool y al atributo diciendo cosas
// distintas.
public interface BootstrapMethodsAttribute extends Attribute<BootstrapMethodsAttribute> {

    /** Las filas, en el orden del archivo. */
    List<BootstrapMethodEntry> bootstrapMethods();

    /** Cuántas filas hay. */
    int bootstrapMethodsSize();
}
