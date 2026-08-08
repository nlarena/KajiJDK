// Finding #8 — falta el chequeo de completitud de métodos abstractos (JLS §8.1.1.1).
// Una clase CONCRETA con métodos abstractos heredados sin implementar igual compila. javac real
// la rechaza ("does not override abstract method …").
//
// Esperado (javac real): ERROR — ~24 métodos de List sin implementar.
// Síntoma del bug:        compila sin error (esconde errores reales → AbstractMethodError en runtime).
//
// Repro: cargo run -- --emit KajiLibrary/repros/finding_08.java   (¿compila? entonces el bug sigue)
package java.util;

import java.util.List;

public class P<E> implements List<E> {
    public int size() { return 0; }
}
