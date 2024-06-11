package org.jd.diff;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author prasadjd on 24/06/22

 */
public class JavaClassDiffGenerator {


    private static Set<String> claasToExclude = new HashSet<>();
    static {
        claasToExclude.addAll(Arrays.asList("{Class files to ignore in diff}"));
    }

    public static void main(String s[]) throws IOException {

        String leftDirPath = "{targetProjectPath}";
        String rightDirPath =  "{sourceProjectPath}";



        File leftDirFile = new File(leftDirPath);
        File rightDirFile = new File(rightDirPath);
            //TODO Renamed files need to handle
        BufferedWriter fileWriter = null;
        FileWriter fileWriter1 = null;
        try {
            File finalFile = new File("{Outputfile path}");
            fileWriter1 = new FileWriter(finalFile);
            fileWriter = new BufferedWriter(fileWriter1);
        process(leftDirFile, rightDirFile, fileWriter);
    }finally {
        if(!Objects.isNull(fileWriter)){
            fileWriter.flush();

            fileWriter1.close();
            fileWriter.close();
        }
    }

    }

    private static void process(File leftDirFile, File rightDirFile, BufferedWriter fileWriter) throws IOException {

            if (leftDirFile.isDirectory()) {

                File[] files = leftDirFile.listFiles(new JavaFileNameFilter(true, "java"));
                for (File file : files) {
                    process(file, rightDirFile, fileWriter);
                }

            } else {
                List<File> finalFiles = new ArrayList<>();
                if (!(leftDirFile.getAbsolutePath().toLowerCase(Locale.ROOT).contains("{sourceProjectPath}") ||  leftDirFile.getAbsolutePath().toLowerCase(Locale.ROOT).contains("{targetProjectPath}")) || leftDirFile.getName().toLowerCase(Locale.ROOT).contains("test")) {
                    return;
                }
                String name = leftDirFile.getName();
                if (name.split("\\.")[0].toLowerCase().endsWith("restapi")) {
                    name = name.replace("RestAPI", "Controller");
                } else if (name.split("\\.")[0].toLowerCase().endsWith("restapi")) {
                    name = name.replace("Controller", "RestAPI");

                }
                if(claasToExclude.contains(name.replace(".java",""))){
                    return;
                }
                getFiles(name, rightDirFile, finalFiles);
                if (finalFiles.size() > 1) {
                    fileWriter.write("MORE_FILES"+","+"Found more than one file for the filename %s skipping for now " +
                                    String.join("-",leftDirFile.getName()+finalFiles.stream().map(File::getName).collect(Collectors.toList()))+"\n");
                    fileWriter.flush();
                    System.out.println("MORE_FILES"+","+"Found more than one file for the filename %s skipping for now " +
                            String.join("-",leftDirFile.getName()+finalFiles.stream().map(File::getName).collect(Collectors.toList())));
                    return;
                } else if (finalFiles.isEmpty()) {
                    System.out.println("NO_FILE"+","+"Didn't find even one file for the filename %s skipping for now "+ leftDirFile.getName());
                    fileWriter.write("NO_FILE"+","+"Didn't find even one file for the filename %s skipping for now "+ leftDirFile.getName()+"\n");
                    fileWriter.flush();
                    return;
                }
                File rightFile = finalFiles.get(0);


                HashSet<String> changedMethods = JavaClassDiffGenerator.methodDiffInClass(
                        leftDirFile.getAbsolutePath(),
                        rightFile.getAbsolutePath(), fileWriter
                );

                if(!changedMethods.isEmpty()) {
                    changedMethods = new HashSet<>(changedMethods.stream().map(each -> "DIFF_FILE,"+each).collect(Collectors.toSet()));
                    fileWriter.write(StringUtils.join(changedMethods, "\n"));
                    System.out.println(StringUtils.join(changedMethods, "\n"));
                    fileWriter.flush();

                }

            }

    }

    private static File[] getFiles(String leftFileName, File rightDirFile, List<File> finalFiles) {

        File[] files = rightDirFile.listFiles(new JavaFileNameFilter(false, leftFileName));

        for(File file : files){
            if(file.isDirectory()){
                getFiles(leftFileName, file, finalFiles);
            }else{
                finalFiles.add(file);
            }
        }
        return files;
    }

    private static PrettyPrinterConfiguration ppc = null;

    class ClassPair {
        final ClassOrInterfaceDeclaration clazz;
        final String name;
        ClassPair(ClassOrInterfaceDeclaration c, String n) {
            clazz = c;
            name = n;
        }
    }

    public static PrettyPrinterConfiguration getPPC() {
        if (ppc != null) {
            return ppc;
        }
        PrettyPrinterConfiguration localPpc = new PrettyPrinterConfiguration();
        localPpc.setColumnAlignFirstMethodChain(false);
        localPpc.setColumnAlignParameters(false);
        localPpc.setEndOfLineCharacter("");
        //--localPpc.setIndent("");
        localPpc.setPrintComments(false);
        localPpc.setPrintJavadoc(false);

        ppc = localPpc;
        return ppc;
    }

    public static <N extends Node> List<N> getChildNodesNotInClass(Node n, Class<N> clazz) {
        List<N> nodes = new ArrayList<>();
        for (Node child : n.getChildNodes()) {
            if (child instanceof ClassOrInterfaceDeclaration) {
                // Don't go into a nested class
                continue;
            }
            if (clazz.isInstance(child)) {
                nodes.add(clazz.cast(child));
            }
            nodes.addAll(getChildNodesNotInClass(child, clazz));
        }
        return nodes;
    }

