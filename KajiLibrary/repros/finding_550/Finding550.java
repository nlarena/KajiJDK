/**
 * Finding #550: leer un campo de tipo variable (`E v`) a traves de un tipo parametrizado
 * (`C<String>`) no lleva el `checkcast` que corresponde. `c.v.isEmpty()` emite `getfield v:Object` y
 * enseguida `invokevirtual String.isEmpty`: la HotSpot lo rechaza con un `VerifyError` ("Type
 * 'java/lang/Object' is not assignable to 'java/lang/String'"). El JDK pone `checkcast String`.
 * Nuestra VM no verifica y lo corre bien.
 */
public class Finding550 {
    static class C<E> {
        E v;
        C(E v) { this.v = v; }
    }

    public static int run() {
        C<String> c = new C<String>("xy");
        System.out.println("//550 " + c.v.isEmpty() + " " + c.v.length());
        return c.v.length();
    }

    public static void main(String[] args) { run(); }
}
