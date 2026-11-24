import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Mixin target-selector tool.
 *
 *   dump  <orig.jar> <mc-intermediary.jar> <yarn.tiny> <out.tsv>
 *       For every injector in the original (intermediary) jar, resolve each `method` selector to the
 *       exact intermediary method (name+desc) and write its Yarn named selector WITH descriptor:
 *       mixinClass \t handler \t annotation \t index \t namedSelector \t intermediaryNameDesc
 *
 *   check <orig.jar> <built.jar> <mc-intermediary.jar>
 *       Resolve selectors in both jars against the intermediary MC jar and report any handler whose
 *       targets differ (or don't resolve uniquely).
 */
public class MixinTargets {
    static final Set<String> INJECTORS = Set.of(
            "Lorg/spongepowered/asm/mixin/injection/Inject;", "Lorg/spongepowered/asm/mixin/injection/Redirect;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyArg;", "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;", "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;",
            "Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;", "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;",
            "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;", "Lcom/llamalad7/mixinextras/injector/v2/WrapWithCondition;",
            "Lcom/llamalad7/mixinextras/injector/WrapWithCondition;", "Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;");

    public static void main(String[] a) throws Exception {
        if (a[0].equals("dump")) dump(a[1], a[2], a[3], a[4]);
        else check(a[1], a[2], a[3]);
    }

    static Map<String, ClassNode> load(String jar, boolean code) throws IOException {
        Map<String, ClassNode> m = new HashMap<>();
        try (ZipInputStream z = new ZipInputStream(new FileInputStream(jar))) {
            for (ZipEntry e; (e = z.getNextEntry()) != null; )
                if (e.getName().endsWith(".class")) {
                    ClassNode cn = new ClassNode();
                    new ClassReader(z.readAllBytes()).accept(cn, code ? 0 : ClassReader.SKIP_CODE);
                    m.put(cn.name, cn);
                }
        }
        return m;
    }

    record Sel(String mixin, String handler, String ann, int idx, String target, String raw) {}

    /** All injector selectors in a mixin jar. */
    static List<Sel> selectors(Map<String, ClassNode> jar) {
        List<Sel> out = new ArrayList<>();
        for (ClassNode cn : jar.values()) {
            List<String> targets = mixinTargets(cn);
            if (targets.isEmpty()) continue;
            for (MethodNode mn : cn.methods) {
                List<AnnotationNode> anns = new ArrayList<>();
                if (mn.visibleAnnotations != null) anns.addAll(mn.visibleAnnotations);
                if (mn.invisibleAnnotations != null) anns.addAll(mn.invisibleAnnotations);
                for (AnnotationNode an : anns) {
                    if (!INJECTORS.contains(an.desc)) continue;
                    List<String> methods = values(an, "method");
                    for (int i = 0; i < methods.size(); i++)
                        for (String t : targets) out.add(new Sel(cn.name, mn.name, an.desc, i, t, methods.get(i)));
                }
            }
        }
        out.sort(Comparator.comparing((Sel s) -> s.mixin + "." + s.handler + "/" + s.ann + "/" + s.idx));
        return out;
    }

    @SuppressWarnings("unchecked")
    static List<String> values(AnnotationNode an, String key) {
        if (an.values == null) return List.of();
        for (int i = 0; i < an.values.size(); i += 2)
            if (an.values.get(i).equals(key)) {
                Object v = an.values.get(i + 1);
                return v instanceof List<?> l ? (List<String>) l : List.of((String) v);
            }
        return List.of();
    }

    static List<String> mixinTargets(ClassNode cn) {
        List<String> r = new ArrayList<>();
        List<AnnotationNode> anns = new ArrayList<>();
        if (cn.visibleAnnotations != null) anns.addAll(cn.visibleAnnotations);
        if (cn.invisibleAnnotations != null) anns.addAll(cn.invisibleAnnotations);
        for (AnnotationNode an : anns) {
            if (!an.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) continue;
            for (int i = 0; i < an.values.size(); i += 2) {
                Object v = an.values.get(i + 1);
                if (an.values.get(i).equals("value")) for (Object t : (List<?>) v) r.add(((Type) t).getInternalName());
                if (an.values.get(i).equals("targets")) for (Object t : (List<?>) v) r.add(((String) t).replace('.', '/'));
            }
        }
        return r;
    }

    /** Resolve a selector (name, name+desc, or with owner prefix) to exact methods of the target class. */
    static List<MethodNode> resolve(ClassNode target, String sel) {
        String s = sel;
        if (s.startsWith("L") && s.contains(";")) s = s.substring(s.indexOf(';') + 1);   // Lowner;name(desc)
        String name = s.contains("(") ? s.substring(0, s.indexOf('(')) : s;
        String desc = s.contains("(") ? s.substring(s.indexOf('(')) : null;
        List<MethodNode> r = new ArrayList<>();
        for (MethodNode m : target.methods)
            if (m.name.equals(name) && (desc == null || m.desc.equals(desc))) r.add(m);
        if (r.size() > 1 && desc == null) {   // prefer non-synthetic (bridge) overloads like Mixin does
            List<MethodNode> real = r.stream().filter(m -> (m.access & 0x1000) == 0).toList();
            if (!real.isEmpty()) r = new ArrayList<>(real);
        }
        return r;
    }

    // --- yarn tiny v2: intermediary -> named ------------------------------------------------

    static Map<String, String> clsMap = new HashMap<>();          // intermediary class -> named
    static Map<String, String> mthMap = new HashMap<>();          // intClass.intName+intDesc -> namedName
    static Map<String, String> mthGlobal = new HashMap<>();       // intName -> namedName (intermediary names are global)

    static void loadTiny(String tiny) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(tiny));
        String[] header = lines.get(0).split("\t");
        int iInt = Arrays.asList(header).indexOf("intermediary") - 3, iNamed = Arrays.asList(header).indexOf("named") - 3;
        String curOff = null, curInt = null;
        // first pass classes, second pass members (descs in tiny are in the first namespace: official)
        Map<String, String> offToInt = new HashMap<>();
        for (String l : lines) {
            if (l.startsWith("c\t")) {
                String[] p = l.split("\t");
                offToInt.put(p[1], p[1 + iInt]);
                clsMap.put(p[1 + iInt], p[1 + iNamed]);
            }
        }
        for (String l : lines) {
            if (l.startsWith("c\t")) { String[] p = l.split("\t"); curOff = p[1]; curInt = p[1 + iInt]; continue; }
            if (l.startsWith("\tm\t")) {
                String[] p = l.substring(1).split("\t");
                String offDesc = p[1];
                String intDesc = mapDesc(offDesc, offToInt);
                mthMap.put(curInt + "." + p[2 + iInt] + intDesc, p[2 + iNamed]);
                if (p[2 + iInt].startsWith("method_")) mthGlobal.put(p[2 + iInt], p[2 + iNamed]);
            }
        }
    }

    static String mapDesc(String desc, Map<String, String> m) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < desc.length(); i++) {
            char c = desc.charAt(i);
            if (c == 'L') {
                int e = desc.indexOf(';', i);
                String n = desc.substring(i + 1, e);
                sb.append('L').append(m.getOrDefault(n, n)).append(';');
                i = e;
            } else sb.append(c);
        }
        return sb.toString();
    }

    static void dump(String orig, String mc, String tiny, String out) throws Exception {
        loadTiny(tiny);
        Map<String, ClassNode> mcj = load(mc, false), o = load(orig, false);
        List<String> rows = new ArrayList<>();
        int bad = 0;
        for (Sel s : selectors(o)) {
            ClassNode t = mcj.get(s.target);
            if (t == null) { rows.add(row(s, "?", "TARGET-NOT-IN-MC")); continue; }
            List<MethodNode> r = resolve(t, s.raw);
            if (r.size() != 1) { bad++; rows.add(row(s, "?", "AMBIGUOUS-OR-MISSING:" + r.size())); continue; }
            MethodNode m = r.get(0);
            String named = mthMap.getOrDefault(s.target + "." + m.name + m.desc, mthGlobal.getOrDefault(m.name, m.name));
            rows.add(row(s, named + mapDesc(m.desc, clsMap), m.name + m.desc));
        }
        Files.write(Path.of(out), rows);
        System.out.println("selectors=" + rows.size() + " unresolved=" + bad);
    }

    static String row(Sel s, String named, String inter) {
        return String.join("\t", s.mixin, s.handler, s.ann, String.valueOf(s.idx), named, inter);
    }

    static void check(String orig, String built, String mc) throws Exception {
        Map<String, ClassNode> mcj = load(mc, false);
        Map<String, String> want = new TreeMap<>(), got = new TreeMap<>();
        for (Sel s : selectors(load(orig, false))) want.put(key(s), res(mcj, s));
        Map<String, Map<String, String>> refmap = refmap(built);
        for (Sel s : selectors(load(built, false))) {
            String mapped = refmap.getOrDefault(s.mixin, Map.of()).get(s.raw);
            got.put(key(s), res(mcj, mapped == null ? s : new Sel(s.mixin, s.handler, s.ann, s.idx, s.target, mapped)));
        }
        int ok = 0, bad = 0;
        for (var e : want.entrySet()) {
            String g = got.get(e.getKey());
            if (e.getValue().equals(g)) ok++;
            else { bad++; System.out.println("MISMATCH " + e.getKey() + "\n   original: " + e.getValue() + "\n   built:    " + g); }
        }
        for (String k : got.keySet()) if (!want.containsKey(k)) { bad++; System.out.println("EXTRA " + k + " -> " + got.get(k)); }
        System.out.println("selectors ok=" + ok + " mismatched=" + bad);
    }

    /** refmap.json "mappings": mixinClass -> (named selector -> intermediary selector). */
    static Map<String, Map<String, String>> refmap(String jar) throws IOException {
        Map<String, Map<String, String>> r = new HashMap<>();
        try (ZipInputStream z = new ZipInputStream(new FileInputStream(jar))) {
            for (ZipEntry e; (e = z.getNextEntry()) != null; ) {
                if (!e.getName().endsWith("refmap.json")) continue;
                String json = new String(z.readAllBytes());
                int mi = json.indexOf("\"mappings\"");
                int start = json.indexOf('{', mi), depth = 0, end = start;
                for (int i = start; i < json.length(); i++) {
                    char c = json.charAt(i);
                    if (c == '{') depth++; else if (c == '}' && --depth == 0) { end = i; break; }
                }
                var cls = java.util.regex.Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\{([^}]*)\\}").matcher(json.substring(start + 1, end));
                while (cls.find()) {
                    Map<String, String> mm = r.computeIfAbsent(cls.group(1), k -> new HashMap<>());
                    var kv = java.util.regex.Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"").matcher(cls.group(2));
                    while (kv.find()) mm.put(kv.group(1), kv.group(2));
                }
            }
        }
        return r;
    }

    static String key(Sel s) { return s.mixin + "." + s.handler + " " + s.ann.substring(s.ann.lastIndexOf('/') + 1) + "#" + s.idx + " @" + s.target; }

    static String res(Map<String, ClassNode> mcj, Sel s) {
        ClassNode t = mcj.get(s.target);
        if (t == null) return "target-class-missing:" + s.target;
        List<MethodNode> r = resolve(t, s.raw);
        return r.size() == 1 ? r.get(0).name + r.get(0).desc : "UNRESOLVED(" + r.size() + "):" + s.raw;
    }
}
