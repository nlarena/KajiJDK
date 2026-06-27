// Estáticos de long/double/float (en el mirror, con slots width-aware).
class Statics {
    static long  bigL = 5_000_000_000L;   // > int
    static double pi  = 3.125;
    static float  half = 0.5f;
    static int    tag  = 7;                // un int DESPUÉS de los cat-2

    static double run() {
        bigL = bigL + 1L;                  // putstatic/getstatic long
        return bigL + pi + half + tag;     // 5000000001 + 3.125 + 0.5 + 7 = 5000000011.625
    }
}
