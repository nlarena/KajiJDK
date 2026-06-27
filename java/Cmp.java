// Comparaciones: lcmp/fcmp/dcmp + branches, con long/double en locales (StackMapTable).
class Cmp {
    static int longLess() {            // lcmp ; iflt — long en local + branch
        long a = 5L, b = 9L;
        if (a < b) return 1;
        return 0;                      // → 1
    }
    static double dmax() {             // dcmp + branch — double en local
        double a = 3.5, b = 7.25;
        if (a > b) return a;
        return b;                      // → 7.25
    }
    static int nanLess() {             // fcmpg: NaN no es < 1.0
        float nan = 0.0f / 0.0f;
        if (nan < 1.0f) return 1;
        return 0;                      // → 0
    }
    static long sumWhile() {           // long en local, loop con lcmp en la condición
        long sum = 0L;
        long i = 0L;
        while (i < 5L) {               // lcmp ; ifge (back-edge → StackMapTable)
            sum = sum + i;
            i = i + 1L;
        }
        return sum;                    // 0+1+2+3+4 = 10
    }
}
