import java.awt.AWTError;
import java.awt.AWTPermission;
import java.awt.AlphaComposite;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FontFormatException;
import java.awt.GridBagConstraints;
import java.awt.HeadlessException;
import java.awt.IllegalComponentStateException;
import java.awt.BufferCapabilities;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.DisplayMode;
import java.awt.ImageCapabilities;
import java.awt.Insets;
import java.awt.MenuShortcut;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Locale;

/**
 * Prueba de comportamiento de java.awt, escrita para correr **igual** en esta VM y en el JDK real.
 *
 * <p>{@code run()} devuelve -1 si pasaron todas o el indice de la primera que fallo. Un solo int
 * alcanza para comparar las dos VMs sin depender de que la salida por consola coincida caracter por
 * caracter, y sin que la comparacion dependa de la zona horaria ni del locale.
 *
 * <p>De java.awt aca solo hay clases de datos: geometria entera, margenes, constantes de
 * disposicion y el evento de 1.0. Nada de esto necesita pantalla, y es justamente por eso que se
 * pudo escribir. Lo que se apunta es lo que es facil errar sin que se note:
 *
 * <ul>
 * <li>los {@code toString()}, que el JDK especifica al caracter --incluido el de {@code Event},
 *     que omite los campos en cero;</li>
 * <li>el {@code hashCode()} de {@code Insets}, que no es el {@code 31*x+y} de siempre;</li>
 * <li>el redondeo de {@code Point.setLocation(double, double)}, que no es truncar;</li>
 * <li>los valores de las constantes, sobre todo las que rompen la serie: las teclas con caracter
 *     de {@code Event} y los anclajes de linea de base de {@code GridBagConstraints};</li>
 * <li>que los nombres obsoletos de {@code Rectangle} sean los que hacen el trabajo y los nuevos
 *     los que delegan, y no al reves.</li>
 * </ul>
 */
public class AwtTest {

