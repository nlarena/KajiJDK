// Repro de #234 - una sola invocacion con varios archivos no resolvia cruzado.
//
// `javac --emit A.java B.java` compilaba cada archivo como una unidad INDEPENDIENTE, cada una con
// su propia tabla de simbolos, asi que `A` no veia a `B`. Dos clases que se referencian
// mutuamente pedian un bootstrap de tres pasos -- compilar una con el cuerpo talado, compilar la
// otra, recompilar la primera --, que hubo que hacer tres veces en java.text y una en
// javax.lang.model.type.
//
// El arreglo no es "compartir la tabla": es que el ORDEN DE LAS FASES sea global. Enter de TODAS
// las clases primero -- asi una referencia adelantada tiene a quien resolver, sin importar el
// orden de los archivos --, despues MemberEnter de todas, y recien entonces la resolucion. Los
// `import` siguen siendo por unidad (JLS 7.5), que es lo que corresponde.
//
// Este archivo es la mitad A; la otra es `finding_234b.java`. Se compilan JUNTOS:
//
//   bin\javac.exe --emit KajiLibrary\repros\finding_234.java KajiLibrary\repros\finding_234b.java
//   bin\run-headless.exe KajiLibrary\repros\finding_234.class run   -> 0
//
// Y tiene que dar lo mismo con el orden de los archivos invertido.
public class finding_234 {

    /* Recursion MUTUA entre archivos: ninguno de los dos se puede compilar antes que el otro. */
    static boolean par(int n) {
        if (n == 0) { return true; }
        return finding_234b.impar(n - 1);
    }

    public static int run() {
        if (!finding_234.par(4)) { return 1; }
        if (finding_234.par(3)) { return 2; }
        if (!finding_234b.impar(7)) { return 3; }
        /* Y un campo del otro archivo, no solo un metodo. */
        if (finding_234b.SEMILLA != 41) { return 4; }
        return 0;
    }
}
