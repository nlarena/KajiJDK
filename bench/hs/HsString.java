// HsString — la sexta dimension, y la unica del arnes que toca la BIBLIOTECA.
//
// Dimension: **lectura de caracteres de un String en un lazo caliente** (`charAt`). Las otras cinco
// miden el motor sobre locales, campos, arreglos y despacho, y ninguna llama a un metodo de
// `java.lang`: son libres de `String` por construccion. Por eso ninguna puede ver un cambio de
// COBERTURA de la biblioteca, que es lo que el censo por tiempo de `tools/perfil.py` mide y lo que
// esta fila existe para cronometrar.
//
// Por que `charAt` y no otra cosa: el censo lo midio como el metodo que mas interprete gastaba de
// todo el corpus de `java/texto/` --el 24,2% de los 61,6 millones de opcodes interpretados-- y
// estuvo rechazado por dos motivos encadenados, los dos arreglados: un `new
// StringIndexOutOfBoundsException` en la rama del indice invalido, que no corre nunca, y un
// `invokestatic` al nativo `rawCharAt`.
//
// # Lo que se dejo AFUERA del lazo a proposito
//
//  - **`length()`**, que es invariante del lazo. HotSpot la izaria y nosotros no, asi que tenerla
//    adentro mezclaria el costo de una llamada con el de una optimizacion que este tier no hace, y
//    la razon contra HotSpot de esta fila dejaria de ser el costo de leer un caracter. Se la llama
//    una vez por `step`, que es donde igual se la mide.
//  - **Todo lo que aloca.** Ni `substring` ni concatenacion ni `StringBuilder`: una alocacion en el
//    lazo convierte la fila en una medicion del colector y de los `Status::ALLOC`. El String se
//    construye en `run`, como `HsRefA` construye su arreglo afuera.
//
// El indice va enmascarado a 8 bits contra un String de 256 caracteres, que es el mismo idioma que
// `HsRefA` usa para sus indices: una mascara y no un resto, porque un `%` es una division y la
// division taparia lo que se quiere medir.
//
// La forma es la de las otras cuatro portadas: `run` toma las repeticiones como PARAMETRO y `bench`
// corre K iteraciones en proceso cronometrando cada una con `System.nanoTime` y emite la serie
// completa.
public class HsString {
    static int NEVER = 0;

    static int step(String s, int n) {
        int acc = 1;
        if (HsString.NEVER != 0) {
            acc = acc + n;
        }
        for (int i = 0; i < n; i++) {
            acc = acc + s.charAt(i & 255);
            acc = acc ^ (acc >> 7);
        }
        return acc & 0xFFFFF;
    }

    public static int run(int reps) {
        StringBuilder b = new StringBuilder(256);
        for (int i = 0; i < 256; i++) {
            b.append((char) ('a' + (i & 15)));
        }
        String s = b.toString();
        int acc = 0;
        for (int k = 0; k < reps; k++) {
            acc = (acc + HsString.step(s, 300) + s.length()) & 0xFFFFF;
        }
        return acc;
    }

    public static int bench(int iters, int reps) {
        long[] nanos = new long[iters];
        int acc = 0;
        for (int i = 0; i < iters; i++) {
            long t0 = System.nanoTime();
            acc = acc + HsString.run(reps);
            long t1 = System.nanoTime();
            nanos[i] = t1 - t0;
        }
        for (int i = 0; i < iters; i++) {
            System.out.println(nanos[i]);
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(HsString.bench(Integer.parseInt(args[0]), Integer.parseInt(args[1])));
    }
}
