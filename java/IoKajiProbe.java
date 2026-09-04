/** Sonda temporal: corre las secciones de IoKajiTest de a una para aislar la que rompe. */
public class IoKajiProbe {

    static int corre(int cual) {
        IoKajiTest.fallo = 0;
        try {
            if (cual == 1) {
                IoKajiTest.stringBuffer();
            } else if (cual == 2) {
                IoKajiTest.lineNumber();
            } else if (cual == 3) {
                IoKajiTest.tokenizer();
            } else if (cual == 4) {
                IoKajiTest.archivos();
            } else if (cual == 5) {
                IoKajiTest.tuberias();
            } else if (cual == 6) {
                IoKajiTest.constantes();
            }
        } catch (Exception e) {
            System.out.println("excepcion en " + cual + ": " + e);
            return 999;
        }
        return IoKajiTest.fallo;
    }

    public static int p1() { return corre(1); }

    public static int p2() { return corre(2); }

    public static int p3() { return corre(3); }

    public static int p4() { return corre(4); }

    public static int p5() { return corre(5); }

    public static int p6() { return corre(6); }
}
