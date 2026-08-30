package java.lang;

// KajiLibrary's java.lang.FdLibm — the shared home of the fdlibm routines whose contract is stated
// in ULPs, mirroring the JDK's own package-private `FdLibm`. It exists for the pieces that CANNOT
// live as a self-contained one-liner in Math: the trigonometric argument reduction.
//
// sin/cos/tan all reduce their argument to [-pi/4, pi/4] plus a quadrant, then evaluate a kernel
// polynomial. The reduction is the hard, shared part: a fast path (one subtraction of a multi-word
// pi/2), a medium path (up to |x| < 2^19*pi/2), and a slow path -- `kernelRemPioTwo` -- that does
// multi-precision arithmetic in base 2^24 against a table of the bits of 2/pi, for arguments so
// large that a plain subtraction would have no bits of accuracy left. That table (`ipio2`) is the
// leading 24-bit groups of 2/pi, a mathematical constant, generated rather than transcribed.
//
// Ported from the public-domain fdlibm (SunPro), constant for constant, so the result is bit-for-bit
// the JDK's StrictMath. `Math`/`StrictMath` delegate here.
final class FdLibm {

    private FdLibm() {
    }

    // ---- double <-> 32-bit-word helpers (the C GET/SET_HIGH_WORD, __HI/__LO macros) ----

    private static int hi(double x) {
        return (int) (Double.doubleToRawLongBits(x) >>> 32);
    }

    private static int lo(double x) {
        return (int) Double.doubleToRawLongBits(x);
    }

    private static double withHi(double x, int high) {
        long b = Double.doubleToRawLongBits(x) & 0x00000000ffffffffL;
        return Double.longBitsToDouble((((long) high) << 32) | b);
    }

    private static double withLo(double x, int low) {
        long b = Double.doubleToRawLongBits(x) & 0xffffffff00000000L;
        return Double.longBitsToDouble(b | (((long) low) & 0x00000000ffffffffL));
    }

    // 2^n as an exact multiplier, adequate for the ranges the reduction uses.
    private static double scalbn(double x, int n) {
        return x * Double.longBitsToDouble(((long) (0x3ff + n)) << 52);
    }

    // ---- tables ----

    // The leading 24-bit groups of the fractional part of 2/pi (generated: group k = floor(2/pi *
    // 2^(24k)) mod 2^24). Backs the slow argument reduction.
    private static final int[] ipio2 = {
        0xA2F983, 0x6E4E44, 0x1529FC, 0x2757D1, 0xF534DD, 0xC0DB62, 0x95993C, 0x439041, 0xFE5163,
        0xABDEBB, 0xC561B7, 0x246E3A, 0x424DD2, 0xE00649, 0x2EEA09, 0xD1921C, 0xFE1DEB, 0x1CB129,
        0xA73EE8, 0x8235F5, 0x2EBB44, 0x84E99C, 0x7026B4, 0x5F7E41, 0x3991D6, 0x398353, 0x39F49C,
        0x845F8B, 0xBDF928, 0x3B1FF8, 0x97FFDE, 0x05980F, 0xEF2F11, 0x8B5A0A, 0x6D1F6D, 0x367ECF,
        0x27CB09, 0xB74F46, 0x3F669E, 0x5FEA2D, 0x7527BA, 0xC7EBE5, 0xF17B3D, 0x0739F7, 0x8A5292,
        0xEA6BFB, 0x5FB11F, 0x8D5D08, 0x560330, 0x46FC7B, 0x6BABF0, 0xCFBC20, 0x9AF436, 0x1DA9E3,
        0x91615E, 0xE61B08, 0x659985, 0x5F14A0, 0x68408D, 0xFFD880, 0x4D7327, 0x310606, 0x1556CA,
        0x73A8C9, 0x60E27B, 0xC08C6B, 0x47C419, 0xC367CD, 0xDCE809, 0x2A8359
    };

