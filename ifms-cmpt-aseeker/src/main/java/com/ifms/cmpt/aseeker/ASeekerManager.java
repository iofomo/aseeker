package com.ifms.cmpt.aseeker;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.ifms.cmpt.utils.FileUtils;
import com.ifms.cmpt.utils.Logger;
import com.ifms.cmpt.utils.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class ASeekerManager {
    private static final String TAG = "ASeekerManager";

    private static Set<String> sIgnoreCaches = new HashSet<String>();
    private static Set<String> sMatchCaches = new HashSet<String>();
    private static Set<String> sIgnoreFiles = new HashSet<>();

    /***
     * @brief: 获取SDK实例，未初始化前返回NULL，方便在初始化后的其他地方使用
     * */
    private final String mDataPath, mTempPath;
    private String mTag;
    private final ASeekerConfig mConfig;
    private ASeekerManager(String dataPath, String tempPath) {
        mDataPath = dataPath;
        mTempPath = tempPath;
        mConfig = ASeekerConfig.creator(mDataPath + "/res");
    }

    private static ASeekerManager sSelf = null;
    public static ASeekerManager getInstance(String dataPath, String tempPath) {
        if (null == sSelf) {
            synchronized (ASeekerManager.class) {
                if (null == sSelf) {
                    sSelf = new ASeekerManager(dataPath, tempPath);
                }
            }
        }
        return sSelf;
    }

    public static ASeekerManager getInstance() { return sSelf; }

    public static int getMaxLevel() {
        if (null != sSelf) {
            return sSelf.mConfig.getMaxLevel();
        }
        return 0;
    }

    public static String getMappingFile(String k) {
        if (null != sSelf) {
            String v = sSelf.mConfig.getMappingFile(k);
            if (null != v) return v;
        }
        return k;
    }

    public static List<String> getTemplateLines() {
        if (null != sSelf) {
            return sSelf.mConfig.getTemplateLines();
        }
        return new ArrayList<>();
    }

    public String getDataPath() { return mDataPath; }
    public String getTempPath() { return mTempPath; }

    public String getOutPath() {
        return mDataPath + "/out/" + getOSVersionName(mTag);
    }

    public ASeekerConfig getConfig() { return mConfig; }

    public String getAppBasePath() {
        return mScanPath + File.separator + "frameworks" + File.separator + "base" + File.separator + "core" + File.separator + "java";
    }

    private String mSDKSourcePath;
    public String getAndroidSDKSourcePath() {
        if (null != mSDKSourcePath) return mSDKSourcePath;
        final String path = mConfig.getAndroidHomePath();
        if (TextUtils.isEmpty(path)) {
            mSDKSourcePath = "";
        } else {
            mSDKSourcePath = path + File.separator + "sources" + File.separator + "android-" + mTag;
        }
        return mSDKSourcePath;
    }

    private void loadCaches(String fileName, Set<String> caches) {
        if (0 < caches.size()) return;
        BufferedReader reader = null;
        try {
            String line;
            reader = new BufferedReader(new FileReader(mDataPath + "/res/" + fileName));
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#")) continue;
                caches.add(line);
            }
        } catch (Throwable e) {
            Logger.e(e);
        } finally {
            FileUtils.closeQuietly(reader);
        }
    }

    private String mScanPath;
    public String getScanPath() { return mScanPath; }

    public void parse(String tag, String path) {
        mTag = tag;
        mScanPath = path;
        loadCaches( "match.txt", sMatchCaches);
        loadCaches( "ignore.txt", sIgnoreCaches);
        loadCaches( "ignore-file.txt", sIgnoreFiles);
        traverseDir(new File(path));
    }

    private static String javaPath(File file) {
        String name;
        while (null != (file = file.getParentFile())) {
            name = file.getName();
            if (TextUtils.equals(name, "java") ||
                TextUtils.equals(name, "src") ||
                TextUtils.equals(name, "aidl")
                ) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    private static String sAndroidService = null;
    private static String getAndroidService() {
        if (null == sAndroidService) {
            sAndroidService = File.separator + "android" + File.separator + "service" + File.separator;
        }
        return sAndroidService;
    }

    private void traverseDir(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                traverseDir(file);
                continue;
            }
            if (!file.isFile()) continue;
            String name = file.getName();
            if (!name.toLowerCase().endsWith(".aidl")) continue;
            name = name.substring(0, name.length()-".aidl".length()) + ".aidl";
            if (sIgnoreFiles.contains(name)) continue;

            final String aidlFile = file.getAbsolutePath().substring(mScanPath.length());
            if (0 < aidlFile.indexOf(getAndroidService())) {
                Logger.e(TAG, "Ignore android service aidl: " + aidlFile);
                continue;
            }

            Logger.d(TAG, aidlFile);
            File desFile = new File(getTempPath(), FileUtils.getTempName());
            try {
                if (!AidlFormator.format(file, desFile)) continue;

                VisitorArgument va = new VisitorArgument(mTag, file, 1);
                if (va.isValid()) {
                    parseFile(desFile, va, true);
                } else {
                    Logger.e(TAG, "Unknown Java path: " + aidlFile);
                }
            } catch (Throwable e) {
                Logger.e(e);
            } finally {
                desFile.delete();
            }
        }
    }

    private static File searchClassFile(String baseJavaPath, File aidlFile, String clsPathName, String fname) {
        File clsFile;
        // 1. search from current path
        clsFile = new File(aidlFile.getParent(), fname + ".java");
        if (clsFile.isFile()) return clsFile;

        // 2. search from current base java path
        if (!TextUtils.isEmpty(baseJavaPath)) {
            clsFile = new File(baseJavaPath, clsPathName + ".java");
            if (clsFile.isFile()) return clsFile;
        }

        // 3. search from base path
        String basePath = getInstance().getAppBasePath();
        if (!TextUtils.equals(basePath, baseJavaPath)) {
            clsFile = new File(basePath, clsPathName + ".java");
            if (clsFile.isFile()) return clsFile;
        }

        // 4. search from SDK source path
        String srcPath = getInstance().getAndroidSDKSourcePath();
        if (!TextUtils.isEmpty(srcPath)) {
            clsFile = new File(srcPath, clsPathName + ".java");
            if (clsFile.isFile()) return clsFile;
        }
        return null;
    }

    static class  VisitorArgument {
        private final String mPath;
        private final String mTag;// such as "9.0", "10.0"
        private Map<String, String> classes = new HashMap<>();
        private Set<String> innerClasses = new HashSet<String>();
        private Map<String, Integer> values = new HashMap<String, Integer>();
        private final File mFile;
        private final int mLevel;
        private String reason = "";

        public VisitorArgument(String tag, File file, int level) {
            mTag = tag;
            mFile = file;
            mLevel = level;
            mPath = javaPath(mFile);
        }

        public int getLevel() { return mLevel; }
        public File getFile() { return mFile; }
        public boolean isValid() { return TextUtils.isValid(mPath); }

        public void setValue(String name, int v) {
            values.put(name, v);
        }

        public void setReason(String r) {
            reason = null == r ? "" : r;
        }
        public void addReason(String r) {
            if (null != r) {
                reason = r + reason;
            }
        }
        public String getReason() { return reason; }

        public int getValue(String name) {
            Integer v = values.get(name);
            return null == v ? 0 : v.intValue();
        }

        public void addInnerClass(String clsName) {
            innerClasses.add(clsName);
        }

        public boolean isInnerClass(String clsName) {
            return innerClasses.contains(clsName);
        }

        public void addImportClass(String clsName) {
            String[] items = clsName.split("\\.");
            classes.put(items[items.length-1], clsName);
        }

        public File getClassFile(String name) {
            if (name.startsWith("I") && 2 <= name.length()) {
                char ch = name.charAt(1);
                if ('A' <= ch && ch <= 'Z') {
                    Logger.w(TAG, "ignore class: " + name + " in " + getFile());
                    return null;// ignore IBinder extends or implements class
                }
            }

            String clsName = classes.get(name);

            File file;
            if (TextUtils.isEmpty(clsName)) {
                file = searchClassFile(mPath, mFile, name, name);
            } else {
                String[] items = clsName.split("\\.");
                final String pathName = TextUtils.join(File.separator, items);
                file = searchClassFile(mPath, mFile, pathName, items[items.length - 1]);
            }
            if (null == file) {
                Logger.w(TAG, "unknown class: " + name + " in " + getFile());
                setValue(name, TYPE_Not_Found);
            }
            return file;
        }

        public String getTag() { return mTag; }

        private int mResult = 0;
        public int getResult() { return mResult; }
        public void setResult(int v) { mResult = v; }
    }

    public static String getOSVersionName(String tag) {
        switch (tag) {
        case "28": return "9.0";
        case "29": return "10.0";
        case "30": return "11.0";
        case "31": return "12.0";
        case "32": return "12.1";
        case "33": return "13.0";
        case "34": return "14.0";
        case "35": return "15.0";
        case "36": return "16.0";
        case "37": return "17.0";
        case "38": return "18.0";
        default: return tag;
        }
    }

    private static ParseResult<CompilationUnit> parseFile(File file, final VisitorArgument _arg, final boolean _isAIDL) throws Throwable {
        ParseResult<CompilationUnit> result = new JavaParser().parse(file);
        result.ifSuccessful(compilationUnit -> {
            List<ImportDeclaration> importDeclarations = compilationUnit.getImports();
            for (ImportDeclaration importDeclaration : importDeclarations) {
                _arg.addImportClass(importDeclaration.getName().asString());
            }
            compilationUnit.accept(new InnerClassVisitor(), _arg);
            compilationUnit.accept(new ClassVisitor(_isAIDL), _arg);
        });
        List<Problem> pp = result.getProblems();
        if (null != pp && 0 < pp.size()) {
            for (Problem p : pp) {
                Logger.e(TAG, p.toString());
            }
            Logger.e(TAG, "Fail: " + _arg.getFile());
            File bk = new File(file.getAbsolutePath() + "-bark");
            boolean succ = FileUtils.copyFile(file, bk);
            Logger.e(TAG, "backup: " + succ + ", " + bk);
        }
        return result;
    }

    private static void parseSuperClass(final VisitorArgument _arg, CompilationUnit cu) {
        for (TypeDeclaration<?> type : cu.getTypes()) {
            if (!(type instanceof ClassOrInterfaceDeclaration)) continue;
            final NodeList<ClassOrInterfaceType> exts = ((ClassOrInterfaceDeclaration) type).getExtendedTypes();
            if (null == exts || exts.isEmpty()) continue;
            for (ClassOrInterfaceType ext : exts) {
                String className = ext.getName().toString();
                int typ = parseTypeName(className, null, _arg, false);
                if (TYPE_Match == typ) {
                    _arg.setResult(typ);
                    _arg.addReason(" => " + className);
                    return;
                }
            }
        }
    }

    private static final int TYPE_None = 0;
    private static final int TYPE_String = 1;
    private static final int TYPE_Match = 2;
    private static final int TYPE_Ignore = 3;
    private static final int TYPE_Not_Found = 404;

    private static String formatName(String name, boolean isAIDL) {
        if (name.endsWith("[]")) name = name.substring(0, name.length()-2);
        int pos = name.indexOf("<");
        if (0 < pos) {
            String n = name.substring(pos + 1);
            pos = n.indexOf(">");
            n = n.substring(0, pos);
            pos = n.indexOf(',');
            if (pos < 0) {// such as List<String> -> String
                return n;
            }
            if (isAIDL) {
                // such as Map<Integer, String> -> String
                // such as Map<Integer, List<String>> -> List<String>
                return formatName(n.substring(pos + 1).trim(), isAIDL);
            }
        }
        return name;
    }

    private static int parseTargetName(String targetName, boolean isAIDL) {
        if (TextUtils.isEmpty(targetName)) return TYPE_String;
        if (isAIDL) {
            if (TextUtils.equals(targetName, "path")) return TYPE_Match;
            if (0 < targetName.indexOf("Path")) return TYPE_Match;
        }

        targetName = targetName.toLowerCase();
        if (0 <= targetName.indexOf("pkg")) return TYPE_Match;
        if (0 <= targetName.indexOf("package")) return TYPE_Match;
        if (0 <= targetName.indexOf("package")) return TYPE_Match;
        return TYPE_String;
    }

    private static int parseTypeName(String name, String targetName, VisitorArgument arg, boolean isAIDL) {
        name = formatName(name, isAIDL);
        if (TextUtils.equals(name, "String") || TextUtils.equals(name, "CharSequence")) {
            return parseTargetName(targetName, isAIDL);
        }
        if (sMatchCaches.contains(name)) return TYPE_Match;
        if (sIgnoreCaches.contains(name)) return TYPE_Ignore;
        if (arg.isInnerClass(name)) return TYPE_Ignore;
        int val = arg.getValue(name);
        if (0 != val) return val;

        File file = arg.getClassFile(name);
        if (null == file) return TYPE_Not_Found;

        if (getMaxLevel() < arg.getLevel()) {
            Logger.w(TAG, "ignore by swarch level: " + file);
            return -4;
        }

        File desFile = new File(ASeekerManager.getInstance().getTempPath(), FileUtils.getTempName());
        try {
            if (!AidlFormator.format(file, desFile)) return -2;

            VisitorArgument va = new VisitorArgument(arg.getTag(), file, arg.getLevel() + 1);
            ParseResult<CompilationUnit> result = parseFile(desFile, va, false);
            if (va.getResult() <= 0) {
                parseSuperClass(va, result.getResult().get());
            }
            if (TYPE_Match == va.getResult()) {
                arg.addReason(va.getReason());
            }
            return va.getResult();
        } catch (Throwable e) {
            Logger.w(TAG, "exception: " + name + " in " + file);
            Logger.e(e);
        } finally {
            desFile.delete();
        }
        return -1;
    }

    private static class ClassVisitor extends VoidVisitorAdapter<VisitorArgument> {
        private final boolean mIsAIDL;
        public ClassVisitor(boolean isAIDL) {
            mIsAIDL = isAIDL;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration cid, VisitorArgument arg) {
            super.visit(cid, arg);
//            Logger.d(TAG, "Class: " + cid.getName());
            if (!cid.isTopLevelType()) return;// not for inner class
            if (mIsAIDL) {
//                Logger.d(TAG, ">>> aidl");
                doVisitAIDL(cid, arg);
            } else {
                doVisitClass(cid, arg);
            }
        }
    }

    private static void doVisitAIDL(ClassOrInterfaceDeclaration cid, VisitorArgument arg) {
        Parameter p;
        int retType, methodType, argType;
        String returnName, methodName, argName, tmpName;
        StringBuilder sb = new StringBuilder();
        ProxyCreator pcreator = new ProxyCreator(arg.getFile());
        for (MethodDeclaration method : cid.getMethods()) {
            arg.setReason(null);

            returnName = method.getType().toString();
            retType = parseTypeName(returnName, null, arg, true);
            if (TYPE_Match == retType) {
                arg.addReason("return: " + returnName);
            }

            methodName = method.getName().toString();

            sb.setLength(0);
            sb.append("// " + getOSVersionName(arg.getTag())).append(" ");
            sb.append(returnName).append(" ");
            sb.append(methodName).append("(");

            argType = TYPE_None;
            for (int i=0; i<method.getParameters().size(); ++i) {
                p = method.getParameters().get(i);
                if (0 < i) sb.append(", ");
                argName = p.getType().toString();
                if (TYPE_Match != retType && TYPE_Match != argType) {
                    tmpName = p.getName().toString();
                    argType = parseTypeName(argName, tmpName, arg, true);
                    if (TYPE_Match == argType) {
                        arg.addReason("argument: " + argName + ":" + tmpName);
                    }
                }
                sb.append(argName).append(" ").append(p.getName());
            }
            sb.append(");// ");

            if (TYPE_Match != retType && TYPE_Ignore != retType && TYPE_Match != argType) {
                methodType = parseTargetName(methodName, true);
                if (TYPE_Match == methodType) {
                    arg.addReason("method: " + methodName);
                }
            } else {
                methodType = TYPE_None;
            }

            sb.append(arg.getReason());

            if (TYPE_Match == retType || TYPE_Match == methodType || TYPE_Match == argType) {
//                    Logger.d(TAG, sb.toString());
                pcreator.addLine(methodName, sb.toString());
            }
        }
        if (pcreator.flush(ASeekerManager.getInstance().getOutPath())) {
            Logger.d(TAG, ">>> " + pcreator.getFileName() + ".java");
        }
    }

    private static void doVisitClass(ClassOrInterfaceDeclaration cid, VisitorArgument arg) {
        int typ;
        String tname, fname;
        final List<FieldDeclaration> ff = cid.getFields();
        for (FieldDeclaration field : ff) {
            if (field.hasModifier(Modifier.Keyword.STATIC)) continue;
            tname = field.getElementType().toString();
            fname = field.getVariables().get(0).getName().toString();
            typ = parseTypeName(tname, fname, arg, false);
            if (TYPE_Match == typ) {
                arg.setResult(typ);
                arg.addReason(" -> " + tname + "::" + fname);
//                Logger.d(TAG, "match: " + cid.getName() + ": " + tname + " " + fname);
                break;
            }
        }
    }

    private static class InnerClassVisitor extends VoidVisitorAdapter<VisitorArgument> {

        @Override
        public void visit(final ClassOrInterfaceDeclaration cid, final VisitorArgument arg) {
            super.visit(cid, arg);
            if (!cid.isTopLevelType()) {
//                Logger.d(TAG, "class sub: " + cid.getNameAsString());
                // collect inner class
                arg.addInnerClass(cid.getNameAsString());
            } else {
//                Logger.d(TAG, "class top: " + cid.getNameAsString());
            }
        }

        @Override
        public void visit(final EnumDeclaration cid, final VisitorArgument arg) {
            super.visit(cid, arg);
            arg.addInnerClass(cid.getNameAsString());
        }

    }
}
