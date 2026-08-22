public class BxDbgT {
    static int probe() {
        Long l = 7L;
        return l.longValue() == 7L ? 42 : 41;
    }

    public static int run() {
        Integer a = 5;
        int b = a;
        Integer x = 100, y = 100;
        Integer p = 200, q = 200;
        boolean e = x.equals(100);
        Boolean t = true;
        Character c = 'A';
        try {
            return probe();
        } catch (ArithmeticException ex) {
            return 80;
        } catch (IllegalArgumentException ex) {
            return 81;
        } catch (NegativeArraySizeException ex) {
            return 82;
        } catch (IllegalMonitorStateException ex) {
            return 83;
        } catch (NullPointerException ex) {
            return 90;
        } catch (ClassCastException ex) {
            return 91;
        } catch (ArrayIndexOutOfBoundsException ex) {
            return 92;
        } catch (RuntimeException ex) {
            return 95;
        } catch (Throwable ex) {
            return 99;
        }
    }
}
