// Repro de #251 - una llamada encadenada sobre un tipo que el archivo no NOMBRA se descarta en
// silencio, y el metodo sale con el cuerpo vacio.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_251.java
//   bin\jvm.exe -v KajiLibrary\repros\finding_251.class      -> cuantas(): stack=0, solo `ireturn`
//   bin\run-headless.exe KajiLibrary\repros\finding_251.class cuantas  -> operand stack underflow
//
// `RandomGeneratorFactory.all()` devuelve un Stream, y sobre el se llama `count()`. Este archivo
// nunca NOMBRA a Stream: no hace falta, es Java valido y no se importa lo que no se escribe.
// El compilador no resuelve el `count()`, y en vez de dar error se come la expresion entera —
// `invokestatic all; invokeinterface count; l2i` desaparece y queda un `ireturn` sobre pila vacia.
//
// La contraprueba esta en finding_251b.java: MISMA expresion, mismo `-cp`, pero el archivo
// menciona a Stream en otro metodo. Ahi compila bien y devuelve 12. No hace falta llamar a ese
// otro metodo; alcanza con que el tipo aparezca escrito en algun lado.
//
// El gate no lo puede ver: el descriptor queda perfecto, ()I. Revienta recien al ejecutar.
// Mismo patron que #127 — cuando algo no se puede representar, se emite silencio en vez de error.
import java.util.random.RandomGeneratorFactory;

public class finding_251 {

    public static int cuantas() {
        return (int) RandomGeneratorFactory.all().count();
    }
}
