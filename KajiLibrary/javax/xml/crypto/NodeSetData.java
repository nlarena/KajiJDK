package javax.xml.crypto;

import java.util.Iterator;

/**
 * KajiLibrary's javax.xml.crypto.NodeSetData -- un conjunto de nodos, como dato a firmar.
 *
 * <p>Es {@code Iterable}, asi que entra en un {@code for} mejorado. El parametro de tipo dice de que
 * son los nodos: la implementacion de DOM lo instancia con {@code org.w3c.dom.Node}, pero nada obliga
 * a que sea DOM -- una implementacion sobre otro modelo usa el suyo.
 *
 * <p>Ese parametro llego en Java 9. Antes era una lista sin tipo y habia que castear cada nodo, que
 * sobre datos que vienen de un documento firmado por otro es exactamente donde no conviene adivinar.
 */
public interface NodeSetData<T> extends Data, Iterable<T> {

    /** Los nodos, en orden de documento. */
    Iterator<T> iterator();
}
