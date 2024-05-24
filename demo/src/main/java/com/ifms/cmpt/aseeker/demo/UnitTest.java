package com.ifms.cmpt.aseeker.demo;

import com.ifms.cmpt.aseeker.ASeekerManager;
import com.ifms.cmpt.aseeker.AidlFormator;
import com.ifms.cmpt.utils.Logger;

import java.io.File;

public class UnitTest {
    private static final String TAG = "UintTest";

    public static void unitTest() {
        testParse();
//        testFormat();
    }

    private static void testParse() {
        String tag = "33";// 13.0
        String dataPath = "/Users/test/aseeker";
        String tempPath = dataPath + "/temp";
        String filePath = "/Users/test/aseeker/test";

        ASeekerManager mgr = ASeekerManager.getInstance(dataPath, tempPath);
        mgr.parse(tag, filePath);
    }

    private static void testFormat() {
        String in = "/Users/test/IActivityTaskManager.aidl";
        String out = "/Users/test/IActivityTaskManager-out.aidl";
        boolean succ = AidlFormator.format(new File(in), new File(out));
        Logger.assertTrue(succ);

        in = "/Users/test/IActivityManager.aidl";
        out = "/Users/test/IActivityManager-out.aidl";
        succ = AidlFormator.format(new File(in), new File(out));
        Logger.assertTrue(succ);
    }
}
