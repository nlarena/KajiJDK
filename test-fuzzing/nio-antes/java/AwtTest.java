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
 * Behaviour test of java.awt, written to run **the same** in this VM and in the real JDK.
 *
 * <p>{@code run()} returns -1 if they all passed or the index of the first one that failed. A
 * single int is enough to compare the two VMs without depending on the console output matching
 * character by character, and without the comparison depending on the time zone or the locale.
 *
 * <p>Of java.awt there are only data classes here: integer geometry, insets, layout constants and
 * the event of 1.0. None of this needs a screen, and it is precisely because of that that it could
 * be written. What is aimed at is what is easy to get wrong without it showing:
 *
 * <ul>
 * <li>the {@code toString()}s, which the JDK specifies to the character --including that of
 *     {@code Event}, which omits the fields at zero;</li>
 * <li>the {@code hashCode()} of {@code Insets}, which is not the usual {@code 31*x+y};</li>
 * <li>the rounding of {@code Point.setLocation(double, double)}, which is not truncating;</li>
 * <li>the values of the constants, above all the ones that break the series: the keys with a
 *     character of {@code Event} and the baseline anchors of {@code GridBagConstraints};</li>
 * <li>that the obsolete names of {@code Rectangle} be the ones that do the work and the new ones
 *     the ones that delegate, and not the other way round.</li>
 * </ul>
 */
public class AwtTest {

