/**
 * El #480 corriendo: no alcanza con que compile, el objeto tiene que construirse.
 *
 * <p>El sintoma que el hallazgo describia era un `.class` con un descriptor que no existe --el
 * `invokespecial` al constructor de la interna de la superclase salia sin la instancia envolvente--,
 * que nuestra VM carga igual y una JVM real rechaza con {@code NoSuchMethodError} al construir.
 *
 * <p>Se lee tambien el campo heredado, para que la envolvente que se paso tenga que ser la buena y
 * no solo estar presente.
 *
 * <p>El JDK 25 imprime las mismas dos lineas.
 */
public class Finding480Corre {

    public static int run() {
        Sub s = new Sub();
        Outer.Inner i = s.make("hola");
        System.out.println("//clase " + i.getClass().getName());
        System.out.println("//campo " + i.a);
        return 0;
    }

    public static void main(String[] x) {
        run();
    }
}
