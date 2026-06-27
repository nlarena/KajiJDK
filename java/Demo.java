public class Demo {
    // Doubles x (a var avoids iinc; only iload/iadd/ireturn).
    static int twice(int x) {
        return x + x;
    }

    // A do-while loop (backward if_icmpgt) that calls twice() each iteration and
    // accumulates the result. No System.out — returns the sum so the interpreter
    // can run it end to end. f(3) = twice(1)+twice(2)+twice(3) = 2+4+6 = 12.
    static int f(int a) {
        int b = 0;
        int sum = 0;
        int one = 1;
        do {
            b = b + one;
            sum = sum + twice(b);
        } while (a > b);
        return sum;
    }
}
