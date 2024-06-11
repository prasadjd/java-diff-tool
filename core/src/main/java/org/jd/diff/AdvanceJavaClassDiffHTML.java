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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import org.jsoup.Jsoup;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author prasadjd on 19/10/2022
 */
public class AdvanceJavaClassDiffHTML {


    private static Set<String> claasToExclude = new HashSet<>();
    static {
        claasToExclude.addAll(Arrays.asList(/*"Step",
                "SurveyTokenMetaData",
                "ScheduleServiceImpl",
                "ScheduleHelperServiceImpl",
                "SalesforceServiceImpl",
                "SalesforceRestServiceImpl",
                "ReportMasterCalculatedFieldEvaluator",
                "PublishAdvancedOutReachAction",
                "PermissionJWTHelperImpl",
                "ParticipantValidationVO",
                "ParticipantValidationServiceImpl",
                "ParticipantSyncUtils",
                "ParticipantStateActivity",
                "ParticipantSourceConfiguration",
                "ParticipantSourceConfigurationServiceImpl",
                "ParticipantSourceConfigurationDAOImpl",
                "Optout***.java",
                "OrchestrationEngineServiceImpl",
                "ParticipantActionActivity",
                "ParticipantActivitySnapShot",
                "ParticipantCustomFieldsDaoImpl",
                "ParticipantSaveResult**",
                "ActionResponse",
                "AdvancedOutreachCTADAOImpl",
                "AdvancedOutreachCTAServiceImpl",
                "AdvancedOutreachDAOImpl",
                "AdvancedOutreachNativeCTAActionServiceImpl",
                "AdvancedOutreachPermissionServiceImpl",
                "AdvancedOutreachUtil",
                "AOCTAServiceImpl",
                "AOEventProcessServiceImpl",
                "AddParticipantOnSourceScheduleAction",
                "StandardObjectsUtilsImpl",
                "StandardFieldsServiceImpl",
                "AddParticipantServiceImpl",
                "AddReviewParticipantsToOrchestrationEngineAction",
                "AdvancedOutreachEventRuleExecutorServiceImpl",
                "AdvancedOutreachMessageUniqueIdServiceImpl",
                "AdvancedOutreachParameters",
                "AdvancedOutreachStandardObjectResolverImpl"*/));
    }


    private static int serialNum = 1; //"SKIPPING","NO_BODY_METHOD","LEFT_NO_BODY_METHOD","MORE_FILES","NO_FILE","DIFF_FILE"
    private static final Set<String> headers = Sets.newHashSet("DIFF_FILE");
    private static Set<String> inputFileStr = Sets.newHashSet("{Already verified output file path so that client can ignore false +ves}");

    public static void main(String s[]) throws Exception {


        String leftDirPath = "{path_of_target_project}";
        String rightDirPath =  "{path_of_source_project}";




        File leftDirFile = new File(leftDirPath);
        File rightDirFile = new File(rightDirPath);
        //TODO Renamed files need to handle
        BufferedWriter fileWriter = null;
        FileWriter fileWriter1 = null;
        try {
            File finalFile = new File("{Outputfile path}");
            Map<String, List<String>> inputMap = Maps.newHashMap();
            if(Objects.nonNull(inputFileStr) && !inputFileStr.isEmpty()){
                for(String inF : inputFileStr){

                    File inputFile = new File(inF);
                    List<org.jsoup.nodes.Node> trNodes = Jsoup.parse(inputFile, null).body().childNode(0).childNode(0).childNodes();
                    inputMap.putAll(trNodes.stream().collect(Collectors.toMap(childNode ->
                                    childNode.childNodes().get(1).childNode(0).toString() + childNode.childNodes().get(2).childNode(0).toString() + childNode.childNodes().get(3).childNode(0).toString(), child -> Arrays.asList(getString(child, 4), getString(child, 5)),

                            (address1, address2) -> {
                                System.out.println(String.format("duplicate key found! %s", address1));
                                return address1;
                            }

                    )));
                }
            }

            fileWriter1 = new FileWriter(finalFile);
            fileWriter = new BufferedWriter(fileWriter1);
            fileWriter.write("<html><head>\n" +
                    "<style>\n" +
                    "table {\n" +
                    "  font-family: arial, sans-serif;\n" +
                    "  border-collapse: collapse;\n" +
                    "  width: 100%;\n" +
                    "}\n" +
                    "\n" +
                    "td, th {\n" +
                    "  border: 1px solid #dddddd;\n" +
                    "  text-align: left;\n" +
                    "  padding: 8px;\n" +
                    "}\n" +
                    "\n" +
                    "tr:nth-child(even) {\n" +
                    "  background-color: #dddddd;\n" +
                    "}\n" +
                    "</style>\n" +
                    "</head><body><table><tr><th>S.No</th><th>Type</th><th>Class Name</th><th>Method Name</th><th>Left Changes</th><th>Right Changes</th><th>Left Method</th><th>Right Method</th></tr>");
            process(leftDirFile, rightDirFile, fileWriter, inputMap);
        }finally {
            if(!Objects.isNull(fileWriter)){
                fileWriter.write("</table></body></html>");
                fileWriter.flush();

                fileWriter1.close();
                fileWriter.close();
            }
        }

        System.out.println("Completed!");

    }

