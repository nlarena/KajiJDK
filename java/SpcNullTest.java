// `invokespecial` con el receptor en **null** (findings #564 y #565).
//
// JVMS 6.5 dice que `invokespecial` tira `NullPointerException` si el `objectref` es null, y nadie lo
// preguntaba: el frame se empujaba igual. Un `private` que **no toca `this`** es la unica forma donde
// eso se nota --- cualquier otro cuerpo se caeria en su propio `getfield`, y el interprete tiraria la
// excepcion por el --- asi que devolvia su numero en silencio.
//
// Las tres formas del opcode estan aca: el `private` de la **propia clase** (que es el que se puede
// escribir con receptor null, y el que nuestro javac sigue emitiendo con `invokespecial`), un
// `super.m()` y un constructor, que estan como control --- en esos dos el receptor no puede ser null,
// y tienen que seguir corriendo igual que siempre ---.
//
// Devuelve -1 si todo coincide con el JDK 25, o el numero de la primera comprobacion que no.
public class SpcNullTest {

    private int hueco(int x) {
        return (x * 13) + 5;
    }

    static int viaNull(SpcNullTest o) {
        return o.hueco(3);
    }

    static class Base {
        int val() { return 7; }
    }

    static class Hija extends Base {
        int val() { return 100; }
        int deArriba() { return super.val(); }
    }

    public static int run() {
        // 1. El caso: el receptor es null y el cuerpo nunca lo toca.
        try {
            viaNull(null);
            return 1;
        } catch (NullPointerException e) {
            // es lo que tiene que pasar
        }
        // 2. Con un receptor de verdad, el mismo sitio sigue contestando.
        if (viaNull(new SpcNullTest()) != 44) {
            return 2;
        }
        // 3. Control: `super.m()` --- tambien `invokespecial`, receptor `this`, nunca null.
        if (new Hija().deArriba() != 7) {
            return 3;
        }
        // 4. Control: un constructor --- `invokespecial <init>`.
        if (new Hija().val() != 100) {
            return 4;
        }
        return -1;
    }

    public static void main(String[] a) {
        System.out.println(run());
    }
}
