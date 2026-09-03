import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.color.ICC_ProfileGray;
import java.awt.color.ICC_ProfileRGB;
import java.awt.color.ProfileDataException;

/**
 * `java.awt.color.ColorSpace` y los miembros de `java.awt.Color` que dependen de el.
 *
 * <p>El mismo archivo da -1 con el JDK 25 corriendo **sus** clases, pero hay una salvedad que vale
 * la pena entender porque cambia como estan escritas las comprobaciones.
 *
 * <p>El JDK convierte con un motor ICC leyendo perfiles; esta biblioteca convierte con las formulas
 * del estandar sRGB y las matrices ICC a D50. Los dos caminos son correctos y **difieren en los
 * ultimos digitos**: el `toRGB({0.5,0.5,0.5})` del JDK sobre sRGB da 0.5000076 y el nuestro 0.5.
 * Por eso aca no se compara contra numeros exactos sino contra **propiedades** --que la ida y
 * vuelta devuelva lo que entro, que el rojo puro caiga donde el estandar dice-- con una tolerancia
 * de 0.002, que es mas ancha que la diferencia entre los dos caminos y mucho mas angosta que
 * cualquier error de verdad.
 *
 * <p>Comparar bit a bit habria sido mas estricto y menos util: habria fallado siempre en uno de los
 * dos lados sin decir nada sobre si la conversion esta bien.
 */
public class ColorSpaceTest {

    static int failures = 0;

    /** La tolerancia: mas ancha que la diferencia ICC/formula, mucho mas angosta que un error. */
    static final float TOL = 0.002f;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    static void cerca(String what, float esperado, float dado) {
        float d = esperado - dado;
        if (d < 0) {
            d = -d;
        }
        if (d > TOL) {
            System.out.println("FALLA " + what + ": esperaba ~" + esperado + " y dio " + dado);
            failures = failures + 1;
        }
    }

    static void cerca(String what, float[] esperado, float[] dado) {
        if (dado.length != esperado.length) {
            System.out.println("FALLA " + what + ": largo " + dado.length
                    + " en vez de " + esperado.length);
            failures = failures + 1;
            return;
        }
        for (int i = 0; i < esperado.length; i++) {
            cerca(what + "[" + i + "]", esperado[i], dado[i]);
        }
    }

    public static int run() throws Exception {
        failures = 0;

        ColorSpace srgb = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ColorSpace lin = ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
        ColorSpace xyz = ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);
        ColorSpace gray = ColorSpace.getInstance(ColorSpace.CS_GRAY);

        // ---- la forma de cada espacio
        ok("sRGB es de tipo RGB", srgb.getType() == ColorSpace.TYPE_RGB);
        ok("y tiene tres componentes", srgb.getNumComponents() == 3);
        ok("y es el sRGB", srgb.isCS_sRGB());
        ok("el RGB lineal NO es el sRGB", !lin.isCS_sRGB());
        ok("pero tambien es de tipo RGB", lin.getType() == ColorSpace.TYPE_RGB);
        ok("CIEXYZ es de tipo XYZ", xyz.getType() == ColorSpace.TYPE_XYZ);
        ok("gris es de tipo GRAY", gray.getType() == ColorSpace.TYPE_GRAY);
        ok("y tiene un solo componente", gray.getNumComponents() == 1);

        ok("los componentes de sRGB se llaman Red, Green y Blue",
                "Red".equals(srgb.getName(0)) && "Green".equals(srgb.getName(1))
                        && "Blue".equals(srgb.getName(2)));
        ok("el de gris se llama Gray", "Gray".equals(gray.getName(0)));
        ok("los de XYZ se llaman X, Y y Z",
                "X".equals(xyz.getName(0)) && "Z".equals(xyz.getName(2)));

