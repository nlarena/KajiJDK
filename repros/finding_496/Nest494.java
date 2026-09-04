/** Da -1 con el javac real; con el nuestro, IllegalAccessError en la JVM real. */
public class Nest494 {
    public static int run() { return new q.Sub().armar() == 7 ? -1 : 1; }
    public static void main(String[] a) { System.out.println(run()); }
}
