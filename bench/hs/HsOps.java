// Los receptores que comparten `HsMono` y `HsMega`, portados de `bench/BkOps.java`.
//
// Cuatro clases con el cuerpo IDENTICO letra por letra, a proposito: `HsMono` llama siempre a
// `HsOpA` y `HsMega` rota entre las cuatro, asi que las dos ejecutan la misma aritmetica y devuelven
// el mismo numero. Lo unico que las separa es **cuantas clases distintas ve el sitio de llamada**, y
// por eso cualquier diferencia de tiempo entre esas dos filas es despacho y no trabajo. Son cuatro
// clases y no una con cuatro instancias porque un cache de sitio de llamada discrimina por CLASE.
//
// El `if (NEVER)` se conserva del original: sin el, el cuerpo de `f` seria distinto del que mide la
// linea base de `bench/` y las filas dejarian de ser comparables con ella.
interface HsOp {
    int f(int x);
}

final class HsOpA implements HsOp {
    static int NEVER = 0;
    public int f(int x) { if (HsOpA.NEVER != 0) { return x + 2; } return x + 1; }
}

final class HsOpB implements HsOp {
    static int NEVER = 0;
    public int f(int x) { if (HsOpB.NEVER != 0) { return x + 2; } return x + 1; }
}

final class HsOpC implements HsOp {
    static int NEVER = 0;
    public int f(int x) { if (HsOpC.NEVER != 0) { return x + 2; } return x + 1; }
}

final class HsOpD implements HsOp {
    static int NEVER = 0;
    public int f(int x) { if (HsOpD.NEVER != 0) { return x + 2; } return x + 1; }
}
