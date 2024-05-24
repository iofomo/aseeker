package com.ifms.cmpt.aseeker;

import com.ifms.cmpt.utils.FileUtils;
import com.ifms.cmpt.utils.Logger;

import java.io.File;
import java.util.*;

public class ProxyCreator {
    private static final String TAG = "ProxyCreator";

    private final String mFileName;
    private List<String> lines = new ArrayList<>();
    private int templateLineIndex = 0;

    public ProxyCreator(File file) {
        String fileName = file.getName();
        if (fileName.endsWith(".aidl") || fileName.endsWith(".AIDL")) {
            fileName = fileName.substring(0, fileName.length()-5);
        }
        mFileName = ASeekerManager.getMappingFile(fileName);

        String line;
        List<String> templateLines = ASeekerManager.getTemplateLines();
        while (templateLineIndex < templateLines.size()) {
            line = templateLines.get(templateLineIndex ++);
            if (0 <= line.indexOf("@templateMethod@")) break;
            if (0 <= line.indexOf("@templateFile@")) {
                final String fullName = file.getAbsolutePath();
                final String spath = ASeekerManager.getInstance().getScanPath();
                if (spath.length() < fullName.length()) {
                    lines.add("// source code: " + fullName.substring(spath.length()).replace("\\", "/"));
                }
            } else if (0 <= line.indexOf("@templateClass@")) {
                lines.add(line.replace("@templateClass@", mFileName));
            } else {
                lines.add(line);
            }
        }
    }

    public String getFileName() { return mFileName; }

    private Map<String, String> mCacher = new HashMap<>();
    public void addLine(String funcName, String line) {
        mCacher.put(funcName, line);
    }

    private boolean flushLines() {
        if (mCacher.size() <= 0) return false;
        TreeMap<String, String> sortedMap = new TreeMap<>(mCacher);
        for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
            lines.add("    " + entry.getValue());
            lines.add("    public static final String sMethod_" + entry.getKey() + " = \"" + entry.getKey() + "\";");
        }
        return true;
    }

    public boolean flush(String path) {
        if (flushLines()) {
            List<String> templateLines = ASeekerManager.getTemplateLines();
            while (templateLineIndex < templateLines.size()) {
                lines.add(templateLines.get(templateLineIndex ++));
            }

            File file = new File(path, mFileName + ".java");
            FileUtils.ensurePath(file);
            if (FileUtils.writeLines(lines, file)) return true;
            Logger.e(TAG, "flush fail: " + file);
        } else {
            Logger.d(TAG, "flush ignore for empty");
        }
        return false;
    }
}
