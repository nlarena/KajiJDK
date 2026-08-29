public class Casts {
  public long i2l(int x) { return (long) x; }
  public double i2d(int x) { return (double) x; }
  public int l2i(long x) { return (int) x; }
  public int d2i(double x) { return (int) x; }
  public float d2f(double x) { return (float) x; }
  public byte i2b(int x) { return (byte) x; }
  public char i2c(int x) { return (char) x; }
  public short i2s(int x) { return (short) x; }
  public double mix(int a, long b, float c) { return a + b + c; }
}
