// Repro de #297 - un escape ilegal en un literal se aceptaba en silencio, tragandose la barra.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_297.java
//   bin\run-headless.exe KajiLibrary\repros\finding_297.class regex
//
// Este archivo son los CONTROLES: los escapes que SI existen, que tienen que seguir andando. El
// que tiene que ser rechazado esta en finding_297b.java.
//
// ANTES, `"\d"` compilaba y valia `"d"`: la barra se descartaba y quedaba la letra. El javac real
// lo rechaza con "illegal escape character", y por buenas razones -- el unico motivo para escribir
// `\d` en un literal es querer el patron `\d` de una expresion regular, o sea haber escrito una
// barra de menos. Con la barra tragada, `Pattern.compile("\d")` compila un patron que matchea la
// LETRA `d` en vez de un digito, y nadie se entera.
//
// Lo que destapo el arreglo, y es el mejor argumento a favor: DOS archivos de la propia biblioteca
// usaban `\x27` --sintaxis de C y de Python, que en Java no existe-- para escribir una comilla
// simple. `MissingFormatArgumentException.getMessage()` devolvia
//
//   Format specifier x27%sx27
//
// en vez de
//
//   Format specifier '%s'
//
// De paso se completo el escape **octal** (§3.10.7), que estaba a medias: solo `\0` se decodificaba,
// y `\101` daba la letra `1` seguida de `01` en vez de la `A`.
//
// `regex` -> 3, `octal` -> 65, `comunes` -> 1, `comillas` -> 39.
import java.util.regex.Pattern;

public class finding_297 {

    // El caso que motivo todo: un patron de regex escrito con la barra que corresponde.
    public static int regex() {
        int r = 0;
        r = r + (Pattern.compile("\\d").matcher("5").matches() ? 1 : 0);
        r = r + (Pattern.compile("\\d").matcher("d").matches() ? 0 : 2);
        return r;
    }

    // El escape octal, de uno a tres digitos. `\101` es la A.
    public static int octal() {
        char a = '\101';
        char nul = '\0';
        char maximo = '\377';
        int r = 0;
        r = r + (a == 'A' ? 65 : 0);
        r = r + (nul == 0 ? 0 : 1000);
        r = r + (maximo == 255 ? 0 : 2000);
        // `\7` seguido de un digito que NO es octal corta en el primero
        String corto = "\78";
        r = r + (corto.length() == 2 && corto.charAt(0) == 7 && corto.charAt(1) == '8' ? 0 : 4000);
        return r;
    }

    // Los de siempre, que no se tocaron.
    public static int comunes() {
        String s = "\n\t\r\b\f\\\"";
        int r = 0;
        r = r + (s.length() == 7 ? 1 : 0);
        r = r + (s.charAt(0) == 10 && s.charAt(1) == 9 && s.charAt(2) == 13 ? 0 : 100);
        r = r + (s.charAt(3) == 8 && s.charAt(4) == 12 ? 0 : 200);
        r = r + (s.charAt(5) == 92 && s.charAt(6) == 34 ? 0 : 400);
        return r;
    }

    // La comilla simple: en un literal de cadena va sola, y en uno de caracter va escapada.
    public static int comillas() {
        String enCadena = "'";
        char suelta = '\'';
        int r = 0;
        r = r + (enCadena.charAt(0) == 39 ? 39 : 0);
        r = r + (suelta == 39 ? 0 : 1000);
        return r;
    }
}
