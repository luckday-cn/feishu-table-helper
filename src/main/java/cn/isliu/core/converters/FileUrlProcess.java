package cn.isliu.core.converters;

import cn.isliu.core.client.FsClient;
import cn.isliu.core.config.FsConfig;
import cn.isliu.core.utils.FsApiUtil;
import cn.isliu.core.utils.FileUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import cn.isliu.core.logging.FsLogger;

public class FileUrlProcess implements FieldValueProcess<String> {

    // 使用统一的FsLogger替代java.util.logging.Logger

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
        boolean cover = FsConfig.getInstance().isCover();
        if (!cover && value != null) {
            String str = value.toString();
            byte[] imageData = FileUtil.getImageData(str);
        }
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
            FsApiUtil.downloadMaterial(fileToken, filePath , FsClient.getInstance().getClient(), null);
            url = filePath;
        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 根据文件FileToken下载失败！fileToken: {}, e: {}", fileToken, e.getMessage());
            isSuccess = false;
        }

        if (!isSuccess) {
            String tmpUrl = FsApiUtil.downloadTmpMaterialUrl(fileToken, FsClient.getInstance().getClient());
            // 根据临时下载地址下载
            FileUtil.downloadFile(tmpUrl, filePath);
        }

        FsLogger.info("【飞书表格】 文件上传-飞书图片上传成功！fileToken: {}, filePath: {}", fileToken, filePath);
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
            FsApiUtil.downloadMaterial(token, path , FsClient.getInstance().getClient(), null);
            url = path;
        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 附件-根据文件FileToken下载失败！fileToken: {}, e: {}", token, e.getMessage());
            isSuccess = false;
        }

        if (!isSuccess) {
            String tmpUrl = FsApiUtil.downloadTmpMaterialUrl(token, FsClient.getInstance().getClient());
            FileUtil.downloadFile(tmpUrl, path);
        }

        FsLogger.info("【飞书表格】 文件上传-附件上传成功！fileToken: {}, filePath: {}", token, path);
        return url;
    }
}
