// Repro del finding #308: `() -> c[1]++` era un NO-OP.
//
// El peor tipo de bug: sin error, sin aviso, y solo en una de dos formas que el JLS declara
// equivalentes.
//
//   a()  () -> c[1]++        expresion-lambda    ->  daba 0   (mal)
//   b()  () -> { c[1]++; }   bloque              ->  daba 1   (bien)
//
// El cuerpo-expresion de una lambda con SAM `void` es una **posicion de descarte**, igual que una
// sentencia-expresion: ahi `x++` es `x += 1`. Pero se lo bajaba por el camino de expresion en vez
// del de descarte, asi que el `++` llegaba crudo al generador; para un elemento de arreglo eso
// emitia algo que no incrementaba nada. Para una variable local no se notaba (`iinc` anda igual).
//
// Con las cuatro en verde da 1111, que es lo que da `java` real.
public class finding_308 {

    static int a() {
        int[] c = new int[2];
        Runnable r = () -> c[1]++;
        r.run();
        return c[1];
    }

    static int b() {
        int[] c = new int[2];
        Runnable r = () -> { c[1]++; };
        r.run();
        return c[1];
    }

    static int d() {
        int[] c = new int[2];
        Runnable r = () -> { ++c[1]; };
        r.run();
        return c[1];
    }

    static int e() {
        int[] c = new int[2];
        Runnable r = () -> { c[1] += 1; };
        r.run();
        return c[1];
    }

    public static int run() {
        return a() * 1000 + b() * 100 + d() * 10 + e();
    }
}
