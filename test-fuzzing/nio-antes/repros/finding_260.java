// #260 -- el operador condicional `? :` colapsa char/byte/short a int.
//
// En Java el tipo del condicional con las dos ramas del mismo tipo primitivo ES ese tipo
// (JLS 15.25): `b ? x : y` con dos `char` da `char`. El nuestro lo entrega como `int`, asi que
// devolverlo desde un metodo `char` es "tipo de retorno incompatible".
//
// No depende de COMO se escriban las ramas -- variables, literales o casts fallan igual -- ni es
// exclusivo de `char`: `byte` y `short` hacen lo mismo. `int` y `long` andan, y el `if/else`
// equivalente anda. O sea: no es el condicional lo que esta roto, es el tipo que se le calcula
// cuando el resultado es un entero ANGOSTO.
//
// Compilar cada metodo por separado: el javac corta en el primer error.
public class finding_260 {

    // --- fallan ---
    static char charVar(boolean b, char x, char y) { return b ? x : y; }
    static char charCast(boolean b, int x, int y) { return b ? (char) x : (char) y; }
    static char charLit(boolean b) { return b ? 'a' : 'b'; }
    static byte byteCast(boolean b, int x, int y) { return b ? (byte) x : (byte) y; }
    static short shortCast(boolean b, int x, int y) { return b ? (short) x : (short) y; }

    // --- andan ---
    static int intVar(boolean b, int x, int y) { return b ? x : y; }
    static long longVar(boolean b, long x, long y) { return b ? x : y; }
    static char ifElse(boolean b, char x, char y) { if (b) { return x; } return y; }

    // El JDK 25 imprime "aAAa1"; el nuestro no llega a compilar.
    public static void main(String[] a) {
        System.out.println("" + charVar(true, 'a', 'b') + charCast(true, 65, 66)
                + ifElse(true, 'A', 'B') + charLit(true) + byteCast(true, 1, 2));
    }
}
