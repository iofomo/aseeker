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
        String dataPath = "/Users/tiony.bo/workspace/mts/server/aseeker";
        String tempPath = dataPath + "/temp";
        String filePath = "/Users/tiony.bo/workspace/mts/server/aseeker/test";

        ASeekerManager mgr = ASeekerManager.getInstance(dataPath, tempPath);
        mgr.parse(tag, filePath);
    }

    private static void testFormat() {
        String in = "/Users/tiony.bo/Downloads/IActivityTaskManager.aidl";
        String out = "/Users/tiony.bo/Downloads/IActivityTaskManager-out.aidl";
        boolean succ = AidlFormator.format(new File(in), new File(out));
        Logger.assertTrue(succ);

        in = "/Users/tiony.bo/Downloads/IActivityManager.aidl";
        out = "/Users/tiony.bo/Downloads/IActivityManager-out.aidl";
        succ = AidlFormator.format(new File(in), new File(out));
        Logger.assertTrue(succ);
    }
}
