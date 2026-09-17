import java.math.BigInteger;

// El caso que encontró el fuzzer, reducido: desplazar a la derecha un BigInteger negativo.
//
// La especificación de BigInteger.shiftRight dice que el resultado es floor(this / 2^n): redondea
// hacia menos infinito, igual que el operador >> sobre un int. Por eso -2 >> 7 da -1 y no 0.
//
// Cada línea imprime la operación y su resultado. Hay dos controles que tienen que coincidir en
// cualquier implementación correcta o incorrecta: un positivo, y un negativo que se divide exacto,
// donde no se pierde ningún bit y truncar o redondear dan lo mismo.
public class ShiftRightNegativo {
    static void mostrar(String operacion, BigInteger resultado) {
        System.out.println(operacion + " = " + resultado);
    }

    static int run() {
        mostrar("-2 >> 7", new BigInteger("-2").shiftRight(7));
        mostrar("-129 >> 7", new BigInteger("-129").shiftRight(7));
        mostrar("-1 >> 1", new BigInteger("-1").shiftRight(1));
        mostrar("-5 << -1", new BigInteger("-5").shiftLeft(-1));
        mostrar("control: 129 >> 7", new BigInteger("129").shiftRight(7));
        mostrar("control: -128 >> 7", new BigInteger("-128").shiftRight(7));
        return 0;
    }

    public static void main(String[] args) {
        run();
    }
}
