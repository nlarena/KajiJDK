// La otra mitad de #297: lo que ahora tiene que ser RECHAZADO.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_297b.java
//
// **Este archivo NO compila a proposito**, como finding_208 y finding_293b.
//
// Los tres literales de abajo llevan un escape que en Java no existe. Antes los tres compilaban
// tragandose la barra, asi que `"\d"` valia `"d"` y `"\x27"` valia `"x27"`. Ahora los tres dan
//
//   error: literal string invalido
//
// El javac del JDK 25 los rechaza tambien, con "illegal escape character".
//
// El compilador corta en el primero, asi que hay que comentar los de arriba para ver los de abajo.
public class finding_297b {

    // (1) el clasico: un patron de regex con una barra de menos
    public static int regexMalEscrito() {
        return "\d+".length();
    }

    // (2) el que estaba en la biblioteca: sintaxis hexadecimal de C, que Java no tiene
    public static int hexDeC() {
        return "\x27".length();
    }

    // (3) y una letra cualquiera
    public static int letraCualquiera() {
        return "\q".length();
    }
}
