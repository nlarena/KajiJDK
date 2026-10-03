// Un método caliente que inlinea muchos cuerpos chicos: la forma de `AtomTest.run`, que el censo
// rechazaba como `TooBig` con 15 bytes de código.
//
// **Por qué se rechazaba.** Cada cuerpo inlineado reserva su propia región de operandos en el marco
// nativo, y el total tiene un tope (`MAX_STACK_SLOTS`). `run` llama a cuatro helpers, y cada uno
// llama diez veces a `leaf`: son 45 cuerpos candidatos de dos o tres posiciones cada uno. Los
// presupuestos de profundidad y de bytes ya caían a una llamada real al agotarse, pero el del marco
// se chequeaba recién al final del plan, sin caída posible, así que tiraba la compilación entera.
//
// **Lo que mide es el OSR.** El único bucle es el de `run`; los helpers y `leaf` compilan solos con o
// sin el arreglo.
//
// Compilada con el `javac` del JDK 25, no con el nuestro (FZ-003).
public class JcFrame {
    static int leaf(int x, int y) {
        return (x * 31 + y) ^ (x >>> 3);
    }

    static int a(int i) {
        int s = i;
        s = leaf(s, 1); s = leaf(s, 2); s = leaf(s, 3); s = leaf(s, 4); s = leaf(s, 5);
        s = leaf(s, 6); s = leaf(s, 7); s = leaf(s, 8); s = leaf(s, 9); s = leaf(s, 10);
        return s;
    }

    static int b(int i) {
        int s = i + 7;
        s = leaf(s, 11); s = leaf(s, 12); s = leaf(s, 13); s = leaf(s, 14); s = leaf(s, 15);
        s = leaf(s, 16); s = leaf(s, 17); s = leaf(s, 18); s = leaf(s, 19); s = leaf(s, 20);
        return s;
    }

    static int c(int i) {
        int s = i ^ 0x5A5A;
        s = leaf(s, 21); s = leaf(s, 22); s = leaf(s, 23); s = leaf(s, 24); s = leaf(s, 25);
        s = leaf(s, 26); s = leaf(s, 27); s = leaf(s, 28); s = leaf(s, 29); s = leaf(s, 30);
        return s;
    }

    static int d(int i) {
        int s = i * 3;
        s = leaf(s, 31); s = leaf(s, 32); s = leaf(s, 33); s = leaf(s, 34); s = leaf(s, 35);
        s = leaf(s, 36); s = leaf(s, 37); s = leaf(s, 38); s = leaf(s, 39); s = leaf(s, 40);
        return s;
    }

    static int run() {
        int acc = 0;
        for (int i = 0; i < 20000; i++) {
            acc = (acc * 17 + a(i) + b(i) + c(i) + d(i)) & 0xFFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
