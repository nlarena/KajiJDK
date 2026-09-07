// **Control de efecto cero** de `BkRefW`: el mismo programa más la rama que nunca se toma.
// Ver `BkArithC` para por qué el veneno es un `invokedynamic` y qué lo custodia.
public class BkRefWC {
    static int NEVER = 0;

    Object a;
    Object b;

    static int step(BkRefWC o, Object x, Object y, int n) {
        int acc = 1;
        if (NEVER != 0) {
            acc = acc + ("" + n).length();
        }
        for (int i = 0; i < n; i++) {
            o.a = x;
            o.b = y;
            acc = acc + i;
            acc = acc ^ (acc >> 7);
        }
        return acc & 0xFFFFF;
    }

    static int run() {
        BkRefWC o = new BkRefWC();
        Object x = new Object();
        Object y = new Object();
        int acc = 0;
        if (NEVER != 0) {
            acc = acc + ("" + acc).length();
        }
        for (int k = 0; k < 1000; k++) {
            acc = (acc + step(o, x, y, 300)) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
