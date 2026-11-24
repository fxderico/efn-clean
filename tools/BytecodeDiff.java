import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Compares the instruction streams of every method in two sets of classes (the original jar vs.
 * classes recompiled from this source tree) after normalising away differences that cannot change
 * behaviour: local-variable slot numbers, CHECKCASTs, labels/frames/line numbers, and the numbering
 * of synthetic lambda methods. Prints every method whose normalised body differs.
 *
 * Usage: java -cp asm.jar:asm-tree.jar BytecodeDiff.java <original.jar> <classes-dir-or-jar> [skipPrefix...]
 */
public class BytecodeDiff {
    public static void main(String[] a) throws Exception {
        if (a[0].equals("--dump")) {   // --dump <jar-or-dir> <outdir> <class/Name.method>...
            Map<String, ClassNode> cs = load(Path.of(a[1]));
            for (int i = 3; i < a.length; i++) {
                String cls = a[i].substring(0, a[i].lastIndexOf('.')), mth = a[i].substring(a[i].lastIndexOf('.') + 1);
                ClassNode cn = cs.get(cls);
                if (cn == null) { System.out.println("no class " + cls); continue; }
                for (MethodNode mn : cn.methods) if (mn.name.equals(mth))
                    Files.write(Path.of(a[2], cls.replace('/', '_') + "." + mth + mn.desc.replaceAll("[^A-Za-z0-9]", "_") + ".txt"), norm(mn));
            }
            return;
        }
        Map<String, ClassNode> orig = load(Path.of(a[0])), mine = load(Path.of(a[1]));
        Set<String> skip = new HashSet<>();
        Path dumpDir = null;
        for (int i = 2; i < a.length; i++) {
            if (a[i].startsWith("--dumpdiffs=")) { dumpDir = Path.of(a[i].substring(12)); Files.createDirectories(dumpDir); }
            else skip.add(a[i]);
        }
        int classes = 0, identical = 0, methods = 0, same = 0;
        List<String> report = new ArrayList<>();
        for (var e : new TreeMap<>(orig).entrySet()) {
            String name = e.getKey();
            if (skip.stream().anyMatch(name::startsWith)) continue;
            classes++;
            ClassNode o = e.getValue(), m = mine.get(name);
            if (m == null) { report.add("MISSING CLASS " + name); continue; }
            Map<String, List<String>> om = bodies(o), mm = bodies(m);
            boolean classSame = true;
            for (var me : om.entrySet()) {
                methods++;
                List<String> mb = mm.get(me.getKey());
                if (mb == null) { report.add("MISSING METHOD " + name + "." + me.getKey()); classSame = false; continue; }
                if (mb.equals(me.getValue())) { same++; continue; }
                classSame = false;
                if (dumpDir != null) {
                    String base = (name + "." + me.getKey()).replaceAll("[^A-Za-z0-9.$_]", "_");
                    if (base.length() > 180) base = base.substring(0, 160) + Integer.toHexString(base.hashCode());
                    Files.write(dumpDir.resolve(base + ".orig"), me.getValue());
                    Files.write(dumpDir.resolve(base + ".mine"), mb);
                }
                report.add(String.format("DIFF %s.%s  orig=%d mine=%d  sim=%.2f", name, me.getKey(), me.getValue().size(), mb.size(), sim(me.getValue(), mb)));
            }
            for (String k : mm.keySet()) if (!om.containsKey(k)) { report.add("EXTRA METHOD " + name + "." + k); classSame = false; }
            if (classSame) identical++;
        }
        report.forEach(System.out::println);
        System.out.printf("SUMMARY classes=%d identical=%d methods=%d identicalMethods=%d%n", classes, identical, methods, same);
    }

    static Map<String, ClassNode> load(Path p) throws IOException {
        Map<String, ClassNode> r = new HashMap<>();
        if (Files.isDirectory(p)) {
            try (var s = Files.walk(p)) {
                for (Path f : s.filter(x -> x.toString().endsWith(".class")).toList()) add(r, Files.readAllBytes(f));
            }
        } else try (ZipInputStream z = new ZipInputStream(new FileInputStream(p.toFile()))) {
            for (ZipEntry e; (e = z.getNextEntry()) != null; ) if (e.getName().endsWith(".class")) add(r, z.readAllBytes());
        }
        return r;
    }

    static void add(Map<String, ClassNode> r, byte[] b) {
        ClassNode cn = new ClassNode();
        new ClassReader(b).accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        r.put(cn.name, cn);
    }