    // High words of n*(pi/2) for n = 1..32 (generated), for the medium path's cancellation check.
    private static final int[] npio2_hw = {
        0x3FF921FB, 0x400921FB, 0x4012D97C, 0x401921FB, 0x401F6A7A, 0x4022D97C, 0x4025FDBB,
        0x402921FB, 0x402C463A, 0x402F6A7A, 0x4031475C, 0x4032D97C, 0x40346B9C, 0x4035FDBB,
        0x40378FDB, 0x403921FB, 0x403AB41B, 0x403C463A, 0x403DD85A, 0x403F6A7A, 0x40407E4C,
        0x4041475C, 0x4042106C, 0x4042D97C, 0x4043A28C, 0x40446B9C, 0x404534AC, 0x4045FDBB,
        0x4046C6CB, 0x40478FDB, 0x404858EB, 0x404921FB
    };

    // pi/2 split into 24-bit pieces, for reconstructing the reduced value in kernelRemPioTwo.
    private static final double[] PIo2 = {
        1.57079625129699707031e+00, 7.54978941586159635335e-08, 5.39030252995776476554e-15,
        3.28200341580791294123e-22, 1.27065575308067607349e-29, 1.22933308981111328932e-36,
        2.73370053816464559624e-44, 2.16741683877804819444e-51
    };

    private static final int[] init_jk = {2, 3, 4, 6};
    private static final double two24 = 1.67772160000000000000e+07;
    private static final double twon24 = 5.96046447753906250000e-08;
    private static final double zero = 0.0d;
    private static final double one = 1.0d;
    private static final double half = 0.5d;
    private static final double invpio2 = 6.36619772367581382433e-01;
    private static final double pio2_1 = 1.57079632673412561417e+00;
    private static final double pio2_1t = 6.07710050650619224932e-11;
    private static final double pio2_2 = 6.07710050630396597660e-11;
    private static final double pio2_2t = 2.02226624879595063154e-21;
    private static final double pio2_3 = 2.02226624871116645580e-21;
    private static final double pio2_3t = 8.47842766036889956997e-32;

