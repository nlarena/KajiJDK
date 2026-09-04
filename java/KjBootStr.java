// Fixture de arranque: lo minimo que distingue una biblioteca de otra a traves de `String`.
//
// Existe para el hito K7 «que los tests del JIT booteen como la VM». El arnes de `jit_tests`
// arrancaba desde `boot/` y `run-headless` desde `KajiLibrary`, o sea que los dos corrian un
// `java.lang.String` distinto y nadie lo comprobaba. Esta clase se corre por las dos rutas y tiene
// que dar lo mismo.
//
// Toca tres cosas y ninguna imprime — imprimir crashea la VM (FZ-010) y concatenar con un `int`
// tambien (FZ-011), asi que el fixture se queda del lado que funciona:
//
//   1. `length()` sobre un literal — una llamada a un metodo de instancia de `String`;
//   2. la **identidad** de dos literales iguales, que es el pool de internado (FZ-008 vivia aca);
//   3. `charAt`, que lee el arreglo de respaldo y depende de como la biblioteca lo guarda.
public class KjBootStr {
    static int run() {
        String a = "kaji";
        String b = "kaji";
        String c = "kaj";
        int n = 0;
        n = (n * 31) + a.length();
        n = (n * 31) + ((a == b) ? 1 : 0);
        n = (n * 31) + ((a == c) ? 1 : 0);
        n = (n * 31) + a.charAt(0);
        n = (n * 31) + a.charAt(3);
        n = (n * 31) + (a.equals(b) ? 1 : 0);
        n = (n * 31) + (a.equals(c) ? 1 : 0);
        return n;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