    /** method key -> normalised body. Synthetic lambdas are keyed by descriptor + body hash so numbering doesn't matter. */
    static Map<String, List<String>> bodies(ClassNode cn) {
        Map<String, List<String>> r = new TreeMap<>();
        Map<String, Integer> lambdaSeen = new HashMap<>();
        for (MethodNode mn : cn.methods) {
            List<String> body = norm(mn);
            String key = mn.name + mn.desc;
            if (mn.name.startsWith("lambda$")) {
                String base = "lambda" + mn.desc + "#" + Integer.toHexString(body.hashCode());
                int n = lambdaSeen.merge(base, 1, Integer::sum);
                key = base + "/" + n;
            }
            r.put(key, body);
        }
        return r;
    }

    /** Conditional jumps compared as families, so an inverted branch (IFEQ vs IFNE + GOTO) still matches. */
    static String jumpFamily(int op) {
        return switch (op) {
            case Opcodes.IFEQ, Opcodes.IFNE -> "IF_Z";
            case Opcodes.IFLT, Opcodes.IFGE -> "IF_LT_GE";
            case Opcodes.IFGT, Opcodes.IFLE -> "IF_GT_LE";
            case Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE -> "IF_ICMP_EQ_NE";
            case Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE -> "IF_ICMP_LT_GE";
            case Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE -> "IF_ICMP_GT_LE";
            case Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE -> "IF_ACMP";
            case Opcodes.IFNULL, Opcodes.IFNONNULL -> "IF_NULL";
            default -> Printer.OPCODES[op];
        };
    }

    static List<String> norm(MethodNode mn) {
        List<String> out = new ArrayList<>();
        // locals renumbered by first use: catches "used the wrong variable" while ignoring slot allocation
        Map<Integer, Integer> canon = new HashMap<>();
        int params = (mn.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
        for (var t : org.objectweb.asm.Type.getArgumentTypes(mn.desc)) params += t.getSize();
        for (AbstractInsnNode in : mn.instructions) {
            int op = in.getOpcode();
            if (op < 0 || op == Opcodes.GOTO) continue;           // labels/frames/lines; block layout
            String s = Printer.OPCODES[op];
            switch (in) {
                case TypeInsnNode t -> { if (op == Opcodes.CHECKCAST) continue; s += " " + t.desc; }
                case VarInsnNode v -> s += " " + var(canon, v.var, params);
                case IincInsnNode i -> s += " " + var(canon, i.var, params) + " " + i.incr;
                case IntInsnNode i -> s += " " + i.operand;
                case LdcInsnNode l -> s += " " + l.cst;
                case JumpInsnNode j -> s = jumpFamily(op);
                case FieldInsnNode f -> s += " " + f.owner + "." + (f.name.startsWith("val$") ? "val$" : f.name) + ":" + f.desc;
                case MethodInsnNode m -> s += " " + m.owner + "." + lam(m.name) + m.desc;
                case InvokeDynamicInsnNode d -> {
                    s += " " + d.name + d.desc;
                    for (Object arg : d.bsmArgs) s += " " + (arg instanceof Handle h ? h.getOwner() + "." + lam(h.getName()) + h.getDesc() : String.valueOf(arg));
                }
                case TableSwitchInsnNode t -> s += " " + t.min + ".." + t.max;
                case LookupSwitchInsnNode l -> s += " " + l.keys;
                case MultiANewArrayInsnNode m -> s += " " + m.desc + " " + m.dims;
                default -> { }
            }
            out.add(s);
        }
        // "DUP; xSTORE v" == "xSTORE v; xLOAD v"
        for (int i = 0; i + 1 < out.size(); i++) {
            String a = out.get(i), b = out.get(i + 1);
            if ((a.equals("DUP") || a.equals("DUP2")) && b.matches("[ILFDA]STORE .*")) {
                out.set(i, b);
                out.set(i + 1, b.charAt(0) + "LOAD" + b.substring(6));
            }
        }
        return out;
    }

    static String var(Map<Integer, Integer> canon, int slot, int params) {
        if (slot < params) return "p" + slot;
        return "v" + canon.computeIfAbsent(slot, k -> canon.size());
    }

    static String lam(String n) { return n.startsWith("lambda$") ? "lambda$" : n; }

    static double sim(List<String> x, List<String> y) {   // LCS ratio
        int[][] d = new int[x.size() + 1][y.size() + 1];
        for (int i = 1; i <= x.size(); i++) for (int j = 1; j <= y.size(); j++)
            d[i][j] = x.get(i - 1).equals(y.get(j - 1)) ? d[i - 1][j - 1] + 1 : Math.max(d[i - 1][j], d[i][j - 1]);
        return 2.0 * d[x.size()][y.size()] / Math.max(1, x.size() + y.size());
    }
}