    // fdlibm k_rem_pio2.c: multi-precision reduction for large arguments.
    private static int kernelRemPioTwo(double[] x, double[] y, int e0, int nx, int prec) {
        int jz, jx, jv, jp, jk, carry, n, i, j, k, m, q0, ih;
        double z, fw;
        int[] iq = new int[20];
        double[] f = new double[20];
        double[] fq = new double[20];
        double[] q = new double[20];
        jk = init_jk[prec];
        jp = jk;
        jx = nx - 1;
        jv = (e0 - 3) / 24;
        if (jv < 0) {
            jv = 0;
        }
        q0 = e0 - 24 * (jv + 1);
        j = jv - jx;
        m = jx + jk;
        for (i = 0; i <= m; i++) {
            f[i] = (j < 0) ? zero : (double) ipio2[j];
            j++;
        }
        for (i = 0; i <= jk; i++) {
            fw = 0.0d;
            for (j = 0; j <= jx; j++) {
                fw += x[j] * f[jx + i - j];
            }
            q[i] = fw;
        }
        jz = jk;
        while (true) {
            i = 0;
            j = jz;
            z = q[jz];
            while (j > 0) {
                fw = (double) ((int) (twon24 * z));
                iq[i] = (int) (z - two24 * fw);
                z = q[j - 1] + fw;
                i++;
                j--;
            }
            z = scalbn(z, q0);
            z -= 8.0d * Math.floor(z * 0.125d);
            n = (int) z;
            z -= (double) n;
            ih = 0;
            if (q0 > 0) {
                i = (iq[jz - 1] >> (24 - q0));
                n += i;
                iq[jz - 1] -= i << (24 - q0);
                ih = iq[jz - 1] >> (23 - q0);
            } else if (q0 == 0) {
                ih = iq[jz - 1] >> 23;
            } else if (z >= 0.5d) {
                ih = 2;
            }
            if (ih > 0) {
                n += 1;
                carry = 0;
                for (i = 0; i < jz; i++) {
                    j = iq[i];
                    if (carry == 0) {
                        if (j != 0) {
                            carry = 1;
                            iq[i] = 0x1000000 - j;
                        }
                    } else {
                        iq[i] = 0xffffff - j;
                    }
                }
                if (q0 > 0) {
                    if (q0 == 1) {
                        iq[jz - 1] &= 0x7fffff;
                    } else if (q0 == 2) {
                        iq[jz - 1] &= 0x3fffff;
                    }
                }
                if (ih == 2) {
                    z = one - z;
                    if (carry != 0) {
                        z -= scalbn(one, q0);
                    }
                }
            }
            if (z == zero) {
                j = 0;
                for (i = jz - 1; i >= jk; i--) {
                    j |= iq[i];
                }
                if (j == 0) {
                    for (k = 1; iq[jk - k] == 0; k++) {
                    }
                    for (i = jz + 1; i <= jz + k; i++) {
                        f[jx + i] = (double) ipio2[jv + i];
                        fw = 0.0d;
                        for (j = 0; j <= jx; j++) {
                            fw += x[j] * f[jx + i - j];
                        }
                        q[i] = fw;
                    }
                    jz += k;
                    continue;
                }
            }
            break;
        }
        if (z == 0.0d) {
            jz -= 1;
            q0 -= 24;
            while (iq[jz] == 0) {
                jz--;
                q0 -= 24;
            }
        } else {
            z = scalbn(z, -q0);
            if (z >= two24) {
                fw = (double) ((int) (twon24 * z));
                iq[jz] = (int) (z - two24 * fw);
                jz += 1;
                q0 += 24;
                iq[jz] = (int) fw;
            } else {
                iq[jz] = (int) z;
            }
        }
        fw = scalbn(one, q0);
        for (i = jz; i >= 0; i--) {
            q[i] = fw * (double) iq[i];
            fw *= twon24;
        }
        for (i = jz; i >= 0; i--) {
            fw = 0.0d;
            for (k = 0; k <= jp && k <= jz - i; k++) {
                fw += PIo2[k] * q[i + k];
            }
            fq[jz - i] = fw;
        }
        switch (prec) {
            case 0:
                fw = 0.0d;
                for (i = jz; i >= 0; i--) {
                    fw += fq[i];
                }
                y[0] = (ih == 0) ? fw : -fw;
                break;
            case 1:
            case 2:
                fw = 0.0d;
                for (i = jz; i >= 0; i--) {
                    fw += fq[i];
                }
                y[0] = (ih == 0) ? fw : -fw;
                fw = fq[0] - fw;
                for (i = 1; i <= jz; i++) {
                    fw += fq[i];
                }
                y[1] = (ih == 0) ? fw : -fw;
                break;
            case 3:
                for (i = jz; i > 0; i--) {
                    fw = fq[i - 1] + fq[i];
                    fq[i] += fq[i - 1] - fw;
                    fq[i - 1] = fw;
                }
                for (i = jz; i > 1; i--) {
                    fw = fq[i - 1] + fq[i];
                    fq[i] += fq[i - 1] - fw;
                    fq[i - 1] = fw;
                }
                fw = 0.0d;
                for (i = jz; i >= 2; i--) {
                    fw += fq[i];
                }
                if (ih == 0) {
                    y[0] = fq[0];
                    y[1] = fq[1];
                    y[2] = fw;
                } else {
                    y[0] = -fq[0];
                    y[1] = -fq[1];
                    y[2] = -fw;
                }
                break;
            default:
                break;
        }
        return n & 7;
    }

