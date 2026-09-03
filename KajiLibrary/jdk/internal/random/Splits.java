package jdk.internal.random;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.stream.Stream;

// KajiLibrary's jdk.internal.random.Splits -- la parte comun de `splits(...)`, compartida por los
// ocho generadores LXM.
//
// Existe porque en el JDK esto vive en una jerarquia de clases abstractas
// (`AbstractSpliteratorGenerator` -> `AbstractSplittableGenerator` ->
// `AbstractSplittableWithBrineGenerator`) que esta biblioteca no tiene: aca cada generador es
// `final` e implementa la interfaz directo. La logica igual tiene que estar en un solo lugar, asi
// que esta aca.
//
// **Una diferencia con el JDK, y es la de siempre en esta biblioteca**: el flujo es *ansioso*. El
// JDK devuelve un `Stream` perezoso sobre un spliterator que va partiendo a medida que se lo
// consume, y ademas usa una **salmuera con sal** --digitos de 4 bits que garantizan que dos hijos
// de ramas distintas del arbol de particion nunca compartan `a`--. Aca los `streamSize` hijos se
// construyen de una y la salmuera de cada uno sale del `source`, que da la misma garantia para un
// solo nivel de particion pero no para un arbol.
//
// Se documenta en vez de fingir: quien parta en un solo nivel --que es lo que hace el 99 % del
// codigo-- obtiene generadores independientes; quien arme un arbol profundo tiene menos garantia
// que en el JDK.
final class Splits {

    private Splits() {
    }

    static Stream<SplittableGenerator> de(SplittableGenerator padre, long streamSize,
            SplittableGenerator source) {
        if (streamSize < 0L) {
            throw new IllegalArgumentException("size must be non-negative");
        }
        if (source == null) {
            throw new NullPointerException("source");
        }
        List<SplittableGenerator> hijos = new ArrayList<SplittableGenerator>();
        long i = 0L;
        while (i < streamSize) {
            hijos.add(padre.split(source));
            i = i + 1L;
        }
        return hijos.stream();
    }
}
