package q;

// La mitad de abajo. Nombra a `p.Base` **cualificada**, que es lo que disparaba el #314: en una
// compilacion multi-unidad el generador no buscaba las clases del fuente por su nombre completo, y
// como el shadowing del fuente impide cargar la del classpath, no quedaba ningun camino.
//
// Se reproduce emitiendo las dos **juntas**:
//
//     javac --emit scratchpad/zz314/p/Base.java scratchpad/zz314/q/Impl.java
//
// Antes del arreglo: "el generador de bytecode no puede resolver el tipo `p.Base`".
// Por separado siempre anduvo, porque ahi la hermana llega como externa por el `-cp`.
public final class Impl implements p.Base {

    public int valor() {
        return 42;
    }
}
