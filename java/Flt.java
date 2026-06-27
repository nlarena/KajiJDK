// float (categoría-1, f32): ldc + fstore + fload + fadd + freturn (sin huecos).
class Flt {
    static float run() {
        float a = 2.5f;
        float b = 1.25f;
        return a + b;   // 3.75
    }
}