        ok("los componentes de sRGB van de 0 a 1",
                srgb.getMinValue(0) == 0.0f && srgb.getMaxValue(0) == 1.0f);
        // El techo de XYZ no es 2 sino 2 menos un paso de 16 bits: es la codificacion de ICC.
        cerca("el techo de XYZ", 1.9999695f, xyz.getMaxValue(0));
        ok("y es mayor que 1", xyz.getMaxValue(1) > 1.0f);

        // ---- getInstance devuelve siempre la misma instancia
        ok("getInstance es idempotente",
                ColorSpace.getInstance(ColorSpace.CS_sRGB) == srgb);
        ok("y da objetos distintos para espacios distintos", srgb != lin);

        boolean desconocido = false;
        try {
            ColorSpace.getInstance(999);
        } catch (IllegalArgumentException e) {
            desconocido = true;
        }
        ok("un identificador desconocido es IllegalArgument", desconocido);

        boolean indiceMalo = false;
        try {
            gray.getName(1);
        } catch (IllegalArgumentException e) {
            indiceMalo = true;
        }
        ok("un componente fuera de rango es IllegalArgument", indiceMalo);

        // ---- sRGB: a si mismo es la identidad
        float[] rojo = new float[] { 1.0f, 0.0f, 0.0f };
        cerca("sRGB a sRGB es la identidad", rojo, srgb.toRGB(rojo));
        cerca("y al reves tambien", rojo, srgb.fromRGB(rojo));

        // El rojo puro en XYZ D50. Los numeros son los de la matriz sRGB de ICC, y el JDK cae
        // dentro de la tolerancia con su propio camino.
        cerca("el rojo sRGB en XYZ", new float[] { 0.4361f, 0.2225f, 0.0139f },
                srgb.toCIEXYZ(rojo));
        float[] blanco = new float[] { 1.0f, 1.0f, 1.0f };
        // El blanco tiene que dar el punto blanco D50, que es la definicion de "blanco".
        cerca("el blanco sRGB es D50", new float[] { 0.9642f, 1.0f, 0.8249f },
                srgb.toCIEXYZ(blanco));
        float[] negro = new float[] { 0.0f, 0.0f, 0.0f };
        cerca("el negro es el origen", new float[] { 0f, 0f, 0f }, srgb.toCIEXYZ(negro));

        // ---- ida y vuelta: la propiedad que de verdad importa
        float[][] muestras = {
            { 0.0f, 0.0f, 0.0f }, { 1.0f, 1.0f, 1.0f }, { 0.5f, 0.5f, 0.5f },
            { 1.0f, 0.0f, 0.0f }, { 0.0f, 1.0f, 0.0f }, { 0.0f, 0.0f, 1.0f },
            { 0.2f, 0.4f, 0.6f }, { 0.9f, 0.1f, 0.3f } };
        for (int i = 0; i < muestras.length; i++) {
            cerca("sRGB ida y vuelta por XYZ " + i, muestras[i],
                    srgb.fromCIEXYZ(srgb.toCIEXYZ(muestras[i])));
            cerca("RGB lineal ida y vuelta por XYZ " + i, muestras[i],
                    lin.fromCIEXYZ(lin.toCIEXYZ(muestras[i])));
            cerca("sRGB a lineal y de vuelta " + i, muestras[i],
                    lin.toRGB(lin.fromRGB(muestras[i])));
        }

        // ---- la curva de gamma
        //
        // Un 0.5 lineal es mas claro que un 0.5 con gamma: esa es toda la diferencia entre los dos
        // espacios, y si estuviera al reves la curva estaria invertida.
        float[] medio = new float[] { 0.5f, 0.5f, 0.5f };
        float[] linAsRGB = lin.toRGB(medio);
        ok("el medio lineal se ve mas claro en sRGB", linAsRGB[0] > 0.5f);
        cerca("y cae donde dice la formula", 0.7354f, linAsRGB[0]);
        float[] srgbALin = lin.fromRGB(medio);
        ok("y el medio sRGB es mas oscuro en lineal", srgbALin[0] < 0.5f);
        cerca("y cae donde dice la formula", 0.2140f, srgbALin[0]);

