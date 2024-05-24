package com.ifms.cmpt.aseeker.demo;

import com.ifms.cmpt.aseeker.ASeekerManager;
import com.ifms.cmpt.utils.FileUtils;
import com.ifms.cmpt.utils.Logger;
import com.ifms.cmpt.utils.TextUtils;

import java.io.File;

public class Main {
    private static final String TAG = "main";

    public static void main(String[] args) {
        if (null == args || args.length <= 0) {
            Logger.initWithSysOut(Logger.DEVELOP);
            UnitTest.unitTest();
            return;
        }
        if (3 <= args.length) {
            final String cmd = args[0];
            if (TextUtils.equals(cmd, "-p")) {
                // -p 28 /Users/${user}/android/android_9.0_r13/frameworks
                final String tag = args[1];
                final String dataPath = getJarPath();
                final String logFile = dataPath + "/out/log_" + ASeekerManager.getOSVersionName(tag) + ".txt";
                FileUtils.ensurePath(logFile);
                Logger.initWithFile(Logger.DEVELOP, logFile);

                final String filePath = args[2];
                final String tempPath = dataPath + "/temp";
                ASeekerManager mgr = ASeekerManager.getInstance(dataPath, tempPath);
                mgr.parse(tag, filePath);
                Logger.d(TAG, "done.");
                return;
            }
        }

        Logger.e(TAG, "Unknown command");
        Logger.d(TAG, "aseeker [-options]");
        Logger.d(TAG, "  -p [SDK version code] [source code path]");
        Logger.d(TAG, "     such as: -p 33 /Users/abc/android_13.0_r13/frameworks");
    }

    private static String getJarPath() {
        try {
            return new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        } catch (Throwable e) {
            Logger.e(e);
        }
        return null;
    }

}
