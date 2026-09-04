import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.SetOfIntegerSyntax;

// Comportamiento de javax.print.attribute.SetOfIntegerSyntax, para correr con las dos VMs y
// comparar.
//
// Es la mejor prueba diferencial del paquete: la clase es funcion pura --entra una cadena o un
// int[][], sale una forma canonica-- y no necesita impresora, locale, zona horaria ni reloj. Este
// archivo compila igual contra el javax.print.attribute del JDK real (modulo java.desktop) que
// contra el nuestro, asi que `run()` devolviendo -1 en los dos lados quiere decir que las dos
// implementaciones coinciden en cada aserto, no que las dos pasan pruebas distintas.
//
// Lo que NO se compara aca esta al final, en el comentario de los casos de desborde: hay un caso
// donde las dos implementaciones difieren a proposito y esta documentado en la fuente.
public class PrnSetIntSyntaxTest {

    // SetOfIntegerSyntax es abstracta y sus constructores son protected: hace falta una subclase
    // para tocarla desde afuera. Son dos para poder probar que `equals` cruza subclases.
    static class Conj extends SetOfIntegerSyntax {
        Conj(String s) {
            super(s);
        }

        Conj(int[][] m) {
            super(m);
        }

        Conj(int m) {
            super(m);
        }

        Conj(int lb, int ub) {
            super(lb, ub);
        }
    }

    static class Otro extends SetOfIntegerSyntax {
        Otro(String s) {
            super(s);
        }
    }

    static class Ent extends IntegerSyntax {
        Ent(int v) {
            super(v);
        }
    }

