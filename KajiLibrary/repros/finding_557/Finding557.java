/**
 * Finding #557: una creacion **calificada** de una interna de segundo nivel (`i.new Dentro()`,
 * §15.9.1) no resuelve.
 *
 * El JDK 25 imprime `//557 ok`. Nuestro javac corta con *"el generador de bytecode todavia no
 * soporta una llamada que no resolvio a ningun metodo"* sobre `i.new Dentro().v()`. Con la misma
 * interna instanciada **desde adentro** de su envolvente (`new Dentro()`) anda, que es lo que
 * delimita el defecto: es la forma calificada la que no resuelve.
 */
public class Finding557 {

    class Interna {
        class Dentro {
            String v() { return "ok"; }
        }
        String desdeAdentro() { return new Dentro().v(); }   // control: esta forma anda
    }

    public static int run() {
        Finding557 t = new Finding557();
        Finding557.Interna i = t.new Interna();
        System.out.println("//557 " + i.new Dentro().v());
        System.out.println("//control " + i.desdeAdentro());
        return 0;
    }

    public static void main(String[] args) {
        run();
    }
}
