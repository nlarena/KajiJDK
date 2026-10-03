// Un helper inlineado con un slot que javac reusa entre dos bloques hermanos, muerto en un guard que
// **dispara**.
//
// **Por qué se rechazaba.** `t` (un `int`) y `o` (un `Object`) viven en bloques hermanos, así que
// javac les da el mismo slot. Después del `if` ese slot llega `Int` por una rama y `Reference` por la
// otra: el mapa de tipos lo marca `Conflict`, que es la prueba de que está muerto. La llamada virtual
// de abajo lleva un guard de clase, y un guard es un sitio de resume. En la raíz eso no molesta —el
// intérprete conserva el valor que el frame ya tenía—, pero `h` está inlineado en `run`, su frame no
// existe todavía, y el chequeo de reconstruibilidad exigía que **todo** local de un frame inlineado
// nombrara un valor. Así que `run` entero se rechazaba como `Unrebuildable`.
//
// **Por qué el guard tiene que disparar.** Que `run` compile prueba que el chequeo ya no rechaza;
// no prueba que la reconstrucción del otro lado ande. En la iteración 15000 `cur` pasa de un `A` a un
// `B`, el guard de clase falla dentro del `h` inlineado, y el intérprete tiene que materializar el
// frame de `h` con el slot en conflicto escrito como `Int(0)`. Si eso estuviera mal, el resultado
// dejaría de coincidir con el `java` real o el colector vería un entero como puntero.
//
// **Los dos objetos existen desde el principio** (`a` y `b`): un `new B()` dentro de `run` sería una
// alocación de una clase sin inicializar cuando `run` se compila, y eso rechaza el método por otra
// razón.
//
// Compilada con el `javac` del JDK 25, no con el nuestro (FZ-003).
public class JcDead {
    static class A {
        int v() {
            return 3;
        }
    }

    static class B extends A {
        int v() {
            return 7;
        }
    }

    static final A a = new A();
    static final A b = new B();
    static A cur = a;
    static Object marker = new Object();

    static int h(int i) {
        if ((i & 1) == 0) {
            int t = i * 3;
            i = t + 1;
        } else {
            Object o = marker;
            i = i + (o == null ? 0 : 5);
        }
        return i + cur.v();
    }

    static int run() {
        int acc = 0;
        for (int i = 0; i < 20000; i++) {
            if (i == 15000) {
                cur = b;
            }
            acc = (acc * 31 + h(i)) & 0xFFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
