// Repro de #238 y #124 - un campo inicializado en una INTERFAZ.
//
// JLS 9.3 dice que los campos de una interfaz son implicitamente `public static final`, y
// JVMS 2.9.2 que sus inicializadores van al <clinit>. El javac miraba los modificadores
// DECLARADOS, que en una interfaz vienen vacios, con dos consecuencias:
//
//   #238  el campo salia con flags: (0x0000) -- ni public, ni static, ni final -- y sin
//         ConstantValue, o sea un class file que el javap real marca invalido.
//   #124  el campo se tomaba por uno DE INSTANCIA, asi que su inicializador se bajaba a un
//         constructor sintetizado SOBRE LA INTERFAZ: un <init>()V ilegal, emitido como
//         `default`, que arranca con aload_0; invokespecial Object.<init> sobre un `this`
//         que no puede existir. Nadie lo llama, asi que el campo quedaba sin asignar.
//
// Esperado ahora, con `javap -v`:
//   NOPOS  -> flags (0x0019) ACC_PUBLIC, ACC_STATIC, ACC_FINAL  + ConstantValue: long -1l
//   DONE   -> idem, ConstantValue: int 65535
//   NOMBRE -> idem, ConstantValue: String "kaji"
//   CALC   -> flags (0x0019), sin ConstantValue (no es expresion constante) y asignado en <clinit>
//   y NINGUN metodo <init> en la interfaz.
public interface finding_238 {

    long NOPOS = -1L;

    char DONE = '￿';

    String NOMBRE = "kaji";

    /* No es una expresion constante: no lleva ConstantValue, se asigna en el <clinit>. */
    int CALC = "abc".length();

    long dame();
}
