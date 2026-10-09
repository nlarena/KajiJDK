// Repro del finding #309: un `++`/`--` sobre algo que NO es un local, en posicion de VALOR, no
// emitia NADA. Sin codigo y sin error.
//
//   long getAndIncrement() { return value++; }
//     0: lstore_1     <- guarda desde una pila VACIA
//
// El `value++` desaparecia. `incdec` empezaba con
//
//     let Some(Binding::Local { slot }) = target.binding else { return };
//
// y ese `return` mudo era todo. En posicion de **descarte** no se veia --ahi el desugar reescribe
// `x++` a `x += 1` y lo resuelve el camino de asignacion compuesta--, asi que el bug solo aparecia
// cuando alguien usaba el valor.
//
// Lo destapo `AtomicLong.getAndIncrement()`, que es exactamente esa linea y estaba inutilizable:
// cualquier llamada terminaba en "operand stack underflow". `return contador++;` es una de las
// formas mas comunes que tiene Java.
//
// Cubre las cuatro clases de destino por las dos posiciones (prefijo/postfijo). Con todo bien da
// 1234567890123456, que es lo que da `java` real.
public class finding_309 {

    long campoLong = 10L;
    int campoInt = 10;
    double campoDouble = 10.0d;
    static long estatico = 10L;
    static int estaticoInt = 10;

    // ---- campo de instancia, valor ----
    int a() { long v = campoLong++; return (int) (v * 10 + campoLong); }        // 10*10+11 = 111
    int b() { long v = ++campoLong; return (int) (v * 10 + campoLong); }        // 11*10+11 = 121
    int c() { int v = campoInt++; return v * 10 + campoInt; }                   // 111
    int d() { int v = --campoInt; return v * 10 + campoInt; }                   //  9*10+9 =  99
    int e() { double v = campoDouble++; return (int) (v * 10 + campoDouble); }  // 111

    // ---- estatico, valor ----
    static int f() { long v = estatico++; return (int) (v * 10 + estatico); }   // 111
    static int g() { int v = ++estaticoInt; return v * 10 + estaticoInt; }      // 121

    // ---- elemento de arreglo, valor ----
    static int h() {
        long[] arr = new long[] {10L};
        long v = arr[0]++;
        return (int) (v * 10 + arr[0]);                                         // 111
    }

    static int i() {
        int[] arr = new int[] {10};
        int v = ++arr[0];
        return v * 10 + arr[0];                                                 // 121
    }

    // ---- el elemento se evalua UNA sola vez ----
    //
    // Es la parte que el juego de pila tiene que garantizar: si el destino se re-evaluara para el
    // store, el efecto del indice correria dos veces.
    static int llamadas = 0;

    static int indice() {
        llamadas = llamadas + 1;
        return 0;
    }

    static int j() {
        int[] arr = new int[] {10};
        llamadas = 0;
        int v = arr[indice()]++;
        return v * 100 + arr[0] * 10 + llamadas;                                // 10*100+11*10+1 = 1111
    }

    // ---- y el caso que lo destapo ----
    static int k() {
        java.util.concurrent.atomic.AtomicLong c = new java.util.concurrent.atomic.AtomicLong(1L);
        long uno = c.getAndIncrement();
        long dos = c.getAndIncrement();
        return (int) (uno * 100 + dos * 10 + c.get());                          // 100+20+3 = 123
    }

    public static int run() {
        finding_309 p = new finding_309();
        // Se mezclan en un solo entero para que cualquiera que falle mueva el resultado.
        long r = 0;
        r = r * 1000 + p.a();
        r = r * 1000 + p.b();
        r = r * 1000 + p.c();
        r = r * 1000 + p.d();
        r = r * 1000 + p.e();
        r = r * 1000 + f();
        r = r * 1000 + g();
        r = r * 1000 + h();
        r = r * 1000 + i();
        r = r * 10000 + j();
        r = r * 1000 + k();
        return (int) r;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
