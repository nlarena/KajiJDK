// Repro del finding #304: el diagnostico señalaba el archivo EQUIVOCADO.
//
//   javac --emit Aaa.java Bbb.java
//
// Antes imprimia:
//
//   Aaa.java:3: error: no se encuentra el simbolo: noExiste
//           return 1;                 <- la linea 3 de Aaa, que no tiene nada que ver
//                  ^
//     ubicacion: clase Bbb            <- y aca decia Bbb, contradiciendose solo
//
// El error trae linea y columna pero no traia el archivo, y el driver lo adivinaba: tomaba el
// primero con suficientes lineas. La posicion correcta con el archivo equivocado es la peor
// combinacion posible, porque manda a leer codigo sano.
//
// Este archivo NO tiene errores. El error esta en Bbb.java:3, y ahi tiene que decir.
public class Aaa {
    public static int uno() {
        return 1;
    }
}
