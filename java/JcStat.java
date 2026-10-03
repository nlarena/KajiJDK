// Estáticos y campos `boolean`/`byte`/`char`/`short` en un bucle caliente — la fixture del ancho
// que este tier NO codifica, porque no hace falta: esta VM guarda cada uno de ellos en **un slot de
// 4 bytes**, igual que un `int` (`field_slots` da 1 para todo lo que no sea `J`/`D`), y los lee
// de vuelta con `read_u32 as i32`. El emisor decide el ancho del acceso por el `Kind`, así que
// tratarlos como `Int` produce el mismo `mov` de 4 bytes que hace el intérprete.
//
// **Las cuatro escrituras van con su estrechamiento explícito** (`(byte)`, `(char)`, `(short)`) y
// el `boolean` se niega: así javac emite `i2b`/`i2c`/`i2s` antes de cada `putstatic`/`putfield` y
// el valor que llega al slot es el que llegaría en el JDK real. Sin eso el resultado dependería de
// si la VM trunca al guardar, que no es lo que este test mide.
//
// Compilada **con el `javac` del JDK 25** (`C:\Program Files\Java\jdk-25\bin\javac`), no con el
// nuestro: lo que fija el test es lo que imprime el `java` real, y para eso el `.class` tiene que
// ser el que ese `java` corrió. Ver FZ-003.
public class JcStat {
    static boolean flag;
    static byte b;
    static char c = 'a';
    static short s = 1000;
    static JcStat self = new JcStat();

    boolean f;
    short t = 7;

    static int run() {
        int acc = 0;
        for (int i = 0; i < 20000; i++) {
            flag = !flag;
            b = (byte) (b + 7);
            c = (char) (c + 1);
            s = (short) (s - 3);
            JcStat o = self;
            o.f = !o.f;
            o.t = (short) (o.t + 5);
            acc = (acc * 31 + (flag ? 1 : 0) + b + c + s + (o.f ? 2 : 0) + o.t) & 0xFFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
