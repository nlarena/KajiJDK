/**
 * Finding #553: `Externa.super.m()` escrito en una clase **interna** llama al metodo de la interna.
 *
 * El JDK sintetiza en `Externa` un puente (`access$001`) que hace el `invokespecial` sobre la
 * superclase, y la interna llama a ese. Nosotros emitimos `aload_0; invokespecial` desde la interna,
 * con la interna de receptor: la HotSpot lo rechaza ("Bad invokespecial instruction: current class
 * isn't assignable to reference class") y nuestra VM corre `Finding553.m()` --la version de la
 * subclase-- en vez de la de `Base553`. El JDK imprime `//553 Base553.m Finding553.m`; nuestra VM,
 * `//553 Finding553.m Finding553.m`, en silencio.
 */
class Base553 {
    String m() { return "Base553.m"; }
}

public class Finding553 extends Base553 {
    String m() { return "Finding553.m"; }

    class Interna {
        String u() { return Finding553.super.m() + " " + m(); }
    }

    public static int run() {
        String r = new Finding553().new Interna().u();
        System.out.println("//553 " + r);
        return r.length();
    }

    public static void main(String[] args) { run(); }
}