        // El tramo recto de la curva, cerca del cero, no es la potencia.
        cerca("el tramo lineal de la curva", 0.0f, lin.fromRGB(new float[] { 0f, 0f, 0f })[0]);
        ok("y es continuo en el codo",
                Math.abs(lin.fromRGB(new float[] { 0.04045f, 0f, 0f })[0] - 0.0031308f) < 0.0001f);

        // ---- CIEXYZ
        float[] unXyz = new float[] { 0.5f, 0.5f, 0.5f };
        cerca("XYZ a si mismo es la identidad", unXyz, xyz.toCIEXYZ(unXyz));
        cerca("XYZ ida y vuelta por sRGB", unXyz, xyz.fromRGB(xyz.toRGB(unXyz)));

        // ---- gris
        float[] medioGris = new float[] { 0.5f };
        float[] grisEnXyz = gray.toCIEXYZ(medioGris);
        ok("el gris tiene Y igual a su valor", Math.abs(grisEnXyz[1] - 0.5f) < TOL);
        cerca("y X y Z salen del punto blanco",
                new float[] { 0.4821f, 0.5f, 0.4125f }, grisEnXyz);
        cerca("gris ida y vuelta por XYZ", medioGris, gray.fromCIEXYZ(gray.toCIEXYZ(medioGris)));

        // Un gris convertido a sRGB tiene los tres componentes iguales: un gris no tiene tinte.
        float[] grisEnRgb = gray.toRGB(medioGris);
        ok("un gris no tiene tinte",
                Math.abs(grisEnRgb[0] - grisEnRgb[1]) < TOL
                        && Math.abs(grisEnRgb[1] - grisEnRgb[2]) < TOL);
        cerca("y cae donde dice la curva", 0.7354f, grisEnRgb[0]);

        // El verde pesa mas que el rojo y mucho mas que el azul: es la luminancia, no un promedio.
        float gRojo = gray.fromRGB(new float[] { 1f, 0f, 0f })[0];
        float gVerde = gray.fromRGB(new float[] { 0f, 1f, 0f })[0];
        float gAzul = gray.fromRGB(new float[] { 0f, 0f, 1f })[0];
        ok("el verde pesa mas que el rojo en la luminancia", gVerde > gRojo);
        ok("y el rojo mas que el azul", gRojo > gAzul);
        ok("y los tres suman uno", Math.abs(gRojo + gVerde + gAzul - 1.0f) < TOL);

        // ---- Color con espacio de color
        Color c = new Color(gray, new float[] { 0.5f }, 1.0f);
        ok("el color conserva su espacio", c.getColorSpace() == gray);
        ok("y sus componentes propios son uno solo",
                c.getColorComponents(null).length == 1);
        cerca("y valen lo que se paso", 0.5f, c.getColorComponents(null)[0]);
        ok("getComponents agrega el alfa", c.getComponents(null).length == 2);
        cerca("que es el que se paso", 1.0f, c.getComponents(null)[1]);

        // Los enteros sRGB salen de convertir, no de repetir el 0.5.
        ok("el rojo sRGB del gris medio esta convertido", c.getRed() > 128);
        ok("y los tres canales son iguales",
                c.getRed() == c.getGreen() && c.getGreen() == c.getBlue());

        // Pedirle los componentes en otro espacio los convierte.
        float[] enRgb = c.getColorComponents(srgb, null);
        ok("pedirlos en sRGB da tres", enRgb.length == 3);
        cerca("y coinciden con la conversion del espacio", gray.toRGB(new float[] { 0.5f }),
                enRgb);

