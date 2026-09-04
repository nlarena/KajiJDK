package jdk.internal.classfile.impl;

import java.lang.classfile.BufWriter;
import java.lang.classfile.constantpool.ConstantPool;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.constantpool.PoolEntry;

/**
 * El bufer de bytes en el que se escribe un `.class`.
 *
 * <p>Crece solo, en potencias de dos: duplicar es lo que hace que escribir N bytes cueste O(N) en
 * total y no O(N^2). Nada mas que eso.
 *
 * <p><strong>`patchInt` es la razon por la que esto es un arreglo y no un flujo.</strong> Varias
 * estructuras del formato llevan adelante un largo que solo se sabe despues de escribir el
 * contenido --el de un atributo, el de `code`-- y la unica forma de no recorrer todo dos veces es
 * dejar el hueco, seguir, y volver a taparlo.
 */
public final class BufWriterImpl implements BufWriter {

    private final ConstantPoolBuilder pool;
    private byte[] buf;
    private int size;

    /** Un bufer que escribe indices contra ese pool. */
    public BufWriterImpl(ConstantPoolBuilder pool) {
        this.pool = pool;
        this.buf = new byte[1024];
        this.size = 0;
    }

    public ConstantPoolBuilder constantPool() {
        return this.pool;
    }

    /**
     * Si una entrada de ese pool se puede escribir por su indice tal cual.
     *
     * <p>Solo si es **el mismo** pool: un indice de otro pool nombra otra cosa. Quien escriba una
     * entrada ajena tiene que adoptarla primero.
     */
    public boolean canWriteDirect(ConstantPool other) {
        return this.pool == other;
    }

    /** Reserva lugar para al menos esos bytes mas. */
    public void reserveSpace(int freeBytes) {
        this.grow(this.size + freeBytes);
    }

    private void grow(int needed) {
        if (needed <= this.buf.length) {
            return;
        }
        int n = this.buf.length;
        while (n < needed) {
            n = n * 2;
        }
        byte[] mas = new byte[n];
        System.arraycopy(this.buf, 0, mas, 0, this.size);
        this.buf = mas;
    }

    public void writeU1(int x) {
        this.grow(this.size + 1);
        this.buf[this.size] = (byte) x;
        this.size = this.size + 1;
    }

    public void writeU2(int x) {
        this.grow(this.size + 2);
        this.buf[this.size] = (byte) (x >> 8);
        this.buf[this.size + 1] = (byte) x;
        this.size = this.size + 2;
    }

    public void writeInt(int x) {
        this.grow(this.size + 4);
        this.buf[this.size] = (byte) (x >> 24);
        this.buf[this.size + 1] = (byte) (x >> 16);
        this.buf[this.size + 2] = (byte) (x >> 8);
        this.buf[this.size + 3] = (byte) x;
        this.size = this.size + 4;
    }

    public void writeLong(long x) {
        this.writeInt((int) (x >> 32));
        this.writeInt((int) x);
    }

    public void writeFloat(float x) {
        this.writeInt(Float.floatToRawIntBits(x));
    }

    public void writeDouble(double x) {
        this.writeLong(Double.doubleToRawLongBits(x));
    }

    public void writeBytes(byte[] arr) {
        this.writeBytes(arr, 0, arr.length);
    }

    public void writeBytes(byte[] arr, int offset, int length) {
        this.grow(this.size + length);
        System.arraycopy(arr, offset, this.buf, this.size, length);
        this.size = this.size + length;
    }

    /** Reescribe `intSize` bytes en `offset` con ese valor. Ver la nota de la clase. */
    public void patchInt(int offset, int intSize, int value) {
        for (int i = 0; i < intSize; i++) {
            this.buf[offset + i] = (byte) (value >> ((intSize - 1 - i) * 8));
        }
    }

    /** Escribe ese valor en `intSize` bytes. */
    public void writeIntBytes(int intSize, long value) {
        for (int i = 0; i < intSize; i++) {
            this.writeU1((int) (value >> ((intSize - 1 - i) * 8)));
        }
    }

    /**
     * El indice de esa entrada, en dos bytes.
     *
     * <p>Si la entrada viene de **otro** pool se la adopta primero. Sin eso, transformar una clase
     * escribe los indices del pool original en el archivo nuevo: el `.class` sale bien formado y
     * apunta a cualquier cosa, asi que no falla al escribirlo sino al leerlo, con un mensaje que no
     * dice nada del lugar donde estuvo el error.
     */
    public void writeIndex(PoolEntry entry) {
        if (entry == null) {
            throw new NullPointerException("no hay entrada de pool que escribir");
        }
        this.writeU2(this.adopt(entry).index());
    }

    /** El indice de esa entrada, o cero si es `null`. */
    public void writeIndexOrZero(PoolEntry entry) {
        this.writeU2(entry == null ? 0 : this.adopt(entry).index());
    }

    /**
     * El indice que esa entrada tiene **en este pool**, adoptandola si venia de otro.
     *
     * <p>Lo necesita quien escribe el indice en un solo byte (`ldc`), donde `writeIndex` no sirve.
     * Toda escritura de un indice tiene que pasar por aca o por `writeIndex`: escribir
     * `entry.index()` a secas es lo que produce un archivo que apunta al pool equivocado.
     */
    public int indexOf(PoolEntry entry) {
        return this.adopt(entry).index();
    }

    private PoolEntry adopt(PoolEntry entry) {
        if (entry.constantPool() == this.pool) {
            return entry;
        }
        if (this.pool instanceof ConstantPoolBuilderImpl) {
            return ((ConstantPoolBuilderImpl) this.pool).adoptEntry(entry);
        }
        // Un pool que no es el nuestro y que no sabe adoptar: no hay forma de traducir el indice, y
        // escribirlo tal cual seria escribir un archivo que miente.
        throw new IllegalArgumentException(
                "la entrada viene de otro pool y este no sabe adoptarla: " + entry);
    }

    public int size() {
        return this.size;
    }

    /** Los bytes escritos, en un arreglo del tamano justo. */
    public byte[] toByteArray() {
        byte[] out = new byte[this.size];
        System.arraycopy(this.buf, 0, out, 0, this.size);
        return out;
    }
}