    // fdlibm e_rem_pio2.c: reduce x to y[0]+y[1] in [-pi/4, pi/4], returning the quadrant.
    private static int remPioTwo(double x, double[] y) {
        double z;
        double w;
        double t;
        double r;
        double fn;
        double[] tx = new double[3];
        int e0;
        int i;
        int j;
        int nx;
        int n;
        int ix;
        int hx;
        int low;
        hx = hi(x);
        ix = hx & 0x7fffffff;
        if (ix <= 0x3fe921fb) {           // |x| <= pi/4
            y[0] = x;
            y[1] = 0;
            return 0;
        }
        if (ix < 0x4002d97c) {            // |x| < 3pi/4: n = +-1
            if (hx > 0) {
                z = x - pio2_1;
                if (ix != 0x3ff921fb) {
                    y[0] = z - pio2_1t;
                    y[1] = (z - y[0]) - pio2_1t;
                } else {
                    z -= pio2_2;
                    y[0] = z - pio2_2t;
                    y[1] = (z - y[0]) - pio2_2t;
                }
                return 1;
            } else {
                z = x + pio2_1;
                if (ix != 0x3ff921fb) {
                    y[0] = z + pio2_1t;
                    y[1] = (z - y[0]) + pio2_1t;
                } else {
                    z += pio2_2;
                    y[0] = z + pio2_2t;
                    y[1] = (z - y[0]) + pio2_2t;
                }
                return -1;
            }
        }
        if (ix <= 0x413921fb) {           // |x| <= 2^19*(pi/2): medium size
            t = Math.abs(x);
            n = (int) (t * invpio2 + half);
            fn = (double) n;
            r = t - fn * pio2_1;
            w = fn * pio2_1t;
            if (n < 32 && ix != npio2_hw[n - 1]) {
                y[0] = r - w;
            } else {
                j = ix >> 20;
                y[0] = r - w;
                int high = hi(y[0]);
                i = j - ((high >> 20) & 0x7ff);
                if (i > 16) {
                    t = r;
                    w = fn * pio2_2;
                    r = t - w;
                    w = fn * pio2_2t - ((t - r) - w);
                    y[0] = r - w;
                    high = hi(y[0]);
                    i = j - ((high >> 20) & 0x7ff);
                    if (i > 49) {
                        t = r;
                        w = fn * pio2_3;
                        r = t - w;
                        w = fn * pio2_3t - ((t - r) - w);
                        y[0] = r - w;
                    }
                }
            }
            y[1] = (r - y[0]) - w;
            if (hx < 0) {
                y[0] = -y[0];
                y[1] = -y[1];
                return -n;
            }
            return n;
        }
        if (ix >= 0x7ff00000) {           // inf or NaN
            y[0] = y[1] = x - x;
            return 0;
        }
        low = lo(x);
        z = withLo(0.0d, low);
        e0 = (ix >> 20) - 1046;
        z = withHi(z, ix - (e0 << 20));
        for (i = 0; i < 2; i++) {
            tx[i] = (double) ((int) z);
            z = (z - tx[i]) * two24;
        }
        tx[2] = z;
        nx = 3;
        while (tx[nx - 1] == zero) {
            nx--;
        }
        n = kernelRemPioTwo(tx, y, e0, nx, 2);
        if (hx < 0) {
            y[0] = -y[0];
            y[1] = -y[1];
            return -n;
        }
        return n;
    }

    // ---- kernels on the reduced range ----

    private static final double S1 = -1.66666666666666324348e-01;
    private static final double S2 = 8.33333333332248946124e-03;
    private static final double S3 = -1.98412698298579493134e-04;
    private static final double S4 = 2.75573137070700676789e-06;
    private static final double S5 = -2.50507602534068634195e-08;
    private static final double S6 = 1.58969099521155010221e-10;

    private static double kernelSin(double x, double y, int iy) {
        double z;
        double r;
        double v;
        int ix = hi(x) & 0x7fffffff;
        if (ix < 0x3e400000) {
            if ((int) x == 0) {
                return x;
            }
        }
        z = x * x;
        v = z * x;
        r = S2 + z * (S3 + z * (S4 + z * (S5 + z * S6)));
        if (iy == 0) {
            return x + v * (S1 + z * r);
        }
        return x - ((z * (half * y - v * r) - y) - v * S1);
    }

    private static final double C1 = 4.16666666666666019037e-02;
    private static final double C2 = -1.38888888888741095749e-03;
    private static final double C3 = 2.48015872894767294178e-05;
    private static final double C4 = -2.75573143513906633035e-07;
    private static final double C5 = 2.08757232129817482790e-09;
    private static final double C6 = -1.13596475577881948265e-11;

    private static double kernelCos(double x, double y) {
        double a;
        double hz;
        double z;
        double r;
        double qx;
        int ix = hi(x) & 0x7fffffff;
        if (ix < 0x3e400000) {
            if (((int) x) == 0) {
                return one;
            }
        }
        z = x * x;
        r = z * (C1 + z * (C2 + z * (C3 + z * (C4 + z * (C5 + z * C6)))));
        if (ix < 0x3FD33333) {
            return one - (0.5d * z - (z * r - x * y));
        }
        if (ix > 0x3fe90000) {
            qx = 0.28125d;
        } else {
            qx = withLo(withHi(0.0d, ix - 0x00200000), 0);
        }
        hz = 0.5d * z - qx;
        a = one - qx;
        return a - (hz - (z * r - x * y));
    }