    private static String getString(org.jsoup.nodes.Node child, int index) {
        if(Objects.isNull(child.childNodes().get(index)) || child.childNodes().get(index).childNodes().isEmpty() || Objects.isNull(child.childNodes().get(index).childNode(0))){
            return StringUtils.EMPTY;
        }
        //if(child.childNodes().get(index).childNode(0).childNodes().isEmpty()){
            return Jsoup.parse(child.childNodes().get(index).toString()).text();
        //}
        //return child.childNodes().get(index).childNode(0).childNode(0).toString();
    }

    private static void process(File leftDirFile, File rightDirFile, BufferedWriter fileWriter, Map<String, List<String>> inputMap) throws Exception {

        if (leftDirFile.isDirectory()) {

            File[] files = leftDirFile.listFiles(new JavaFileNameFilter(true, "java"));
            for (File file : files) {
                process(file, rightDirFile, fileWriter, inputMap);
            }

        } else {
            List<File> finalFiles = new ArrayList<>();
            if (!(leftDirFile.getAbsolutePath().toLowerCase(Locale.ROOT).contains("{sourceProjectPackage}") || leftDirFile.getAbsolutePath().toLowerCase(Locale.ROOT).contains("{targetProjectPackage}")) || leftDirFile.getName().toLowerCase(Locale.ROOT).contains("test")) {
                return;
            }
            String name = leftDirFile.getName();
            if (name.split("\\.")[0].toLowerCase().endsWith("restapi")) {
                name = name.replace("RestAPI", "Controller");
            } else if (name.split("\\.")[0].toLowerCase().endsWith("restapi")) {
                name = name.replace("Controller", "RestAPI");

            }
            if (claasToExclude.contains(name.replace(".java", ""))) {
                return;
            }
            getFiles(name, rightDirFile, finalFiles);
            if (finalFiles.size() > 1) {
                /*fileWriter.write("<tr>"+"<td>MORE_FILES</td>"+"<td>"+"Found more than one file for the filename %s skipping for now </td>" +
                        String.join("-",leftDirFile.getName()+finalFiles.stream().map(File::getName).collect(Collectors.toList()))+"</tr>");
                fileWriter.flush();*/
                writeToFile("MORE_FILES", fileWriter, "Found more than one file for the filename %s skipping for now " +
                        String.join("-", leftDirFile.getName() + finalFiles.stream().map(File::getName).collect(Collectors.toList())));
                System.out.println("MORE_FILES" + "," + "Found more than one file for the filename %s skipping for now " +
                        String.join("-", leftDirFile.getName() + finalFiles.stream().map(File::getName).collect(Collectors.toList())));
                return;
            } else if (finalFiles.isEmpty()) {
                System.out.println("NO_FILE" + "," + "Didn't find even one file for the filename %s skipping for now " + leftDirFile.getName());
                /*fileWriter.write("<tr>"+"<td>NO_FILE</td>"+"<td>"+String.format("Didn't find even one file for the filename %s skipping for now ", leftDirFile.getName())+"</tr>");
                fileWriter.flush();*/
                writeToFile("NO_FILE", fileWriter, String.format("Didn't find even one file for the filename %s skipping for now ", leftDirFile.getName()));
                return;
            }
            for (File rightFile : finalFiles){


                if (headers.contains("DIFF_FILE")) {
                    List<String> changedMethods = AdvanceJavaClassDiffHTML.methodDiffInClass(
                            leftDirFile.getAbsolutePath(),
                            rightFile.getAbsolutePath(), fileWriter, inputMap
                    );

                    if (!changedMethods.isEmpty()) {
                        changedMethods = new ArrayList<>(changedMethods.stream().map(each -> "<tr>" + "<td>" + serialNum++ + "</td><td>DIFF_FILE</td>" + each).collect(Collectors.toList()));
                        fileWriter.write(StringUtils.join(changedMethods, "</tr>"));
                        fileWriter.flush();
                        System.out.println(StringUtils.join(changedMethods, "\n"));

                    }

                }
            }
        }

    }

