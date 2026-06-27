// long con PARÁMETROS categoría-2: `add(long a, long b)` — a en local 0-1, b en 2-3.
// El invokestatic ubica los args con huecos; el verificador arma los locales iniciales
// con la mitad alta. lload_0 / lload_2 dentro de add deben leer a y b.
class LAdd {
    static long add(long a, long b) {
        return a + b;
    }
    static long run() {
        return add(10L, 20L);   // 30
    }
}
