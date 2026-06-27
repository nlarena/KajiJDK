// Manipulación de pila: pop (descartar retorno), dup (asignación encadenada),
// dup2 (compound assign sobre array), y un long en la pila.
class Stk {
    static int helper() { return 99; }

    static int withPop() {
        helper();              // invokestatic + pop (descarta el int)
        return 42;
    }
    static int chainAssign() {
        int x, y;
        x = y = 7;             // iconst; dup; istore; istore
        return x + y;          // 14
    }
    static int compound() {
        int[] a = new int[1];
        a[0] = 10;
        a[0] += 5;             // dup2 (arrayref, index) + iaload/iadd/iastore
        return a[0];           // 15
    }
    static long longCompound() {
        long[] a = new long[1];
        a[0] = 100L;
        a[0] += 1L;            // dup2 sobre (arrayref,index); laload/ladd/lastore
        return a[0];           // 101
    }
}
