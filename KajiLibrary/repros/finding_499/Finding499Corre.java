/**
 * El #499 corriendo: una anonima en un inicializador de campo tiene que **funcionar**, no solo
 * compilar. Cubre las cuatro formas de la tabla del hallazgo, y ademas dos que el repro original no
 * tenia y que son las que podrian salir mal al bajarla a miembro en vez de a clase local:
 *
 * <ul>
 *   <li>una anonima de campo de **instancia** que usa un campo de la envolvente (necesita `this$0`);
 *   <li>una anonima con **argumentos de super**, que sintetiza el constructor de reenvio.
 * </ul>
 *
 * <p>El JDK 25 imprime las mismas seis lineas.
 */
public class Finding499Corre {

    interface F {
        String f();
    }

    static class Base {
        final int n;
        Base(int n) { this.n = n; }
        String di() { return "base" + this.n; }
    }

    int dato = 7;

    static F campoEstatico = new F() {
        public String f() { return "estatico"; }
    };

    F campoDeInstancia = new F() {
        public String f() { return "instancia"; }
    };

    F usaLaEnvolvente = new F() {
        public String f() { return "envolvente" + dato; }
    };

    static Base conArgs = new Base(3) {
        String di() { return "sobre" + this.n; }
    };

    static F dentroDeUnMetodo() {
        return new F() {
            public String f() { return "metodo"; }
        };
    }

    static class Impl implements F {
        public String f() { return "conNombre"; }
    }

    static F conClaseConNombre = new Impl();

    public static int run() {
        Finding499Corre o = new Finding499Corre();
        System.out.println("//1 " + campoEstatico.f());
        System.out.println("//2 " + o.campoDeInstancia.f());
        System.out.println("//3 " + o.usaLaEnvolvente.f());
        System.out.println("//4 " + conArgs.di());
        System.out.println("//5 " + dentroDeUnMetodo().f());
        System.out.println("//6 " + conClaseConNombre.f());
        return 0;
    }

    public static void main(String[] a) { run(); }
}
