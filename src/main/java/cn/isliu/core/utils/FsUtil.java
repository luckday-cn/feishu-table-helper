package cn.isliu.core.utils;

import java.net.MalformedURLException;
import java.net.URL;

public class FsUtil {

    public static final int FS_MAX_COLUMNS_PER_REQUEST = 100;
    public static final int FS_MAX_DIMENSION_LENGTH = 5000;
    public static final String ROWS = "ROWS";

    public static String getSheetTokenByFsLink(String fsLink) {
        if (fsLink == null) {
            return null;
        }
        // 获取url 最后一个斜杆后的数据，不包含url参数
        String path;
        try {
            path = new URL(fsLink).getPath();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
