package com.ifms.cmpt.aseeker;

import com.ifms.cmpt.utils.FileUtils;
import com.ifms.cmpt.utils.Logger;
import com.ifms.cmpt.utils.TextUtils;

import java.io.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AidlFormator {
    private static final String TAG = "AidlFormator";

    // rm UNsupport keywords for JavaParser
    public static boolean format(File srcFile, File desFile) {
        List<String> lines = FileUtils.readLines(srcFile);
        if (null == lines || lines.size() <= 0) {
            Logger.e(TAG, "read fail: " + srcFile);
            return false;
        }

        doInit();
        FileUtils.ensurePath(desFile);

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(desFile, false));
            for (String line : lines) {
                line = formatLine(line);
                writer.write(line);
                writer.newLine();
            }
            return true;
        } catch (Throwable e) {
            Logger.e(e);
        } finally {
            FileUtils.closeQuietly(writer);
        }
        return false;
    }

    private static Pattern[] PATTERNS = null;

    private static void doInit() {
        sAtFinished = false;
        sEnumFinished = false;
        if (null != PATTERNS) return;
        PATTERNS = new Pattern[] {
            Pattern.compile("(?<![\\w])IN\\s+([a-zA-Z_])"),
            Pattern.compile("(?<![\\w])in\\s+([a-zA-Z_])"),
            Pattern.compile("(?<![\\w])OUT\\s+([a-zA-Z_])"),
            Pattern.compile("(?<![\\w])out\\s+([a-zA-Z_])"),
            Pattern.compile("(?<![\\w])INOUT\\s+([a-zA-Z_])"),
            Pattern.compile("(?<![\\w])inout\\s+([a-zA-Z_])"),
        };
    }

    private static final String PATTERN_AT = "@\\w[\\w.]*\\s*(\\([^)]*\\))?"; // rm @Nullable, @android.app.XXX(xxx)
    private static final String PATTERN_ABSTRACT = "\\)\\s*=\\s*\\d+\\s*;$";// rm ") = ${number};"
    private static final String PATTERN_BRIEF = "/\\*.*?\\*/";// rm "/*xxx*/"
    private static final String ONEWAY = "oneway ";
    private static final String PARCELABLE = "parcelable ";
    private static final String UNION = "union ";

    private static String doFormatLine(String line, Pattern pattern) {
        Matcher matcher = pattern.matcher(line);

        // 创建StringBuffer用于构建最终的字符串
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            // 将找到的符合条件的字符串替换为空
            // 注意这里对应的是捕获组外的部分替换为空，保留捕获组内的首个字母或下划线
            matcher.appendReplacement(result, "$1");
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static boolean sAtFinished = false, sEnumFinished = false;
    public static String formatLine(String line) {
        String l = line.trim();
        if (l.startsWith("//") || l.startsWith("*") || l.startsWith("/*")) return line;

//            @IntDef(prefix = { "NETWORK_TYPE_" }, value = {
//                    NETWORK_TYPE_NONE,
//                    NETWORK_TYPE_ANY,
//                    NETWORK_TYPE_UNMETERED,
//                    NETWORK_TYPE_NOT_ROAMING,
//                    NETWORK_TYPE_CELLULAR,
//            })
        if (sAtFinished) {
            if (l.endsWith(")")) {
                sAtFinished = false;
            }
            return "";
        }
        if (l.startsWith("@")) {
            int pos = l.indexOf('(');
            if (0 < pos && l.substring(1, pos).trim().indexOf(' ') < 0 && l.indexOf(')') < 0) {
                sAtFinished = true;
                return "";
            }
        }

//            enum XXX {
//                    NETWORK_TYPE_NONE,
//                    NETWORK_TYPE_ANY,
//                    NETWORK_TYPE_UNMETERED,
//                    NETWORK_TYPE_NOT_ROAMING,
//                    NETWORK_TYPE_CELLULAR,
//            }
        if (sEnumFinished) {
            if (l.endsWith("}")) {
                sEnumFinished = false;
            }
            return "";
        }
        if (l.startsWith("enum ") && 0 < l.indexOf('{') && (l.indexOf('}') < 0)) {//
            sEnumFinished = true;
            return "";
        }

        line = line.replaceAll(PATTERN_BRIEF, "");
        line = line.replaceAll(PATTERN_ABSTRACT, ");");
        line = line.replace("@interface ", "interface ");
        line = line.replace("AndroidFuture<int>", "AndroidFuture<Integer>");
        line = line.replace("AndroidFuture<long>", "AndroidFuture<Long>");
        line = line.replace("AndroidFuture<boolean>", "AndroidFuture<Boolean>");
        line = line.replaceAll("\\bconst\\b", "final");

        boolean mark = l.startsWith("@");
        line = line.replaceAll(PATTERN_AT, "");

        l = line.trim();
        if (TextUtils.isEmpty(l)) return "";
        if (mark && l.endsWith("\")")) return "";

        if (l.startsWith(ONEWAY)) {// such as "oneway interface IContent {" -> "interface IContent {"
            line = l.substring(ONEWAY.length());
        } else if (l.startsWith(PARCELABLE)) {
            if (l.endsWith(";")) {// such as "parcelable BlobHandle;"
                return "";
            }
            // such as "parcelable abc {" -> "public class abc {"
            line = "public class " + l.substring(PARCELABLE.length());
        } else if (l.startsWith(UNION) && l.endsWith("{")) {// such as "union XXX {" -> "public class XXX {"
            line = "public class " + l.substring(UNION.length());
        }

        for (Pattern p : PATTERNS) {
            line = doFormatLine(line, p);
        }
        return line;
    }

}
