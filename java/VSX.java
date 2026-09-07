import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorShape;
import jdk.incubator.vector.VectorSpecies;

/**
 * Comprueba {@code VectorSpecies} y {@code VectorShape} contra el JDK 25.
 *
 * <p>Arma una linea por cada especie con todo lo que sabe contestar sin fabricar un vector, y la
 * compara con la que produjo el JDK real, escrita abajo. {@link #run()} devuelve el indice de la
 * primera que no coincide, o -1 si coinciden todas: un entero alcanza porque {@code run-headless} no
 * vacia la consola.
 *
 * <p>Quedan afuera {@code vectorType()} y {@code maskType()}, que aca contestan la clase publica
 * ({@code IntVector}) donde el JDK contesta una privada del paquete ({@code Int128Vector}). La
 * diferencia esta explicada en {@code Especie}: las clases del JDK existen porque cada una lleva su
 * implementacion intrinseca, y sin intrinsecos no hay nada que las distinga.
 */
public class VSX {

    static final Class<?>[] TS = {byte.class, short.class, int.class, long.class,
                                  float.class, double.class};

    // Las cuatro formas de tamano fijo. S_Max_BIT queda afuera a proposito: nombra el vector mas
    // grande de la maquina, que en el JDK de esta computadora son 256 bits y aca son 64 porque no
    // hay intrinsecos a quien preguntarle. Compararla contra el JDK no probaria nada; lo que se
    // puede exigir es que sea coherente consigo misma, y de eso se ocupa maquina().
    static final VectorShape[] SH = {VectorShape.S_64_BIT, VectorShape.S_128_BIT,
                                     VectorShape.S_256_BIT, VectorShape.S_512_BIT};

    static final long[] VALORES = {0, 1, 127, 128, -128, -129, 32767, 32768, 16777216L,
                                   16777217L, 9007199254740992L, 9007199254740993L,
                                   2147483647L, 2147483648L, -2147483649L, Long.MAX_VALUE};

    static final int[] LARGOS = {0, 1, 3, 4, 5, 7, 8, 9, 15, 16, 17, -1, -4, -5, 1000,
                                 2147483647};

    static final int TOTAL = 44;

