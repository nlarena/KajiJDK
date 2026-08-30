// Repro del finding #312: un `import` explicito de una clase del MISMO round no resolvia en
// posicion de expresion.
//
//   javac --emit pp/Tipo.java qq/Uso.java   ->  antes: "no se encuentra el simbolo: variable Tipo"
//
// Es el hermano del #303. Aquel era el `import java.lang.*` implicito; este es un `import` escrito.
// Y el sintoma decia **variable**, no tipo, porque en posicion de TIPO si resolvia -- solo faltaba
// en posicion de expresion, que es donde `Tipo.uno()` pone el nombre.
//
// La causa es la misma guardia: `try_load` ve que el tipo ya existe en el fuente, no carga nada del
// classpath --correcto-- y sale **sin anotar que ese nombre corto lo designa**. Arreglado con un
// mapa aparte de `externals`, porque meterlo ahi habria aflojado los chequeos de miembros.
package pp;

public class Tipo {
    public static int uno() {
        return 1;
    }
}
