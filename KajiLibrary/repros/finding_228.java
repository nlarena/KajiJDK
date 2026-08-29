// Repro de #228 - un literal char/String con escape de SUSTITUTO se rechazaba.
//
//   bin\javac.exe --emit KajiLibrary\repros\finding_228.java
//   javap -p -constants KajiLibrary\repros\finding_228.class
//
// Antes: '\ud800' daba "error: literal char invalido", para un literal que es Java valido y
// que el propio JDK usa (`Character.MIN_HIGH_SURROGATE` se declara asi).
//
// La causa no estaba en el lexer sino en la REPRESENTACION: el literal se decodificaba a un
// `char` de Rust, y un `char` de Rust es un *scalar value* de Unicode, que por definicion
// EXCLUYE los sustitutos. Un `char` de Java es otra cosa: una unidad de codigo UTF-16
// (JLS 3.1). Son dos tipos con el mismo nombre y distinto dominio, y el literal caia en la
// diferencia. Ahora se guarda como `u16`, que es lo que un `char` de Java es.
//
// El PAR sustituto en un String tenia el mismo origen y otra cara: cada escape se decodificaba
// por separado, asi que el alto fallaba solo. Ahora la decodificacion pasa por UTF-16 y
// `from_utf16` los junta en el caracter suplementario que son.
//
// Queda un caso sin soporte, y falla FUERTE en vez de en silencio: un sustituto SUELTO dentro
// de un String. Un `String` de Rust tampoco lo sostiene, y sostenerlo pide llevar todos los
// literales como Vec<u16>.
public class finding_228 {
    public static char alto()   { return '\ud800'; }   // MIN_HIGH_SURROGATE
    public static char bajo()   { return '\udfff'; }   // MAX_LOW_SURROGATE
    public static char bmp()    { return '\u00ff'; }
    public static String par()  { return "\ud83d\ude00"; }  // un solo caracter suplementario
}
