// Repro de #250 - el desugar de un enum llamaba a `Enum.valueOf(Class, String)`, que no existe aca.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_250.java
//   bin\jvm.exe --javap -p "KajiLibrary\repros\finding_250$Color.class"
//
// ANTES: cualquier enum servia de repro, sin usarlo ni llamar a `valueOf` en la fuente — el
// metodo SINTETIZADO era el que traia la referencia:
//
//   error: no se encuentra el metodo: valueOf   (simbolo: valueOf(Class, String))
//
// KajiLibrary no declara `Enum.valueOf(Class, String)` a proposito, y su `Enum.java` lo explica:
// el del JDK reflexiona sobre el `$VALUES` de la clase, y el nuestro sintetiza dentro de cada
// enum un `valueOf(String)` autocontenido que no necesita reflexion.
//
// AHORA: **compila**, y el enum emite el metodo autocontenido:
//
//   public static finding_250$Color valueOf(java.lang.String);
//
// `#250` figura cerrado en COMPILER_FINDINGS.md, con la aclaracion de que no era del compilador
// sino de la biblioteca. Queda como REGRESION: si el desugar vuelve a emitir la forma de dos
// argumentos, este archivo deja de compilar.
public class finding_250 {

    enum Color { ROJO, VERDE }

    public static int cuantos() {
        return Color.values().length;
    }
}