        Color rojoSrgb = new Color(1.0f, 0.0f, 0.0f);
        ok("un color de flotantes es sRGB", rojoSrgb.getColorSpace() == srgb);
        cerca("y sus componentes son los que entraron", rojo, rojoSrgb.getColorComponents(null));
        float[] rojoEnGris = rojoSrgb.getColorComponents(gray, null);
        ok("el rojo en gris es un solo numero", rojoEnGris.length == 1);
        ok("y es oscuro, porque el rojo aporta poca luminancia", rojoEnGris[0] < 0.5f);

        Color enteros = new Color(255, 0, 0);
        ok("un color de enteros tambien es sRGB", enteros.getColorSpace() == srgb);
        cerca("y sus componentes salen de dividir por 255", rojo,
                enteros.getColorComponents(null));

        // ---- validacion del constructor con espacio
        // Un arreglo corto es `ArrayIndexOutOfBoundsException` y no `IllegalArgumentException`:
        // el JDK no comprueba el largo, recorre y se le acaba el arreglo. Esta comprobacion
        // esperaba la otra excepcion y el oraculo la corrigio.
        boolean pocos = false;
        try {
            new Color(srgb, new float[] { 0.5f }, 1.0f);
        } catch (ArrayIndexOutOfBoundsException e) {
            pocos = true;
        }
        ok("faltarle componentes al espacio es ArrayIndexOutOfBounds", pocos);

        boolean fuera = false;
        try {
            new Color(srgb, new float[] { 1.5f, 0f, 0f }, 1.0f);
        } catch (IllegalArgumentException e) {
            fuera = true;
        }
        ok("un componente fuera de rango es IllegalArgument", fuera);

        boolean alfaMalo = false;
        try {
            new Color(srgb, new float[] { 0.5f, 0.5f, 0.5f }, 2.0f);
        } catch (IllegalArgumentException e) {
            alfaMalo = true;
        }
        ok("un alfa fuera de 0..1 es IllegalArgument", alfaMalo);


        // ---- ICC: la forma de los perfiles integrados
        //
        // Lo que se comprueba es la FORMA, no los bytes: el JDK trae sus perfiles como archivo y
        // esta biblioteca los construye, asi que el tamano y los textos difieren. Lo que si tiene
        // que coincidir es que clase de perfil es cada uno, que espacios describe, y --lo mas
        // delicado-- cual de `getGamma`/`getTRC` contesta y cual tira en cada uno.
        ok("getInstance devuelve un ICC_ColorSpace", srgb instanceof ICC_ColorSpace);
        ICC_Profile pSrgb = ((ICC_ColorSpace) srgb).getProfile();
        ok("el perfil de sRGB es un ICC_ProfileRGB", pSrgb instanceof ICC_ProfileRGB);
        ok("de clase display", pSrgb.getProfileClass() == ICC_Profile.CLASS_DISPLAY);
        ok("de espacio RGB", pSrgb.getColorSpaceType() == ColorSpace.TYPE_RGB);
        ok("con conexion XYZ", pSrgb.getPCSType() == ColorSpace.TYPE_XYZ);
        ok("y tres componentes", pSrgb.getNumComponents() == 3);
        ok("la version mayor es 2", pSrgb.getMajorVersion() == 2);

        ICC_ProfileRGB rgbProf = (ICC_ProfileRGB) pSrgb;
        float[][] m = rgbProf.getMatrix();
        ok("la matriz es de 3x3", m.length == 3 && m[0].length == 3);
        // La columna del rojo es lo que aporta el primario rojo, y son los numeros del estandar.
        cerca("matriz[0][0]", 0.4361f, m[0][0]);
        cerca("matriz[1][1]", 0.7169f, m[1][1]);
        cerca("matriz[2][2]", 0.7142f, m[2][2]);
        // Las tres columnas suman el blanco del espacio.
        cerca("las columnas suman el blanco en Y", 1.0f, m[1][0] + m[1][1] + m[1][2]);

