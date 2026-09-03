package java.lang.classfile.constantpool;

import java.lang.classfile.BootstrapMethodEntry;
import java.util.Iterator;
import java.util.NoSuchElementException;

// El pool de constantes de una clase (JVMS §4.4), más la tabla del atributo `BootstrapMethods`, que
// la API trata como una segunda mitad del pool porque `CONSTANT_Dynamic` y `CONSTANT_InvokeDynamic`
// la indexan igual que a él.
//
// `size()` es el `constant_pool_count` del archivo: uno MÁS que el índice más alto usable. El índice
// 0 no existe por definición del formato, y las ranuras que siguen a un `long` o a un `double`
// tampoco. El `iterator()` de acá salta las dos cosas: recorre entradas reales, no ranuras.
public interface ConstantPool extends Iterable<PoolEntry> {

    /** La entrada en `index`. Tira `ConstantPoolException` si el índice no es una entrada válida. */
    PoolEntry entryByIndex(int index);

    /** El `constant_pool_count`: uno más que el índice más alto. */
    int size();

    /**
     * La entrada en `index`, exigiendo que sea del tipo `cls`. Tira `ConstantPoolException` si el
     * índice no vale o si la entrada es de otra clase — que es la razón de ser del método: un lector
     * que aceptara la entrada equivocada acá dejaría pasar un archivo mal formado.
     */
    <T extends PoolEntry> T entryByIndex(int index, Class<T> cls);

    /** Recorre las entradas reales del pool, en orden de índice. */
    default Iterator<PoolEntry> iterator() {
        return new IteradorDePool(this);
    }

    /** La entrada `index` de la tabla de `BootstrapMethods`. */
    BootstrapMethodEntry bootstrapMethodEntry(int index);

    /** Cuántos métodos de arranque tiene la clase. */
    int bootstrapMethodCount();
}

// El iterador del `default` de arriba. Es una clase de paquete y no una anónima porque tiene estado
// —el índice— y porque saltar el hueco de `long`/`double` se lee mejor con nombre.
final class IteradorDePool implements Iterator<PoolEntry> {

    private final ConstantPool pool;
    private int indice;

    IteradorDePool(ConstantPool pool) {
        this.pool = pool;
        this.indice = 1;
    }

    public boolean hasNext() {
        return this.indice < this.pool.size();
    }

    public PoolEntry next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        PoolEntry e = this.pool.entryByIndex(this.indice);
        this.indice += e.width();
        return e;
    }
}