    public static int run() {
        int i = 0;

        // --- Point: enteros, pero hereda Point2D ---
        Point p = new Point(3, 4);
        if (p.x != 3 || p.y != 4) return i; i++;                                       // 0
        if (new Point().x != 0 || new Point().y != 0) return i; i++;                   // 1
        if (!new Point(new Point(1, 2)).toString()
                .equals("java.awt.Point[x=1,y=2]")) return i; i++;                     // 2
        if (p.getX() != 3.0 || p.getY() != 4.0) return i; i++;                         // 3
        if (!p.toString().equals("java.awt.Point[x=3,y=4]")) return i; i++;            // 4
        // getLocation devuelve una copia, no this: si devolviera this, mover la copia moveria el
        // original y todo el codigo que guarda una posicion "de antes" quedaria roto.
        if (p.getLocation() == p) return i; i++;                                       // 5
        if (!p.getLocation().equals(p)) return i; i++;                                 // 6
        if (new Point(0, 0).distance(3, 4) != 5.0) return i; i++;                      // 7
        // Un Point y un Point2D.Double con el mismo valor son iguales en los dos sentidos.
        if (!p.equals(new Point2D.Double(3, 4))) return i; i++;                        // 8
        if (!new Point2D.Double(3, 4).equals(p)) return i; i++;                        // 9
        if (p.hashCode() != new Point2D.Double(3, 4).hashCode()) return i; i++;        // 10
        Point pm = new Point(3, 4);
        pm.translate(1, 1);
        if (pm.x != 4 || pm.y != 5) return i; i++;                                     // 11
        pm.move(-1, -1);
        if (pm.x != -1 || pm.y != -1) return i; i++;                                   // 12
        pm.setLocation(new Point(7, 8));
        if (pm.x != 7 || pm.y != 8) return i; i++;                                     // 13
        // El redondeo del setLocation en coma flotante es floor(v + 0.5), no un cast: para los
        // negativos truncar daria -2 y el JDK da -3.
        pm.setLocation(2.6, -2.6);
        if (pm.x != 3) return i; i++;                                                  // 14
        if (pm.y != -3) return i; i++;                                                 // 15
        pm.setLocation(2.4, -2.4);
        if (pm.x != 2 || pm.y != -2) return i; i++;                                    // 16
        pm.setLocation(-0.5, 0.5);
        if (pm.x != 0 || pm.y != 1) return i; i++;                                     // 17

        // --- Insets ---
        Insets in = new Insets(1, 2, 3, 4);
        if (in.top != 1 || in.left != 2 || in.bottom != 3 || in.right != 4) return i; i++; // 18
        if (!in.toString()
                .equals("java.awt.Insets[top=1,left=2,bottom=3,right=4]")) return i; i++; // 19
        // Cantor aplicado tres veces. El valor concreto importa: es API observable y una formula
        // "razonable" pero distinta romperia cualquier tabla hash serializada.
        if (in.hashCode() != 577) return i; i++;                                       // 20
        if (new Insets(0, 0, 0, 0).hashCode() != 0) return i; i++;                      // 21
        if (!in.equals(new Insets(1, 2, 3, 4))) return i; i++;                         // 22
        if (in.equals(new Insets(1, 2, 3, 5))) return i; i++;                          // 23
        if (in.equals("no soy un Insets")) return i; i++;                               // 24
        Object copia = in.clone();
        if (!(copia instanceof Insets)) return i; i++;                                 // 25
        if (copia == in) return i; i++;                                                // 26
        if (!in.equals(copia)) return i; i++;                                          // 27
        Insets iset = new Insets(1, 1, 1, 1);
        iset.set(5, 6, 7, 8);
        if (iset.top != 5 || iset.left != 6 || iset.bottom != 7 || iset.right != 8) return i; i++; // 28

        // --- Transparency: tres enteros, y el orden importa ---
        if (Transparency.OPAQUE != 1) return i; i++;                                   // 29
        if (Transparency.BITMASK != 2) return i; i++;                                  // 30
        if (Transparency.TRANSLUCENT != 3) return i; i++;                              // 31

        // --- GridBagConstraints: constantes y valores por defecto ---
        if (GridBagConstraints.RELATIVE != -1) return i; i++;                          // 32
        if (GridBagConstraints.REMAINDER != 0) return i; i++;                          // 33
        if (GridBagConstraints.NONE != 0) return i; i++;                               // 34
        if (GridBagConstraints.BOTH != 1) return i; i++;                               // 35
        if (GridBagConstraints.HORIZONTAL != 2) return i; i++;                         // 36
        if (GridBagConstraints.VERTICAL != 3) return i; i++;                           // 37
        if (GridBagConstraints.CENTER != 10) return i; i++;                            // 38
        if (GridBagConstraints.NORTH != 11) return i; i++;                             // 39
        if (GridBagConstraints.NORTHWEST != 18) return i; i++;                         // 40
        if (GridBagConstraints.PAGE_START != 19) return i; i++;                        // 41
        if (GridBagConstraints.LAST_LINE_END != 26) return i; i++;                     // 42
        // Aca la serie se rompe: los anclajes de linea de base saltan a multiplos de 256 para que
        // el layout los distinga de los otros con una comparacion de rango.
        if (GridBagConstraints.BASELINE != 256) return i; i++;                         // 43
        if (GridBagConstraints.BASELINE_LEADING != 512) return i; i++;                 // 44
        if (GridBagConstraints.BELOW_BASELINE_TRAILING != 2304) return i; i++;         // 45
        GridBagConstraints g = new GridBagConstraints();
        if (g.gridx != GridBagConstraints.RELATIVE) return i; i++;                     // 46
        if (g.gridy != GridBagConstraints.RELATIVE) return i; i++;                     // 47
        if (g.gridwidth != 1 || g.gridheight != 1) return i; i++;                      // 48
        if (g.weightx != 0.0 || g.weighty != 0.0) return i; i++;                       // 49
        if (g.anchor != GridBagConstraints.CENTER) return i; i++;                      // 50
        if (g.fill != GridBagConstraints.NONE) return i; i++;                          // 51
        if (g.ipadx != 0 || g.ipady != 0) return i; i++;                               // 52
        if (!g.insets.equals(new Insets(0, 0, 0, 0))) return i; i++;                   // 53
        GridBagConstraints g2 = new GridBagConstraints(1, 2, 3, 4, 0.5, 0.25,
                GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 5, 6);
        if (g2.gridx != 1 || g2.gridy != 2) return i; i++;                             // 54
        if (g2.gridwidth != 3 || g2.gridheight != 4) return i; i++;                    // 55
        if (g2.weightx != 0.5 || g2.weighty != 0.25) return i; i++;                    // 56
        if (g2.anchor != GridBagConstraints.NORTH) return i; i++;                      // 57
        if (g2.fill != GridBagConstraints.BOTH) return i; i++;                         // 58
        if (g2.ipadx != 5 || g2.ipady != 6) return i; i++;                             // 59
        // El clone copia los Insets aparte: compartirlos haria que tocar la copia toque el original.
        GridBagConstraints gc = (GridBagConstraints) g2.clone();
        if (gc.insets == g2.insets) return i; i++;                                     // 60
        if (!gc.insets.equals(g2.insets)) return i; i++;                               // 61
        gc.insets.top = 99;
        if (g2.insets.top != 1) return i; i++;                                         // 62

        // --- Event: constantes ---
        if (Event.SHIFT_MASK != 1) return i; i++;                                      // 63
        if (Event.CTRL_MASK != 2) return i; i++;                                       // 64
        if (Event.META_MASK != 4) return i; i++;                                       // 65
        if (Event.ALT_MASK != 8) return i; i++;                                        // 66
        if (Event.HOME != 1000) return i; i++;                                         // 67
        if (Event.INSERT != 1025) return i; i++;                                       // 68
        if (Event.F1 != 1008 || Event.F12 != 1019) return i; i++;                      // 69
        // Estas cinco rompen la serie: son el ASCII del caracter, no un codigo de accion.
        if (Event.ENTER != 10) return i; i++;                                          // 70
        if (Event.BACK_SPACE != 8) return i; i++;                                      // 71
        if (Event.TAB != 9) return i; i++;                                             // 72
        if (Event.ESCAPE != 27) return i; i++;                                         // 73
        if (Event.DELETE != 127) return i; i++;                                        // 74
        if (Event.WINDOW_DESTROY != 201 || Event.WINDOW_MOVED != 205) return i; i++;   // 75
        if (Event.KEY_PRESS != 401 || Event.KEY_ACTION_RELEASE != 404) return i; i++;  // 76
        if (Event.MOUSE_DOWN != 501 || Event.MOUSE_DRAG != 506) return i; i++;         // 77
        if (Event.SCROLL_LINE_UP != 601 || Event.SCROLL_END != 607) return i; i++;     // 78
        if (Event.LIST_SELECT != 701 || Event.LIST_DESELECT != 702) return i; i++;     // 79
        if (Event.ACTION_EVENT != 1001 || Event.LOST_FOCUS != 1005) return i; i++;     // 80
        // La colision heredada: ACTION_EVENT y PGUP valen lo mismo. No se pisan porque uno vive en
        // el campo id y el otro en key, pero conviene dejarla escrita para que nadie la "corrija".
        if (Event.ACTION_EVENT != Event.END) return i; i++;                            // 81

        // --- Event: campos y aritmetica de bits ---
        Event e = new Event("t", 5L, Event.MOUSE_DOWN, 1, 2, 0, Event.CTRL_MASK | Event.META_MASK);
        if (!"t".equals(e.target)) return i; i++;                                      // 82
        if (e.when != 5L) return i; i++;                                               // 83
        if (e.id != Event.MOUSE_DOWN) return i; i++;                                   // 84
        if (e.x != 1 || e.y != 2) return i; i++;                                       // 85
        if (e.key != 0) return i; i++;                                                 // 86
        if (e.clickCount != 0) return i; i++;                                          // 87
        if (e.arg != null || e.evt != null) return i; i++;                             // 88
        if (e.shiftDown()) return i; i++;                                              // 89
        if (!e.controlDown()) return i; i++;                                           // 90
        if (!e.metaDown()) return i; i++;                                              // 91
        // El toString omite key porque vale cero, y omite arg porque es null.
        if (!e.toString()
                .equals("java.awt.Event[id=501,x=1,y=2,control,meta,target=t]")) return i; i++; // 92
        e.translate(10, 20);
        if (e.x != 11 || e.y != 22) return i; i++;                                     // 93
        Event e2 = new Event(null, 0L, Event.KEY_PRESS, 0, 0, Event.F1, Event.ALT_MASK);
        // ALT_MASK no tiene consultor propio: los tres que hay dan false.
        if (e2.shiftDown() || e2.controlDown() || e2.metaDown()) return i; i++;        // 94
        if (!e2.toString().equals("java.awt.Event[id=401,x=0,y=0,key=1008]")) return i; i++; // 95
        Event e3 = new Event("t", Event.ACTION_EVENT, "arg");
        if (e3.when != 0L || e3.x != 0 || e3.y != 0 || e3.modifiers != 0) return i; i++; // 96
        if (!e3.toString()
                .equals("java.awt.Event[id=1001,x=0,y=0,target=t,arg=arg]")) return i; i++; // 97
        Event e4 = new Event("t", 1L, Event.MOUSE_DOWN, 3, 4, Event.ENTER,
                Event.SHIFT_MASK, "z");
        if (!e4.toString()
                .equals("java.awt.Event[id=501,x=3,y=4,key=10,shift,target=t,arg=z]")) return i; i++; // 98

        // --- Rectangle: lo que Point acaba de desbloquear ---
        if (!new Rectangle(new Point(1, 2)).toString()
                .equals("java.awt.Rectangle[x=1,y=2,width=0,height=0]")) return i; i++; // 99
        // Un Rectangle construido solo con un Point es vacio, no un punto de area cero "presente".
        if (!new Rectangle(new Point(1, 2)).isEmpty()) return i; i++;                  // 100
        if (!new Rectangle(new Point(1, 2), new Dimension(3, 4)).toString()
                .equals("java.awt.Rectangle[x=1,y=2,width=3,height=4]")) return i; i++; // 101
        Rectangle r = new Rectangle(0, 0, 10, 10);
        if (!r.getLocation().equals(new Point(0, 0))) return i; i++;                   // 102
        r.setLocation(new Point(5, 6));
        if (r.x != 5 || r.y != 6 || r.width != 10 || r.height != 10) return i; i++;    // 103
        if (!r.contains(new Point(5, 6))) return i; i++;                               // 104
        if (r.contains(new Point(4, 6))) return i; i++;                                // 105
        Rectangle r2 = new Rectangle(0, 0, 2, 2);
        r2.add(new Point(5, 5));
        if (!r2.toString()
                .equals("java.awt.Rectangle[x=0,y=0,width=5,height=5]")) return i; i++; // 106

        // --- Rectangle: los cuatro nombres de 1.0 ---
        Rectangle r3 = new Rectangle(0, 0, 10, 10);
        r3.reshape(1, 2, 3, 4);
        if (r3.x != 1 || r3.y != 2 || r3.width != 3 || r3.height != 4) return i; i++;  // 107
        r3.resize(7, 8);
        if (r3.x != 1 || r3.y != 2 || r3.width != 7 || r3.height != 8) return i; i++;  // 108
        r3.move(9, 9);
        if (r3.x != 9 || r3.y != 9 || r3.width != 7 || r3.height != 8) return i; i++;  // 109
        Rectangle r4 = new Rectangle(0, 0, 10, 10);
        if (!r4.inside(5, 5)) return i; i++;                                           // 110
        // El borde de arriba/izquierda entra, el de abajo/derecha no: el rectangulo es
        // semiabierto y por eso dos rectangulos pegados no comparten ningun punto.
        if (!r4.inside(0, 0)) return i; i++;                                           // 111
        if (r4.inside(10, 10)) return i; i++;                                          // 112
        if (r4.inside(-1, 5)) return i; i++;                                           // 113
        if (new Rectangle(0, 0, 0, 0).inside(0, 0)) return i; i++;                     // 114
        // Y los nombres nuevos tienen que pasar por los viejos, no duplicar el codigo: una
        // subclase de la epoca redefinia reshape y esperaba ver ahi las llamadas a setBounds.
        Rectangle r5 = new Rectangle();
        r5.setBounds(1, 2, 3, 4);
        if (r5.x != 1 || r5.width != 3) return i; i++;                                 // 115
        r5.setSize(5, 6);
        if (r5.width != 5 || r5.height != 6) return i; i++;                            // 116
        r5.setLocation(7, 8);
        if (r5.x != 7 || r5.y != 8) return i; i++;                                     // 117
        if (!r5.contains(7, 8)) return i; i++;                                         // 118

        // --- las excepciones ---
        if (!new AWTException("x").getMessage().equals("x")) return i; i++;            // 119
        if (!(new AWTException("x") instanceof Exception)) return i; i++;              // 120
        if (!new AWTError("x").getMessage().equals("x")) return i; i++;                // 121
        if (!(new AWTError("x") instanceof Error)) return i; i++;                      // 122
        if (!new FontFormatException("x").getMessage().equals("x")) return i; i++;     // 123
        if (new HeadlessException().getMessage() != null) return i; i++;               // 124
        if (!new HeadlessException("x").getMessage().equals("x")) return i; i++;       // 125
        // Es una UnsupportedOperationException: se puede atrapar sin nombrar java.awt.
        if (!(new HeadlessException() instanceof UnsupportedOperationException)) return i; i++; // 126
        if (new IllegalComponentStateException().getMessage() != null) return i; i++;  // 127
        if (!new IllegalComponentStateException("x").getMessage().equals("x")) return i; i++; // 128
        if (!(new IllegalComponentStateException() instanceof IllegalStateException)) return i; i++; // 129


        // ------------------------------------------------------------------------------------
        // Segunda tanda: color, mezcla y preferencias de dibujo.
        // ------------------------------------------------------------------------------------

        // --- Color: los 32 bits ---
        if (Color.RED.getRGB() != 0xffff0000) return i; i++;                           // 130
        // El hashCode ES el valor empaquetado, no un derivado: es API observable.
        if (Color.RED.hashCode() != Color.RED.getRGB()) return i; i++;                 // 131
        if (new Color(1, 2, 3).getRGB() != 0xff010203) return i; i++;                  // 132
        // El constructor de un solo int fuerza opaco y descarta lo que venga en el alfa.
        if (new Color(0x010203).getRGB() != new Color(1, 2, 3).getRGB()) return i; i++; // 133
        if (new Color(0x80010203, true).getAlpha() != 128) return i; i++;              // 134
        if (new Color(0x80010203, false).getAlpha() != 255) return i; i++;             // 135
        Color c = new Color(1, 2, 3, 4);
        if (c.getRed() != 1 || c.getGreen() != 2 || c.getBlue() != 3) return i; i++;   // 136
        if (c.getAlpha() != 4) return i; i++;                                          // 137
        // El alfa no sale en el toString, ni siquiera cuando no es 255.
        if (!c.toString().equals("java.awt.Color[r=1,g=2,b=3]")) return i; i++;        // 138
        if (!new Color(1, 2, 3, 4).equals(new Color(1, 2, 3, 4))) return i; i++;       // 139
        // Pero si cuenta para el equals, porque getRGB lo incluye.
        if (new Color(1, 2, 3, 4).equals(new Color(1, 2, 3, 5))) return i; i++;        // 140
        if (Color.RED.equals("no soy un Color")) return i; i++;                        // 141

        // --- Color: las constantes que no son las obvias ---
        if (Color.WHITE != Color.white) return i; i++;                                 // 142
        if (Color.GRAY.getRed() != 128) return i; i++;                                 // 143
        if (Color.LIGHT_GRAY.getRed() != 192) return i; i++;                           // 144
        if (Color.DARK_GRAY.getRed() != 64) return i; i++;                             // 145
        // pink no es un rojo palido: el azul acompania al verde para que no vire a naranja.
        if (Color.pink.getRed() != 255 || Color.pink.getGreen() != 175) return i; i++; // 146
        if (Color.pink.getBlue() != 175) return i; i++;                                // 147
        // Y el naranja del AWT es mas amarillo que el "orange" de la web (255,165,0).
        if (Color.orange.getGreen() != 200) return i; i++;                             // 148
        if (Color.orange.getBlue() != 0) return i; i++;                                // 149

        // --- Color: aclarar y oscurecer ---
        if (Color.gray.brighter().getRed() != 182) return i; i++;                      // 150
        if (Color.gray.darker().getRed() != 89) return i; i++;                         // 151
        // El piso: sin el, el negro nunca aclararia porque cero dividido por 0.7 sigue siendo cero.
        if (Color.black.brighter().getRed() != 3) return i; i++;                       // 152
        if (new Color(1, 1, 1).brighter().getRed() != 4) return i; i++;                // 153
        // Oscurecer no necesita piso y el blanco ya no puede aclararse mas.
        if (Color.white.brighter().getRed() != 255) return i; i++;                     // 154
        if (Color.black.darker().getRed() != 0) return i; i++;                         // 155
        if (new Color(10, 10, 10, 7).brighter().getAlpha() != 7) return i; i++;        // 156
        if (new Color(10, 10, 10, 7).darker().getAlpha() != 7) return i; i++;          // 157

        // --- Color: transparencia ---
        if (Color.RED.getTransparency() != Transparency.OPAQUE) return i; i++;         // 158
        // Alfa cero da BITMASK y no TRANSLUCENT: es invisible, no medio visible.
        if (new Color(1, 2, 3, 0).getTransparency() != Transparency.BITMASK) return i; i++; // 159
        if (new Color(1, 2, 3, 128).getTransparency()
                != Transparency.TRANSLUCENT) return i; i++;                            // 160
        if (!(Color.RED instanceof Transparency)) return i; i++;                       // 161

        // --- Color: decode y getColor ---
        if (!Color.decode("#FF0000").equals(Color.RED)) return i; i++;                 // 162
        if (!Color.decode("0x00FF00").equals(Color.GREEN)) return i; i++;              // 163
        // Un decimal pelado tambien vale: 255 son los 8 bits de abajo, o sea el azul.
        if (!Color.decode("255").equals(Color.BLUE)) return i; i++;                    // 164
        if (!decodeTiraNFE("zz")) return i; i++;                                       // 165
        // Una propiedad que no existe: la version sin valor por defecto devuelve null.
        if (Color.getColor("kaji.propiedad.que.no.existe") != null) return i; i++;     // 166
        if (!Color.getColor("kaji.propiedad.que.no.existe", Color.BLUE)
                .equals(Color.BLUE)) return i; i++;                                    // 167
        if (!Color.getColor("kaji.propiedad.que.no.existe", 255)
                .equals(Color.BLUE)) return i; i++;                                    // 168

        // --- Color: los rangos ---
        // El mensaje enumera todos los canales malos, no el primero.
        if (!colorTiraIAE(256, 0, 0)) return i; i++;                                   // 169
        if (!colorTiraIAE(-1, 0, 0)) return i; i++;                                    // 170
        if (!colorTiraIAE(0, 300, 0)) return i; i++;                                   // 171
        if (!colorTiraIAE(0, 0, -1)) return i; i++;                                    // 172
        if (colorTiraIAE(0, 0, 0)) return i; i++;                                      // 173
        if (colorTiraIAE(255, 255, 255)) return i; i++;                                // 174
        if (!mensajeDeRango(-1, 0, 300)
                .equals("Color parameter outside of expected range: Red Blue")) return i; i++; // 175
        if (!alfaTiraIAE(256)) return i; i++;                                          // 176
        if (!mensajeDeAlfa(256)
                .equals("Color parameter outside of expected range: Alpha")) return i; i++; // 177
        if (!floatTiraIAE(1.5f)) return i; i++;                                        // 178
        if (floatTiraIAE(1.0f)) return i; i++;                                         // 179

        // --- Color: HSB ---
        if (Color.HSBtoRGB(0f, 1f, 1f) != 0xffff0000) return i; i++;                   // 180
        if (Color.HSBtoRGB(1f / 3f, 1f, 1f) != 0xff00ff00) return i; i++;              // 181
        if (Color.HSBtoRGB(2f / 3f, 1f, 1f) != 0xff0000ff) return i; i++;              // 182
        // Saturacion cero: gris, y el tono no importa.
        if (Color.HSBtoRGB(0f, 0f, 0.5f) != 0xff808080) return i; i++;                 // 183
        if (Color.HSBtoRGB(0.7f, 0f, 0.5f) != Color.HSBtoRGB(0f, 0f, 0.5f)) return i; i++; // 184
        // El tono es un angulo: se toma modulo 1, asi que 1.25 y 0.25 son el mismo color.
        if (Color.HSBtoRGB(1.25f, 1f, 1f) != Color.HSBtoRGB(0.25f, 1f, 1f)) return i; i++; // 185
        if (Color.HSBtoRGB(-0.75f, 1f, 1f) != Color.HSBtoRGB(0.25f, 1f, 1f)) return i; i++; // 186
        float[] hsb = Color.RGBtoHSB(255, 0, 0, null);
        if (hsb.length != 3) return i; i++;                                            // 187
        if (hsb[0] != 0.0f || hsb[1] != 1.0f || hsb[2] != 1.0f) return i; i++;         // 188
        float[] hsb2 = Color.RGBtoHSB(0, 0, 255, null);
        if (hsb2[0] != 4.0f / 6.0f) return i; i++;                                     // 189
        float[] hsb3 = Color.RGBtoHSB(128, 128, 128, null);
        // Un gris no tiene tono: se devuelve 0 por convencion, no un valor inventado.
        if (hsb3[0] != 0.0f || hsb3[1] != 0.0f) return i; i++;                         // 190
        if (hsb3[2] != 128.0f / 255.0f) return i; i++;                                 // 191
        float[] hsb4 = Color.RGBtoHSB(0, 0, 0, null);
        if (hsb4[0] != 0.0f || hsb4[1] != 0.0f || hsb4[2] != 0.0f) return i; i++;      // 192
        float[] dadoHsb = new float[3];
        if (Color.RGBtoHSB(1, 2, 3, dadoHsb) != dadoHsb) return i; i++;                // 193
        if (!Color.getHSBColor(0f, 1f, 1f).equals(Color.RED)) return i; i++;           // 194

        // --- Color: los componentes en coma flotante ---
        // Un color construido con flotantes los devuelve intactos; pasar por enteros los perderia.
        float[] cf = new Color(0.1f, 0.2f, 0.3f).getRGBColorComponents(null);
        if (cf.length != 3) return i; i++;                                             // 195
        if (cf[0] != 0.1f || cf[1] != 0.2f || cf[2] != 0.3f) return i; i++;            // 196
        // Y el mismo color, visto como entero, esta cuantizado: 0.1*255+0.5 = 26.
        if (new Color(0.1f, 0.2f, 0.3f).getRed() != 26) return i; i++;                 // 197
        // Uno construido con enteros devuelve la division, que para el es exacta.
        float[] ci = new Color(255, 0, 0).getRGBColorComponents(null);
        if (ci[0] != 1.0f || ci[1] != 0.0f || ci[2] != 0.0f) return i; i++;            // 198
        float[] ca = new Color(255, 0, 0).getRGBComponents(null);
        if (ca.length != 4) return i; i++;                                             // 199
        if (ca[3] != 1.0f) return i; i++;                                              // 200
        // Con arreglo dado se escribe ahi y se devuelve ese mismo, no una copia.
        float[] dado4 = new float[4];
        if (new Color(1, 2, 3).getRGBComponents(dado4) != dado4) return i; i++;        // 201
        // getComponents y getColorComponents: como todo Color de aca es sRGB, son los de arriba.
        float[] cc = new Color(0.1f, 0.2f, 0.3f).getComponents(null);
        if (cc.length != 4 || cc[0] != 0.1f || cc[3] != 1.0f) return i; i++;           // 202
        if (new Color(1, 2, 3).getColorComponents(null).length != 3) return i; i++;    // 203

        // --- AlphaComposite: las doce reglas ---
        if (AlphaComposite.CLEAR != 1) return i; i++;                                  // 204
        if (AlphaComposite.SRC != 2) return i; i++;                                    // 205
        if (AlphaComposite.SRC_OVER != 3) return i; i++;                               // 206
        if (AlphaComposite.DST_OVER != 4) return i; i++;                               // 207
        if (AlphaComposite.SRC_IN != 5) return i; i++;                                 // 208
        if (AlphaComposite.DST_IN != 6) return i; i++;                                 // 209
        if (AlphaComposite.SRC_OUT != 7) return i; i++;                                // 210
        if (AlphaComposite.DST_OUT != 8) return i; i++;                                // 211
        // El salto: DST se agrego en 1.4 y se numero al final, no al lado de SRC.
        if (AlphaComposite.DST != 9) return i; i++;                                    // 212
        if (AlphaComposite.SRC_ATOP != 10) return i; i++;                              // 213
        if (AlphaComposite.DST_ATOP != 11) return i; i++;                              // 214
        if (AlphaComposite.XOR != 12) return i; i++;                                   // 215
        if (AlphaComposite.SrcOver.getRule() != AlphaComposite.SRC_OVER) return i; i++; // 216
        if (AlphaComposite.SrcOver.getAlpha() != 1.0f) return i; i++;                  // 217
        if (AlphaComposite.Dst.getRule() != AlphaComposite.DST) return i; i++;         // 218
        // Con alfa 1 se devuelve la constante compartida, no un objeto nuevo.
        if (AlphaComposite.getInstance(AlphaComposite.SRC_OVER)
                != AlphaComposite.SrcOver) return i; i++;                              // 219
        if (AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)
                != AlphaComposite.SrcOver) return i; i++;                              // 220
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f);
        if (ac.getRule() != AlphaComposite.SRC || ac.getAlpha() != 0.5f) return i; i++; // 221
        if (ac == AlphaComposite.Src) return i; i++;                                   // 222
        // El hashCode mezcla el alfa y la regla, en ese orden.
        if (ac.hashCode() != Float.floatToIntBits(0.5f) * 31 + AlphaComposite.SRC) return i; i++; // 223
        if (AlphaComposite.SrcOver.hashCode()
                != Float.floatToIntBits(1.0f) * 31 + AlphaComposite.SRC_OVER) return i; i++; // 224
        if (!ac.equals(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f))) return i; i++; // 225
        if (ac.equals(AlphaComposite.getInstance(AlphaComposite.DST, 0.5f))) return i; i++; // 226
        if (ac.equals(AlphaComposite.getInstance(AlphaComposite.SRC, 0.25f))) return i; i++; // 227
        if (ac.equals("no soy un AlphaComposite")) return i; i++;                      // 228
        // derive: si no cambia nada devuelve this, no una copia.
        if (AlphaComposite.SrcOver.derive(AlphaComposite.SRC_OVER)
                != AlphaComposite.SrcOver) return i; i++;                              // 229
        if (ac.derive(0.5f) != ac) return i; i++;                                      // 230
        if (ac.derive(0.25f).getRule() != AlphaComposite.SRC) return i; i++;           // 231
        if (ac.derive(0.25f).getAlpha() != 0.25f) return i; i++;                       // 232
        if (ac.derive(AlphaComposite.XOR).getAlpha() != 0.5f) return i; i++;           // 233
        if (ac.derive(AlphaComposite.XOR).getRule() != AlphaComposite.XOR) return i; i++; // 234
        if (!acTiraIAE(0, 1.0f)) return i; i++;                                        // 235
        if (!acTiraIAE(13, 1.0f)) return i; i++;                                       // 236
        if (!acTiraIAE(AlphaComposite.SRC, -0.1f)) return i; i++;                      // 237
        if (!acTiraIAE(AlphaComposite.SRC, 1.5f)) return i; i++;                       // 238
        // NaN tambien: la validacion esta escrita en positivo justamente para atraparlo.
        if (!acTiraIAE(AlphaComposite.SRC, Float.NaN)) return i; i++;                  // 239
        if (acTiraIAE(AlphaComposite.SRC, 0.0f)) return i; i++;                        // 240

        // --- RenderingHints ---
        RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        if (rh.size() != 1) return i; i++;                                             // 241
        if (rh.get(RenderingHints.KEY_ANTIALIASING)
                != RenderingHints.VALUE_ANTIALIAS_ON) return i; i++;                   // 242
        if (!rh.containsKey(RenderingHints.KEY_ANTIALIASING)) return i; i++;           // 243
        if (rh.containsKey(RenderingHints.KEY_RENDERING)) return i; i++;               // 244
        if (!rh.containsValue(RenderingHints.VALUE_ANTIALIAS_ON)) return i; i++;       // 245
        if (rh.isEmpty()) return i; i++;                                               // 246
        // Un null en el constructor de Map da un conjunto vacio, no una excepcion.
        if (!new RenderingHints(null).isEmpty()) return i; i++;                        // 247

        // La validacion: cada clave sabe que valores acepta y el resto se rechaza al guardar.
        if (!RenderingHints.KEY_ANTIALIASING
                .isCompatibleValue(RenderingHints.VALUE_ANTIALIAS_ON)) return i; i++;  // 248
        if (RenderingHints.KEY_ANTIALIASING
                .isCompatibleValue(RenderingHints.VALUE_RENDER_QUALITY)) return i; i++; // 249
        if (RenderingHints.KEY_ANTIALIASING.isCompatibleValue("si")) return i; i++;    // 250
        if (RenderingHints.KEY_ANTIALIASING.isCompatibleValue(null)) return i; i++;    // 251
        if (!putTiraIAE(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_RENDER_QUALITY)) return i; i++;                   // 252
        if (!putTiraIAE(RenderingHints.KEY_ANTIALIASING, "si")) return i; i++;         // 253
        // Una clave que no es una Key no da IAE sino ClassCastException: no es un valor malo,
        // es un tipo que no entra.
        if (!putTiraCCE()) return i; i++;                                              // 254

        // La unica clave con valor numerico: un entero entre 100 y 250, cerrado.
        if (!RenderingHints.KEY_TEXT_LCD_CONTRAST
                .isCompatibleValue(Integer.valueOf(100))) return i; i++;               // 255
        if (!RenderingHints.KEY_TEXT_LCD_CONTRAST
                .isCompatibleValue(Integer.valueOf(250))) return i; i++;               // 256
        if (RenderingHints.KEY_TEXT_LCD_CONTRAST
                .isCompatibleValue(Integer.valueOf(99))) return i; i++;                // 257
        if (RenderingHints.KEY_TEXT_LCD_CONTRAST
                .isCompatibleValue(Integer.valueOf(251))) return i; i++;               // 258
        if (RenderingHints.KEY_TEXT_LCD_CONTRAST.isCompatibleValue("x")) return i; i++; // 259

        // Una clave es ella misma y nada mas: el equals es identidad y es final.
        if (!RenderingHints.KEY_ANTIALIASING
                .equals(RenderingHints.KEY_ANTIALIASING)) return i; i++;               // 260
        if (RenderingHints.KEY_ANTIALIASING
                .equals(RenderingHints.KEY_RENDERING)) return i; i++;                  // 261

        RenderingHints rh2 = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        if (!rh.equals(rh2)) return i; i++;                                            // 262
        if (rh.hashCode() != rh2.hashCode()) return i; i++;                            // 263
        // Y es igual a un Map cualquiera con el mismo contenido, no solo a otro RenderingHints.
        HashMap<Object, Object> hm = new HashMap<Object, Object>();
        hm.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (!rh.equals(hm)) return i; i++;                                             // 264
        if (rh.equals("no soy un Map")) return i; i++;                                 // 265

        RenderingHints clon = (RenderingHints) rh.clone();
        clon.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (rh.size() != 1) return i; i++;                                             // 266
        if (clon.size() != 2) return i; i++;                                           // 267
        RenderingHints acc = new RenderingHints(null);
        acc.add(clon);
        if (acc.size() != 2) return i; i++;                                            // 268
        if (acc.remove(RenderingHints.KEY_RENDERING)
                != RenderingHints.VALUE_RENDER_QUALITY) return i; i++;                 // 269
        if (acc.size() != 1) return i; i++;                                            // 270
        if (acc.keySet().size() != 1) return i; i++;                                   // 271
        if (acc.values().size() != 1) return i; i++;                                   // 272
        if (acc.entrySet().size() != 1) return i; i++;                                 // 273
        acc.clear();
        if (!acc.isEmpty()) return i; i++;                                             // 274
        RenderingHints pa = new RenderingHints(null);
        pa.putAll(hm);
        if (pa.size() != 1) return i; i++;                                             // 275
        // putAll de un Map cualquiera valida cada par: la basura no entra.
        if (!putAllTiraCCE()) return i; i++;                                           // 276


        // ------------------------------------------------------------------------------------
        // Tercera tanda: poligono, cursor, sentido de lectura y las capacidades.
        // ------------------------------------------------------------------------------------

        // --- Polygon: caja envolvente y cache ---
        Polygon cuadro = new Polygon(new int[] {0, 10, 10, 0}, new int[] {0, 0, 10, 10}, 4);
        if (!cuadro.getBounds().equals(new Rectangle(0, 0, 10, 10))) return i; i++;    // 277
        if (!cuadro.getBoundingBox().equals(new Rectangle(0, 0, 10, 10))) return i; i++; // 278
        // getBounds2D de un poligono entero devuelve el mismo rectangulo entero.
        if (!cuadro.getBounds2D().equals(new Rectangle(0, 0, 10, 10))) return i; i++;  // 279
        // Devuelve una copia: si devolviera el cache, tocarla corromperia el poligono.
        if (cuadro.getBounds() == cuadro.getBounds()) return i; i++;                   // 280
        if (cuadro.npoints != 4) return i; i++;                                        // 281
        // El poligono copia los arreglos que le pasan; no se queda con los del que llama.
        int[] xs = {0, 1, 2};
        Polygon copiado = new Polygon(xs, new int[] {0, 1, 2}, 3);
        xs[0] = 99;
        if (copiado.xpoints[0] != 0) return i; i++;                                    // 282
        Polygon vacio = new Polygon();
        if (vacio.npoints != 0) return i; i++;                                         // 283
        if (!vacio.getBounds().equals(new Rectangle(0, 0, 0, 0))) return i; i++;       // 284
        if (vacio.contains(0, 0)) return i; i++;                                       // 285
        if (vacio.intersects(0, 0, 1, 1)) return i; i++;                               // 286
        // Los arreglos arrancan en 4 y se duplican; tres puntos entran sin crecer.
        Polygon armado = new Polygon();
        armado.addPoint(1, 1);
        armado.addPoint(5, 1);
        armado.addPoint(5, 5);
        if (armado.npoints != 3) return i; i++;                                        // 287
        if (armado.xpoints.length != 4) return i; i++;                                 // 288
        if (!armado.getBounds().equals(new Rectangle(1, 1, 4, 4))) return i; i++;      // 289
        armado.translate(10, 10);
        if (armado.xpoints[0] != 11) return i; i++;                                    // 290
        if (!armado.getBounds().equals(new Rectangle(11, 11, 4, 4))) return i; i++;    // 291
        armado.reset();
        if (armado.npoints != 0) return i; i++;                                        // 292
        if (!armado.getBounds().equals(new Rectangle(0, 0, 0, 0))) return i; i++;      // 293
        // Tocar los arreglos publicos por afuera obliga a invalidar: el cache no se entera solo.
        Polygon tocado = new Polygon(new int[] {0, 10, 10, 0}, new int[] {0, 0, 10, 10}, 4);
        tocado.getBounds();
        tocado.xpoints[0] = -50;
        tocado.invalidate();
        if (!tocado.getBounds().equals(new Rectangle(-50, 0, 60, 10))) return i; i++;  // 294

        // --- Polygon: adentro y afuera ---
        if (!cuadro.contains(5, 5)) return i; i++;                                     // 295
        if (cuadro.contains(15, 5)) return i; i++;                                     // 296
        // El semiabierto: arriba-izquierda entra, abajo-derecha no. Es lo que evita que dos
        // poligonos pegados compartan pixeles.
        if (!cuadro.contains(0, 0)) return i; i++;                                     // 297
        if (cuadro.contains(10, 10)) return i; i++;                                    // 298
        if (cuadro.contains(10, 5)) return i; i++;                                     // 299
        if (cuadro.contains(5, 10)) return i; i++;                                     // 300
        if (!cuadro.contains(new Point(5, 5))) return i; i++;                          // 301
        if (!cuadro.contains(new Point2D.Double(5, 5))) return i; i++;                 // 302
        if (!cuadro.contains(5.5, 5.5)) return i; i++;                                 // 303
        if (!cuadro.inside(5, 5)) return i; i++;                                       // 304
        // Un triangulo, que es donde el conteo de cruces se gana el sueldo.
        Polygon tri = new Polygon(new int[] {0, 10, 5}, new int[] {0, 0, 10}, 3);
        if (!tri.contains(5, 1)) return i; i++;                                        // 305
        if (tri.contains(1, 8)) return i; i++;                                         // 306
        if (!tri.contains(5, 9)) return i; i++;                                        // 307
        if (tri.contains(0, 9)) return i; i++;                                         // 308

        // --- Polygon: contra un rectangulo ---
        if (!cuadro.intersects(5, 5, 10, 10)) return i; i++;                           // 309
        if (cuadro.intersects(20, 20, 5, 5)) return i; i++;                            // 310
        if (!cuadro.intersects(new Rectangle2D.Double(5, 5, 1, 1))) return i; i++;     // 311
        if (!cuadro.contains(2, 2, 3, 3)) return i; i++;                               // 312
        // Se sale por abajo y por la derecha: intersecta pero no esta contenido.
        if (cuadro.contains(5, 5, 10, 10)) return i; i++;                              // 313
        if (!cuadro.contains(new Rectangle2D.Double(2, 2, 3, 3))) return i; i++;       // 314
        if (tri.intersects(0, 8, 2, 2)) return i; i++;                                 // 315
        if (!tri.contains(4, 1, 2, 2)) return i; i++;                                  // 316

        // --- Polygon: el recorrido ---
        PathIterator pi = cuadro.getPathIterator(null);
        // Par-impar, no no-cero: en un poligono que se cruza a si mismo el adentro alterna.
        if (pi.getWindingRule() != PathIterator.WIND_EVEN_ODD) return i; i++;          // 317
        double[] co = new double[6];
        if (pi.currentSegment(co) != PathIterator.SEG_MOVETO) return i; i++;           // 318
        if (co[0] != 0.0 || co[1] != 0.0) return i; i++;                               // 319
        pi.next();
        if (pi.currentSegment(co) != PathIterator.SEG_LINETO) return i; i++;           // 320
        if (co[0] != 10.0 || co[1] != 0.0) return i; i++;                              // 321
        int segmentos = 0;
        PathIterator pi2 = cuadro.getPathIterator(null);
        while (!pi2.isDone()) {
            segmentos++;
            pi2.next();
        }
        // Cuatro vertices mas el cierre: cinco segmentos, no cuatro.
        if (segmentos != 5) return i; i++;                                             // 322
        PathIterator pi3 = cuadro.getPathIterator(null);
        for (int k = 0; k < 4; k++) {
            pi3.next();
        }
        if (pi3.currentSegment(co) != PathIterator.SEG_CLOSE) return i; i++;           // 323
        // Un poligono vacio no emite ni siquiera el cierre.
        if (!vacio.getPathIterator(null).isDone()) return i; i++;                      // 324
        // La tolerancia de aplanado no cambia nada: un poligono ya es todo rectas.
        int segmentosPlanos = 0;
        PathIterator pi4 = cuadro.getPathIterator(null, 1.0);
        while (!pi4.isDone()) {
            segmentosPlanos++;
            pi4.next();
        }
        if (segmentosPlanos != 5) return i; i++;                                       // 325

        // --- Polygon: los errores del constructor ---
        // Dos excepciones distintas para dos errores distintos, y el orden importa.
        if (!polyTiraIOOBE()) return i; i++;                                           // 326
        if (!polyTiraNASE()) return i; i++;                                            // 327

        // --- Cursor ---
        if (Cursor.DEFAULT_CURSOR != 0) return i; i++;                                 // 328
        if (Cursor.CROSSHAIR_CURSOR != 1) return i; i++;                               // 329
        if (Cursor.TEXT_CURSOR != 2) return i; i++;                                    // 330
        if (Cursor.WAIT_CURSOR != 3) return i; i++;                                    // 331
        if (Cursor.SW_RESIZE_CURSOR != 4) return i; i++;                               // 332
        if (Cursor.E_RESIZE_CURSOR != 11) return i; i++;                               // 333
        if (Cursor.HAND_CURSOR != 12) return i; i++;                                   // 334
        if (Cursor.MOVE_CURSOR != 13) return i; i++;                                   // 335
        // Fuera de la serie a proposito: no es un tipo, es "ninguno de los de arriba".
        if (Cursor.CUSTOM_CURSOR != -1) return i; i++;                                 // 336
        for (int t = 0; t <= 13; t++) {
            if (Cursor.getPredefinedCursor(t).getType() != t) return i;
            if (Cursor.getPredefinedCursor(t).getName() == null) return i;
        }
        i++;                                                                           // 337
        // Se comparten: son inmutables, y una ventana con cien componentes no necesita cien.
        if (Cursor.getPredefinedCursor(0) != Cursor.getPredefinedCursor(0)) return i; i++; // 338
        if (Cursor.getDefaultCursor() != Cursor.getPredefinedCursor(0)) return i; i++; // 339
        // El nombre depende del idioma del sistema, asi que se comprueba la forma y no el texto.
        Cursor cur = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        if (!cur.toString().equals("java.awt.Cursor[" + cur.getName() + "]")) return i; i++; // 340
        // new Cursor(t) da un objeto nuevo pero con el mismo nombre que el predefinido.
        if (new Cursor(3) == Cursor.getPredefinedCursor(3)) return i; i++;             // 341
        if (!new Cursor(3).getName().equals(cur.getName())) return i; i++;             // 342
        if (!cursorTiraIAE(14)) return i; i++;                                         // 343
        if (!cursorTiraIAE(-1)) return i; i++;                                         // 344
        if (!nuevoCursorTiraIAE(14)) return i; i++;                                    // 345
        if (cursorTiraIAE(13)) return i; i++;                                          // 346

        // --- ComponentOrientation ---
        if (!ComponentOrientation.LEFT_TO_RIGHT.isHorizontal()) return i; i++;         // 347
        if (!ComponentOrientation.LEFT_TO_RIGHT.isLeftToRight()) return i; i++;        // 348
        if (!ComponentOrientation.RIGHT_TO_LEFT.isHorizontal()) return i; i++;         // 349
        if (ComponentOrientation.RIGHT_TO_LEFT.isLeftToRight()) return i; i++;         // 350
        // UNKNOWN no es un tercer sentido: contesta igual que LEFT_TO_RIGHT y solo se distingue
        // comparando identidad. Quien quiera preguntarle al usuario en vez de adivinar mira eso.
        if (!ComponentOrientation.UNKNOWN.isHorizontal()) return i; i++;               // 351
        if (!ComponentOrientation.UNKNOWN.isLeftToRight()) return i; i++;              // 352
        if (ComponentOrientation.UNKNOWN == ComponentOrientation.LEFT_TO_RIGHT) return i; i++; // 353
        if (ComponentOrientation.getOrientation(Locale.US)
                != ComponentOrientation.LEFT_TO_RIGHT) return i; i++;                  // 354
        if (ComponentOrientation.getOrientation(Locale.JAPAN)
                != ComponentOrientation.LEFT_TO_RIGHT) return i; i++;                  // 355
        // Los cinco codigos de derecha a izquierda, incluido el "iw" viejo del hebreo.
        if (ComponentOrientation.getOrientation(orientacionDe("ar"))
                != ComponentOrientation.RIGHT_TO_LEFT) return i; i++;                  // 356
        if (ComponentOrientation.getOrientation(orientacionDe("he"))
                != ComponentOrientation.RIGHT_TO_LEFT) return i; i++;                  // 357
        if (ComponentOrientation.getOrientation(orientacionDe("iw"))
                != ComponentOrientation.RIGHT_TO_LEFT) return i; i++;                  // 358
        if (ComponentOrientation.getOrientation(orientacionDe("fa"))
                != ComponentOrientation.RIGHT_TO_LEFT) return i; i++;                  // 359
        if (ComponentOrientation.getOrientation(orientacionDe("ur"))
                != ComponentOrientation.RIGHT_TO_LEFT) return i; i++;                  // 360

        // --- MenuShortcut ---
        MenuShortcut ms = new MenuShortcut(65);
        if (ms.getKey() != 65) return i; i++;                                          // 361
        if (ms.usesShiftModifier()) return i; i++;                                     // 362
        if (ms.hashCode() != 65) return i; i++;                                        // 363
        MenuShortcut msShift = new MenuShortcut(65, true);
        if (!msShift.usesShiftModifier()) return i; i++;                               // 364
        // El complemento a uno: ~65 es negativo y ningun codigo de tecla lo es, asi que Ctrl+A y
        // Ctrl+Shift+A no pueden colisionar en el mapa de atajos.
        if (msShift.hashCode() != -66) return i; i++;                                  // 365
        if (msShift.hashCode() == ms.hashCode()) return i; i++;                        // 366
        if (!ms.equals(new MenuShortcut(65))) return i; i++;                           // 367
        if (ms.equals(msShift)) return i; i++;                                         // 368
        if (ms.equals("no soy un MenuShortcut")) return i; i++;                        // 369
        if (ms.equals((MenuShortcut) null)) return i; i++;                             // 370

        // --- DisplayMode ---
        // Los dos "no aplica" no son intercambiables: -1 para la profundidad, 0 para la frecuencia.
        if (DisplayMode.BIT_DEPTH_MULTI != -1) return i; i++;                          // 371
        if (DisplayMode.REFRESH_RATE_UNKNOWN != 0) return i; i++;                      // 372
        DisplayMode dm = new DisplayMode(800, 600, 32, 60);
        if (dm.getWidth() != 800 || dm.getHeight() != 600) return i; i++;              // 373
        if (dm.getBitDepth() != 32 || dm.getRefreshRate() != 60) return i; i++;        // 374
        if (!dm.toString().equals("800x600x32bpp@60Hz")) return i; i++;                // 375
        // Los pesos 7 y 13 son primos distintos: sin ellos 800x600 y 600x800 colisionarian.
        if (dm.hashCode() != 2404) return i; i++;                                      // 376
        if (new DisplayMode(600, 800, 32, 60).hashCode() != dm.hashCode()) return i; i++; // 377
        if (!dm.equals(new DisplayMode(800, 600, 32, 60))) return i; i++;              // 378
        if (dm.equals(new DisplayMode(800, 600, 32, 61))) return i; i++;               // 379
        if (dm.equals("no soy un DisplayMode")) return i; i++;                         // 380
        if (dm.equals((DisplayMode) null)) return i; i++;                              // 381
        // Los dos valores especiales se imprimen con palabras, no con el numero.
        if (!new DisplayMode(800, 600, DisplayMode.BIT_DEPTH_MULTI,
                DisplayMode.REFRESH_RATE_UNKNOWN).toString()
                .equals("800x600x[Multi depth]@[Unknown refresh rate]")) return i; i++; // 382

        // --- ImageCapabilities y BufferCapabilities ---
        ImageCapabilities ic = new ImageCapabilities(true);
        if (!ic.isAccelerated()) return i; i++;                                        // 383
        // false en la clase base: quien de verdad sabe es VolatileImage, que sobreescribe.
        if (ic.isTrueVolatile()) return i; i++;                                        // 384
        if (new ImageCapabilities(false).isAccelerated()) return i; i++;               // 385
        if (ic.clone() == ic) return i; i++;                                           // 386
        if (!((ImageCapabilities) ic.clone()).isAccelerated()) return i; i++;          // 387
        BufferCapabilities bc = new BufferCapabilities(new ImageCapabilities(true),
                new ImageCapabilities(false), BufferCapabilities.FlipContents.BACKGROUND);
        if (!bc.getFrontBufferCapabilities().isAccelerated()) return i; i++;           // 388
        if (bc.getBackBufferCapabilities().isAccelerated()) return i; i++;             // 389
        if (bc.getFlipContents() != BufferCapabilities.FlipContents.BACKGROUND) return i; i++; // 390
        // isPageFlipping no es un campo aparte: es "hay FlipContents". Asi no se pueden
        // contradecir.
        if (!bc.isPageFlipping()) return i; i++;                                       // 391
        BufferCapabilities sinFlip = new BufferCapabilities(new ImageCapabilities(true),
                new ImageCapabilities(false), null);
        if (sinFlip.isPageFlipping()) return i; i++;                                   // 392
        if (sinFlip.getFlipContents() != null) return i; i++;                          // 393
        if (bc.isFullScreenRequired()) return i; i++;                                  // 394
        if (bc.isMultiBufferAvailable()) return i; i++;                                // 395
        if (bc.clone() == bc) return i; i++;                                           // 396
        if (!bcTiraIAE()) return i; i++;                                               // 397
        if (!BufferCapabilities.FlipContents.UNDEFINED.toString()
                .equals("undefined")) return i; i++;                                   // 398
        if (!BufferCapabilities.FlipContents.BACKGROUND.toString()
                .equals("background")) return i; i++;                                  // 399
        if (!BufferCapabilities.FlipContents.PRIOR.toString().equals("prior")) return i; i++; // 400
        if (!BufferCapabilities.FlipContents.COPIED.toString().equals("copied")) return i; i++; // 401
        if (BufferCapabilities.FlipContents.UNDEFINED.hashCode() != 0) return i; i++;  // 402
        if (BufferCapabilities.FlipContents.COPIED.hashCode() != 3) return i; i++;     // 403

        // --- AWTPermission ---
        AWTPermission ap = new AWTPermission("showWindowWithoutWarningBanner");
        if (!ap.getName().equals("showWindowWithoutWarningBanner")) return i; i++;     // 404
        // No tiene acciones: la cadena vacia, no null.
        if (!ap.getActions().equals("")) return i; i++;                                // 405
        // El comodin de BasicPermission funciona sin que AWTPermission agregue nada.
        if (!new AWTPermission("*").implies(ap)) return i; i++;                        // 406
        if (new AWTPermission("otra").implies(ap)) return i; i++;                      // 407
        // El segundo parametro se ignora: esta solo para el cargador de politicas.
        if (!new AWTPermission("x", "loQueSea").getActions().equals("")) return i; i++; // 408

        return -1;
    }

    private static Locale orientacionDe(String idioma) {
        return new Locale(idioma);
    }

    private static boolean polyTiraIOOBE() {
        try {
            new Polygon(new int[] {0, 1}, new int[] {0, 1}, 3);
            return false;
        } catch (IndexOutOfBoundsException e) {
            return true;
        }
    }

    private static boolean polyTiraNASE() {
        try {
            new Polygon(new int[] {0}, new int[] {0}, -1);
            return false;
        } catch (NegativeArraySizeException e) {
            return true;
        }
    }

    private static boolean cursorTiraIAE(int tipo) {
        try {
            Cursor.getPredefinedCursor(tipo);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean nuevoCursorTiraIAE(int tipo) {
        try {
            new Cursor(tipo);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean bcTiraIAE() {
        try {
            new BufferCapabilities(null, new ImageCapabilities(false), null);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean decodeTiraNFE(String s) {
        try {
            Color.decode(s);
            return false;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private static boolean colorTiraIAE(int r, int g, int b) {
        try {
            new Color(r, g, b);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean alfaTiraIAE(int a) {
        try {
            new Color(0, 0, 0, a);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean floatTiraIAE(float v) {
        try {
            new Color(v, 0f, 0f);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    // El mensaje es parte de lo que se prueba: enumera todos los canales fuera de rango, en el
    // orden Alpha, Red, Green, Blue, y no solo el primero que falla.
    private static String mensajeDeRango(int r, int g, int b) {
        try {
            new Color(r, g, b);
            return "";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private static String mensajeDeAlfa(int a) {
        try {
            new Color(0, 0, 0, a);
            return "";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private static boolean acTiraIAE(int rule, float alpha) {
        try {
            AlphaComposite.getInstance(rule, alpha);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean putTiraIAE(Object clave, Object valor) {
        try {
            new RenderingHints(null).put(clave, valor);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean putTiraCCE() {
        try {
            new RenderingHints(null).put("no soy una Key", "x");
            return false;
        } catch (ClassCastException e) {
            return true;
        }
    }

    private static boolean putAllTiraCCE() {
        try {
            HashMap<Object, Object> basura = new HashMap<Object, Object>();
            basura.put("no soy una Key", "x");
            new RenderingHints(null).putAll(basura);
            return false;
        } catch (ClassCastException e) {
            return true;
        }
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
