// Repro de #503: un `case` cuya etiqueta es una constante `static final int` de OTRA clase
// no se pliega, y el generador de bytecode la rechaza.
//
//   bin/javac.exe --emit -cp KajiLibrary Finding503.java  -> "un `case` que no es una constante entera"
//
// El JDK 25 compila el archivo entero. La JLS 14.11 exige que la etiqueta sea una expresion
// constante (15.29), y un `static final int` inicializado con un literal lo es.
//
// Lo que separa el caso que falla de los que andan es DE DONDE sale la constante:
//   A (literal), B (del mismo tipo), C (de otra clase del mismo archivo), D (de una interfaz
//   del mismo archivo)  -> compilan y emiten
//   E (de una clase del CLASSPATH, leida de un .class)                  -> falla
// O sea: el plegado lee constantes del arbol de sintaxis y no del atributo ConstantValue de un
// .class ya compilado.
public class Finding503 {

    static final int PROPIA = 7;

    interface Ajena {
        int DE_INTERFAZ = 3;
    }

    static class OtraClase {
        static final int DE_CLASE = 5;
    }

    // A: literal -- compila
    static int a(int x) {
        switch (x) {
            case 7: return 1;
            default: return 0;
        }
    }

    // B: constante del MISMO tipo -- ?
    static int b(int x) {
        switch (x) {
            case PROPIA: return 1;
            default: return 0;
        }
    }

    // C: constante de otra clase del mismo archivo -- ?
    static int c(int x) {
        switch (x) {
            case OtraClase.DE_CLASE: return 1;
            default: return 0;
        }
    }

    // D: constante de una interfaz -- ?
    static int d(int x) {
        switch (x) {
            case Ajena.DE_INTERFAZ: return 1;
            default: return 0;
        }
    }

    // E: constante de una clase del classpath -- el caso que aparecio
    static int e(int x) {
        switch (x) {
            case java.sql.Types.BIT: return 1;
            default: return 0;
        }
    }
}
