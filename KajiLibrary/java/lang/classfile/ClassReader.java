package java.lang.classfile;

import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantPool;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.util.Optional;
import java.util.function.Function;

// La vista de bajo nivel de un `.class` que se está leyendo: el arreglo de bytes con acceso por
// offset, más el pool ya construido. Es lo que recibe un {@link AttributeMapper} para interpretar el
// cuerpo de un atributo, y es el único lugar de la API donde se habla de offsets absolutos.
//
// Todos los `read*` validan el rango antes de tocar el arreglo y tiran
// `ConstantPoolException` si el offset se sale del archivo. Esa decisión es deliberada: un lector
// que devuelve basura para un archivo truncado es peor que uno que falla.
public interface ClassReader extends ConstantPool {

    /** Los mapeadores de atributos a medida que se registraron al abrir el archivo. */
    Function<Utf8Entry, AttributeMapper<?>> customAttributes();

    /** El `access_flags` de la clase, crudo. */
    int flags();

    /** La entrada `this_class`. */
    ClassEntry thisClassEntry();

    /** La entrada `super_class`; vacío si el índice es 0. */
    Optional<ClassEntry> superclassEntry();

    /** El largo del archivo en bytes. */
    int classfileLength();

    /** La entrada cuyo índice es el u2 que está en `offset`. */
    PoolEntry readEntry(int offset);

    /** Como `readEntry`, exigiendo que la entrada sea de la clase `cls`. */
    <T extends PoolEntry> T readEntry(int offset, Class<T> cls);

    /** Como `readEntry`, pero devuelve `null` si el índice es 0. */
    PoolEntry readEntryOrNull(int offset);

    /** Como `readEntry(int, Class)`, pero devuelve `null` si el índice es 0. */
    <T extends PoolEntry> T readEntryOrNull(int offset, Class<T> cls);

    /** El byte sin signo en `offset`. */
    int readU1(int offset);

    /** Los dos bytes sin signo en `offset`. */
    int readU2(int offset);

    /** El byte con signo en `offset`. */
    int readS1(int offset);

    /** Los dos bytes con signo en `offset`. */
    int readS2(int offset);

    /** Los cuatro bytes en `offset`, como `int`. */
    int readInt(int offset);

    /** Los ocho bytes en `offset`, como `long`. */
    long readLong(int offset);

    /** Los cuatro bytes en `offset`, como `float`. */
    float readFloat(int offset);

    /** Los ocho bytes en `offset`, como `double`. */
    double readDouble(int offset);

    /** Una copia de `len` bytes desde `offset`. */
    byte[] readBytes(int offset, int len);

    /** Copia `len` bytes desde `offset` a `buf`. */
    void copyBytesTo(BufWriter buf, int offset, int len);
}
