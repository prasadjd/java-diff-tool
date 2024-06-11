package org.jd.diff;


import java.io.File;
import java.util.*;

/**
 * @author prasadjd on 22/06/22
 */
public class FindDuplicateFiles {

    public static final Map<String, Integer> count = new HashMap();

    public static void main(String[] args) {
        List<String> results = new ArrayList<String>();


        File[] files = new File("{path_of_target_project}").listFiles();
//If this pathname does not denote a directory, then listFiles() returns null.

        for (File file : files) {
            recur(file);
        }

        for (Map.Entry<String, Integer> entry : count.entrySet()){
            if(entry.getValue()>1){
                System.out.println(entry.getKey().split("\\.")[0]);
            }
        }

    }

    public static void recur(File file){

        if(file.isDirectory()){
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                recur(files[i]);
            }
        }else {
            if (file.getName().endsWith("java") && file.getName().toLowerCase().equalsIgnoreCase("dao")) {
                if (count.containsKey(file.getName())) {
                    Integer integer = count.get(file.getName());
                    count.put(file.getName(), integer + 1);
                } else {
                    count.put(file.getName(), 1);
                }
            }
        }
    }
}
