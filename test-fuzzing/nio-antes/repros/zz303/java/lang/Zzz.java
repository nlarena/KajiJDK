// Repro del finding #303: un tipo de `java.lang` **del mismo round** no lo veia el
// `import java.lang.*` implicito de otra unidad.
//
//   javac --emit java/lang/Zzz.java pp/Uso.java     ->  antes: "no se encuentra el simbolo: Zzz"
//   javac --emit java/lang/Zzz.java                     ambos por separado: andaba
//   javac --emit pp/Uso.java
//
// Esa asimetria es la firma del bug: el import implicito solo miraba el class finder, asi que veia
// el .class en disco y no el hermano que se estaba compilando al lado.
//
// Salio compilando `java.lang.StrictMath` junto con `java.util.Random`, que lo nombra.
package java.lang;

public class Zzz {
    public static int uno() {
        return 1;
    }
}
