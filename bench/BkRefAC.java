// **Control de efecto cero** de `BkRefA`: el mismo programa más la rama que nunca se toma.
// Ver `BkArithC` para por qué el veneno es un `invokedynamic` y qué lo custodia.
public class BkRefAC {
    static int NEVER = 0;

    static int step(Object[] arr, Object x, Object y, int n) {
        int acc = 1;
        if (NEVER != 0) {
            acc = acc + ("" + n).length();
        }
        for (int i = 0; i < n; i++) {
            arr[i & 255] = x;
            arr[(i + 1) & 255] = y;
            acc = acc + i;
            acc = acc ^ (acc >> 7);
        }
        return acc & 0xFFFFF;
    }

    static int run() {
        Object[] arr = new Object[256];
        Object x = new Object();
        Object y = new Object();
        int acc = 0;
        if (NEVER != 0) {
            acc = acc + ("" + acc).length();
        }
        for (int k = 0; k < 1000; k++) {
            acc = (acc + step(arr, x, y, 300)) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
