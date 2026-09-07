// **Control de efecto cero** de `BkMono`: el mismo programa más la rama que nunca se toma.
// Ver `BkArithC` para por qué el veneno es un `invokedynamic` y qué lo custodia.
public class BkMonoC {
    static int NEVER = 0;

    static int step(BkOp[] ops, int n) {
        int acc = 1;
        if (NEVER != 0) {
            acc = acc + ("" + n).length();
        }
        for (int i = 0; i < n; i++) {
            acc = ops[i & 3].f(acc);
            acc = acc ^ (acc >> 7);
        }
        return acc & 0xFFFFF;
    }

    static int run() {
        BkOp a = new BkOpPA();
        BkOp[] ops = { a, a, a, a };
        int acc = 0;
        if (NEVER != 0) {
            acc = acc + ("" + acc).length();
        }
        for (int k = 0; k < 1000; k++) {
            acc = (acc + step(ops, 300)) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
