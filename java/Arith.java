// Resto de aritmética: división, resto, negación, shifts, bitwise, iinc.
class Arith {
    static int intMix() {            // idiv, irem, ineg, ishl, iand
        int a = 17, b = 5;
        return (a / b) + (a % b) + (-a) + (b << 2) + (a & 6);  // 3+2-17+20+0 = 8
    }
    static long longMix() {          // ldiv, lrem, lshl, lor
        long a = 1000L;
        return (a / 7L) + (a % 7L) + (a << 1) + (a | 1L);      // 142+6+2000+1001 = 3149
    }
    static double dblMix() {         // ddiv, drem, dneg
        double a = 7.5, b = 2.0;
        return (a / b) + (a % b) + (-b);                       // 3.75+1.5-2.0 = 3.25
    }
    static int loop() {              // iinc + if_icmplt
        int sum = 0;
        for (int i = 0; i < 5; i++) sum += i;
        return sum;                                            // 10
    }
    static int divZero() {           // idiv /0 → ArithmeticException, capturada
        try {
            int a = 10, b = 0;
            return a / b;
        } catch (ArithmeticException e) {
            return -1;                                         // capturó → -1
        }
    }
}
