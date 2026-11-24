import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;

import java.nio.file.*;
import java.util.*;

/** Rewrites mixin `method = {...}` selectors to the exact named descriptors from MixinTargets dump. */
public class ApplySelectors {
    public static void main(String[] a) throws Exception {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        Path src = Path.of(a[0]);
        // mixin -> handler -> annSimple -> idx -> selector
        Map<String, Map<String, Map<String, TreeMap<Integer, String>>>> m = new HashMap<>();
        for (String l : Files.readAllLines(Path.of(a[1]))) {
            String[] p = l.split("\t");
            if (p[4].equals("?")) continue;
            String ann = p[2].substring(p[2].lastIndexOf('/') + 1).replace(";", "");
            m.computeIfAbsent(p[0], k -> new HashMap<>()).computeIfAbsent(p[1], k -> new HashMap<>())
                    .computeIfAbsent(ann, k -> new TreeMap<>()).put(Integer.parseInt(p[3]), p[4]);
        }
        int changed = 0, missing = 0;
        for (var e : m.entrySet()) {
            Path f = src.resolve(e.getKey() + ".java");
            if (!Files.exists(f)) { System.out.println("no source for " + e.getKey()); missing++; continue; }
            CompilationUnit cu = StaticJavaParser.parse(f);
            boolean touched = false;
            for (MethodDeclaration md : cu.findAll(MethodDeclaration.class)) {
                var anns = e.getValue().get(md.getNameAsString());
                if (anns == null) continue;
                for (AnnotationExpr an : md.getAnnotations()) {
                    var sels = anns.get(an.getNameAsString());
                    if (sels == null || !(an instanceof NormalAnnotationExpr na)) continue;
                    for (MemberValuePair mv : na.getPairs()) {
                        if (!mv.getNameAsString().equals("method")) continue;
                        NodeList<Expression> vals = new NodeList<>();
                        for (String s : sels.values()) vals.add(new StringLiteralExpr(s));
                        mv.setValue(new ArrayInitializerExpr(vals));
                        touched = true; changed++;
                    }
                }
            }
            if (touched) Files.writeString(f, cu.toString());
        }
        System.out.println("annotations rewritten=" + changed + " missingSources=" + missing);
    }
}
