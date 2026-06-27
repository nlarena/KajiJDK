// invokevirtual con params long.
class InstLong {
    static long run() {
        Adder ad = new Adder();
        return ad.plus(3L, 4L);   // 7
    }
}
