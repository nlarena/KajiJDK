// Repro de #239 y #245 - un tipo ANIDADO de otra unidad de compilacion era innombrable, y dos de
// las tres formas fallaban EN SILENCIO.
//
//   bin\javac.exe --emit KajiLibrary\repros\finding_239_ext.java
//   bin\javac.exe --emit -cp KajiLibrary\repros KajiLibrary\repros\finding_239.java
//   javap -p KajiLibrary\repros\finding_239.class
//
// Las tres formas y lo que daban:
//
//   finding_239_ext.Kind pick()            error duro (#101) -- este ya andaba
//   import ...Kind; + `Kind pick()`        compilaba -> `java.lang.Object pick()`
//   import ...Marker; + `implements`       compilaba -> la clausula DESAPARECIA del class file
//   import ...*; + nombre simple           compilaba -> `java.lang.Object` (#245)
//
// Eran DOS cosas encadenadas:
//
//   1. Traducir el nombre punteado con un `replace('.', "/")` a secas nunca encuentra un anidado:
//      `p.Outer.Kind` se buscaba como `p/Outer/Kind` y el archivo se llama `p/Outer$Kind`. El
//      fuente no distingue un paquete de un tipo envolvente, asi que hay que probar las dos.
//   2. Aun cargandolo, su clave en el espacio de externos es `Outer$Kind` y el fuente escribe
//      `Kind`. Hace falta un ALIAS -- y solo para el nombre que la unidad escribio: registrar cada
//      nombre interior de oficio hace que `java.lang.Thread$State` reclame la clave `State` y tape
//      cualquier `State` propio (se probo, y rompe java.util.concurrent.StructuredTaskScope).
//
// Esperado ahora: las cuatro formas emiten `finding_239_ext$Kind` / `implements ...$Marker`.
import finding_239_ext.Kind;
import finding_239_ext.Marker;

public class finding_239 implements Marker {

    /* Calificado (#101). */
    finding_239_ext.Kind calificado() { return null; }

    /* Nombre simple via import de tipo unico (#239). */
    Kind porImport() { return null; }
}
