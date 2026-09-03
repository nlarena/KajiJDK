package java.lang.classfile;

// La raíz de todo lo que puede formar parte de un archivo `.class` visto como una secuencia de
// piezas: atributos, banderas, modelos de miembro e instrucciones. No declara nada; su trabajo es
// dar un tipo común a lo que un {@link ClassFileBuilder} acepta y un {@link CompoundElement} emite.
//
// En el JDK esta interfaz es `sealed`. Acá no lo es, por la razón que explica {@code PoolEntry}: el
// sellado obligaría al paquete público a nombrar sus implementaciones internas.
public interface ClassFileElement {
}
