package cn.isliu.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

public class FileUtil {

    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);
    public static final String DEFAULT_FILE_PATH = "fs";

    public static byte [] getImageData(String filePath) {
        byte[] imageData = new byte[0];
        try {
            // 如果 filePath 是 URL，则使用网络请求获取图片
            if (filePath.startsWith("http")) {
                URL url = new URL(filePath);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                connection.connect();

                try (InputStream input = connection.getInputStream()) {
                    imageData = readStream(input);
                }
            } else {
                // 否则当作本地文件处理
                File imageFile = new File(filePath);
                imageData = Files.readAllBytes(imageFile.toPath());
            }
        } catch (IOException e) {
            log.error("【巨量广告助手】 读取图片文件异常！参数：{}，错误信息：{}", filePath, e.getMessage(), e);
        }
        return imageData;
    }

    public static byte[] readStream(InputStream input) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * 根据 URL 下载文件到本地路径
     *
     * @param fileURL 文件的 URL 地址
     * @param savePath 保存文件的本地路径
     * @throws IOException 如果下载或写入过程中发生错误
     */
    public static void downloadFile(String fileURL, String savePath) {
        HttpURLConnection httpConn = null;
        try {
            URL url = new URL(fileURL);

            // 打开连接
            httpConn = (HttpURLConnection) url.openConnection();
            int responseCode = httpConn.getResponseCode();

            // 检查响应码是否为 HTTP OK
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 获取输入流
                InputStream inputStream = httpConn.getInputStream();

                // 创建文件输出流
                FileOutputStream outputStream = new FileOutputStream(savePath);

                // 缓冲区
                byte[] buffer = new byte[1024];
                int bytesRead;

                // 写入文件
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                // 关闭流
                outputStream.close();
                inputStream.close();

                log.info("文件下载成功。保存路径: {}", savePath);
            } else {
                log.error("文件下载失败。响应码: {}", responseCode);
            }
        } catch (Exception e) {
            log.error("文件下载失败。", e);
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    /**
     * 获取系统临时目录路径，并确保目录存在
     * @return 临时目录路径
     */
    public static String getRootPath() {
        // 获取系统临时目录
        String tempDir = System.getProperty("java.io.tmpdir") + File.separator + DEFAULT_FILE_PATH ;
        // 确保路径以文件分隔符结尾
        if (!tempDir.endsWith(File.separator)) {
            tempDir += File.separator;
        }
        
        // 确保目录存在
        File dir = new File(tempDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        return tempDir;
    }
}