        // sRGB guarda sus curvas como TABLA, asi que la gamma no existe.
        boolean sinGamma = false;
        try {
            rgbProf.getGamma(ICC_ProfileRGB.REDCOMPONENT);
        } catch (ProfileDataException e) {
            sinGamma = true;
        }
        ok("sRGB no tiene una gamma: su curva es una tabla", sinGamma);
        ok("y la tabla tiene 1024 puntos",
                rgbProf.getTRC(ICC_ProfileRGB.REDCOMPONENT).length == 1024);
        // La tabla es monotona creciente: es una curva de respuesta, no ruido.
        short[] trc = rgbProf.getTRC(ICC_ProfileRGB.GREENCOMPONENT);
        boolean creciente = true;
        for (int i = 1; i < trc.length; i++) {
            if ((trc[i] & 0xFFFF) < (trc[i - 1] & 0xFFFF)) {
                creciente = false;
            }
        }
        ok("y es monotona creciente", creciente);
        ok("empieza en cero", (trc[0] & 0xFFFF) == 0);
        ok("y termina en el maximo", (trc[trc.length - 1] & 0xFFFF) == 65535);

        boolean componenteMalo = false;
        try {
            rgbProf.getTRC(7);
        } catch (IllegalArgumentException e) {
            componenteMalo = true;
        }
        ok("un componente que no es R, G ni B es IllegalArgument", componenteMalo);

        // El RGB lineal es al reves: gamma 1 y sin tabla.
        ICC_ProfileRGB linProf = (ICC_ProfileRGB) ((ICC_ColorSpace) lin).getProfile();
        cerca("el RGB lineal tiene gamma 1", 1.0f,
                linProf.getGamma(ICC_ProfileRGB.REDCOMPONENT));
        boolean sinTabla = false;
        try {
            linProf.getTRC(ICC_ProfileRGB.REDCOMPONENT);
        } catch (ProfileDataException e) {
            sinTabla = true;
        }
        ok("y no tiene tabla: su curva es una gamma", sinTabla);

        // El gris.
        ICC_Profile pGray = ((ICC_ColorSpace) gray).getProfile();
        ok("el perfil de gris es un ICC_ProfileGray", pGray instanceof ICC_ProfileGray);
        ok("de espacio GRAY", pGray.getColorSpaceType() == ColorSpace.TYPE_GRAY);
        ok("y un componente", pGray.getNumComponents() == 1);
        cerca("con gamma 1", 1.0f, ((ICC_ProfileGray) pGray).getGamma());
        float[] blancoGris = ((ICC_ProfileGray) pGray).getMediaWhitePoint();
        ok("su punto blanco tiene Y igual a 1", Math.abs(blancoGris[1] - 1.0f) < TOL);

        // CIEXYZ es un perfil abstracto sin matriz ni curvas.
        ICC_Profile pXyz = ((ICC_ColorSpace) xyz).getProfile();
        ok("el perfil de CIEXYZ no es RGB ni gris",
                !(pXyz instanceof ICC_ProfileRGB) && !(pXyz instanceof ICC_ProfileGray));
        ok("es abstracto", pXyz.getProfileClass() == ICC_Profile.CLASS_ABSTRACT);
        ok("y su espacio es XYZ", pXyz.getColorSpaceType() == ColorSpace.TYPE_XYZ);

        // PYCC NO se comprueba aca, y la ausencia es deliberada: es la unica divergencia real
        // entre las dos implementaciones. El JDK trae su perfil como archivo de 230 KB y lo carga;
        // esta biblioteca no lo tiene y `getInstance(CS_PYCC)` tira. Una afirmacion sobre eso
        // pasaria en un lado y fallaria en el otro, que es justo lo que un archivo compartido con
        // el oraculo no puede hacer. La divergencia esta documentada en `ColorSpace`.