    static final String[] ESPERADO = {
        "Species[byte, 8, S_64_BIT]|byte|8|8|64|8|S_64_BIT|0,0;0,0;0,0;0,0;0,0;0,0;8,8;8,8;8,8;16,16;16,16;-8,-8;-8,-8;-8,-8;1000,1000;2147483640,2147483640;|1110100000000000|0,0;-2,-2;-4,-4;-8,-8;2,0;0,-2;-2,-4;-4,-8;4,0;2,-2;0,-4;-2,-8;8,0;4,-2;2,-4;0,-8;4,0;2,-2;0,-4;-2,-8;8,0;4,-2;2,-4;0,-8;",
        "Species[byte, 16, S_128_BIT]|byte|8|16|128|16|S_128_BIT|0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;16,16;16,16;-16,-16;-16,-16;-16,-16;992,992;2147483632,2147483632;|1110100000000000|2,2;0,0;-2,-2;-4,-4;4,2;2,0;0,-2;-2,-4;8,2;4,0;2,-2;0,-4;16,2;8,0;4,-2;2,-4;8,2;4,0;2,-2;0,-4;16,2;8,0;4,-2;2,-4;",
        "Species[byte, 32, S_256_BIT]|byte|8|32|256|32|S_256_BIT|0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;-32,-32;-32,-32;-32,-32;992,992;2147483616,2147483616;|1110100000000000|4,4;2,2;0,0;-2,-2;8,4;4,2;2,0;0,-2;16,4;8,2;4,0;2,-2;32,4;16,2;8,0;4,-2;16,4;8,2;4,0;2,-2;32,4;16,2;8,0;4,-2;",
        "Species[byte, 64, S_512_BIT]|byte|8|64|512|64|S_512_BIT|0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;-64,-64;-64,-64;-64,-64;960,960;2147483584,2147483584;|1110100000000000|8,8;4,4;2,2;0,0;16,8;8,4;4,2;2,0;32,8;16,4;8,2;4,0;64,8;32,4;16,2;8,0;32,8;16,4;8,2;4,0;64,8;32,4;16,2;8,0;",
        "Species[short, 4, S_64_BIT]|short|16|4|64|8|S_64_BIT|0,0;0,0;0,0;4,4;4,4;4,4;8,8;8,8;12,12;16,16;16,16;-4,-4;-4,-4;-8,-8;1000,1000;2147483644,2147483644;|1111111000000000|-2,0;-4,-2;-8,-4;-16,-8;0,0;-2,-2;-4,-4;-8,-8;2,0;0,-2;-2,-4;-4,-8;4,0;2,-2;0,-4;-2,-8;2,0;0,-2;-2,-4;-4,-8;4,0;2,-2;0,-4;-2,-8;",
        "Species[short, 8, S_128_BIT]|short|16|8|128|16|S_128_BIT|0,0;0,0;0,0;0,0;0,0;0,0;8,8;8,8;8,8;16,16;16,16;-8,-8;-8,-8;-8,-8;1000,1000;2147483640,2147483640;|1111111000000000|0,2;-2,0;-4,-2;-8,-4;2,2;0,0;-2,-2;-4,-4;4,2;2,0;0,-2;-2,-4;8,2;4,0;2,-2;0,-4;4,2;2,0;0,-2;-2,-4;8,2;4,0;2,-2;0,-4;",
        "Species[short, 16, S_256_BIT]|short|16|16|256|32|S_256_BIT|0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;16,16;16,16;-16,-16;-16,-16;-16,-16;992,992;2147483632,2147483632;|1111111000000000|2,4;0,2;-2,0;-4,-2;4,4;2,2;0,0;-2,-2;8,4;4,2;2,0;0,-2;16,4;8,2;4,0;2,-2;8,4;4,2;2,0;0,-2;16,4;8,2;4,0;2,-2;",
        "Species[short, 32, S_512_BIT]|short|16|32|512|64|S_512_BIT|0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;-32,-32;-32,-32;-32,-32;992,992;2147483616,2147483616;|1111111000000000|4,8;2,4;0,2;-2,0;8,8;4,4;2,2;0,0;16,8;8,4;4,2;2,0;32,8;16,4;8,2;4,0;16,8;8,4;4,2;2,0;32,8;16,4;8,2;4,0;",
        "Species[int, 2, S_64_BIT]|int|32|2|64|8|S_64_BIT|0,0;0,0;2,2;4,4;4,4;6,6;8,8;8,8;14,14;16,16;16,16;-2,-2;-4,-4;-6,-6;1000,1000;2147483646,2147483646;|1111111111001000|-4,0;-8,-2;-16,-4;-32,-8;-2,0;-4,-2;-8,-4;-16,-8;0,0;-2,-2;-4,-4;-8,-8;2,0;0,-2;-2,-4;-4,-8;0,0;-2,-2;-4,-4;-8,-8;2,0;0,-2;-2,-4;-4,-8;",
        "Species[int, 4, S_128_BIT]|int|32|4|128|16|S_128_BIT|0,0;0,0;0,0;4,4;4,4;4,4;8,8;8,8;12,12;16,16;16,16;-4,-4;-4,-4;-8,-8;1000,1000;2147483644,2147483644;|1111111111001000|-2,2;-4,0;-8,-2;-16,-4;0,2;-2,0;-4,-2;-8,-4;2,2;0,0;-2,-2;-4,-4;4,2;2,0;0,-2;-2,-4;2,2;0,0;-2,-2;-4,-4;4,2;2,0;0,-2;-2,-4;",
        "Species[int, 8, S_256_BIT]|int|32|8|256|32|S_256_BIT|0,0;0,0;0,0;0,0;0,0;0,0;8,8;8,8;8,8;16,16;16,16;-8,-8;-8,-8;-8,-8;1000,1000;2147483640,2147483640;|1111111111001000|0,4;-2,2;-4,0;-8,-2;2,4;0,2;-2,0;-4,-2;4,4;2,2;0,0;-2,-2;8,4;4,2;2,0;0,-2;4,4;2,2;0,0;-2,-2;8,4;4,2;2,0;0,-2;",
        "Species[int, 16, S_512_BIT]|int|32|16|512|64|S_512_BIT|0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;16,16;16,16;-16,-16;-16,-16;-16,-16;992,992;2147483632,2147483632;|1111111111001000|2,8;0,4;-2,2;-4,0;4,8;2,4;0,2;-2,0;8,8;4,4;2,2;0,0;16,8;8,4;4,2;2,0;8,8;4,4;2,2;0,0;16,8;8,4;4,2;2,0;",
        "Species[long, 1, S_64_BIT]|long|64|1|64|8|S_64_BIT|0,0;1,1;3,3;4,4;5,5;7,7;8,8;9,9;15,15;16,16;17,17;-1,-1;-4,-4;-5,-5;1000,1000;2147483647,2147483647;|1111111111111111|-8,0;-16,-2;-32,-4;-64,-8;-4,0;-8,-2;-16,-4;-32,-8;-2,0;-4,-2;-8,-4;-16,-8;0,0;-2,-2;-4,-4;-8,-8;-2,0;-4,-2;-8,-4;-16,-8;0,0;-2,-2;-4,-4;-8,-8;",
        "Species[long, 2, S_128_BIT]|long|64|2|128|16|S_128_BIT|0,0;0,0;2,2;4,4;4,4;6,6;8,8;8,8;14,14;16,16;16,16;-2,-2;-4,-4;-6,-6;1000,1000;2147483646,2147483646;|1111111111111111|-4,2;-8,0;-16,-2;-32,-4;-2,2;-4,0;-8,-2;-16,-4;0,2;-2,0;-4,-2;-8,-4;2,2;0,0;-2,-2;-4,-4;0,2;-2,0;-4,-2;-8,-4;2,2;0,0;-2,-2;-4,-4;",
        "Species[long, 4, S_256_BIT]|long|64|4|256|32|S_256_BIT|0,0;0,0;0,0;4,4;4,4;4,4;8,8;8,8;12,12;16,16;16,16;-4,-4;-4,-4;-8,-8;1000,1000;2147483644,2147483644;|1111111111111111|-2,4;-4,2;-8,0;-16,-2;0,4;-2,2;-4,0;-8,-2;2,4;0,2;-2,0;-4,-2;4,4;2,2;0,0;-2,-2;2,4;0,2;-2,0;-4,-2;4,4;2,2;0,0;-2,-2;",
        "Species[long, 8, S_512_BIT]|long|64|8|512|64|S_512_BIT|0,0;0,0;0,0;0,0;0,0;0,0;8,8;8,8;8,8;16,16;16,16;-8,-8;-8,-8;-8,-8;1000,1000;2147483640,2147483640;|1111111111111111|0,8;-2,4;-4,2;-8,0;2,8;0,4;-2,2;-4,0;4,8;2,4;0,2;-2,0;8,8;4,4;2,2;0,0;4,8;2,4;0,2;-2,0;8,8;4,4;2,2;0,0;",
        "Species[float, 2, S_64_BIT]|float|32|2|64|8|S_64_BIT|0,0;0,0;2,2;4,4;4,4;6,6;8,8;8,8;14,14;16,16;16,16;-2,-2;-4,-4;-6,-6;1000,1000;2147483646,2147483646;|1111111110100101|-4,0;-8,-2;-16,-4;-32,-8;-2,0;-4,-2;-8,-4;-16,-8;0,0;-2,-2;-4,-4;-8,-8;2,0;0,-2;-2,-4;-4,-8;0,0;-2,-2;-4,-4;-8,-8;2,0;0,-2;-2,-4;-4,-8;",
        "Species[float, 4, S_128_BIT]|float|32|4|128|16|S_128_BIT|0,0;0,0;0,0;4,4;4,4;4,4;8,8;8,8;12,12;16,16;16,16;-4,-4;-4,-4;-8,-8;1000,1000;2147483644,2147483644;|1111111110100101|-2,2;-4,0;-8,-2;-16,-4;0,2;-2,0;-4,-2;-8,-4;2,2;0,0;-2,-2;-4,-4;4,2;2,0;0,-2;-2,-4;2,2;0,0;-2,-2;-4,-4;4,2;2,0;0,-2;-2,-4;",
        "Species[float, 8, S_256_BIT]|float|32|8|256|32|S_256_BIT|0,0;0,0;0,0;0,0;0,0;0,0;8,8;8,8;8,8;16,16;16,16;-8,-8;-8,-8;-8,-8;1000,1000;2147483640,2147483640;|1111111110100101|0,4;-2,2;-4,0;-8,-2;2,4;0,2;-2,0;-4,-2;4,4;2,2;0,0;-2,-2;8,4;4,2;2,0;0,-2;4,4;2,2;0,0;-2,-2;8,4;4,2;2,0;0,-2;",
        "Species[float, 16, S_512_BIT]|float|32|16|512|64|S_512_BIT|0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;16,16;16,16;-16,-16;-16,-16;-16,-16;992,992;2147483632,2147483632;|1111111110100101|2,8;0,4;-2,2;-4,0;4,8;2,4;0,2;-2,0;8,8;4,4;2,2;0,0;16,8;8,4;4,2;2,0;8,8;4,4;2,2;0,0;16,8;8,4;4,2;2,0;",
        "Species[double, 1, S_64_BIT]|double|64|1|64|8|S_64_BIT|0,0;1,1;3,3;4,4;5,5;7,7;8,8;9,9;15,15;16,16;17,17;-1,-1;-4,-4;-5,-5;1000,1000;2147483647,2147483647;|1111111111101111|-8,0;-16,-2;-32,-4;-64,-8;-4,0;-8,-2;-16,-4;-32,-8;-2,0;-4,-2;-8,-4;-16,-8;0,0;-2,-2;-4,-4;-8,-8;-2,0;-4,-2;-8,-4;-16,-8;0,0;-2,-2;-4,-4;-8,-8;",
        "Species[double, 2, S_128_BIT]|double|64|2|128|16|S_128_BIT|0,0;0,0;2,2;4,4;4,4;6,6;8,8;8,8;14,14;16,16;16,16;-2,-2;-4,-4;-6,-6;1000,1000;2147483646,2147483646;|1111111111101111|-4,2;-8,0;-16,-2;-32,-4;-2,2;-4,0;-8,-2;-16,-4;0,2;-2,0;-4,-2;-8,-4;2,2;0,0;-2,-2;-4,-4;0,2;-2,0;-4,-2;-8,-4;2,2;0,0;-2,-2;-4,-4;",
        "Species[double, 4, S_256_BIT]|double|64|4|256|32|S_256_BIT|0,0;0,0;0,0;4,4;4,4;4,4;8,8;8,8;12,12;16,16;16,16;-4,-4;-4,-4;-8,-8;1000,1000;2147483644,2147483644;|1111111111101111|-2,4;-4,2;-8,0;-16,-2;0,4;-2,2;-4,0;-8,-2;2,4;0,2;-2,0;-4,-2;4,4;2,2;0,0;-2,-2;2,4;0,2;-2,0;-4,-2;4,4;2,2;0,0;-2,-2;",
        "Species[double, 8, S_512_BIT]|double|64|8|512|64|S_512_BIT|0,0;0,0;0,0;0,0;0,0;0,0;8,8;8,8;8,8;16,16;16,16;-8,-8;-8,-8;-8,-8;1000,1000;2147483640,2147483640;|1111111111101111|0,8;-2,4;-4,2;-8,0;2,8;0,4;-2,2;-4,0;4,8;2,4;0,2;-2,0;8,8;4,4;2,2;0,0;4,8;2,4;0,2;-2,0;8,8;4,4;2,2;0,0;",
        "elementSize|byte|8",
        "elementSize|short|16",
        "elementSize|int|32",
        "elementSize|long|64",
        "elementSize|float|32",
        "elementSize|double|64",
        "Species[byte, 8, S_64_BIT]|byte|8|8|64|8|S_64_BIT|0,0;0,0;0,0;0,0;0,0;0,0;8,8;8,8;8,8;16,16;16,16;-8,-8;-8,-8;-8,-8;1000,1000;2147483640,2147483640;|1110100000000000|0,0;-2,-2;-4,-4;-8,-8;2,0;0,-2;-2,-4;-4,-8;4,0;2,-2;0,-4;-2,-8;8,0;4,-2;2,-4;0,-8;4,0;2,-2;0,-4;-2,-8;8,0;4,-2;2,-4;0,-8;#Species[byte, 16, S_128_BIT]|byte|8|16|128|16|S_128_BIT|0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;16,16;16,16;-16,-16;-16,-16;-16,-16;992,992;2147483632,2147483632;|1110100000000000|2,2;0,0;-2,-2;-4,-4;4,2;2,0;0,-2;-2,-4;8,2;4,0;2,-2;0,-4;16,2;8,0;4,-2;2,-4;8,2;4,0;2,-2;0,-4;16,2;8,0;4,-2;2,-4;#Species[byte, 32, S_256_BIT]|byte|8|32|256|32|S_256_BIT|0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;-32,-32;-32,-32;-32,-32;992,992;2147483616,2147483616;|1110100000000000|4,4;2,2;0,0;-2,-2;8,4;4,2;2,0;0,-2;16,4;8,2;4,0;2,-2;32,4;16,2;8,0;4,-2;16,4;8,2;4,0;2,-2;32,4;16,2;8,0;4,-2;#Species[byte, 64, S_512_BIT]|byte|8|64|512|64|S_512_BIT|0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;-64,-64;-64,-64;-64,-64;960,960;2147483584,2147483584;|1110100000000000|8,8;4,4;2,2;0,0;16,8;8,4;4,2;2,0;32,8;16,4;8,2;4,0;64,8;32,4;16,2;8,0;32,8;16,4;8,2;4,0;64,8;32,4;16,2;8,0;",
        "Species[short, 4, S_64_BIT]|short|16|4|64|8|S_64_BIT|0,0;0,0;0,0;4,4;4,4;4,4;8,8;8,8;12,12;16,16;16,16;-4,-4;-4,-4;-8,-8;1000,1000;2147483644,2147483644;|1111111000000000|-2,0;-4,-2;-8,-4;-16,-8;0,0;-2,-2;-4,-4;-8,-8;2,0;0,-2;-2,-4;-4,-8;4,0;2,-2;0,-4;-2,-8;2,0;0,-2;-2,-4;-4,-8;4,0;2,-2;0,-4;-2,-8;#Species[short, 8, S_128_BIT]|short|16|8|128|16|S_128_BIT|0,0;0,0;0,0;0,0;0,0;0,0;8,8;8,8;8,8;16,16;16,16;-8,-8;-8,-8;-8,-8;1000,1000;2147483640,2147483640;|1111111000000000|0,2;-2,0;-4,-2;-8,-4;2,2;0,0;-2,-2;-4,-4;4,2;2,0;0,-2;-2,-4;8,2;4,0;2,-2;0,-4;4,2;2,0;0,-2;-2,-4;8,2;4,0;2,-2;0,-4;#Species[short, 16, S_256_BIT]|short|16|16|256|32|S_256_BIT|0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;16,16;16,16;-16,-16;-16,-16;-16,-16;992,992;2147483632,2147483632;|1111111000000000|2,4;0,2;-2,0;-4,-2;4,4;2,2;0,0;-2,-2;8,4;4,2;2,0;0,-2;16,4;8,2;4,0;2,-2;8,4;4,2;2,0;0,-2;16,4;8,2;4,0;2,-2;#Species[short, 32, S_512_BIT]|short|16|32|512|64|S_512_BIT|0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;-32,-32;-32,-32;-32,-32;992,992;2147483616,2147483616;|1111111000000000|4,8;2,4;0,2;-2,0;8,8;4,4;2,2;0,0;16,8;8,4;4,2;2,0;32,8;16,4;8,2;4,0;16,8;8,4;4,2;2,0;32,8;16,4;8,2;4,0;",
        "Species[int, 2, S_64_BIT]|int|32|2|64|8|S_64_BIT|0,0;0,0;2,2;4,4;4,4;6,6;8,8;8,8;14,14;16,16;16,16;-2,-2;-4,-4;-6,-6;1000,1000;2147483646,2147483646;|1111111111001000|-4,0;-8,-2;-16,-4;-32,-8;-2,0;-4,-2;-8,-4;-16,-8;0,0;-2,-2;-4,-4;-8,-8;2,0;0,-2;-2,-4;-4,-8;0,0;-2,-2;-4,-4;-8,-8;2,0;0,-2;-2,-4;-4,-8;#Species[int, 4, S_128_BIT]|int|32|4|128|16|S_128_BIT|0,0;0,0;0,0;4,4;4,4;4,4;8,8;8,8;12,12;16,16;16,16;-4,-4;-4,-4;-8,-8;1000,1000;2147483644,2147483644;|1111111111001000|-2,2;-4,0;-8,-2;-16,-4;0,2;-2,0;-4,-2;-8,-4;2,2;0,0;-2,-2;-4,-4;4,2;2,0;0,-2;-2,-4;2,2;0,0;-2,-2;-4,-4;4,2;2,0;0,-2;-2,-4;#Species[int, 8, S_256_BIT]|int|32|8|256|32|S_256_BIT|0,0;0,0;0,0;0,0;0,0;0,0;8,8;8,8;8,8;16,16;16,16;-8,-8;-8,-8;-8,-8;1000,1000;2147483640,2147483640;|1111111111001000|0,4;-2,2;-4,0;-8,-2;2,4;0,2;-2,0;-4,-2;4,4;2,2;0,0;-2,-2;8,4;4,2;2,0;0,-2;4,4;2,2;0,0;-2,-2;8,4;4,2;2,0;0,-2;#Species[int, 16, S_512_BIT]|int|32|16|512|64|S_512_BIT|0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;16,16;16,16;-16,-16;-16,-16;-16,-16;992,992;2147483632,2147483632;|1111111111001000|2,8;0,4;-2,2;-4,0;4,8;2,4;0,2;-2,0;8,8;4,4;2,2;0,0;16,8;8,4;4,2;2,0;8,8;4,4;2,2;0,0;16,8;8,4;4,2;2,0;",
        "Species[long, 1, S_64_BIT]|long|64|1|64|8|S_64_BIT|0,0;1,1;3,3;4,4;5,5;7,7;8,8;9,9;15,15;16,16;17,17;-1,-1;-4,-4;-5,-5;1000,1000;2147483647,2147483647;|1111111111111111|-8,0;-16,-2;-32,-4;-64,-8;-4,0;-8,-2;-16,-4;-32,-8;-2,0;-4,-2;-8,-4;-16,-8;0,0;-2,-2;-4,-4;-8,-8;-2,0;-4,-2;-8,-4;-16,-8;0,0;-2,-2;-4,-4;-8,-8;#Species[long, 2, S_128_BIT]|long|64|2|128|16|S_128_BIT|0,0;0,0;2,2;4,4;4,4;6,6;8,8;8,8;14,14;16,16;16,16;-2,-2;-4,-4;-6,-6;1000,1000;2147483646,2147483646;|1111111111111111|-4,2;-8,0;-16,-2;-32,-4;-2,2;-4,0;-8,-2;-16,-4;0,2;-2,0;-4,-2;-8,-4;2,2;0,0;-2,-2;-4,-4;0,2;-2,0;-4,-2;-8,-4;2,2;0,0;-2,-2;-4,-4;#Species[long, 4, S_256_BIT]|long|64|4|256|32|S_256_BIT|0,0;0,0;0,0;4,4;4,4;4,4;8,8;8,8;12,12;16,16;16,16;-4,-4;-4,-4;-8,-8;1000,1000;2147483644,2147483644;|1111111111111111|-2,4;-4,2;-8,0;-16,-2;0,4;-2,2;-4,0;-8,-2;2,4;0,2;-2,0;-4,-2;4,4;2,2;0,0;-2,-2;2,4;0,2;-2,0;-4,-2;4,4;2,2;0,0;-2,-2;#Species[long, 8, S_512_BIT]|long|64|8|512|64|S_512_BIT|0,0;0,0;0,0;0,0;0,0;0,0;8,8;8,8;8,8;16,16;16,16;-8,-8;-8,-8;-8,-8;1000,1000;2147483640,2147483640;|1111111111111111|0,8;-2,4;-4,2;-8,0;2,8;0,4;-2,2;-4,0;4,8;2,4;0,2;-2,0;8,8;4,4;2,2;0,0;4,8;2,4;0,2;-2,0;8,8;4,4;2,2;0,0;",
        "Species[float, 2, S_64_BIT]|float|32|2|64|8|S_64_BIT|0,0;0,0;2,2;4,4;4,4;6,6;8,8;8,8;14,14;16,16;16,16;-2,-2;-4,-4;-6,-6;1000,1000;2147483646,2147483646;|1111111110100101|-4,0;-8,-2;-16,-4;-32,-8;-2,0;-4,-2;-8,-4;-16,-8;0,0;-2,-2;-4,-4;-8,-8;2,0;0,-2;-2,-4;-4,-8;0,0;-2,-2;-4,-4;-8,-8;2,0;0,-2;-2,-4;-4,-8;#Species[float, 4, S_128_BIT]|float|32|4|128|16|S_128_BIT|0,0;0,0;0,0;4,4;4,4;4,4;8,8;8,8;12,12;16,16;16,16;-4,-4;-4,-4;-8,-8;1000,1000;2147483644,2147483644;|1111111110100101|-2,2;-4,0;-8,-2;-16,-4;0,2;-2,0;-4,-2;-8,-4;2,2;0,0;-2,-2;-4,-4;4,2;2,0;0,-2;-2,-4;2,2;0,0;-2,-2;-4,-4;4,2;2,0;0,-2;-2,-4;#Species[float, 8, S_256_BIT]|float|32|8|256|32|S_256_BIT|0,0;0,0;0,0;0,0;0,0;0,0;8,8;8,8;8,8;16,16;16,16;-8,-8;-8,-8;-8,-8;1000,1000;2147483640,2147483640;|1111111110100101|0,4;-2,2;-4,0;-8,-2;2,4;0,2;-2,0;-4,-2;4,4;2,2;0,0;-2,-2;8,4;4,2;2,0;0,-2;4,4;2,2;0,0;-2,-2;8,4;4,2;2,0;0,-2;#Species[float, 16, S_512_BIT]|float|32|16|512|64|S_512_BIT|0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;0,0;16,16;16,16;-16,-16;-16,-16;-16,-16;992,992;2147483632,2147483632;|1111111110100101|2,8;0,4;-2,2;-4,0;4,8;2,4;0,2;-2,0;8,8;4,4;2,2;0,0;16,8;8,4;4,2;2,0;8,8;4,4;2,2;0,0;16,8;8,4;4,2;2,0;",
        "Species[double, 1, S_64_BIT]|double|64|1|64|8|S_64_BIT|0,0;1,1;3,3;4,4;5,5;7,7;8,8;9,9;15,15;16,16;17,17;-1,-1;-4,-4;-5,-5;1000,1000;2147483647,2147483647;|1111111111101111|-8,0;-16,-2;-32,-4;-64,-8;-4,0;-8,-2;-16,-4;-32,-8;-2,0;-4,-2;-8,-4;-16,-8;0,0;-2,-2;-4,-4;-8,-8;-2,0;-4,-2;-8,-4;-16,-8;0,0;-2,-2;-4,-4;-8,-8;#Species[double, 2, S_128_BIT]|double|64|2|128|16|S_128_BIT|0,0;0,0;2,2;4,4;4,4;6,6;8,8;8,8;14,14;16,16;16,16;-2,-2;-4,-4;-6,-6;1000,1000;2147483646,2147483646;|1111111111101111|-4,2;-8,0;-16,-2;-32,-4;-2,2;-4,0;-8,-2;-16,-4;0,2;-2,0;-4,-2;-8,-4;2,2;0,0;-2,-2;-4,-4;0,2;-2,0;-4,-2;-8,-4;2,2;0,0;-2,-2;-4,-4;#Species[double, 4, S_256_BIT]|double|64|4|256|32|S_256_BIT|0,0;0,0;0,0;4,4;4,4;4,4;8,8;8,8;12,12;16,16;16,16;-4,-4;-4,-4;-8,-8;1000,1000;2147483644,2147483644;|1111111111101111|-2,4;-4,2;-8,0;-16,-2;0,4;-2,2;-4,0;-8,-2;2,4;0,2;-2,0;-4,-2;4,4;2,2;0,0;-2,-2;2,4;0,2;-2,0;-4,-2;4,4;2,2;0,0;-2,-2;#Species[double, 8, S_512_BIT]|double|64|8|512|64|S_512_BIT|0,0;0,0;0,0;0,0;0,0;0,0;8,8;8,8;8,8;16,16;16,16;-8,-8;-8,-8;-8,-8;1000,1000;2147483640,2147483640;|1111111111101111|0,8;-2,4;-4,2;-8,0;2,8;0,4;-2,2;-4,0;4,8;2,4;0,2;-2,0;8,8;4,4;2,2;0,0;4,8;2,4;0,2;-2,0;8,8;4,4;2,2;0,0;",
        "igualdad|true|true|false|false",
        "withLanes|Species[long, 2, S_128_BIT]|Species[byte, 16, S_128_BIT]|withShape|Species[int, 16, S_512_BIT]",
        "check|Species[int, 4, S_128_BIT]",
        "check-mal|java.lang.ClassCastException|Species[int, 4, S_128_BIT]: required long but found int",
        "checkValue-msg|java.lang.IllegalArgumentException|Vector creation failed: value 2147483648 cannot be represented in ETYPE int; result of cast is -2147483648",
        "forBitSize|S_Max_BIT",
        "forBitSize-mal|java.lang.IllegalArgumentException",
        "forIndexBitSize|S_64_BIT|S_64_BIT|S_128_BIT|S_256_BIT|S_512_BIT|S_Max_BIT|S_64_BIT|S_64_BIT|S_128_BIT|S_256_BIT|S_512_BIT|S_Max_BIT|S_64_BIT|S_64_BIT|S_128_BIT|S_256_BIT|S_512_BIT|S_Max_BIT|S_64_BIT|S_64_BIT|S_128_BIT|S_256_BIT|S_512_BIT|S_Max_BIT|S_64_BIT|S_64_BIT|S_128_BIT|S_256_BIT|S_512_BIT|S_Max_BIT|S_64_BIT|S_64_BIT|S_128_BIT|S_256_BIT|S_512_BIT|S_Max_BIT",
    };

