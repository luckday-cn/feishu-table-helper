package cn.isliu.core;

import java.util.Arrays;
import java.util.Objects;

public class FileData {

    private String sheetId;
    private String spreadsheetToken;
    private String fileName;
    private byte[] imageData;
    private String position;
    private String fileType;
    private String fileUrl;

    public FileData() {}

    public FileData(String sheetId, String spreadsheetToken, String fileName, byte[] imageData, String position, String fileType) {
        this.sheetId = sheetId;
        this.spreadsheetToken = spreadsheetToken;
        this.fileName = fileName;
        this.imageData = imageData;
        this.position = position;
        this.fileType = fileType;
    }

    public String getSheetId() {
        return sheetId;
    }

    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }

    public String getSpreadsheetToken() {
        return spreadsheetToken;
    }

    public void setSpreadsheetToken(String spreadsheetToken) {
        this.spreadsheetToken = spreadsheetToken;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FileData fileData = (FileData) o;
        return Objects.equals(sheetId, fileData.sheetId) && Objects.equals(spreadsheetToken, fileData.spreadsheetToken) && Objects.equals(fileName, fileData.fileName) && Objects.deepEquals(imageData, fileData.imageData) && Objects.equals(position, fileData.position) && Objects.equals(fileType, fileData.fileType) && Objects.equals(fileUrl, fileData.fileUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sheetId, spreadsheetToken, fileName, Arrays.hashCode(imageData), position, fileType, fileUrl);
    }

    @Override
    public String toString() {
        return "FileData{" +
                "sheetId='" + sheetId + '\'' +
                ", spreadsheetToken='" + spreadsheetToken + '\'' +
                ", fileName='" + fileName + '\'' +
                ", imageData=" + Arrays.toString(imageData) +
                ", position='" + position + '\'' +
                ", fileType='" + fileType + '\'' +
                ", fileUrl='" + fileUrl + '\'' +
                '}';
    }
}