    private static final double pio4 = 7.85398163397448278999e-01;
    private static final double pio4lo = 3.06161699786838301793e-17;
    private static final double[] T = {
        3.33333333333334091986e-01, 1.33333333333201242699e-01, 5.39682539762260521377e-02,
        2.18694882948595424599e-02, 8.86323982359930005737e-03, 3.59207910759131235356e-03,
        1.45620945432529025516e-03, 5.88041240820264096874e-04, 2.46463134818469906812e-04,
        7.81794442939557092300e-05, 7.14072491382608190305e-05, -1.85586374855275456654e-05,
        2.59073051863633712884e-05
    };

    private static double kernelTan(double x, double y, int iy) {
        double z;
        double r;
        double v;
        double w;
        double s;
        int hx = hi(x);
        int ix = hx & 0x7fffffff;
        if (ix < 0x3e300000) {
            if ((int) x == 0) {
                int low = lo(x);
                if (((ix | low) | (iy + 1)) == 0) {
                    return one / Math.abs(x);
                }
                return (iy == 1) ? x : -one / x;
            }
        }
        if (ix >= 0x3FE59428) {
            if (hx < 0) {
                x = -x;
                y = -y;
            }
            z = pio4 - x;
            w = pio4lo - y;
            x = z + w;
            y = 0.0d;
        }
        z = x * x;
        w = z * z;
        r = T[1] + w * (T[3] + w * (T[5] + w * (T[7] + w * (T[9] + w * T[11]))));
        v = z * (T[2] + w * (T[4] + w * (T[6] + w * (T[8] + w * (T[10] + w * T[12])))));
        s = z * x;
        r = y + z * (s * (r + v) + y);
        r += T[0] * s;
        w = x + r;
        if (ix >= 0x3FE59428) {
            v = (double) iy;
            return (double) (1 - ((hx >> 30) & 2)) * (v - 2.0d * (x - (w * w / (w + v) - r)));
        }
        if (iy == 1) {
            return w;
        }
        double a;
        double tt;
        z = w;
        z = withLo(z, 0);
        v = r - (z - x);
        tt = a = -1.0d / w;
        tt = withLo(tt, 0);
        s = 1.0d + tt * z;
        return tt + a * (s + tt * v);
    }

    // ---- entry points (Math delegates here) ----

    static double sin(double x) {
        double[] y = new double[2];
        int ix = hi(x) & 0x7fffffff;
        if (ix <= 0x3fe921fb) {
            return kernelSin(x, 0.0d, 0);
        } else if (ix >= 0x7ff00000) {
            return x - x;
        }
        int n = remPioTwo(x, y);
        switch (n & 3) {
            case 0:
                return kernelSin(y[0], y[1], 1);
            case 1:
                return kernelCos(y[0], y[1]);
            case 2:
                return -kernelSin(y[0], y[1], 1);
            default:
                return -kernelCos(y[0], y[1]);
        }
    }

    static double cos(double x) {
        double[] y = new double[2];
        int ix = hi(x) & 0x7fffffff;
        if (ix <= 0x3fe921fb) {
            return kernelCos(x, 0.0d);
        } else if (ix >= 0x7ff00000) {
            return x - x;
        }
        int n = remPioTwo(x, y);
        switch (n & 3) {
            case 0:
                return kernelCos(y[0], y[1]);
            case 1:
                return -kernelSin(y[0], y[1], 1);
            case 2:
                return -kernelCos(y[0], y[1]);
            default:
                return kernelSin(y[0], y[1], 1);
        }
    }

    static double tan(double x) {
        double[] y = new double[2];
        int ix = hi(x) & 0x7fffffff;
        if (ix <= 0x3fe921fb) {
            return kernelTan(x, 0.0d, 1);
        } else if (ix >= 0x7ff00000) {
            return x - x;
        }
        int n = remPioTwo(x, y);
        return kernelTan(y[0], y[1], 1 - ((n & 1) << 1));
    }
}
