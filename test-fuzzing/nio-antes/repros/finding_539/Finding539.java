import java.time.Duration;

/**
 * #539 -- un plazo no positivo en {@code Thread.sleep} no cuenta como punto de interrupcion, y un
 * plazo negativo no se rechaza.
 *
 * <p>El JDK 25 imprime:
 * <pre>
 * //cero IE flag=false
 * //long0 IE flag=false
 * //neg volvio
 * </pre>
 *
 * <p>La linea {@code //cero} ya coincide (se arreglo en {@code Thread.sleep(Duration)}). La linea
 * {@code //long0} sigue dando {@code volvio flag=true}: esa sobrecarga la intercepta la VM. La
 * tercera esta para delimitar: una {@code Duration} negativa el JDK tampoco la rechaza, asi que ahi
 * coincidimos y no hay nada que arreglar.
 */
public class Finding539 {
    public static int run() throws Exception {
        Thread.currentThread().interrupt();
        try { Thread.sleep(Duration.ZERO); System.out.println("//cero volvio flag=" + Thread.interrupted()); }
        catch (InterruptedException e) { System.out.println("//cero IE flag=" + Thread.interrupted()); }

        Thread.currentThread().interrupt();
        try { Thread.sleep(0L); System.out.println("//long0 volvio flag=" + Thread.interrupted()); }
        catch (InterruptedException e) { System.out.println("//long0 IE flag=" + Thread.interrupted()); }

        try { Thread.sleep(Duration.ofMillis(-5)); System.out.println("//neg volvio"); }
        catch (IllegalArgumentException e) { System.out.println("//neg IAE"); }
        catch (InterruptedException e) { System.out.println("//neg IE"); }
        return 0;
    }
    public static void main(String[] a) throws Exception { run(); }
}
