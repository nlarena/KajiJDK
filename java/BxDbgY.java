public class BxDbgY {
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
        } catch (Throwable ex) {
            // Fingerprint the exception's runtime class: length*100 + first char.
            String n = ex.getClass().getSimpleName();
            return 100000 + n.length() * 100 + n.charAt(0);
        }
    }
}
