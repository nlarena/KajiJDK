package java.lang.classfile;

import java.lang.classfile.constantpool.ConstantPool;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.constantpool.PoolEntry;

// El buffer donde se arma un `.class`: un arreglo de bytes que crece, más el pool de constantes al
// que van a parar los índices que se escriben. Es big-endian en todo, como el formato.
public interface BufWriter {

    /** El pool contra el que se resuelven los índices que se escriben acá. */
    ConstantPoolBuilder constantPool();

    /** Si los índices de `constantPool` se pueden escribir tal cual. */
    boolean canWriteDirect(ConstantPool constantPool);

    /** Pide lugar para `freeBytes` bytes más. Es una optimización; no cambia el contenido. */
    void reserveSpace(int freeBytes);

    /** Escribe un byte. */
    void writeU1(int x);

    /** Escribe dos bytes, big-endian. */
    void writeU2(int x);

    /** Escribe cuatro bytes, big-endian. */
    void writeInt(int x);

    /** Escribe un `float` en su forma IEEE 754 de cuatro bytes. */
    void writeFloat(float x);

    /** Escribe ocho bytes, big-endian. */
    void writeLong(long x);

    /** Escribe un `double` en su forma IEEE 754 de ocho bytes. */
    void writeDouble(double x);

    /** Escribe el arreglo entero. */
    void writeBytes(byte[] arr);

    /** Escribe `length` bytes desde `offset`. */
    void writeBytes(byte[] arr, int offset, int length);

    /** Pisa `intSize` bytes en `offset` con `value`. Es lo que cierra un `attribute_length`. */
    void patchInt(int offset, int intSize, int value);

    /** Escribe los `intSize` bytes bajos de `value`. */
    void writeIntBytes(int intSize, long value);

    /** Escribe el índice de `entry` como u2. */
    void writeIndex(PoolEntry entry);

    /** Como `writeIndex`, pero un `null` se escribe como el índice 0. */
    void writeIndexOrZero(PoolEntry entry);

    /** Cuántos bytes lleva escritos. */
    int size();
}