    private static boolean writeToFile(String type, BufferedWriter fileWriter, String stringToWrite){
        try {
            if (headers.contains(type)) {
                fileWriter.write("<tr><td>"+serialNum+++"</td><td>"+type+"</td><td>"+stringToWrite+"</td></tr>");
                fileWriter.flush();
                return true;
            }
        }catch (Exception e){

        }
        return false;
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

    private List<AdvanceJavaClassDiffHTML.ClassPair> getClasses(Node n, String parents, boolean inMethod) {
        List<AdvanceJavaClassDiffHTML.ClassPair> pairList = new ArrayList<>();
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
                    pairList.add(new AdvanceJavaClassDiffHTML.ClassPair(c, cName));
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

    private List<AdvanceJavaClassDiffHTML.ClassPair> getClasses(String file) {
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

    public static List<String> methodDiffInClass(String file1, String file2, BufferedWriter fileWriter, Map<String, List<String>> inputMap) throws Exception {
        List<String> changedMethods = new ArrayList<>();
        HashMap<String, String> methods = new HashMap<>();

        AdvanceJavaClassDiffHTML md = new AdvanceJavaClassDiffHTML();

        // Load all the method and constructor values into a Hashmap from File1
        List<AdvanceJavaClassDiffHTML.ClassPair> cList = md.getClasses(file1);

        for (AdvanceJavaClassDiffHTML.ClassPair c : cList) {
            List<ConstructorDeclaration> conList = getChildNodesNotInClass(c.clazz, ConstructorDeclaration.class);
            List<MethodDeclaration> mList = getChildNodesNotInClass(c.clazz, MethodDeclaration.class);
            if(mList.isEmpty() && conList.isEmpty()){
                /*fileWriter.write("<tr><td>SKIPPING</td><td>"+file1+"</td></tr>");
                fileWriter.flush();*/
                writeToFile("SKIPPING", fileWriter, file1);
                return changedMethods;
            }
            for (MethodDeclaration m : mList) {
                String methodSignature = getSignature(c.name, m);

                if (m.getBody().isPresent()) {
                    methods.put(methodSignature, m.getBody().get().toString(getPPC()));
                } else {
                    /*fileWriter.write("<tr><td>NO_BODY_METHOD</td><td>"+methodSignature+"</td></tr>");
                    fileWriter.flush();*/
                    writeToFile("LEFT_NO_BODY_METHOD",fileWriter,methodSignature);

                    System.out.println("Warning: No Body for "+file1+" "+methodSignature);
                    //TODO Skipping
                }
            }
            for (ConstructorDeclaration con : conList) {
                String methodSignature = getSignature(c.name, con);
                methods.put(methodSignature, con.getBody().toString(getPPC()));
            }
        }

        // Compare everything in file2 to what is in file1 and log any differences
        cList = md.getClasses(file2);
        for (AdvanceJavaClassDiffHTML.ClassPair c : cList) {
            List<ConstructorDeclaration> conList = getChildNodesNotInClass(c.clazz, ConstructorDeclaration.class);
            List<MethodDeclaration> mList = getChildNodesNotInClass(c.clazz, MethodDeclaration.class);
            if(mList.isEmpty() && conList.isEmpty()){
                /*fileWriter.write("<tr><td>SKIPPING</td><td>"+file2+"</td></tr>");
                fileWriter.flush();*/
                writeToFile("SKIPPING", fileWriter, file2);
                return changedMethods;
            }
            for (MethodDeclaration m : mList) {
                String methodSignature = getSignature(c.name, m);

                if (m.getBody().isPresent()) {
                    String body1 = methods.remove(methodSignature);
                    String body2 = m.getBody().get().toString(getPPC());

                    if(Objects.isNull(body1)){
                        body1 = StringUtils.EMPTY;
                    }
                    if(Objects.isNull(body2)){
                        body2 = StringUtils.EMPTY;
                    }
                    body1 = getReplaceAll(body1);
                    body2 = getReplaceAll(body2);
                    if(StringUtils.isBlank(body1)){
                        writeToFile("LEFT_NO_BODY_METHOD", fileWriter, methodSignature);
                        continue;
                    }
                    if (body1 == null || !body1.replaceAll(" +", " ").replaceAll("\n", " ").equals(body2.replaceAll(" +", " ").replaceAll("\n", " "))) {
                        // Javassist doesn't add spaces for methods with 2+ parameters...
                        String row = getRow(methodSignature, body1, body2, inputMap);
                        if(StringUtils.isNotBlank(row)) {
                            changedMethods.add(row);
                        }
                    }
                } else {
                    System.out.println("Warning: No Body for "+file2+" "+methodSignature);
                    /*fileWriter.write("<tr><td>NO_BODY_METHOD</td><td></td><td>"+methodSignature+"</td></tr>");
                    fileWriter.flush();*/
                    writeToFile("NO_BODY_METHOD", fileWriter, methodSignature);
                    //TODO Skip[ping
                }
            }
            for (ConstructorDeclaration con : conList) {
                String methodSignature = getSignature(c.name, con);
                String body1 = methods.remove(methodSignature);
                String body2 = con.getBody().toString(getPPC());

                if(Objects.isNull(body1)){
                    body1 = StringUtils.EMPTY;
                }
                if(Objects.isNull(body2)){
                    body2 = StringUtils.EMPTY;
                }
                body1 = getReplaceAll(body1);
                body2 = getReplaceAll(body2);
                if(StringUtils.isBlank(body1)){
                    writeToFile("NO_BODY_METHOD", fileWriter, methodSignature);
                    continue;
                }
                if (body1 == null || !body1.replaceAll(" +", " ").replaceAll("\n", " ").equals(body2.replaceAll(" +", " ").replaceAll("\n", " "))) {
                    // Javassist doesn't add spaces for methods with 2+ parameters...
                    String row = getRow(methodSignature, body1, body2, inputMap);
                    if(StringUtils.isNotBlank(row)) {
                        changedMethods.add(row);
                    }
                }
            }
            // Anything left in methods was only in the first set and so is "changed"
            for (String method : methods.keySet()) {
                writeToFile("LEFT_NO_BODY_METHOD", fileWriter, method);
            }
        }
        return changedMethods;
    }

    private static String getReplaceAll(String body1) {
        //Any regx to ignore in diff
        return body1.replaceAll("LOGGER", "log").replaceAll("", "");
    }

    private static final com.google.googlejavaformat.java.Formatter formatter = new Formatter(JavaFormatterOptions.defaultOptions());
    private static String getRow(String methodSignature, String body1, String body2, Map<String, List<String>> inputMap) throws Exception {

        String[] split = methodSignature.split("\\.");
        String sss = split[0];
        split[0] = "";

        String key = "DIFF_FILE" + sss + String.join(".", split);
        if (!inputMap.containsKey(key) || !inputMap.get(key).get(0).equalsIgnoreCase(Jsoup.parse(getDifference(body1, body2)).text()) || !inputMap.get(key).get(1).equalsIgnoreCase(Jsoup.parse(getDifference(body2, body1)).text())) {
            return "<td>" + String.join("</td><td>", sss, String.join(".", split)) + "</td>" + "<td width=10%;>" + getDifference(body1, body2) + "</td><td width=10%;>" + getDifference(body2, body1) + "</td>" + "<td >" + getString(body1) + "</td><td >" + getString(body2) + "</td>";
        } else {

            return "";
        }
    }

    private static String getString(String body1) throws FormatterException {
        if(StringUtils.isBlank(body1)){
            return body1;
        }
        StringBuilder str = new StringBuilder(formatter.formatSource(("class TTTT {")+body1+"}").replace("class TTTT {",""));
        str.replace(
                str.lastIndexOf("}"),str.lastIndexOf("}")+1,"");
        return str.toString();
    }

    private static String getDifference(String body1, String body2) {//.append(";</span></br>")
        List<AdvanceJavaClassDiffHTML.DiffVO> diffV2 = getDiffV2(body1, body2);
        StringBuilder finalStr = new StringBuilder();

        for(int k=0;k<=diffV2.size()-1;k++) {
            StringBuilder string = new StringBuilder(diffV2.get(k).content);
            String style = diffV2.get(k).span;
            int index = 0;
            int i = 80;
            while (true) {


                if (string.length() <=  index + i) {
                    finalStr.append(style).append(string.substring(index, string.length())).append(";</span></br>");
                    break;
                }
                String ss = string.substring(index, index + i);
                index += i;
                if (ss.trim().length() == 0) {
                    finalStr.append(style).append(ss).append((";</span></br>"));
                    break;
                }
                finalStr.append(style).append(ss).append("</span></br>");
            }
        }


        return finalStr.toString();
    }

    private static String getDiffV1(String body1, String body2) {
        return StringUtils.difference(body1.replaceAll(" +", " "), body2.replaceAll(" +", " ")).replaceAll(";", ";</br>");
    }

    private static List<AdvanceJavaClassDiffHTML.DiffVO> getDiffV2(String body1, String body2) {
        body1=body1.replaceAll(" +", " ");
        body2=body2.replaceAll(" +", " ");
        String[] splitStr1 = body1.split(";");
        String[] splitStr2 = body2.split(";");
        List<AdvanceJavaClassDiffHTML.DiffVO> diffVOS = new ArrayList<>();

        for(int i=0;(i<=splitStr1.length-1 || i<=splitStr2.length-1);i++){

            String s1 = i<splitStr1.length-1 ? splitStr1[i] : "";
            String s2 = i<splitStr2.length-1 ? splitStr2[i] : "";
            s1 = s1.replaceAll(" +", " ").replaceAll("\n", " ");
            s2 = s2.replaceAll(" +", " ").replaceAll("\n", " ");
            if(!s1.equalsIgnoreCase(s2) && s1.trim().length()!=0){

                boolean exist = checkIfStringExist(s1, splitStr2, i+1);
                if(exist){
                    diffVOS.add(new AdvanceJavaClassDiffHTML.DiffVO("<span style=\"color:#"+convertStringToHex(s1)+"\">",s1));
                }else {

                    diffVOS.add(new AdvanceJavaClassDiffHTML.DiffVO("<span style=\"color:#003380\">", s1));
                }
            }
        }

        return diffVOS;
    }

    public static String convertStringToHex(String str) {

        // display in uppercase
        //char[] chars = Hex.encodeHex(str.getBytes(StandardCharsets.UTF_8), false);

        // display in lowercase, default
        char[] chars = Hex.encodeHex(str.getBytes(StandardCharsets.UTF_8));

        return String.valueOf(chars);
    }

    private static class DiffVO{
        public DiffVO(String span, String content){
            this.span=span;
            this.content = content;
        }
        public String span;
        public String content;
    }

    private static boolean checkIfStringExist(String s1, String[] str2, int index){

        for(int i=0;i<=str2.length-1;i++){

            String s2 = str2[i];

            if(s1.equalsIgnoreCase(s2)){

                return true;

            }
        }

        return false;
    }

    private static void removeComments(Node node) {
        for (Comment child : node.getAllContainedComments()) {
            child.remove();
        }
    }
}
