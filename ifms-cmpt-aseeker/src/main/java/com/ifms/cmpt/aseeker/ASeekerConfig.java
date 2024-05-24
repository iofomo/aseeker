package com.ifms.cmpt.aseeker;

import com.ifms.cmpt.utils.FileUtils;
import com.ifms.cmpt.utils.Logger;
import com.ifms.cmpt.utils.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ASeekerConfig {
    private Map<String, String> mConfigs = new HashMap<>();
    private Map<String, String> mMappingFiles = new HashMap<>();
    private ArrayList<String> mTemplateLines = new ArrayList<>();
    private ASeekerConfig() {

    }

    public static ASeekerConfig creator(String path) {
        ASeekerConfig config = new ASeekerConfig();
        config.doLoad(path);
        return config;
    }

    private void doLoad(String resPath) {
        doLoadKeyValueFile(new File(resPath, "config.txt"), mConfigs);
        doLoadKeyValueFile(new File(resPath, "mapping.txt"), mMappingFiles);
        FileUtils.readLines(mTemplateLines, new File(resPath, "template.java"));
    }

    private static void doLoadKeyValueFile(File file, Map<String, String> cfg) {
        List<String> lines = FileUtils.readLines(file);
        if (null == lines || lines.size() <= 0) return;

        int pos;
        for (String line : lines) {
            line = line.trim();
            if (TextUtils.isEmpty(line) || line.startsWith("#")) continue;
            pos = line.indexOf('=');
            if (pos <= 0) continue;
            cfg.put(line.substring(0, pos).trim(), line.substring(pos+1).trim());
        }
    }

    public String get(String k) {
        return mConfigs.get(k);
    }

    public boolean containsKey(String k) {
        return mConfigs.containsKey(k);
    }

    public String getMappingFile(String k) {
        return mMappingFiles.get(k);
    }

    public List<String> getTemplateLines() {
        return mTemplateLines;
    }

    public String getAndroidHomePath() {
        String path = get("ANDROID_HOME");
        if (!TextUtils.isEmpty(path) && new File(path).isDirectory()) {
            return path;
        }
        path = System.getenv("ANDROID_HOME");
        if (!TextUtils.isEmpty(path) && new File(path).isDirectory()) {
            return path;
        }
        path = System.getenv("ANDROID_SDK");
        if (!TextUtils.isEmpty(path) && new File(path).isDirectory()) {
            return path;
        }
        return null;
    }

    private int mMaxLevel = 0;
    public int getMaxLevel() {
        if (mMaxLevel <= 0) {
            final String ml = get("MAX_LEVEL");
            if (!TextUtils.isEmpty(ml)) {
                try {
                    mMaxLevel = Integer.parseInt(ml.trim());
                } catch (Throwable e) {
                    Logger.e(e);
                }
            }
            if (mMaxLevel <= 0 || 100 < mMaxLevel) {
                mMaxLevel = 3;
            }
        }
        return mMaxLevel;
    }
}
