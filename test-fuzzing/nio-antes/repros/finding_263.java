// Repro de #263 - un receptor cuyo tipo es una VARIABLE DE TIPO con cota externa no resolvia
// ningun miembro.
//
// El sintoma parecia caprichoso: con la cota escrita por nombre CALIFICADO andaba, y con la misma
// cota por `import` + nombre simple no. La causa no estaba en la resolucion sino antes: el
// recolector de nombres a cargar del classpath no miraba las COTAS de los parametros de tipo, asi
// que un tipo que solo aparece ahi nunca se cargaba. Con el nombre calificado, la resolucion lo
// traia por otro camino -- de ahi la asimetria.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_263.java
//   bin\run-headless.exe KajiLibrary\repros\finding_263.class run   -> 0
import java.util.List;

public class finding_263 {

    /* La cota viene por `import` y por nombre simple: el caso que fallaba. */
    static class PorImport<L extends List<String>> {
        L lista;
        int cuantos() { return this.lista.size(); }
    }

    /* Control: la MISMA cota, calificada. Andaba antes tambien. */
    static class Calificada<L extends java.util.List<String>> {
        L lista;
        int cuantos() { return this.lista.size(); }
    }

    /* Y con la cota en un parametro de tipo del METODO, no de la clase. */
    static <L extends List<String>> int porMetodo(L lista) { return lista.size(); }

    public static int run() {
        PorImport<List<String>> a = new PorImport<List<String>>();
        a.lista = new java.util.ArrayList<String>();
        a.lista.add("kaji");
        if (a.cuantos() != 1) { return 1; }
        if (finding_263.porMetodo(a.lista) != 1) { return 2; }
        return 0;
    }
}
