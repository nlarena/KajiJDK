// Repro de #233 - no se emitian puentes para overrides covariantes ABSTRACTOS.
//
// El puente lo necesita el LLAMADOR que ve el supertipo, no la implementacion: por eso hace falta
// aunque el metodo que estrecha el retorno sea abstracto. El javac del JDK 25 lo emite igual, y
// concreto -- `aload_0; invokevirtual <el angosto>; areturn` --; el despacho virtual lo lleva al
// override real de la subclase concreta.
//
//   bin\javac.exe --emit KajiLibrary\repros\finding_233.java
//   javap -p -s KajiLibrary\repros\finding_233$Media.class
//        -> ademas del `abstract Caja dame()`, un `Object dame()` (0x1041 BRIDGE+SYNTHETIC)
//   bin\run-headless.exe KajiLibrary\repros\finding_233.class run   -> 0
//
// Costo medido de no emitirlos: ~25 miembros de java.nio (`Buffer slice()`, `duplicate()`, ...).
public class finding_233 {

    abstract static class Base {
        abstract Object dame();
    }

    /* Estrecha el retorno y sigue siendo ABSTRACTA: es el caso que faltaba. */
    abstract static class Media extends Base {
        public abstract Caja dame();
    }

    /* El tipo estrechado es propio a proposito: asi el repro no depende de `String` ni de sus
       nativos, y mide solo el puente. */
    static class Caja {
        int n;
        Caja(int n) { this.n = n; }
    }

    static class Hoja extends Media {
        public Caja dame() { return new Caja(7); }
    }

    public static int run() {
        Hoja h = new Hoja();
        /* Por el tipo concreto: el metodo angosto, sin puente. */
        if (h.dame().n != 7) { return 1; }
        /* Por el supertipo que declara `Object dame()`: pasa por el PUENTE de `Media`. */
        Base b = h;
        Object o = b.dame();
        if (o == null) { return 2; }
        /* Y por el intermedio abstracto, que es el que antes no lo emitia. */
        Media m = h;
        if (m.dame().n != 7) { return 3; }
        return 0;
    }
}
