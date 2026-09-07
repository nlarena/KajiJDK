import javax.swing.*; import javax.swing.text.*;
public class TPT { public static int run(){
  JEditorPane p = new JEditorPane();
  EditorKit k = new StyledEditorKit();
  String ct = k.getContentType();
  System.out.println("//ct="+ct);
  System.out.println("//nombre="+JEditorPane.getEditorKitClassNameForContentType(ct));
  System.out.println("//nombre plain="+JEditorPane.getEditorKitClassNameForContentType("text/plain"));
  try { p.setEditorKitForContentType(ct, k); System.out.println("//registro ok"); } catch (Throwable e){ System.out.println("//registro: "+e); }
  try { p.setEditorKit(k); System.out.println("//setEditorKit ok"); } catch (Throwable e){ System.out.println("//setEditorKit: "+e); }
  return 0; } }
