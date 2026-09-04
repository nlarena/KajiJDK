public class JcCat2 {
    // El caso que rompia: un long ENTRE dos argumentos. El que viene despues cae un slot mas
    // alla, asi que copiar "operando k -> local k" lo lee del lugar equivocado.
    static int medio(int a, long b, int c) { return (a * 1000) + ((int) b) + (c * 7); }
    // Un long al principio: el siguiente argumento arranca en el slot 2, no en el 1.
    static int primero(long a, int b) { return ((int) a) + (b * 13); }
    // Dos categoria-2 seguidas. El double no se estrecha (d2i esta fuera del subconjunto):
    // se lo compara, que es lo que un programa real hace con el.
    static int dos(long a, double b, int c) { return ((int) a) + ((b > 0.0) ? 1 : 0) + c; }
    // Mezclada con referencias, para que el receptor y los objetos corran los slots tambien.
    // Referencias entre argumentos categoria-2. Sin llamarles metodos: `String.length()` baja a
    // `rawLength`, que es nativo, y el llamador entero se rechazaria por eso y no por esto.
    static int conRef(String s, long a, String t, double b) {
        return ((s == t) ? 2 : 1) + ((int) a) + ((b < 100.0) ? 5 : 0);
    }
    // Que el long se USE como long: si la mitad alta se pierde o se lee del slot de al lado,
    // esto lo ve — un cast a int solo miraria los 32 bits bajos.
    static long entero(long a, int b, long c) { return (a * 31L) + (long) b + c; }
    // Instancia: el receptor ocupa el slot 0 y corre todo lo demas.
    int campo;
    JcCat2(int v) { this.campo = v; }
    int metodo(long a, int b) { return campo + ((int) a) + (b * 3); }

    static int run() {
        int acc = 0;
        for (int w = 0; w < 200; w++) {
            acc = (acc * 31) + medio(w, 0x0000000700000005L, w + 1);
            acc = (acc * 31) + primero(0x00000009FFFFFFFFL, w);
            acc = (acc * 31) + dos(w + 2L, w + 0.5, w);
            acc = (acc * 31) + conRef("abc", w + 3L, "de", w + 1.5);
            acc = (acc * 31) + (int) (entero(0x0000000100000002L, w, -3L) >>> 24);
            acc = (acc * 31) + new JcCat2(w).metodo(w + 4L, w);
        }
        return acc;
    }
    public static void main(String[] a) { System.out.println(run()); }
}
