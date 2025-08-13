package cn.isliu.core.config;

import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.utils.FsClientUtil;

public class FsConfig {


    public static int headLine = 1;

    public static int titleLine = 1;

    public static boolean isCover = false;
    public static boolean CELL_TEXT = false;
    public static String FORE_COLOR = "#000000";
    public static String BACK_COLOR = "#d5d5d5";

    public static void initConfig(String appId, String appSecret) {
        FsClientUtil.initFeishuClient(appId, appSecret);
    }

    public static void initConfig(int headLine, int titleLine, String appId, String appSecret) {
        FsConfig.headLine = headLine;
        FsConfig.titleLine = titleLine;
        FsClientUtil.initFeishuClient(appId, appSecret);
    }

    public static void initConfig(int headLine, FeishuClient client) {
        FsConfig.headLine = headLine;
        FsClientUtil.client = client;
    }

    public static void initConfig(FeishuClient client) {
        FsClientUtil.client = client;
    }

    public static int getHeadLine() {
        return headLine;
    }

    public static int getTitleLine() {
        return titleLine;
    }

    public static FeishuClient getFeishuClient() {
        return FsClientUtil.client;
    }

    public static void setHeadLine(int headLine) {
        FsConfig.headLine = headLine;
    }

    public static void setTitleLine(int titleLine) {
        FsConfig.titleLine = titleLine;
    }
}