    public static int run() {
        int n;

        // ---- canonicalizacion desde la forma de texto ----

        n = 1;
        if (!new Conj("1-3,5,7-9").toString().equals("1-3,5,7-9")) {
            return n;
        }
        n = 2;
        // Desordenado: se ordena.
        if (!new Conj("7-9,1-3,5").toString().equals("1-3,5,7-9")) {
            return n;
        }
        n = 3;
        // Adyacentes (ub+1 == lb): se fusionan.
        if (!new Conj("1-3,4-6").toString().equals("1-6")) {
            return n;
        }
        n = 4;
        // Solapados: se fusionan.
        if (!new Conj("1-5,3-9").toString().equals("1-9")) {
            return n;
        }
        n = 5;
        // Contenido: el chico desaparece adentro del grande.
        if (!new Conj("0-10,3-5").toString().equals("0-10")) {
            return n;
        }
        n = 6;
        // Enteros sueltos adyacentes: 1,2,3 es un rango.
        if (!new Conj("1,2,3").toString().equals("1-3")) {
            return n;
        }
        n = 7;
        // Repetido: idempotente.
        if (!new Conj("4,4,4").toString().equals("4")) {
            return n;
        }
        n = 8;
        // Un rango de un solo elemento se imprime sin guion.
        if (!new Conj("5-5").toString().equals("5")) {
            return n;
        }
        n = 9;
        // Rango invertido en la forma de texto: NO es un error, es un rango vacio que se descarta.
        if (!new Conj("9-3").toString().equals("")) {
            return n;
        }
        n = 10;
        if (!new Conj("1-3,9-5,7").toString().equals("1-3,7")) {
            return n;
        }

        // ---- la sintaxis de la cadena ----

        n = 11;
        // El separador tambien puede ser dos puntos.
        if (!new Conj("1:3").toString().equals("1-3")) {
            return n;
        }
        n = 12;
        // Espacios entre tokens, en todas las posiciones donde se permiten.
        if (!new Conj("  1 - 3 ,  5  ").toString().equals("1-3,5")) {
            return n;
        }
        n = 13;
        // La cadena vacia y la de solo espacios son el conjunto vacio, no un error.
        if (!new Conj("").toString().equals("") || !new Conj("   ").toString().equals("")) {
            return n;
        }
        n = 14;
        // null tambien es el conjunto vacio.
        if (!new Conj((String) null).toString().equals("")) {
            return n;
        }
        n = 15;
        // Coma colgando: error. El estado "recien vimos una coma" no acepta el fin de la cadena.
        if (!tiraIAE("1,")) {
            return n;
        }
        n = 16;
        // Guion colgando: error, por el mismo motivo.
        if (!tiraIAE("1-")) {
            return n;
        }
        n = 17;
        // Coma al principio: error.
        if (!tiraIAE(",1")) {
            return n;
        }
        n = 18;
        // Espacio adentro de un entero: corta el entero y despues no sabe que hacer con el 2.
        if (!tiraIAE("1 2")) {
            return n;
        }
        n = 19;
        // Un separador que no es ni "-" ni ":".
        if (!tiraIAE("1..3")) {
            return n;
        }
        n = 20;
        // Basura pura.
        if (!tiraIAE("x") || !tiraIAE("1-3;5")) {
            return n;
        }
        n = 21;
        // Dos guiones seguidos: el segundo cae en "antes del limite superior", que quiere digito.
        if (!tiraIAE("1--3")) {
            return n;
        }

        // ---- la forma int[][] ----

        n = 22;
        if (!new Conj(new int[][] {{1, 3}, {5, 5}}).toString().equals("1-3,5")) {
            return n;
        }
        n = 23;
        // Una fila de largo 1 es el entero suelto.
        if (!new Conj(new int[][] {{7}}).toString().equals("7")) {
            return n;
        }
        n = 24;
        if (!new Conj(new int[][] {{5, 7}, {1, 3}}).toString().equals("1-3,5-7")) {
            return n;
        }
        n = 25;
        if (!new Conj(new int[][] {{1, 3}, {4, 6}}).toString().equals("1-6")) {
            return n;
        }
        n = 26;
        // Un arreglo null es el conjunto vacio; uno de largo cero tambien.
        if (!new Conj((int[][]) null).toString().equals("")
                || !new Conj(new int[0][]).toString().equals("")) {
            return n;
        }
        n = 27;
        // Fila de largo 3: error.
        try {
            new Conj(new int[][] {{1, 2, 3}});
            return n;
        } catch (IllegalArgumentException e) {
            // esperado
        }
        n = 28;
        // Negativo en un rango NO vacio: error.
        try {
            new Conj(new int[][] {{-1, 5}});
            return n;
        } catch (IllegalArgumentException e) {
            // esperado
        }
        n = 29;
        // Negativo en un rango VACIO: no es error. El chequeo de signo esta guardado detras de
        // `lb <= ub`, asi que un rango invertido ni se mira.
        if (!new Conj(new int[][] {{5, 3}}).toString().equals("")) {
            return n;
        }
        n = 30;
        if (!new Conj(new int[][] {{-1, -5}}).toString().equals("")) {
            return n;
        }
        n = 31;
        // Una fila null: NullPointerException, no IllegalArgumentException.
        try {
            new Conj(new int[][] {null});
            return n;
        } catch (NullPointerException e) {
            // esperado
        }

        // ---- los constructores de un entero y de un rango ----

        n = 32;
        if (!new Conj(7).toString().equals("7")) {
            return n;
        }
        n = 33;
        try {
            new Conj(-1);
            return n;
        } catch (IllegalArgumentException e) {
            // esperado
        }
        n = 34;
        if (!new Conj(2, 5).toString().equals("2-5")) {
            return n;
        }
        n = 35;
        // Rango invertido: conjunto vacio, y el signo ni se mira.
        if (!new Conj(5, 2).toString().equals("") || !new Conj(-1, -5).toString().equals("")) {
            return n;
        }
        n = 36;
        try {
            new Conj(-1, 5);
            return n;
        } catch (IllegalArgumentException e) {
            // esperado
        }

        // ---- equals / hashCode: el punto de la canonicalizacion ----

        n = 37;
        // Escritos distinto, mismo conjunto.
        if (!new Conj("1-3,4-6").equals(new Conj("1-6"))) {
            return n;
        }
        n = 38;
        if (new Conj("1-3,4-6").hashCode() != new Conj("1-6").hashCode()) {
            return n;
        }
        n = 39;
        if (!new Conj("3,1,2").equals(new Conj(new int[][] {{1, 3}}))) {
            return n;
        }
        n = 40;
        // equals cruza subclases: pregunta por `instanceof SetOfIntegerSyntax`, no por getClass().
        if (!new Conj("1-6").equals(new Otro("1-6"))) {
            return n;
        }
        n = 41;
        if (new Conj("1-6").equals(new Conj("1-7")) || new Conj("1-6").equals("1-6")
                || new Conj("1-6").equals(null)) {
            return n;
        }
        n = 42;
        // El hash es la suma de los extremos; el del vacio es 0.
        if (new Conj("1-3,5").hashCode() != 1 + 3 + 5 + 5) {
            return n;
        }
        n = 43;
        if (new Conj("").hashCode() != 0) {
            return n;
        }
        n = 44;
        if (!new Conj("").equals(new Conj(9, 3))) {
            return n;
        }

        // ---- getMembers ----

        n = 45;
        int[][] m = new Conj("1-3,5").getMembers();
        if (m.length != 2 || m[0][0] != 1 || m[0][1] != 3 || m[1][0] != 5 || m[1][1] != 5) {
            return n;
        }
        n = 46;
        // Es una copia: tocarla no toca el conjunto.
        Conj c46 = new Conj("1-3");
        int[][] m46 = c46.getMembers();
        m46[0][0] = 99;
        m46[0][1] = 99;
        if (!c46.toString().equals("1-3") || c46.getMembers()[0][0] != 1) {
            return n;
        }
        n = 47;
        if (new Conj("").getMembers().length != 0) {
            return n;
        }

        // ---- contains ----

        n = 48;
        Conj c = new Conj("1-3,7");
        if (c.contains(0) || !c.contains(1) || !c.contains(2) || !c.contains(3) || c.contains(4)
                || c.contains(6) || !c.contains(7) || c.contains(8)) {
            return n;
        }
        n = 49;
        if (c.contains(-1)) {
            return n;
        }
        n = 50;
        if (!c.contains(new Ent(2)) || c.contains(new Ent(5))) {
            return n;
        }
        n = 51;
        if (new Conj("").contains(0)) {
            return n;
        }

        // ---- next ----

        n = 52;
        if (c.next(-1) != 1 || c.next(0) != 1 || c.next(1) != 2 || c.next(2) != 3) {
            return n;
        }
        n = 53;
        // Cae en el hueco: salta al arranque del rango siguiente.
        if (c.next(3) != 7 || c.next(5) != 7) {
            return n;
        }
        n = 54;
        // Ya no hay mas: -1, que nunca puede ser un miembro porque no se admiten negativos.
        if (c.next(7) != -1 || c.next(100) != -1) {
            return n;
        }
        n = 55;
        if (new Conj("").next(-1) != -1) {
            return n;
        }
        n = 56;
        // El recorrido completo que documenta el JDK.
        StringBuilder sb = new StringBuilder();
        int i = -1;
        while ((i = c.next(i)) != -1) {
            sb.append(i);
            sb.append(' ');
        }
        if (!sb.toString().equals("1 2 3 7 ")) {
            return n;
        }

        // ---- toString del conjunto vacio y de uno grande ----

        n = 57;
        if (!new Conj("").toString().equals("")) {
            return n;
        }
        n = 58;
        if (!new Conj("2147483647").toString().equals("2147483647")) {
            return n;
        }
        n = 59;
        // Un rango que llega al maximo no se fusiona con lo que sigue por dar la vuelta.
        if (!new Conj(new int[][] {{2147483647, 2147483647}, {1, 2}}).toString()
                .equals("1-2,2147483647")) {
            return n;
        }
        n = 60;
        if (!new Conj(new int[][] {{0, 2147483647}, {2147483647, 2147483647}}).toString()
                .equals("0-2147483647")) {
            return n;
        }

        // ---- digitos y espacios NO ASCII ----
        //
        // El reconocedor del JDK usa Character.digit(c, 10) y Character.isWhitespace(c), no un
        // rango ASCII escrito a mano, y la diferencia se ve. Las cadenas se arman con codigos
        // numericos y no con literales para que ni la codificacion del archivo ni el
        // preprocesador de escapes Unicode entren en la prueba.
        n = 61;
        // U+0661 U+0662: los digitos arabigo-indios 1 y 2. Son digitos decimales para
        // Character.digit, asi que el reconocedor los acumula igual que '1' y '2'.
        if (!new Conj(cadena(0x0661, 0x0662)).toString().equals("12")) {
            return n;
        }
        n = 62;
        // U+2028, el separador de linea de Unicode: es espacio en blanco para
        // Character.isWhitespace aunque no sea ASCII.
        if (!new Conj(cadena(0x2028, 0x31)).toString().equals("1")) {
            return n;
        }
        n = 63;
        // U+00A0, el espacio duro: NO es espacio en blanco para Character.isWhitespace --su razon
        // de ser es justamente no separar--, asi que aca es basura y el reconocedor tira.
        if (!tiraIAE(cadena(0x31, 0x00A0, 0x32))) {
            return n;
        }

        return -1;
    }

    // Una cadena a partir de codigos de caracter, para no depender de la codificacion del
    // archivo ni del preprocesador de escapes Unicode.
    static String cadena(int a, int b) {
        return new String(new char[] {(char) a, (char) b});
    }

    static String cadena(int a, int b, int c) {
        return new String(new char[] {(char) a, (char) b, (char) c});
    }

    // Si construir el conjunto con esa cadena tira IllegalArgumentException.
    static boolean tiraIAE(String s) {
        try {
            new Conj(s);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }
}
