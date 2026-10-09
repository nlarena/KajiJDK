/**
 * Finding #544: el `StackMapTable` nombra a una variable de tipo como si fuera una clase.
 *
 * Un metodo con un parametro de tipo `K` y un salto lleva un frame con `locals = [..., class K]`.
 * No existe ninguna clase `K`: la HotSpot, que verifica toda clase de aplicacion, intenta cargarla y
 * el programa muere con `NoClassDefFoundError: K` antes de correr `main`. El javac del JDK escribe la
 * erasure, `class java/lang/Object`. Nuestra VM no lee el `StackMapTable`, asi que aca corre.
 */
public class Finding544<K> {
    int f(boolean c, K x) {
        int s = 0;
        if (c) {
            s = 1;
        }
        return s + x.hashCode();
    }

    public static int run() {
        return new Finding544<String>().f(true, "a") - 97;
    }

    public static void main(String[] args) {
        System.out.println("//544 " + run());
    }
}
