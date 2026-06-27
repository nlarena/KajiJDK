// Variantes de dup/pop que javac emite cuando una asignación se reusa como valor,
// o cuando se descarta un long/double.
class StkX {
    int  f;
    long lf;

    static long lh() { return 5L; }

    static long popsLong() {       // pop2 — descartar un long retornado
        lh();
        return 1L;
    }
    static int dupX1() {           // dup_x1 — putfield reusado como valor
        StkX o = new StkX();
        int v = (o.f = 7);
        return v + o.f;            // 14
    }
    static int dupX2() {           // dup_x2 — iastore reusado como valor
        int[] a = new int[1];
        int v = (a[0] = 9);
        return v + a[0];           // 18
    }
    static long dup2X1() {         // dup2_x1 — putfield long reusado
        StkX o = new StkX();
        long v = (o.lf = 8L);
        return v + o.lf;           // 16
    }
    static long dup2X2() {        // dup2_x2 — lastore reusado como valor
        long[] a = new long[1];
        long v = (a[0] = 8L);
        return v + a[0];           // 16
    }
}
