public class Loop {
    // do-while whose bottom condition `a > b` compiles to a backward if_icmpgt
    // (the loop's back-edge). Increments use a variable (`one`), not a literal,
    // so javac emits iload/iadd/istore instead of iinc — keeping the body within
    // the opcodes our interpreter knows. f(5) counts 5 iterations and returns 5.
    public static int f(int a) {
        int b = 0;
        int c = 0;
        int one = 1;
        do {
            b = b + one;
            c = c + one;
        } while (a > b);
        return c;
    }
}
