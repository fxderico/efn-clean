// Lists checkcast/instanceof types in the built jar that the original method never used.
// Decompilers sometimes invent a wrong cast; BytecodeDiff ignores casts, so check them here.
// java -cp asm.jar:asm-tree.jar tools/CastDiff.java <original.jar> build/libs/<built>.jar
import java.util.*; import java.util.zip.*; import java.io.*;
import org.objectweb.asm.*; import org.objectweb.asm.tree.*;
public class CastDiff {
  static Map<String,ClassNode> load(String p) throws Exception { Map<String,ClassNode> m=new HashMap<>(); try(ZipFile z=new ZipFile(p)){ for(var e:Collections.list(z.entries())){ if(!e.getName().endsWith(".class")) continue; ClassNode cn=new ClassNode(); new ClassReader(z.getInputStream(e).readAllBytes()).accept(cn,0); m.put(cn.name,cn);} } return m; }
  static Map<String,Integer> types(MethodNode mn){ Map<String,Integer> s=new TreeMap<>(); for(var in:mn.instructions){ if(in instanceof TypeInsnNode t && (t.getOpcode()==Opcodes.CHECKCAST||t.getOpcode()==Opcodes.INSTANCEOF)) s.merge((t.getOpcode()==Opcodes.CHECKCAST?"cast ":"inst ")+t.desc,1,Integer::sum);} return s; }
  public static void main(String[] a) throws Exception { var o=load(a[0]); var b=load(a[1]); int n=0;
    for(var e:b.entrySet()){ ClassNode oc=o.get(e.getKey()); if(oc==null) continue;
      for(MethodNode bm:e.getValue().methods){ MethodNode om=null; for(MethodNode x:oc.methods) if(x.name.equals(bm.name)&&x.desc.equals(bm.desc)) om=x; if(om==null) continue;
        var ot=types(om); var bt=types(bm); for(var t:bt.keySet()) if(!ot.containsKey(t) && !t.endsWith("java/lang/Object")) { System.out.println(e.getKey()+"."+bm.name+bm.desc+"  EXTRA "+t+"   orig="+ot.keySet()); n++; } } }
    System.out.println("total "+n); } }
