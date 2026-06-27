public class Recursion {
    // Base cases compare against 1 (not 0) so javac emits if_icmpgt — the branch
    // we have — instead of the compare-with-zero ifle/ifgt we don't yet support.

    // factorial: fact(5) = 120. Uses imul, isub, if_icmpgt, recursion.
    static int fact(int n) {
        if (n <= 1) return 1;
        return n * fact(n - 1);
    }

    // fibonacci (double recursion): fib(10) = 55. Uses iadd, isub, if_icmpgt.
    static int fib(int n) {
        if (n <= 1) return n;
        return fib(n - 1) + fib(n - 2);
    }

    // sum 1..n: sum(5) = 15. Uses iadd, isub, if_icmpgt, recursion.
    static int sum(int n) {
        if (n <= 1) return n;
        return n + sum(n - 1);
    }
}
