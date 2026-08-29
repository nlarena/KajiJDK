// Repro de #226 - el nativo `java/lang/String.valueOf([CII)Ljava/lang/String;`.
//
// Es la costura por la que KajiLibrary produce TODOS sus String: StringBuilder.toString,
// String.substring y la concatenacion `"a" + x` bajan todos ahi. Sin el nativo no se podia
// producir un String desde codigo de biblioteca.
//
// OJO al compilar: hace falta `-cp KajiLibrary`. El `boot/java/lang/String.class` declara
// unicamente `valueOf(Object)`, asi que sin classpath la llamada no resuelve -- y hasta que se
// arreglo #261 eso no daba error, daba un `.class` mudo con la pila corrida.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_226.java
//   bin\run-headless.exe KajiLibrary\repros\finding_226.class run     -> 0
public class finding_226 {

    public static int run() {
        char[] c = new char[5];
        c[0] = 'k';
        c[1] = 'a';
        c[2] = 'j';
        c[3] = 'i';
        c[4] = '!';

        /* El nativo, directo: entero, una tajada del medio, y la tajada vacia. */
        if (!String.valueOf(c, 0, 5).equals("kaji!")) {
            return 1;
        }
        if (!String.valueOf(c, 1, 3).equals("aji")) {
            return 2;
        }
        if (!String.valueOf(c, 0, 0).equals("")) {
            return 3;
        }

        /* Y los tres consumidores que el finding daba por rotos. */
        StringBuilder sb = new StringBuilder();
        sb.append("ka");
        sb.append("ji");
        if (!sb.toString().equals("kaji")) {
            return 4;
        }

        String s = "kajijdk";
        if (!s.substring(0, 4).equals("kaji")) {
            return 5;
        }
        if (!s.substring(4).equals("jdk")) {
            return 6;
        }

        int n = 7;
        if (!("n=" + n).equals("n=7")) {
            return 7;
        }
        return 0;
    }
}
