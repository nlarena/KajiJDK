// Repro de #209 - el literal de clase de un primitivo no parseaba.
//
//   bin\javac.exe --emit KajiLibrary\repros\finding_209.java
//   javap -p -c KajiLibrary\repros\finding_209.class
//
// Antes: `int.class` daba "error: se esperaba una expresion, se encontro Int". El nombre del
// tipo es una KEYWORD, asi que no llegaba por el camino de un identificador y `primary()` se
// quedaba sin caso. Es la unica forma en que un primitivo aparece donde va una expresion.
//
// Consecuencia concreta que tenia: no habia NINGUNA expresion Java cuyo valor fuera el mirror
// de un primitivo, asi que MethodType.unwrap() no se podia implementar - y el escape clasico,
// Integer.TYPE, esta declarado en el JDK justamente como `= int.class`.
//
// Esperado ahora, identico al javac real (JLS 15.8.2):
//
//   int.class     getstatic java/lang/Integer.TYPE:Ljava/lang/Class;
//   void.class    getstatic java/lang/Void.TYPE:Ljava/lang/Class;
//   int[].class   ldc class "[I"          <- un array SI es un ldc, pero de su DESCRIPTOR
//   Integer.class ldc class java/lang/Integer
//
// No es un ldc porque no hay entrada CONSTANT_Class para `int`: no es una clase.
//
// Nota: `Void.TYPE` no existia en KajiLibrary cuando se escribio esto -- Void.java lo daba por
// diferido "hasta que la VM soporte la clase primitiva void", una nota que habia sobrevivido a su
// razon-. Se agrego el mismo dia (#270), asi que las cuatro formas corren de punta a punta:
// void.class == Void.TYPE, != Void.class, y su getName() es "void".
public class finding_209 {
    public static Class<?> primitivo() { return int.class; }
    public static Class<?> vacio()     { return void.class; }
    public static Class<?> arreglo()   { return int[].class; }
    public static Class<?> clase()     { return Integer.class; }
}