    static String desc(VectorSpecies<?> s) {
        StringBuilder b = new StringBuilder();
        b.append(s).append('|').append(s.elementType().getName());
        b.append('|').append(s.elementSize()).append('|').append(s.length());
        b.append('|').append(s.vectorBitSize()).append('|').append(s.vectorByteSize());
        b.append('|').append(s.vectorShape());
        b.append('|');
        for (int i = 0; i < LARGOS.length; i++) {
            b.append(s.loopBound(LARGOS[i])).append(',');
            b.append(s.loopBound((long) LARGOS[i])).append(';');
        }
        b.append('|');
        for (int i = 0; i < VALORES.length; i++) {
            try {
                s.checkValue(VALORES[i]);
                b.append('1');
            } catch (IllegalArgumentException e) {
                b.append('0');
            }
        }
        b.append('|');
        for (int i = 0; i < TS.length; i++) {
            for (int j = 0; j < SH.length; j++) {
                VectorSpecies<?> o = VectorSpecies.of(TS[i], SH[j]);
                b.append(s.partLimit(o, true)).append(',');
                b.append(s.partLimit(o, false)).append(';');
            }
        }
        return b.toString();
    }

    static String[] actual() {
        String[] a = new String[TOTAL];
        int k = 0;
        for (int i = 0; i < TS.length; i++) {
            for (int j = 0; j < SH.length; j++) {
                a[k++] = desc(VectorSpecies.of(TS[i], SH[j]));
            }
        }
        for (int i = 0; i < TS.length; i++) {
            a[k++] = "elementSize|" + TS[i].getName() + "|" + VectorSpecies.elementSize(TS[i]);
        }
        a[k++] = desc(ByteVector.SPECIES_64) + "#" + desc(ByteVector.SPECIES_128)
                + "#" + desc(ByteVector.SPECIES_256) + "#" + desc(ByteVector.SPECIES_512);
        a[k++] = desc(ShortVector.SPECIES_64) + "#" + desc(ShortVector.SPECIES_128)
                + "#" + desc(ShortVector.SPECIES_256) + "#" + desc(ShortVector.SPECIES_512);
        a[k++] = desc(IntVector.SPECIES_64) + "#" + desc(IntVector.SPECIES_128)
                + "#" + desc(IntVector.SPECIES_256) + "#" + desc(IntVector.SPECIES_512);
        a[k++] = desc(LongVector.SPECIES_64) + "#" + desc(LongVector.SPECIES_128)
                + "#" + desc(LongVector.SPECIES_256) + "#" + desc(LongVector.SPECIES_512);
        a[k++] = desc(FloatVector.SPECIES_64) + "#" + desc(FloatVector.SPECIES_128)
                + "#" + desc(FloatVector.SPECIES_256) + "#" + desc(FloatVector.SPECIES_512);
        a[k++] = desc(DoubleVector.SPECIES_64) + "#" + desc(DoubleVector.SPECIES_128)
                + "#" + desc(DoubleVector.SPECIES_256) + "#" + desc(DoubleVector.SPECIES_512);

        VectorSpecies<Integer> s = IntVector.SPECIES_128;
        a[k++] = "igualdad|" + s.equals(VectorSpecies.of(int.class, VectorShape.S_128_BIT))
                + "|" + (s.hashCode()
                        == VectorSpecies.of(int.class, VectorShape.S_128_BIT).hashCode())
                + "|" + s.equals(IntVector.SPECIES_256)
                + "|" + s.equals(VectorSpecies.of(float.class, VectorShape.S_128_BIT));
        a[k++] = "withLanes|" + s.withLanes(long.class) + "|" + s.withLanes(byte.class)
                + "|withShape|" + s.withShape(VectorShape.S_512_BIT);
        // El resultado se arma en una variable y recien despues se guarda. Escrito como
        // `a[k++] = ... llamada()`, el indice se incrementa ANTES de evaluar la llamada, asi que
        // cuando esta tira, el `catch` incrementa de nuevo y se saltea una posicion.
        String r;
        try {
            r = "check|" + s.check(int.class);
        } catch (Throwable e) {
            r = "check|" + e;
        }
        a[k++] = r;
        try {
            s.check(long.class);
            r = "check-mal|sin error";
        } catch (Throwable e) {
            r = "check-mal|" + e.getClass().getName() + "|" + e.getMessage();
        }
        a[k++] = r;
        try {
            r = "checkValue-msg|sin error|" + s.checkValue(2147483648L);
        } catch (Throwable e) {
            r = "checkValue-msg|" + e.getClass().getName() + "|" + e.getMessage();
        }
        a[k++] = r;
        try {
            r = "forBitSize|" + VectorShape.forBitSize(1024);
        } catch (Throwable e) {
            r = "forBitSize|" + e.getClass().getName() + "|" + e.getMessage();
        }
        a[k++] = r;
        try {
            VectorShape.forBitSize(100);
            r = "forBitSize-mal|sin error";
        } catch (Throwable e) {
            r = "forBitSize-mal|" + e.getClass().getName();
        }
        a[k++] = r;
        StringBuilder ib = new StringBuilder("forIndexBitSize");
        for (int i = 0; i < TS.length; i++) {
            for (int n = 0; n < 6; n++) {
                int bits = 32 << n;
                try {
                    ib.append('|').append(VectorShape.forIndexBitSize(bits,
                            VectorSpecies.elementSize(TS[i])));
                } catch (Throwable e) {
                    ib.append('|').append(e.getClass().getSimpleName());
                }
            }
        }
        a[k++] = ib.toString();
        return a;
    }

