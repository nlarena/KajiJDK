package java.awt.geom;

import java.awt.Shape;

// java.awt.geom.GeneralPath de KajiLibrary -- un camino de precision float. Superficie publica
// completa: cuatro constructores y nada mas.
//
// No agrega comportamiento: es exactamente `Path2D.Float` con otro nombre. Existe porque es anterior
// a Path2D (viene de Java 1.2, Path2D es de la 6) y hay codigo que la nombra. Se la deja `final`
// como en el JDK.
//
// Del constructor de paquete `GeneralPath(int, byte[], int, float[], int)` que lista el JDK no hay
// nada que cumplir: no es publico ni protegido, asi que no es contrato -- es la puerta que usa el
// pipeline de dibujo interno para armar un camino sin copiar, y ese pipeline no existe aca.
// El `extends` va con el nombre completo por el finding #465: escrito `Path2D.Float`, el generador
// de bytecode resuelve `Float` contra `java.lang` en vez de contra los miembros de `Path2D`, se
// cree que la superclase es `java.lang.Float` y no le encuentra el constructor `(int, int)`. El
// verificador de tipos si lo resuelve bien --`--check` pasa y `--emit` no--, asi que el archivo
// parece sano hasta que se lo intenta emitir. Con el nombre calificado entero anda.
public final class GeneralPath extends java.awt.geom.Path2D.Float {

    public GeneralPath() {
        super(Path2D.WIND_NON_ZERO, Path2D.INIT_SIZE);
    }

    public GeneralPath(int rule) {
        super(rule, Path2D.INIT_SIZE);
    }

    public GeneralPath(int rule, int initialCapacity) {
        super(rule, initialCapacity);
    }

    public GeneralPath(Shape s) {
        super(s, null);
    }
}
