// The measurement `BmField` cannot be — dimension: **instance-field writes**.
//
// `BmField.run` allocates one small object per outer iteration and then hammers `getfield`/
// `putfield` on it; the allocation is what keeps the whole method outside the compiled subset, and
// the field writes never were the only reason. Here the hammering is a method of its own, so the
// allocation stays in `run` and the field traffic is what gets compiled — the same split
// `JdArray` makes for `iastore`.
class JdBox {
    int a;
    int b;
}

public class JdField {
    static int churn(JdBox c, int n) {
        int acc = 0;
        for (int j = 0; j < n; j++) {
            c.a = (c.a + j) & 0xFFFF;
            c.b = (c.b ^ c.a) & 0xFFFF;
            acc = acc + c.a + c.b;
        }
        return acc & 0xFFFFF;
    }

    static int run() {
        int acc = 0;
        for (int i = 0; i < 2000; i++) {
            JdBox c = new JdBox();
            c.a = i;
            c.b = i + 1;
            acc = (acc + churn(c, 500)) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
