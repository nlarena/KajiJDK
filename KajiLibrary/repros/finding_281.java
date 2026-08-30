// Repro de #281 - javac no podia CREAR arreglos multidimensionales.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_281.java
//   run-headless KajiLibrary\repros\finding_281.class inicializadorAnidado   -> 1
//   run-headless KajiLibrary\repros\finding_281.class dosDimensiones         -> 2
//   run-headless KajiLibrary\repros\finding_281.class dimensionParcial       -> 2
//
// ANTES fallaban tres formas, y fallaban distinto:
//
//   new Object[][] { { "k", "v" } }   error: se esperaba una expresion, se encontro LBrace
//   new Object[2][3]                  error: tipo incompatible en `a`
//   new int[2][]                      error: tipo incompatible en `a`
//
// OJO CON EL ALCANCE, que se anoto mal la primera vez: la forma DECLARATIVA
// `int[][] a = { {1,2}, {3,4} }` **siempre funciono**, porque pasa por `var_init`. Lo unico que
// no parseaba era `new int[][] { ... }`. Por eso el arreglo del parser fue de una linea.
//
// Lo que ya andaba y acota el hallazgo: el TIPO `Object[][]` como campo, parametro o retorno
// —descriptor `[[Ljava/lang/Object;`, identico al del JDK— y `.length` sobre un parametro
// bidimensional.
//
// AHORA: **compila y corre**, en tres puntos:
//
//   attribute.rs  el tipo de un `new` de array se envuelve una vez por cada `[]`
//   codegen.rs    mas de una dimension con tamano emite `multianewarray` (0xc5)
//   parser.rs     los elementos de un `{ ... }` se leen con `var_init`, que sabe anidar
//
// `new int[2][3]` emite `multianewarray #N, 2 // class "[[I"`, igual que el JDK. La VM ya tenia
// el opcode desde siempre; lo que faltaba era producirlo.
//
// Donde mordia: `java.util.ListResourceBundle.getContents()` devuelve `Object[][]` por contrato,
// asi que la clase estaba completa y correcta y aun asi NINGUNA subclase se podia escribir con
// nuestro propio javac. Hoy `java/Msgs.java` la subclasea y `RbTest` da 111111211, lo mismo que
// `java` real.
//
// Queda como REGRESION.
public class finding_281 {

    public static int inicializadorAnidado() {
        Object[][] tabla = new Object[][] { { "k", "v" } };
        return tabla.length;
    }

    public static int dosDimensiones() {
        Object[][] a = new Object[2][3];
        return a.length;
    }

    public static int dimensionParcial() {
        int[][] a = new int[2][];
        a[0] = new int[3];
        return a.length;
    }

    // Esto compila: el tipo bidimensional es representable, solo no se puede construir.
    public static int largo(Object[][] m) {
        return m.length;
    }
}
