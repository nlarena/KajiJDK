package repro101;

// Repro de #101 - un nombre CALIFICADO de tipo anidado, `Outer.Nested`, no resolvia.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_101.java
//
// ANTES: solo andaba el nombre simple `Nested` —en scope o traido por un import de un solo
// tipo—. La forma calificada fallaba, dentro del mismo archivo y entre archivos:
//
//   Flag[] viaSimple()                 OK
//   finding_101.Flag[] viaQualified()  error: no se encuentra el simbolo: finding_101.Flag
//
// El rodeo era importar el tipo anidado y usar el nombre simple.
//
// AHORA: **compila entero**. `#101` figura arreglado y verificado (2026-08-24) en
// COMPILER_FINDINGS.md. Comprobado de nuevo en la tanda de colecciones: `Map.Entry` como tipo de
// retorno, como parametro y en `implements Map.Entry<K, V>` resuelve sin rodeo, que es lo que
// permitio escribir `FixedEntry` sin recurrir al nombre binario `Map$Entry`.
//
// Queda como REGRESION.
public class finding_101 {

    enum Flag {
        X,
        Y
    }

    Flag[] viaSimple() {
        return null;                       // nombre simple: siempre anduvo
    }

    finding_101.Flag[] viaQualified() {
        return null;                       // nombre calificado: es el que fallaba con #101
    }
}
