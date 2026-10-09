// Contraprueba de #251: la MISMA expresion que finding_251.cuantas(), pero este archivo nombra
// a Stream. Compila bien y devuelve 12.
//
//   bin\run-headless.exe KajiLibrary\repros\finding_251b.class cuantas  -> Some(Int(12))
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Stream;

public class finding_251b {

    public static int cuantas() {
        return (int) RandomGeneratorFactory.all().count();
    }

    /** Solo esta aca para que el tipo aparezca escrito. Nadie lo llama. */
    public static int nombraStream() {
        Stream<String> s = Stream.of(new String[] {"a"});
        return (int) s.count();
    }
}
