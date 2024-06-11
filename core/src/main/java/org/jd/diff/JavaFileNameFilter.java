package org.jd.diff;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;

/**
 * @author prasadjd on 24/06/22
 */
public class JavaFileNameFilter implements /*FilenameFilter,*/ FileFilter {
    Boolean endsWith ;
    String subFileName ;
    public JavaFileNameFilter(boolean b, String java) {
        endsWith = b;
        subFileName = java;
    }

    /*@Override
    public boolean accept(File dir, String name) {
        return name.endsWith("java");
    }*/

    @Override
    public boolean accept(File file) {
        if(endsWith && (file.getName().endsWith(subFileName) || file.isDirectory())) {
            return true;
        }else if(!endsWith && (file.getName().equalsIgnoreCase(subFileName) || file.isDirectory())){
            return true;
        }else{
            return false;
        }
    }
}