    public static int run() {
        int i = 0;

        // --- Point: integers, but it inherits Point2D ---
        Point p = new Point(3, 4);
        if (p.x != 3 || p.y != 4) return i; i++;                                       // 0
        if (new Point().x != 0 || new Point().y != 0) return i; i++;                   // 1
        if (!new Point(new Point(1, 2)).toString()
                .equals("java.awt.Point[x=1,y=2]")) return i; i++;                     // 2
        if (p.getX() != 3.0 || p.getY() != 4.0) return i; i++;                         // 3
        if (!p.toString().equals("java.awt.Point[x=3,y=4]")) return i; i++;            // 4
        // getLocation returns a copy, not this: if it returned this, moving the copy would move the
        // original and all the code that keeps a position "from before" would be broken.
        if (p.getLocation() == p) return i; i++;                                       // 5
        if (!p.getLocation().equals(p)) return i; i++;                                 // 6
        if (new Point(0, 0).distance(3, 4) != 5.0) return i; i++;                      // 7
        // A Point and a Point2D.Double with the same value are equal in both directions.
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
        // The rounding of setLocation in floating point is floor(v + 0.5), not a cast: for the
        // negative ones truncating would give -2 and the JDK gives -3.
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
        // Cantor applied three times. The concrete value matters: it is observable API and a
        // "reasonable" but different formula would break any serialised hash table.
        if (in.hashCode() != 577) return i; i++;                                       // 20
        if (new Insets(0, 0, 0, 0).hashCode() != 0) return i; i++;                      // 21
        if (!in.equals(new Insets(1, 2, 3, 4))) return i; i++;                         // 22
        if (in.equals(new Insets(1, 2, 3, 5))) return i; i++;                          // 23
        if (in.equals("I am not an Insets")) return i; i++;                               // 24
        Object copy = in.clone();
        if (!(copy instanceof Insets)) return i; i++;                                 // 25
        if (copy == in) return i; i++;                                                // 26
        if (!in.equals(copy)) return i; i++;                                          // 27
        Insets iset = new Insets(1, 1, 1, 1);
        iset.set(5, 6, 7, 8);
        if (iset.top != 5 || iset.left != 6 || iset.bottom != 7 || iset.right != 8) return i; i++; // 28

        // --- Transparency: three integers, and the order matters ---
        if (Transparency.OPAQUE != 1) return i; i++;                                   // 29
        if (Transparency.BITMASK != 2) return i; i++;                                  // 30
        if (Transparency.TRANSLUCENT != 3) return i; i++;                              // 31

        // --- GridBagConstraints: constants and default values ---
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
        // Here the series breaks: the baseline anchors jump to multiples of 256 so that the layout
        // tells them apart from the others with a range comparison.
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
        // The clone copies the Insets apart: sharing them would make touching the copy touch the
        // original.
        GridBagConstraints gc = (GridBagConstraints) g2.clone();
        if (gc.insets == g2.insets) return i; i++;                                     // 60
        if (!gc.insets.equals(g2.insets)) return i; i++;                               // 61
        gc.insets.top = 99;
        if (g2.insets.top != 1) return i; i++;                                         // 62

        // --- Event: constants ---
        if (Event.SHIFT_MASK != 1) return i; i++;                                      // 63
        if (Event.CTRL_MASK != 2) return i; i++;                                       // 64
        if (Event.META_MASK != 4) return i; i++;                                       // 65
        if (Event.ALT_MASK != 8) return i; i++;                                        // 66
        if (Event.HOME != 1000) return i; i++;                                         // 67
        if (Event.INSERT != 1025) return i; i++;                                       // 68
        if (Event.F1 != 1008 || Event.F12 != 1019) return i; i++;                      // 69
        // These five break the series: they are the ASCII of the character, not an action code.
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
        // The inherited collision: ACTION_EVENT and PGUP are worth the same. They do not step on
        // each other because one lives in the id field and the other in key, but it is worth
        // writing down so that nobody "corrects" it.
        if (Event.ACTION_EVENT != Event.END) return i; i++;                            // 81

        // --- Event: fields and bit arithmetic ---
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
        // The toString omits key because it is worth zero, and omits arg because it is null.
        if (!e.toString()
                .equals("java.awt.Event[id=501,x=1,y=2,control,meta,target=t]")) return i; i++; // 92
        e.translate(10, 20);
        if (e.x != 11 || e.y != 22) return i; i++;                                     // 93
        Event e2 = new Event(null, 0L, Event.KEY_PRESS, 0, 0, Event.F1, Event.ALT_MASK);
        // ALT_MASK has no consultor of its own: the three there are give false.
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

        // --- Rectangle: what Point has just unblocked ---
        if (!new Rectangle(new Point(1, 2)).toString()
                .equals("java.awt.Rectangle[x=1,y=2,width=0,height=0]")) return i; i++; // 99
        // A Rectangle built only with a Point is empty, not a "present" point of zero area.
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

        // --- Rectangle: the four names of 1.0 ---
        Rectangle r3 = new Rectangle(0, 0, 10, 10);
        r3.reshape(1, 2, 3, 4);
        if (r3.x != 1 || r3.y != 2 || r3.width != 3 || r3.height != 4) return i; i++;  // 107
        r3.resize(7, 8);
        if (r3.x != 1 || r3.y != 2 || r3.width != 7 || r3.height != 8) return i; i++;  // 108
        r3.move(9, 9);
        if (r3.x != 9 || r3.y != 9 || r3.width != 7 || r3.height != 8) return i; i++;  // 109
        Rectangle r4 = new Rectangle(0, 0, 10, 10);
        if (!r4.inside(5, 5)) return i; i++;                                           // 110
        // The top/left edge counts, the bottom/right one does not: the rectangle is half open and
        // that is why two rectangles stuck together share no point.
        if (!r4.inside(0, 0)) return i; i++;                                           // 111
        if (r4.inside(10, 10)) return i; i++;                                          // 112
        if (r4.inside(-1, 5)) return i; i++;                                           // 113
        if (new Rectangle(0, 0, 0, 0).inside(0, 0)) return i; i++;                     // 114
        // And the new names have to go through the old ones, not duplicate the code: a subclass of
        // the time redefined reshape and expected to see the calls to setBounds there.
        Rectangle r5 = new Rectangle();
        r5.setBounds(1, 2, 3, 4);
        if (r5.x != 1 || r5.width != 3) return i; i++;                                 // 115
        r5.setSize(5, 6);
        if (r5.width != 5 || r5.height != 6) return i; i++;                            // 116
        r5.setLocation(7, 8);
        if (r5.x != 7 || r5.y != 8) return i; i++;                                     // 117
        if (!r5.contains(7, 8)) return i; i++;                                         // 118

        // --- the exceptions ---
        if (!new AWTException("x").getMessage().equals("x")) return i; i++;            // 119
        if (!(new AWTException("x") instanceof Exception)) return i; i++;              // 120
        if (!new AWTError("x").getMessage().equals("x")) return i; i++;                // 121
        if (!(new AWTError("x") instanceof Error)) return i; i++;                      // 122
        if (!new FontFormatException("x").getMessage().equals("x")) return i; i++;     // 123
        if (new HeadlessException().getMessage() != null) return i; i++;               // 124
        if (!new HeadlessException("x").getMessage().equals("x")) return i; i++;       // 125
        // It is an UnsupportedOperationException: it can be caught without naming java.awt.
        if (!(new HeadlessException() instanceof UnsupportedOperationException)) return i; i++; // 126
        if (new IllegalComponentStateException().getMessage() != null) return i; i++;  // 127
        if (!new IllegalComponentStateException("x").getMessage().equals("x")) return i; i++; // 128
        if (!(new IllegalComponentStateException() instanceof IllegalStateException)) return i; i++; // 129


        // ------------------------------------------------------------------------------------
        // Second batch: colour, compositing and drawing preferences.
        // ------------------------------------------------------------------------------------

        // --- Color: the 32 bits ---
        if (Color.RED.getRGB() != 0xffff0000) return i; i++;                           // 130
        // The hashCode IS the packed value, not a derivative: it is observable API.
        if (Color.RED.hashCode() != Color.RED.getRGB()) return i; i++;                 // 131
        if (new Color(1, 2, 3).getRGB() != 0xff010203) return i; i++;                  // 132
        // The single-int constructor forces opaque and discards whatever comes in the alpha.
        if (new Color(0x010203).getRGB() != new Color(1, 2, 3).getRGB()) return i; i++; // 133
        if (new Color(0x80010203, true).getAlpha() != 128) return i; i++;              // 134
        if (new Color(0x80010203, false).getAlpha() != 255) return i; i++;             // 135
        Color c = new Color(1, 2, 3, 4);
        if (c.getRed() != 1 || c.getGreen() != 2 || c.getBlue() != 3) return i; i++;   // 136
        if (c.getAlpha() != 4) return i; i++;                                          // 137
        // The alpha does not come out in the toString, not even when it is not 255.
        if (!c.toString().equals("java.awt.Color[r=1,g=2,b=3]")) return i; i++;        // 138
        if (!new Color(1, 2, 3, 4).equals(new Color(1, 2, 3, 4))) return i; i++;       // 139
        // But it does count for equals, because getRGB includes it.
        if (new Color(1, 2, 3, 4).equals(new Color(1, 2, 3, 5))) return i; i++;        // 140
        if (Color.RED.equals("no soy un Color")) return i; i++;                        // 141

        // --- Color: the constants that are not the obvious ones ---
        if (Color.WHITE != Color.white) return i; i++;                                 // 142
        if (Color.GRAY.getRed() != 128) return i; i++;                                 // 143
        if (Color.LIGHT_GRAY.getRed() != 192) return i; i++;                           // 144
        if (Color.DARK_GRAY.getRed() != 64) return i; i++;                             // 145
        // pink is not a pale red: the blue accompanies the green so that it does not turn
        // orange.
        if (Color.pink.getRed() != 255 || Color.pink.getGreen() != 175) return i; i++; // 146
        if (Color.pink.getBlue() != 175) return i; i++;                                // 147
        // And the orange of AWT is more yellow than the "orange" of the web (255,165,0).
        if (Color.orange.getGreen() != 200) return i; i++;                             // 148
        if (Color.orange.getBlue() != 0) return i; i++;                                // 149

        // --- Color: brightening and darkening ---
        if (Color.gray.brighter().getRed() != 182) return i; i++;                      // 150
        if (Color.gray.darker().getRed() != 89) return i; i++;                         // 151
        // The floor: without it, black would never brighten because zero divided by 0.7 is still
        // zero.
        if (Color.black.brighter().getRed() != 3) return i; i++;                       // 152
        if (new Color(1, 1, 1).brighter().getRed() != 4) return i; i++;                // 153
        // Darkening needs no floor and white can no longer be brightened.
        if (Color.white.brighter().getRed() != 255) return i; i++;                     // 154
        if (Color.black.darker().getRed() != 0) return i; i++;                         // 155
        if (new Color(10, 10, 10, 7).brighter().getAlpha() != 7) return i; i++;        // 156
        if (new Color(10, 10, 10, 7).darker().getAlpha() != 7) return i; i++;          // 157

        // --- Color: transparency ---
        if (Color.RED.getTransparency() != Transparency.OPAQUE) return i; i++;         // 158
        // Alpha zero gives BITMASK and not TRANSLUCENT: it is invisible, not half visible.
        if (new Color(1, 2, 3, 0).getTransparency() != Transparency.BITMASK) return i; i++; // 159
        if (new Color(1, 2, 3, 128).getTransparency()
                != Transparency.TRANSLUCENT) return i; i++;                            // 160
        if (!(Color.RED instanceof Transparency)) return i; i++;                       // 161

        // --- Color: decode and getColor ---
        if (!Color.decode("#FF0000").equals(Color.RED)) return i; i++;                 // 162
        if (!Color.decode("0x00FF00").equals(Color.GREEN)) return i; i++;              // 163
        // A bare decimal is valid too: 255 is the 8 bits from below, that is, the blue.
        if (!Color.decode("255").equals(Color.BLUE)) return i; i++;                    // 164
        if (!decodeThrowsNFE("zz")) return i; i++;                                       // 165
        // A property that does not exist: the version with no default value returns null.
        if (Color.getColor("kaji.propiedad.que.no.existe") != null) return i; i++;     // 166
        if (!Color.getColor("kaji.propiedad.que.no.existe", Color.BLUE)
                .equals(Color.BLUE)) return i; i++;                                    // 167
        if (!Color.getColor("kaji.propiedad.que.no.existe", 255)
                .equals(Color.BLUE)) return i; i++;                                    // 168

        // --- Color: the ranges ---
        // The message enumerates every bad channel, not the first.
        if (!colorThrowsIAE(256, 0, 0)) return i; i++;                                   // 169
        if (!colorThrowsIAE(-1, 0, 0)) return i; i++;                                    // 170
        if (!colorThrowsIAE(0, 300, 0)) return i; i++;                                   // 171
        if (!colorThrowsIAE(0, 0, -1)) return i; i++;                                    // 172
        if (colorThrowsIAE(0, 0, 0)) return i; i++;                                      // 173
        if (colorThrowsIAE(255, 255, 255)) return i; i++;                                // 174
        if (!rangeMessage(-1, 0, 300)
                .equals("Color parameter outside of expected range: Red Blue")) return i; i++; // 175
        if (!alphaThrowsIAE(256)) return i; i++;                                          // 176
        if (!alphaMessage(256)
                .equals("Color parameter outside of expected range: Alpha")) return i; i++; // 177
        if (!floatThrowsIAE(1.5f)) return i; i++;                                        // 178
        if (floatThrowsIAE(1.0f)) return i; i++;                                         // 179

        // --- Color: HSB ---
        if (Color.HSBtoRGB(0f, 1f, 1f) != 0xffff0000) return i; i++;                   // 180
        if (Color.HSBtoRGB(1f / 3f, 1f, 1f) != 0xff00ff00) return i; i++;              // 181
        if (Color.HSBtoRGB(2f / 3f, 1f, 1f) != 0xff0000ff) return i; i++;              // 182
        // Saturation zero: grey, and the hue does not matter.
        if (Color.HSBtoRGB(0f, 0f, 0.5f) != 0xff808080) return i; i++;                 // 183
        if (Color.HSBtoRGB(0.7f, 0f, 0.5f) != Color.HSBtoRGB(0f, 0f, 0.5f)) return i; i++; // 184
        // The hue is an angle: it is taken modulo 1, so 1.25 and 0.25 are the same colour.
        if (Color.HSBtoRGB(1.25f, 1f, 1f) != Color.HSBtoRGB(0.25f, 1f, 1f)) return i; i++; // 185
        if (Color.HSBtoRGB(-0.75f, 1f, 1f) != Color.HSBtoRGB(0.25f, 1f, 1f)) return i; i++; // 186
        float[] hsb = Color.RGBtoHSB(255, 0, 0, null);
        if (hsb.length != 3) return i; i++;                                            // 187
        if (hsb[0] != 0.0f || hsb[1] != 1.0f || hsb[2] != 1.0f) return i; i++;         // 188
        float[] hsb2 = Color.RGBtoHSB(0, 0, 255, null);
        if (hsb2[0] != 4.0f / 6.0f) return i; i++;                                     // 189
        float[] hsb3 = Color.RGBtoHSB(128, 128, 128, null);
        // A grey has no hue: 0 is returned by convention, not an invented value.
        if (hsb3[0] != 0.0f || hsb3[1] != 0.0f) return i; i++;                         // 190
        if (hsb3[2] != 128.0f / 255.0f) return i; i++;                                 // 191
        float[] hsb4 = Color.RGBtoHSB(0, 0, 0, null);
        if (hsb4[0] != 0.0f || hsb4[1] != 0.0f || hsb4[2] != 0.0f) return i; i++;      // 192
        float[] givenHsb = new float[3];
        if (Color.RGBtoHSB(1, 2, 3, givenHsb) != givenHsb) return i; i++;                // 193
        if (!Color.getHSBColor(0f, 1f, 1f).equals(Color.RED)) return i; i++;           // 194

        // --- Color: the components in floating point ---
        // A colour built with floats returns them intact; going through integers would lose them.
        float[] cf = new Color(0.1f, 0.2f, 0.3f).getRGBColorComponents(null);
        if (cf.length != 3) return i; i++;                                             // 195
        if (cf[0] != 0.1f || cf[1] != 0.2f || cf[2] != 0.3f) return i; i++;            // 196
        // And the same colour, seen as an integer, is quantised: 0.1*255+0.5 = 26.
        if (new Color(0.1f, 0.2f, 0.3f).getRed() != 26) return i; i++;                 // 197
        // One built with integers returns the division, which for it is exact.
        float[] ci = new Color(255, 0, 0).getRGBColorComponents(null);
        if (ci[0] != 1.0f || ci[1] != 0.0f || ci[2] != 0.0f) return i; i++;            // 198
        float[] ca = new Color(255, 0, 0).getRGBComponents(null);
        if (ca.length != 4) return i; i++;                                             // 199
        if (ca[3] != 1.0f) return i; i++;                                              // 200
        // With an array given it is written there and that same one is returned, not a copy.
        float[] given4 = new float[4];
        if (new Color(1, 2, 3).getRGBComponents(given4) != given4) return i; i++;        // 201
        // getComponents and getColorComponents: since every Color here is sRGB, they are the ones
        // above.
        float[] cc = new Color(0.1f, 0.2f, 0.3f).getComponents(null);
        if (cc.length != 4 || cc[0] != 0.1f || cc[3] != 1.0f) return i; i++;           // 202
        if (new Color(1, 2, 3).getColorComponents(null).length != 3) return i; i++;    // 203

        // --- AlphaComposite: the twelve rules ---
        if (AlphaComposite.CLEAR != 1) return i; i++;                                  // 204
        if (AlphaComposite.SRC != 2) return i; i++;                                    // 205
        if (AlphaComposite.SRC_OVER != 3) return i; i++;                               // 206
        if (AlphaComposite.DST_OVER != 4) return i; i++;                               // 207
        if (AlphaComposite.SRC_IN != 5) return i; i++;                                 // 208
        if (AlphaComposite.DST_IN != 6) return i; i++;                                 // 209
        if (AlphaComposite.SRC_OUT != 7) return i; i++;                                // 210
        if (AlphaComposite.DST_OUT != 8) return i; i++;                                // 211
        // The jump: DST was added in 1.4 and numbered at the end, not next to SRC.
        if (AlphaComposite.DST != 9) return i; i++;                                    // 212
        if (AlphaComposite.SRC_ATOP != 10) return i; i++;                              // 213
        if (AlphaComposite.DST_ATOP != 11) return i; i++;                              // 214
        if (AlphaComposite.XOR != 12) return i; i++;                                   // 215
        if (AlphaComposite.SrcOver.getRule() != AlphaComposite.SRC_OVER) return i; i++; // 216
        if (AlphaComposite.SrcOver.getAlpha() != 1.0f) return i; i++;                  // 217
        if (AlphaComposite.Dst.getRule() != AlphaComposite.DST) return i; i++;         // 218
        // With alpha 1 the shared constant is returned, not a new object.
        if (AlphaComposite.getInstance(AlphaComposite.SRC_OVER)
                != AlphaComposite.SrcOver) return i; i++;                              // 219
        if (AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)
                != AlphaComposite.SrcOver) return i; i++;                              // 220
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f);
        if (ac.getRule() != AlphaComposite.SRC || ac.getAlpha() != 0.5f) return i; i++; // 221
        if (ac == AlphaComposite.Src) return i; i++;                                   // 222
        // The hashCode mixes the alpha and the rule, in that order.
        if (ac.hashCode() != Float.floatToIntBits(0.5f) * 31 + AlphaComposite.SRC) return i; i++; // 223
        if (AlphaComposite.SrcOver.hashCode()
                != Float.floatToIntBits(1.0f) * 31 + AlphaComposite.SRC_OVER) return i; i++; // 224
        if (!ac.equals(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f))) return i; i++; // 225
        if (ac.equals(AlphaComposite.getInstance(AlphaComposite.DST, 0.5f))) return i; i++; // 226
        if (ac.equals(AlphaComposite.getInstance(AlphaComposite.SRC, 0.25f))) return i; i++; // 227
        if (ac.equals("no soy un AlphaComposite")) return i; i++;                      // 228
        // derive: if nothing changes it returns this, not a copy.
        if (AlphaComposite.SrcOver.derive(AlphaComposite.SRC_OVER)
                != AlphaComposite.SrcOver) return i; i++;                              // 229
        if (ac.derive(0.5f) != ac) return i; i++;                                      // 230
        if (ac.derive(0.25f).getRule() != AlphaComposite.SRC) return i; i++;           // 231
        if (ac.derive(0.25f).getAlpha() != 0.25f) return i; i++;                       // 232
        if (ac.derive(AlphaComposite.XOR).getAlpha() != 0.5f) return i; i++;           // 233
        if (ac.derive(AlphaComposite.XOR).getRule() != AlphaComposite.XOR) return i; i++; // 234
        if (!acThrowsIAE(0, 1.0f)) return i; i++;                                        // 235
        if (!acThrowsIAE(13, 1.0f)) return i; i++;                                       // 236
        if (!acThrowsIAE(AlphaComposite.SRC, -0.1f)) return i; i++;                      // 237
        if (!acThrowsIAE(AlphaComposite.SRC, 1.5f)) return i; i++;                       // 238
        // NaN too: the validation is written in the positive precisely in order to catch it.
        if (!acThrowsIAE(AlphaComposite.SRC, Float.NaN)) return i; i++;                  // 239
        if (acThrowsIAE(AlphaComposite.SRC, 0.0f)) return i; i++;                        // 240

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
        // A null in the Map constructor gives an empty set, not an exception.
        if (!new RenderingHints(null).isEmpty()) return i; i++;                        // 247

        // The validation: each key knows which values it accepts and the rest is rejected when
        // storing.
        if (!RenderingHints.KEY_ANTIALIASING
                .isCompatibleValue(RenderingHints.VALUE_ANTIALIAS_ON)) return i; i++;  // 248
        if (RenderingHints.KEY_ANTIALIASING
                .isCompatibleValue(RenderingHints.VALUE_RENDER_QUALITY)) return i; i++; // 249
        if (RenderingHints.KEY_ANTIALIASING.isCompatibleValue("si")) return i; i++;    // 250
        if (RenderingHints.KEY_ANTIALIASING.isCompatibleValue(null)) return i; i++;    // 251
        if (!putThrowsIAE(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_RENDER_QUALITY)) return i; i++;                   // 252
        if (!putThrowsIAE(RenderingHints.KEY_ANTIALIASING, "si")) return i; i++;         // 253
        // A key that is not a Key does not give IAE but ClassCastException: it is not a bad value,
        // it is a type that does not fit.
        if (!putThrowsCCE()) return i; i++;                                              // 254

        // The only key with a numeric value: an integer between 100 and 250, closed.
        if (!RenderingHints.KEY_TEXT_LCD_CONTRAST
                .isCompatibleValue(Integer.valueOf(100))) return i; i++;               // 255
        if (!RenderingHints.KEY_TEXT_LCD_CONTRAST
                .isCompatibleValue(Integer.valueOf(250))) return i; i++;               // 256
        if (RenderingHints.KEY_TEXT_LCD_CONTRAST
                .isCompatibleValue(Integer.valueOf(99))) return i; i++;                // 257
        if (RenderingHints.KEY_TEXT_LCD_CONTRAST
                .isCompatibleValue(Integer.valueOf(251))) return i; i++;               // 258
        if (RenderingHints.KEY_TEXT_LCD_CONTRAST.isCompatibleValue("x")) return i; i++; // 259

        // A key is itself and nothing else: the equals is identity and it is final.
        if (!RenderingHints.KEY_ANTIALIASING
                .equals(RenderingHints.KEY_ANTIALIASING)) return i; i++;               // 260
        if (RenderingHints.KEY_ANTIALIASING
                .equals(RenderingHints.KEY_RENDERING)) return i; i++;                  // 261

        RenderingHints rh2 = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        if (!rh.equals(rh2)) return i; i++;                                            // 262
        if (rh.hashCode() != rh2.hashCode()) return i; i++;                            // 263
        // And it is equal to just any Map with the same contents, not only to another
        // RenderingHints.
        HashMap<Object, Object> hm = new HashMap<Object, Object>();
        hm.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (!rh.equals(hm)) return i; i++;                                             // 264
        if (rh.equals("no soy un Map")) return i; i++;                                 // 265

        RenderingHints cloned = (RenderingHints) rh.clone();
        cloned.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (rh.size() != 1) return i; i++;                                             // 266
        if (cloned.size() != 2) return i; i++;                                           // 267
        RenderingHints acc = new RenderingHints(null);
        acc.add(cloned);
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
        // putAll of just any Map validates each pair: the rubbish does not get in.
        if (!putAllThrowsCCE()) return i; i++;                                           // 276


        // ------------------------------------------------------------------------------------
        // Third batch: polygon, cursor, reading direction and the capabilities.
        // ------------------------------------------------------------------------------------

        // --- Polygon: bounding box and cache ---
        Polygon frame = new Polygon(new int[] {0, 10, 10, 0}, new int[] {0, 0, 10, 10}, 4);
        if (!frame.getBounds().equals(new Rectangle(0, 0, 10, 10))) return i; i++;    // 277
        if (!frame.getBoundingBox().equals(new Rectangle(0, 0, 10, 10))) return i; i++; // 278
        // getBounds2D of an integer polygon returns the same integer rectangle.
        if (!frame.getBounds2D().equals(new Rectangle(0, 0, 10, 10))) return i; i++;  // 279
        // It returns a copy: if it returned the cache, touching it would corrupt the polygon.
        if (frame.getBounds() == frame.getBounds()) return i; i++;                   // 280
        if (frame.npoints != 4) return i; i++;                                        // 281
        // The polygon copies the arrays it is passed; it does not keep the caller's.
        int[] xs = {0, 1, 2};
        Polygon copied = new Polygon(xs, new int[] {0, 1, 2}, 3);
        xs[0] = 99;
        if (copied.xpoints[0] != 0) return i; i++;                                    // 282
        Polygon empty = new Polygon();
        if (empty.npoints != 0) return i; i++;                                         // 283
        if (!empty.getBounds().equals(new Rectangle(0, 0, 0, 0))) return i; i++;       // 284
        if (empty.contains(0, 0)) return i; i++;                                       // 285
        if (empty.intersects(0, 0, 1, 1)) return i; i++;                               // 286
        // The arrays start at 4 and double; three points fit without growing.
        Polygon built = new Polygon();
        built.addPoint(1, 1);
        built.addPoint(5, 1);
        built.addPoint(5, 5);
        if (built.npoints != 3) return i; i++;                                        // 287
        if (built.xpoints.length != 4) return i; i++;                                 // 288
        if (!built.getBounds().equals(new Rectangle(1, 1, 4, 4))) return i; i++;      // 289
        built.translate(10, 10);
        if (built.xpoints[0] != 11) return i; i++;                                    // 290
        if (!built.getBounds().equals(new Rectangle(11, 11, 4, 4))) return i; i++;    // 291
        built.reset();
        if (built.npoints != 0) return i; i++;                                        // 292
        if (!built.getBounds().equals(new Rectangle(0, 0, 0, 0))) return i; i++;      // 293
        // Touching the public arrays from outside forces an invalidation: the cache does not find
        // out by itself.
        Polygon touched = new Polygon(new int[] {0, 10, 10, 0}, new int[] {0, 0, 10, 10}, 4);
        touched.getBounds();
        touched.xpoints[0] = -50;
        touched.invalidate();
        if (!touched.getBounds().equals(new Rectangle(-50, 0, 60, 10))) return i; i++;  // 294

        // --- Polygon: inside and outside ---
        if (!frame.contains(5, 5)) return i; i++;                                     // 295
        if (frame.contains(15, 5)) return i; i++;                                     // 296
        // The half-open rule: top-left counts, bottom-right does not. It is what keeps two
        // polygons stuck together from sharing pixels.
        if (!frame.contains(0, 0)) return i; i++;                                     // 297
        if (frame.contains(10, 10)) return i; i++;                                    // 298
        if (frame.contains(10, 5)) return i; i++;                                     // 299
        if (frame.contains(5, 10)) return i; i++;                                     // 300
        if (!frame.contains(new Point(5, 5))) return i; i++;                          // 301
        if (!frame.contains(new Point2D.Double(5, 5))) return i; i++;                 // 302
        if (!frame.contains(5.5, 5.5)) return i; i++;                                 // 303
        if (!frame.inside(5, 5)) return i; i++;                                       // 304
        // A triangle, which is where the crossing count earns its keep.
        Polygon tri = new Polygon(new int[] {0, 10, 5}, new int[] {0, 0, 10}, 3);
        if (!tri.contains(5, 1)) return i; i++;                                        // 305
        if (tri.contains(1, 8)) return i; i++;                                         // 306
        if (!tri.contains(5, 9)) return i; i++;                                        // 307
        if (tri.contains(0, 9)) return i; i++;                                         // 308

        // --- Polygon: against a rectangle ---
        if (!frame.intersects(5, 5, 10, 10)) return i; i++;                           // 309
        if (frame.intersects(20, 20, 5, 5)) return i; i++;                            // 310
        if (!frame.intersects(new Rectangle2D.Double(5, 5, 1, 1))) return i; i++;     // 311
        if (!frame.contains(2, 2, 3, 3)) return i; i++;                               // 312
        // It goes out at the bottom and on the right: it intersects but is not contained.
        if (frame.contains(5, 5, 10, 10)) return i; i++;                              // 313
        if (!frame.contains(new Rectangle2D.Double(2, 2, 3, 3))) return i; i++;       // 314
        if (tri.intersects(0, 8, 2, 2)) return i; i++;                                 // 315
        if (!tri.contains(4, 1, 2, 2)) return i; i++;                                  // 316

        // --- Polygon: the walk ---
        PathIterator pi = frame.getPathIterator(null);
        // Even-odd, not non-zero: in a polygon that crosses itself the inside alternates.
        if (pi.getWindingRule() != PathIterator.WIND_EVEN_ODD) return i; i++;          // 317
        double[] co = new double[6];
        if (pi.currentSegment(co) != PathIterator.SEG_MOVETO) return i; i++;           // 318
        if (co[0] != 0.0 || co[1] != 0.0) return i; i++;                               // 319
        pi.next();
        if (pi.currentSegment(co) != PathIterator.SEG_LINETO) return i; i++;           // 320
        if (co[0] != 10.0 || co[1] != 0.0) return i; i++;                              // 321
        int segments = 0;
        PathIterator pi2 = frame.getPathIterator(null);
        while (!pi2.isDone()) {
            segments++;
            pi2.next();
        }
        // Four vertices plus the closing: five segments, not four.
        if (segments != 5) return i; i++;                                             // 322
        PathIterator pi3 = frame.getPathIterator(null);
        for (int k = 0; k < 4; k++) {
            pi3.next();
        }
        if (pi3.currentSegment(co) != PathIterator.SEG_CLOSE) return i; i++;           // 323
        // An empty polygon does not emit even the closing.
        if (!empty.getPathIterator(null).isDone()) return i; i++;                      // 324
        // The flattening tolerance changes nothing: a polygon is all straight lines already.
        int flatSegments = 0;
        PathIterator pi4 = frame.getPathIterator(null, 1.0);
        while (!pi4.isDone()) {
            flatSegments++;
            pi4.next();
        }
        if (flatSegments != 5) return i; i++;                                       // 325

        // --- Polygon: the errors of the constructor ---
        // Two different exceptions for two different errors, and the order matters.
        if (!polyThrowsIOOBE()) return i; i++;                                           // 326
        if (!polyThrowsNASE()) return i; i++;                                            // 327

        // --- Cursor ---
        if (Cursor.DEFAULT_CURSOR != 0) return i; i++;                                 // 328
        if (Cursor.CROSSHAIR_CURSOR != 1) return i; i++;                               // 329
        if (Cursor.TEXT_CURSOR != 2) return i; i++;                                    // 330
        if (Cursor.WAIT_CURSOR != 3) return i; i++;                                    // 331
        if (Cursor.SW_RESIZE_CURSOR != 4) return i; i++;                               // 332
        if (Cursor.E_RESIZE_CURSOR != 11) return i; i++;                               // 333
        if (Cursor.HAND_CURSOR != 12) return i; i++;                                   // 334
        if (Cursor.MOVE_CURSOR != 13) return i; i++;                                   // 335
        // Outside the series on purpose: it is not a type, it is "none of the ones above".
        if (Cursor.CUSTOM_CURSOR != -1) return i; i++;                                 // 336
        for (int t = 0; t <= 13; t++) {
            if (Cursor.getPredefinedCursor(t).getType() != t) return i;
            if (Cursor.getPredefinedCursor(t).getName() == null) return i;
        }
        i++;                                                                           // 337
        // They are shared: they are immutable, and a window with a hundred components does not need
        // a hundred.
        if (Cursor.getPredefinedCursor(0) != Cursor.getPredefinedCursor(0)) return i; i++; // 338
        if (Cursor.getDefaultCursor() != Cursor.getPredefinedCursor(0)) return i; i++; // 339
        // The name depends on the language of the system, so the shape is checked and not the
        // text.
        Cursor cur = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        if (!cur.toString().equals("java.awt.Cursor[" + cur.getName() + "]")) return i; i++; // 340
        // new Cursor(t) gives a new object but with the same name as the predefined one.
        if (new Cursor(3) == Cursor.getPredefinedCursor(3)) return i; i++;             // 341
        if (!new Cursor(3).getName().equals(cur.getName())) return i; i++;             // 342
        if (!cursorThrowsIAE(14)) return i; i++;                                         // 343
        if (!cursorThrowsIAE(-1)) return i; i++;                                         // 344
        if (!newCursorThrowsIAE(14)) return i; i++;                                    // 345
        if (cursorThrowsIAE(13)) return i; i++;                                          // 346

        // --- ComponentOrientation ---
        if (!ComponentOrientation.LEFT_TO_RIGHT.isHorizontal()) return i; i++;         // 347
        if (!ComponentOrientation.LEFT_TO_RIGHT.isLeftToRight()) return i; i++;        // 348
        if (!ComponentOrientation.RIGHT_TO_LEFT.isHorizontal()) return i; i++;         // 349
        if (ComponentOrientation.RIGHT_TO_LEFT.isLeftToRight()) return i; i++;         // 350
        // UNKNOWN is not a third direction: it answers the same as LEFT_TO_RIGHT and is only told
        // apart by comparing identity. Whoever wants to ask the user instead of guessing looks at
        // that.
        if (!ComponentOrientation.UNKNOWN.isHorizontal()) return i; i++;               // 351
        if (!ComponentOrientation.UNKNOWN.isLeftToRight()) return i; i++;              // 352
        if (ComponentOrientation.UNKNOWN == ComponentOrientation.LEFT_TO_RIGHT) return i; i++; // 353
        if (ComponentOrientation.getOrientation(Locale.US)
                != ComponentOrientation.LEFT_TO_RIGHT) return i; i++;                  // 354
        if (ComponentOrientation.getOrientation(Locale.JAPAN)
                != ComponentOrientation.LEFT_TO_RIGHT) return i; i++;                  // 355
        // The five right-to-left codes, including the old "iw" of Hebrew.
        if (ComponentOrientation.getOrientation(orientationOf("ar"))
                != ComponentOrientation.RIGHT_TO_LEFT) return i; i++;                  // 356
        if (ComponentOrientation.getOrientation(orientationOf("he"))
                != ComponentOrientation.RIGHT_TO_LEFT) return i; i++;                  // 357
        if (ComponentOrientation.getOrientation(orientationOf("iw"))
                != ComponentOrientation.RIGHT_TO_LEFT) return i; i++;                  // 358
        if (ComponentOrientation.getOrientation(orientationOf("fa"))
                != ComponentOrientation.RIGHT_TO_LEFT) return i; i++;                  // 359
        if (ComponentOrientation.getOrientation(orientationOf("ur"))
                != ComponentOrientation.RIGHT_TO_LEFT) return i; i++;                  // 360

        // --- MenuShortcut ---
        MenuShortcut ms = new MenuShortcut(65);
        if (ms.getKey() != 65) return i; i++;                                          // 361
        if (ms.usesShiftModifier()) return i; i++;                                     // 362
        if (ms.hashCode() != 65) return i; i++;                                        // 363
        MenuShortcut msShift = new MenuShortcut(65, true);
        if (!msShift.usesShiftModifier()) return i; i++;                               // 364
        // The one's complement: ~65 is negative and no key code is, so Ctrl+A and Ctrl+Shift+A
        // cannot collide in the map of shortcuts.
        if (msShift.hashCode() != -66) return i; i++;                                  // 365
        if (msShift.hashCode() == ms.hashCode()) return i; i++;                        // 366
        if (!ms.equals(new MenuShortcut(65))) return i; i++;                           // 367
        if (ms.equals(msShift)) return i; i++;                                         // 368
        if (ms.equals("no soy un MenuShortcut")) return i; i++;                        // 369
        if (ms.equals((MenuShortcut) null)) return i; i++;                             // 370

        // --- DisplayMode ---
        // The two "not applicable" values are not interchangeable: -1 for the depth, 0 for the
        // frequency.
        if (DisplayMode.BIT_DEPTH_MULTI != -1) return i; i++;                          // 371
        if (DisplayMode.REFRESH_RATE_UNKNOWN != 0) return i; i++;                      // 372
        DisplayMode dm = new DisplayMode(800, 600, 32, 60);
        if (dm.getWidth() != 800 || dm.getHeight() != 600) return i; i++;              // 373
        if (dm.getBitDepth() != 32 || dm.getRefreshRate() != 60) return i; i++;        // 374
        if (!dm.toString().equals("800x600x32bpp@60Hz")) return i; i++;                // 375
        // The weights 7 and 13 are different primes: without them 800x600 and 600x800 would
        // collide.
        if (dm.hashCode() != 2404) return i; i++;                                      // 376
        if (new DisplayMode(600, 800, 32, 60).hashCode() != dm.hashCode()) return i; i++; // 377
        if (!dm.equals(new DisplayMode(800, 600, 32, 60))) return i; i++;              // 378
        if (dm.equals(new DisplayMode(800, 600, 32, 61))) return i; i++;               // 379
        if (dm.equals("no soy un DisplayMode")) return i; i++;                         // 380
        if (dm.equals((DisplayMode) null)) return i; i++;                              // 381
        // The two special values are printed with words, not with the number.
        if (!new DisplayMode(800, 600, DisplayMode.BIT_DEPTH_MULTI,
                DisplayMode.REFRESH_RATE_UNKNOWN).toString()
                .equals("800x600x[Multi depth]@[Unknown refresh rate]")) return i; i++; // 382

        // --- ImageCapabilities and BufferCapabilities ---
        ImageCapabilities ic = new ImageCapabilities(true);
        if (!ic.isAccelerated()) return i; i++;                                        // 383
        // false in the base class: the one that really knows is VolatileImage, which overrides.
        if (ic.isTrueVolatile()) return i; i++;                                        // 384
        if (new ImageCapabilities(false).isAccelerated()) return i; i++;               // 385
        if (ic.clone() == ic) return i; i++;                                           // 386
        if (!((ImageCapabilities) ic.clone()).isAccelerated()) return i; i++;          // 387
        BufferCapabilities bc = new BufferCapabilities(new ImageCapabilities(true),
                new ImageCapabilities(false), BufferCapabilities.FlipContents.BACKGROUND);
        if (!bc.getFrontBufferCapabilities().isAccelerated()) return i; i++;           // 388
        if (bc.getBackBufferCapabilities().isAccelerated()) return i; i++;             // 389
        if (bc.getFlipContents() != BufferCapabilities.FlipContents.BACKGROUND) return i; i++; // 390
        // isPageFlipping is not a separate field: it is "there is a FlipContents". That way they
        // cannot contradict each other.
        if (!bc.isPageFlipping()) return i; i++;                                       // 391
        BufferCapabilities sinFlip = new BufferCapabilities(new ImageCapabilities(true),
                new ImageCapabilities(false), null);
        if (sinFlip.isPageFlipping()) return i; i++;                                   // 392
        if (sinFlip.getFlipContents() != null) return i; i++;                          // 393
        if (bc.isFullScreenRequired()) return i; i++;                                  // 394
        if (bc.isMultiBufferAvailable()) return i; i++;                                // 395
        if (bc.clone() == bc) return i; i++;                                           // 396
        if (!bcThrowsIAE()) return i; i++;                                               // 397
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
        // It has no actions: the empty string, not null.
        if (!ap.getActions().equals("")) return i; i++;                                // 405
        // The wildcard of BasicPermission works without AWTPermission adding anything.
        if (!new AWTPermission("*").implies(ap)) return i; i++;                        // 406
        if (new AWTPermission("otra").implies(ap)) return i; i++;                      // 407
        // The second parameter is ignored: it is there only for the policy loader.
        if (!new AWTPermission("x", "loQueSea").getActions().equals("")) return i; i++; // 408

        return -1;
    }

    private static Locale orientationOf(String language) {
        return new Locale(language);
    }

    private static boolean polyThrowsIOOBE() {
        try {
            new Polygon(new int[] {0, 1}, new int[] {0, 1}, 3);
            return false;
        } catch (IndexOutOfBoundsException e) {
            return true;
        }
    }

    private static boolean polyThrowsNASE() {
        try {
            new Polygon(new int[] {0}, new int[] {0}, -1);
            return false;
        } catch (NegativeArraySizeException e) {
            return true;
        }
    }

    private static boolean cursorThrowsIAE(int type) {
        try {
            Cursor.getPredefinedCursor(type);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean newCursorThrowsIAE(int type) {
        try {
            new Cursor(type);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean bcThrowsIAE() {
        try {
            new BufferCapabilities(null, new ImageCapabilities(false), null);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean decodeThrowsNFE(String s) {
        try {
            Color.decode(s);
            return false;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private static boolean colorThrowsIAE(int r, int g, int b) {
        try {
            new Color(r, g, b);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean alphaThrowsIAE(int a) {
        try {
            new Color(0, 0, 0, a);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean floatThrowsIAE(float v) {
        try {
            new Color(v, 0f, 0f);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    // The message is part of what is tested: it enumerates every channel out of range, in the
    // order Alpha, Red, Green, Blue, and not only the first that fails.
    private static String rangeMessage(int r, int g, int b) {
        try {
            new Color(r, g, b);
            return "";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private static String alphaMessage(int a) {
        try {
            new Color(0, 0, 0, a);
            return "";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private static boolean acThrowsIAE(int rule, float alpha) {
        try {
            AlphaComposite.getInstance(rule, alpha);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean putThrowsIAE(Object key, Object value) {
        try {
            new RenderingHints(null).put(key, value);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean putThrowsCCE() {
        try {
            new RenderingHints(null).put("no soy una Key", "x");
            return false;
        } catch (ClassCastException e) {
            return true;
        }
    }

    private static boolean putAllThrowsCCE() {
        try {
            HashMap<Object, Object> rubbish = new HashMap<Object, Object>();
            rubbish.put("no soy una Key", "x");
            new RenderingHints(null).putAll(rubbish);
            return false;
        } catch (ClassCastException e) {
            return true;
        }
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
