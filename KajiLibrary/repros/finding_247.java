// Repro de #247 - toda llamada a un metodo de una interfaz ANIDADA cargada del classpath se
// descartaba en silencio.
//
//   b.accept(1);              emitia:  iconst_1; pop
//   IntStream s = b.build();  no emitia nada: s = b
//
// Y despues reventaba con operand stack underflow. El tipo anidado SI resolvia como tipo; lo que
// fallaba era el despacho.
//
// Esperado: acumula() = 3 (dos elementos aceptados), y el bytecode debe mostrar los
// invokeinterface a IntStream$Builder.accept / .build, no un `pop`.
import java.util.stream.IntStream;

public class finding_247 {

    public static int acumula() {
        IntStream.Builder b = IntStream.builder();
        b.accept(1);
        b.accept(2);
        IntStream s = b.build();
        return (int) s.count() + 1;
    }
}