    private List<ClassPair> getClasses(Node n, String parents, boolean inMethod) {
        List<ClassPair> pairList = new ArrayList<>();
        removeComments(n);
        for (Node child : n.getChildNodes()) {
            removeComments(child);
            if (child instanceof ClassOrInterfaceDeclaration) {
                ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration)child;
                String cName = parents+c.getNameAsString();
                if (inMethod) {
                    System.out.println(
                            "WARNING: Class "+cName+" is located inside a method. We cannot predict its name at"
                                    + " compile time so it will not be diffed."
                    );
                } else {
                    pairList.add(new ClassPair(c, cName));
                    pairList.addAll(getClasses(c, cName + "$", inMethod));
                }
            } else if (child instanceof MethodDeclaration || child instanceof ConstructorDeclaration) {
                pairList.addAll(getClasses(child, parents, true));
            } else {
                pairList.addAll(getClasses(child, parents, inMethod));
            }
        }
        return pairList;
    }

    private List<ClassPair> getClasses(String file) {
        try {
            JavaParser javaParser = new JavaParser();
            CompilationUnit cu = javaParser.parse(new File(file)).getResult().get();
            return getClasses(cu, "", false);
        } catch (FileNotFoundException f) {
            throw new RuntimeException("EXCEPTION: Could not find file: "+file);
        }
    }

    public static String getSignature(String className, CallableDeclaration m) {
        return className+"."+m.getSignature().asString();
    }

    public static HashSet<String> methodDiffInClass(String file1, String file2, BufferedWriter fileWriter) throws IOException {
        HashSet<String> changedMethods = new HashSet<>();
        HashMap<String, String> methods = new HashMap<>();

        JavaClassDiffGenerator md = new JavaClassDiffGenerator();

        // Load all the method and constructor values into a Hashmap from File1
        List<ClassPair> cList = md.getClasses(file1);

        for (ClassPair c : cList) {
            List<ConstructorDeclaration> conList = getChildNodesNotInClass(c.clazz, ConstructorDeclaration.class);
            List<MethodDeclaration> mList = getChildNodesNotInClass(c.clazz, MethodDeclaration.class);
            if(mList.isEmpty() && conList.isEmpty()){
                fileWriter.write("SKIPPING,"+file1+"\n");
                fileWriter.flush();
                return changedMethods;
            }
            for (MethodDeclaration m : mList) {
                String methodSignature = getSignature(c.name, m);

                if (m.getBody().isPresent()) {
                    methods.put(methodSignature, m.getBody().get().toString(getPPC()));
                } else {
                    /*fileWriter.write("NO_BODY_METHOD,"+methodSignature);
                    fileWriter.flush();
                    System.out.println("Warning: No Body for "+file1+" "+methodSignature);
                    TODO Skipping
                    */
                }
            }
            for (ConstructorDeclaration con : conList) {
                String methodSignature = getSignature(c.name, con);
                methods.put(methodSignature, con.getBody().toString(getPPC()));
            }
        }

        // Compare everything in file2 to what is in file1 and log any differences
        cList = md.getClasses(file2);
        for (ClassPair c : cList) {
            List<ConstructorDeclaration> conList = getChildNodesNotInClass(c.clazz, ConstructorDeclaration.class);
            List<MethodDeclaration> mList = getChildNodesNotInClass(c.clazz, MethodDeclaration.class);
            if(mList.isEmpty() && conList.isEmpty()){
                fileWriter.write("SKIPPING,"+file2+"\n");
                fileWriter.flush();
                return changedMethods;
            }
            for (MethodDeclaration m : mList) {
                String methodSignature = getSignature(c.name, m);

                if (m.getBody().isPresent()) {
                    String body1 = methods.remove(methodSignature);
                    String body2 = m.getBody().get().toString(getPPC());
                    if (body1 == null || !body1.equals(body2)) {
                        // Javassist doesn't add spaces for methods with 2+ parameters...
                        changedMethods.add(getRow(methodSignature, body1, body2));
                    }
                } else {
                     /*System.out.println("Warning: No Body for "+file2+" "+methodSignature);
                    fileWriter.write("NO_BODY_METHOD,"+methodSignature);
                    fileWriter.flush();
                    TODO Skip[ping
               */ }
            }
            for (ConstructorDeclaration con : conList) {
                String methodSignature = getSignature(c.name, con);
                String body1 = methods.remove(methodSignature);
                String body2 = con.getBody().toString(getPPC());
                if (body1 == null || !body1.equals(body2)) {
                    // Javassist doesn't add spaces for methods with 2+ parameters...
                    changedMethods.add(getRow(methodSignature, body1, body2));
                }
            }
            // Anything left in methods was only in the first set and so is "changed"
            for (String method : methods.keySet()) {
                // Javassist doesn't add spaces for methods with 2+ parameters...
                changedMethods.add(getRow(method, "", ""));
            }
        }
        return changedMethods;
    }

    private static String getRow(String methodSignature, String body1, String body2) {
        return String.join(",", methodSignature.replace(" ", "").replace(",", ": ").split("\\."))+","+ StringUtils.difference(body1, body2)+","+StringUtils.difference(body2, body1);
    }

    private static void removeComments(Node node) {
        for (Comment child : node.getAllContainedComments()) {
            child.remove();
        }
    }
}
