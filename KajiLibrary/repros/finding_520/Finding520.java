/**
 * Un literal de clase de un tipo que no existe.
 *
 * <p>El JDK lo rechaza: <em>cannot find symbol</em>. Este compilador lo acepta en silencio y emite
 * {@code Object.class}, asi que el programa arranca y hace algo distinto de lo que dice.
 *
 * <p>No hace falta que el tipo este importado ni calificado: alcanza con nombrarlo.
 */
public class Finding520 {

    public static void main(String[] a) {
        Class<?> c = NoExisteEstaClase.class;
        System.out.println("el literal dio: " + c);
        System.out.println("es Object: " + (c == Object.class));
    }
}
