/* Repro de COMPILER_FINDINGS #229: las constantes String no-ASCII se leen como bytes
   UTF-8 crudos, un char por byte. Un String de Java es UTF-16: `length()` cuenta
   unidades de codigo, no bytes.

   Compilado con el javac REAL a proposito: su CONSTANT_Utf8 lleva el UTF-8 modificado
   correcto (y el par subrogado para el caracter astral), que es justo lo que nuestro
   lector ya decodifica bien. La perdida ocurre despues, al guardarlo. */
public class Utf16Probe {
    /* U+00F1: un char, dos bytes en UTF-8. */
    public static int len1() { return "ñ".length(); }
    public static int charAt1() { return "ñ".charAt(0); }

    /* U+20AC (euro): un char, TRES bytes en UTF-8. */
    public static int len3() { return "€".length(); }
    public static int charAt3() { return "€".charAt(0); }

    /* Mezcla: ASCII + no-ASCII + ASCII. En Java son 3 chars. */
    public static int lenMixed() { return "añb".length(); }
    public static int charAtMixed() { return "añb".charAt(2); }

    /* U+1D160, astral: DOS unidades UTF-16 (par subrogado), cuatro bytes en UTF-8. */
    public static int lenAstral() { return "𝅘𝅥𝅮".length(); }
    public static int charAtAstral() { return "𝅘𝅥𝅮".charAt(0); }

    /* Control: ASCII puro, que ya andaba. */
    public static int lenAscii() { return "abc".length(); }
}
