public class Catch {
    int m(int x) {
        try {
            return 10 / x;
        } catch (ArithmeticException e) {
            return -1;
        } finally {
            System.out.println("done");
        }
    }
}