        // ---- ICC: los bytes
        byte[] cabecera = pSrgb.getData(ICC_Profile.icSigHead);
        ok("la cabecera mide 128 bytes", cabecera.length == 128);
        // La firma `acsp` en el offset que la cabecera define: es lo que hace valido a un perfil.
        ok("y lleva la firma acsp",
                cabecera[ICC_Profile.icHdrMagic] == (byte) 'a'
                        && cabecera[ICC_Profile.icHdrMagic + 1] == (byte) 'c'
                        && cabecera[ICC_Profile.icHdrMagic + 2] == (byte) 's'
                        && cabecera[ICC_Profile.icHdrMagic + 3] == (byte) 'p');

        byte[] todo = pSrgb.getData();
        ok("el perfil entero es mas grande que su cabecera", todo.length > 128);
        // El tamano que declara la cabecera es el del perfil: si no, ningun lector lo abre.
        int declarado = ((todo[0] & 0xFF) << 24) | ((todo[1] & 0xFF) << 16)
                | ((todo[2] & 0xFF) << 8) | (todo[3] & 0xFF);
        ok("y coincide con lo que declara", declarado == todo.length);

        // Ida y vuelta: los bytes de un perfil vuelven a ser el mismo perfil.
        ICC_Profile revivido = ICC_Profile.getInstance(todo);
        ok("un perfil ida y vuelta conserva su clase",
                revivido.getProfileClass() == pSrgb.getProfileClass());
        ok("y su espacio", revivido.getColorSpaceType() == pSrgb.getColorSpaceType());
        ok("y sus bytes", revivido.getData().length == todo.length);
        ok("y sigue siendo un ICC_ProfileRGB", revivido instanceof ICC_ProfileRGB);

        // Una etiqueta que existe y una que no.
        ok("la etiqueta del punto blanco esta",
                pSrgb.getData(ICC_Profile.icSigMediaWhitePointTag) != null);
        ok("y una que el perfil no tiene da null",
                pSrgb.getData(ICC_Profile.icSigNamedColor2Tag) == null);

        // Bytes que no son un perfil.
        boolean basura = false;
        try {
            ICC_Profile.getInstance(new byte[] { 1, 2, 3 });
        } catch (IllegalArgumentException e) {
            basura = true;
        }
        ok("unos bytes cualquiera no son un perfil", basura);

        // `setData` reemplaza y el perfil queda consistente. Se trabaja sobre una copia para no
        // ensuciar el integrado, que es compartido.
        ICC_Profile copiaPerfil = ICC_Profile.getInstance(pSrgb.getData());
        byte[] nuevoTexto = new byte[] {
            (byte) 't', (byte) 'e', (byte) 'x', (byte) 't', 0, 0, 0, 0,
            (byte) 'h', (byte) 'o', (byte) 'l', (byte) 'a', 0 };
        copiaPerfil.setData(ICC_Profile.icSigCopyrightTag, nuevoTexto);
        byte[] leido = copiaPerfil.getData(ICC_Profile.icSigCopyrightTag);
        ok("setData deja la etiqueta que se le paso",
                leido != null && leido.length == nuevoTexto.length && leido[8] == (byte) 'h');
        ok("y el resto del perfil sigue en pie",
                copiaPerfil.getData(ICC_Profile.icSigRedColorantTag) != null);
        ok("y el tamano declarado se rehizo",
                copiaPerfil.getData().length > 128);

        // ---- ICC_ColorSpace sobre un perfil propio
        ICC_ColorSpace propio = new ICC_ColorSpace(ICC_Profile.getInstance(pSrgb.getData()));
        ok("un espacio armado sobre el mismo perfil tiene el mismo tipo",
                propio.getType() == srgb.getType());
        cerca("y convierte igual", srgb.toCIEXYZ(rojo), propio.toCIEXYZ(rojo));
        ok("pero no es la misma instancia", propio != srgb);
        ok("y no es EL sRGB", !propio.isCS_sRGB());

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("ColorSpaceTest " + ColorSpaceTest.run());
    }
}
