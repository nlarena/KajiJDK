import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.text.AbstractWriter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.MaskFormatter;
import javax.swing.text.NumberFormatter;
import javax.swing.text.PlainDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * Los formateadores y el escritor contra el JDK.
 *
 * <p>Todo lo de aca es logica sin pantalla: convertir entre valor y texto, y recorrer un documento
 * escribiendolo. La fecha usa un formato y una zona horaria fijos para que el resultado no dependa
 * de la maquina.
 */
public class Form1 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    /** Un escritor concreto: recorre el arbol y escribe un elemento por linea, sangrado. */
    static class Escritor extends AbstractWriter {

        Escritor(java.io.Writer w, Document doc) {
            super(w, doc);
        }

        int sangria() {
            return getIndentSpace();
        }

        int ancho() {
            return getLineLength();
        }

        protected void write() throws IOException, BadLocationException {
            escribir(getDocument().getDefaultRootElement());
        }

        private void escribir(Element e) throws IOException, BadLocationException {
            if (!inRange(e)) {
                return;
            }
            indent();
            write("<" + e.getName() + " " + e.getStartOffset() + "," + e.getEndOffset() + ">");
            writeLineSeparator();
            if (e.isLeaf()) {
                incrIndent();
                indent();
                text(e);
                writeLineSeparator();
                decrIndent();
            } else {
                incrIndent();
                for (int i = 0; i < e.getElementCount(); i++) {
                    escribir(e.getElement(i));
                }
                decrIndent();
            }
        }
    }

    /** Cada linea de la salida del escritor, marcada para poder compararla. */
    static void volcar(String titulo, String texto) {
        String[] ls = texto.split("\n", -1);
        linea(titulo + " lineas=" + ls.length);
        for (int i = 0; i < ls.length; i++) {
            linea("  [" + ls[i].replace("\r", "\\r") + "]");
        }
    }

    /** El formateo de una mascara, con el error adentro para que un caso malo no corte el resto. */
    static void formatear(MaskFormatter f, String valor) {
        try {
            linea("  formato [" + valor + "] -> [" + f.valueToString(valor) + "]");
        } catch (ParseException pe) {
            linea("  formato [" + valor + "] -> ERROR " + pe.getMessage()
                    + " en " + pe.getErrorOffset());
        }
    }

    static void probarMascara(MaskFormatter f, String entrada) {
        try {
            Object v = f.stringToValue(entrada);
            linea("  mascara [" + entrada + "] -> [" + v + "]");
        } catch (ParseException pe) {
            linea("  mascara [" + entrada + "] -> ERROR en " + pe.getErrorOffset());
        }
    }

    static void probarNumero(NumberFormatter f, String entrada) {
        try {
            Object v = f.stringToValue(entrada);
            linea("  numero [" + entrada + "] -> [" + v + "] clase="
                    + v.getClass().getName());
        } catch (ParseException pe) {
            linea("  numero [" + entrada + "] -> ERROR en " + pe.getErrorOffset());
        }
    }

    public static int run() {
        try {
            // --- La mascara ---
            MaskFormatter m = new MaskFormatter("###-UU-###");
            m.setPlaceholderCharacter('_');
            linea("mascara=" + m.getMask() + " relleno=" + m.getPlaceholderCharacter()
                    + " literales=" + m.getValueContainsLiteralCharacters());
            formatear(m, null);
            formatear(m, "123AB456");
            formatear(m, "12");
            formatear(m, "123");
            formatear(m, "123-ab-4");
            formatear(m, "123-AB-456");
            probarMascara(m, "123-AB-456");
            probarMascara(m, "123-ab-456");
            probarMascara(m, "12X-AB-456");
            probarMascara(m, "123-AB-45");

            m.setValueContainsLiteralCharacters(false);
            linea("sin literales:");
            formatear(m, "123AB456");
            formatear(m, "123AB4");
            probarMascara(m, "123-AB-456");
            probarMascara(m, "123AB456");

            m.setPlaceholder("xxx-yy-zzz");
            linea("con texto de relleno:");
            formatear(m, "12");
            formatear(m, "");

            MaskFormatter m2 = new MaskFormatter("HH:'x'AAA*");
            linea("mascara2=" + m2.getMask());
            formatear(m2, "aF9z1q");
            formatear(m2, "aF:xA9z");
            probarMascara(m2, "aF:x9z1q");
            probarMascara(m2, "gF:x9z1q");
            probarMascara(m2, "aF:xA9z");

            MaskFormatter m3 = new MaskFormatter("***");
            m3.setValidCharacters("abc");
            linea("mascara3 validos=" + m3.getValidCharacters());
            formatear(m3, "abc");
            formatear(m3, "abd");
            formatear(m3, "a");
            probarMascara(m3, "abc");
            probarMascara(m3, "abd");

            // --- Los numeros ---
            NumberFormatter n = new NumberFormatter(new DecimalFormat("#,##0.##",
                    new java.text.DecimalFormatSymbols(Locale.US)));
            linea("numero pref=" + n.valueToString(Double.valueOf(1234.5)));
            linea("numero entero=" + n.valueToString(Integer.valueOf(-42)));
            probarNumero(n, "1,234.5");
            probarNumero(n, "0");
            probarNumero(n, "hola");

            NumberFormatter ni = new NumberFormatter(new DecimalFormat("0",
                    new java.text.DecimalFormatSymbols(Locale.US)));
            ni.setValueClass(Integer.class);
            probarNumero(ni, "7");
            probarNumero(ni, "-7");

            NumberFormatter nr = new NumberFormatter(new DecimalFormat("0",
                    new java.text.DecimalFormatSymbols(Locale.US)));
            nr.setValueClass(Integer.class);
            nr.setMinimum(Integer.valueOf(0));
            nr.setMaximum(Integer.valueOf(10));
            linea("rango=[" + nr.getMinimum() + "," + nr.getMaximum() + "]");
            probarNumero(nr, "5");
            probarNumero(nr, "11");
            probarNumero(nr, "-1");

            // --- Las fechas ---
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            DateFormatter d = new DateFormatter(sdf);
            Date fija = new Date(1000000000000L);
            linea("fecha=" + d.valueToString(fija));
            Object leida = d.stringToValue("2001-09-09 01:46");
            linea("fecha leida=" + ((Date) leida).getTime());

            // --- El escritor ---
            PlainDocument pd = new PlainDocument();
            pd.insertString(0, "uno\ndos\ntres", null);
            StringWriter sw = new StringWriter();
            Escritor e1 = new Escritor(sw, pd);
            linea("escritor sangria=" + e1.sangria() + " ancho=" + e1.ancho());
            e1.setLineSeparator("\n");
            e1.write();
            volcar("plano", sw.toString());

            DefaultStyledDocument sd = new DefaultStyledDocument();
            SimpleAttributeSet neg = new SimpleAttributeSet();
            StyleConstants.setBold(neg, true);
            sd.insertString(0, "hola", null);
            sd.insertString(4, "MUNDO", neg);
            sd.insertString(9, "\nchau", null);
            StringWriter sw2 = new StringWriter();
            Escritor e2 = new Escritor(sw2, sd);
            e2.setLineSeparator("\n");
            e2.write();
            volcar("estilos", sw2.toString());

            // Un tramo del medio: se ve el recorte de las hojas.
            StringWriter sw3 = new StringWriter();
            Escritor e3 = new Escritor(sw3, sd);
            e3.setLineSeparator("\n");
            volcar("rango entero", "inicio=" + e3.getStartOffset() + " fin=" + e3.getEndOffset());
            e3.write();
            volcar("estilos otra vez", sw3.toString());
        } catch (ParseException pe) {
            linea("EXCEPCION ParseException " + pe.getMessage() + " en " + pe.getErrorOffset());
        } catch (BadLocationException be) {
            linea("EXCEPCION BadLocationException " + be.getMessage());
        } catch (IOException ioe) {
            linea("EXCEPCION IOException " + ioe.getMessage());
        }
        return 0;
    }
}