    /**
     * El indice del primer descriptor que no coincide con el del JDK, o -1 si coinciden todos.
     *
     * @return el indice, o -1
     */
    public static int run() {
        String[] a = actual();
        for (int i = 0; i < TOTAL; i++) {
            if (!a[i].equals(ESPERADO[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Ubica la primera diferencia con mas detalle: {@code indice * 100 + campo}.
     *
     * <p>Los descriptores se comparan campo por campo --lo que va entre barras-- porque uno entero
     * puede tener mil caracteres y saber solo que "difiere" no alcanza para arreglarlo.
     *
     * @return la ubicacion, o -1 si no hay diferencia
     */
    public static int donde() {
        String[] a = actual();
        for (int i = 0; i < TOTAL; i++) {
            if (a[i].equals(ESPERADO[i])) {
                continue;
            }
            String[] x = a[i].split("[|]", -1);
            String[] y = ESPERADO[i].split("[|]", -1);
            for (int j = 0; j < Math.max(x.length, y.length); j++) {
                if (j >= x.length || j >= y.length || !x[j].equals(y[j])) {
                    return i * 100 + j;
                }
            }
            return i * 100 + 99;
        }
        return -1;
    }

    /**
     * Comprueba lo que depende de la maquina, que no se puede comparar contra el JDK.
     *
     * <p>Esta biblioteca dice que el vector mas grande son 64 bits, el minimo que el API admite,
     * porque el maximo real sale de los intrinsecos y no los hay. Lo que se puede exigir es que esa
     * respuesta sea la misma en todos lados: la forma, la forma preferida, las dos especies
     * {@code SPECIES_MAX} y {@code SPECIES_PREFERRED} de las seis clases, y la cantidad de
     * posiciones que sale de dividir.
     *
     * @return 0 si todo es coherente, o el numero de la primera comprobacion que fallo
     */
    public static int maquina() {
        if (VectorShape.S_Max_BIT.vectorBitSize() != 64) {
            return 1;
        }
        if (VectorShape.preferredShape() != VectorShape.S_64_BIT) {
            return 2;
        }
        if (VectorShape.largestShapeFor(int.class) != VectorShape.S_64_BIT) {
            return 3;
        }
        VectorSpecies<?>[] max = {ByteVector.SPECIES_MAX, ShortVector.SPECIES_MAX,
                                  IntVector.SPECIES_MAX, LongVector.SPECIES_MAX,
                                  FloatVector.SPECIES_MAX, DoubleVector.SPECIES_MAX};
        VectorSpecies<?>[] pref = {ByteVector.SPECIES_PREFERRED, ShortVector.SPECIES_PREFERRED,
                                   IntVector.SPECIES_PREFERRED, LongVector.SPECIES_PREFERRED,
                                   FloatVector.SPECIES_PREFERRED, DoubleVector.SPECIES_PREFERRED};
        for (int i = 0; i < TS.length; i++) {
            if (max[i].elementType() != TS[i] || pref[i].elementType() != TS[i]) {
                return 10 + i;
            }
            if (max[i].vectorShape() != VectorShape.S_Max_BIT) {
                return 20 + i;
            }
            if (pref[i].vectorShape() != VectorShape.S_64_BIT) {
                return 30 + i;
            }
            int esperado = 64 / VectorSpecies.elementSize(TS[i]);
            if (max[i].length() != esperado || pref[i].length() != esperado) {
                return 40 + i;
            }
            if (!pref[i].equals(VectorSpecies.ofPreferred(TS[i]))
                    || !max[i].equals(VectorSpecies.of(TS[i], VectorShape.S_Max_BIT))) {
                return 50 + i;
            }
            if (!VectorSpecies.ofLargestShape(TS[i]).equals(
                    VectorSpecies.of(TS[i], VectorShape.S_64_BIT))) {
                return 60 + i;
            }
        }
        return 0;
    }

    public static void main(String[] args) {
        String[] a = actual();
        if (args.length > 0) {
            for (int i = 0; i < a.length; i++) {
                System.out.println(a[i]);
            }
            return;
        }
        int i = run();
        System.out.println(i < 0 ? "sin diferencias"
                : i + ":\n  nuestro=" + a[i] + "\n  jdk    =" + ESPERADO[i]);
    }
}
