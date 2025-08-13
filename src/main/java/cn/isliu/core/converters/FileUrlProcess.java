package cn.isliu.core.converters;

import cn.isliu.core.utils.FsApiUtil;
import cn.isliu.core.utils.FsClientUtil;
import cn.isliu.core.utils.FileUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileUrlProcess implements FieldValueProcess<String> {

    private static final Logger log = Logger.getLogger(FileUrlProcess.class.getName());

    @Override
    public String process(Object value) {
        if (value instanceof String) {
            return value.toString();
        }

        List<String> fileUrls = new ArrayList<>();
        if (value instanceof JsonArray) {
            JsonArray arr = (JsonArray) value;
            for (int i = 0; i < arr.size(); i++) {
                JsonElement jsonElement = arr.get(i);
                if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    String url = getUrlByTextFile(jsonObject);
                    fileUrls.add(url);
                }
            }
        } else if (value instanceof JsonObject) {
            JsonObject jsb = (JsonObject) value;
            String url = getUrlByTextFile(jsb);
            fileUrls.add(url);
        }
        return String.join(",", fileUrls);
    }

    @Override
    public String reverseProcess(Object value) {
        // 简单实现，可以根据需要进行更复杂的反向处理
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private synchronized String getUrlByTextFile(JsonObject jsb) {
        String url = "";
        String cellType = jsb.get("type").getAsString();

        switch (cellType) {
            case "url":
                String link = jsb.get("link").getAsString();
                if (link == null) {
                    url = jsb.get("text").getAsString();
                } else {
                    url = link;
                }
                break;
            case "embed-image":
                url = getImageOssUrl(jsb);
                break;
            case "attachment":
                url = getAttachmentOssUrl(jsb);
                break;
        }
        return url;
    }

    public static String getImageOssUrl(JsonObject jsb) {
        String url = "";
        String fileToken = jsb.get("fileToken").getAsString();

        String fileUuid = UUID.randomUUID().toString();
        String filePath = FileUtil.getRootPath() + File.separator + fileUuid + ".png";

        boolean isSuccess = true;
        try {
            FsApiUtil.downloadMaterial(fileToken, filePath , FsClientUtil.getFeishuClient(), null);
            url = filePath;
        } catch (Exception e) {
            log.log(Level.WARNING,"【飞书表格】 根据文件FileToken下载失败！fileToken: {0}, e: {1}", new Object[]{fileToken, e.getMessage()});
            isSuccess = false;
        }

        if (!isSuccess) {
            String tmpUrl = FsApiUtil.downloadTmpMaterialUrl(fileToken, FsClientUtil.getFeishuClient());
            // 根据临时下载地址下载
            FileUtil.downloadFile(tmpUrl, filePath);
        }

        log.info("【飞书表格】 文件上传-飞书图片上传成功！fileToken: " + fileToken + ", filePath: " + filePath);
        return url;
    }

    public String getAttachmentOssUrl(JsonObject jsb) {
        String url = "";
        String token = jsb.get("fileToken").getAsString();
        String fileName = jsb.get("text").getAsString();

        String fileUuid = UUID.randomUUID().toString();
        String path = FileUtil.getRootPath() + File.separator + fileUuid + fileName;

        boolean isSuccess = true;
        try {
            FsApiUtil.downloadMaterial(token, path , FsClientUtil.getFeishuClient(), null);
            url = path;
        } catch (Exception e) {
            log.log(Level.WARNING, "【飞书表格】 附件-根据文件FileToken下载失败！fileToken: {0}, e: {1}", new Object[]{token, e.getMessage()});
            isSuccess = false;
        }

        if (!isSuccess) {
            String tmpUrl = FsApiUtil.downloadTmpMaterialUrl(token, FsClientUtil.getFeishuClient());
            FileUtil.downloadFile(tmpUrl, path);
        }

        log.info("【飞书表格】 文件上传-附件上传成功！fileToken: " + token + ", filePath: " + path);
        return url;
    }
}
