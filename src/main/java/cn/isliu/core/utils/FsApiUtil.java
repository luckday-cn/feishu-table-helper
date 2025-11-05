package cn.isliu.core.utils;

import cn.isliu.core.CopySheet;
import cn.isliu.core.Reply;
import cn.isliu.core.Sheet;
import cn.isliu.core.SheetMeta;
import cn.isliu.core.ValuesBatch;
import cn.isliu.core.annotation.TableConf;
import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.config.MapSheetConfig;
import cn.isliu.core.exception.FsHelperException;
import cn.isliu.core.logging.FsLogger;
import cn.isliu.core.pojo.ApiResponse;
import cn.isliu.core.pojo.RootFolderMetaResponse;
import cn.isliu.core.service.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.service.drive.v1.model.*;
import com.lark.oapi.service.sheets.v3.model.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import cn.isliu.core.enums.ErrorCode;


/**
 * 飞书API工具类
 * 
 * 封装了与飞书表格API交互的各种操作方法，包括数据读取、元数据获取、单元格合并等操作
 */
public class FsApiUtil {

    private static final Gson gson = new Gson();
    public static final int DEFAULT_ROW_NUM = 1000;

    public static final Map<String, List<String>> m = new HashMap<String, List<String>>() {
        {
            put("Connection", Collections.singletonList("close"));
        }
    };

    /**
     * 获取工作表数据
     *
     * 从指定的飞书表格中读取指定范围的数据
     *
     * @param sheetId 工作表ID
     * @param spreadsheetToken 电子表格Token
     * @param startPosition 起始位置（如"A1"）
     * @param endPosition 结束位置（如"Z100"）
     * @param client 飞书客户端
     * @return 表格数据对象
     */
    public static ValuesBatch getSheetData(String sheetId, String spreadsheetToken, String startPosition, String endPosition, FeishuClient client) {
        ValuesBatch valuesBatch = null;
        try {
            CustomValueService.ValueRequest batchGetRequest = CustomValueService.ValueRequest.batchGetValues()
                    .addRange(sheetId, startPosition, endPosition)
                    .valueRenderOption("Formula")
                    .dateTimeRenderOption("FormattedString")
                    .build();

            CustomValueService.ValueBatchUpdateRequest getBatchRangesRequest = CustomValueService.ValueBatchUpdateRequest.newBuilder()
                    .addRequest(batchGetRequest)
                    .build();

            ApiResponse batchRangeResp = client.customValues().valueBatchUpdate(spreadsheetToken, getBatchRangesRequest);

            if (batchRangeResp.success()) {
                valuesBatch = gson.fromJson(gson.toJson(batchRangeResp.getData()), ValuesBatch.class);
            } else {
                FsLogger.error(ErrorCode.API_CALL_FAILED, "【飞书表格】获取Sheet数据失败！ 错误信息：" + gson.toJson(batchRangeResp));
                throw new FsHelperException("【飞书表格】获取Sheet数据失败！");
            }
        } catch (Exception e) {
            FsLogger.error(ErrorCode.API_CALL_FAILED, "【飞书表格】获取Sheet数据失败！ 错误信息：" + e.getMessage(), "getSheetData", e);
            throw new FsHelperException("【飞书表格】获取Sheet数据失败！");
        }
        return valuesBatch;
    }

    /**
     * 获取工作表元数据
     *
     * 获取指定工作表的元数据信息，包括行列数、工作表名称等
     *
     * @param sheetId 工作表ID
     * @param client 飞书客户端
     * @param spreadsheetToken 电子表格Token
     * @return 工作表对象
     */
    public static Sheet getSheetMetadata(String sheetId, FeishuClient client, String spreadsheetToken) {
        try {
            QuerySpreadsheetSheetReq req = QuerySpreadsheetSheetReq.newBuilder()
                    .spreadsheetToken(spreadsheetToken)
                    .build();

            QuerySpreadsheetSheetResp resp = client.sheets().v3().spreadsheetSheet().query(req, client.getCloseOfficialPool()
                    ? RequestOptions.newBuilder().headers(m).build() : null);

            // 处理服务端错误
            if (resp.success()) {
                // 修复参数转换遗漏问题 - 直接使用添加了注解的类进行转换
                SheetMeta sheetMeta = gson.fromJson(gson.toJson(resp.getData()), SheetMeta.class);
                List<Sheet> sheets = sheetMeta.getSheets();

                AtomicReference<Sheet> sheet = new AtomicReference<>();
                sheets.forEach(s -> {
                    if (s.getSheetId().equals(sheetId)) {
                        sheet.set(s);
                    }
                });

                return sheet.get();
            } else {
                FsLogger.error(ErrorCode.API_CALL_FAILED, "【飞书表格】 获取Sheet元数据异常！错误信息：" + gson.toJson(resp));
                throw new FsHelperException("【飞书表格】 获取Sheet元数据异常！错误信息：" + resp.getMsg());
            }

        } catch (Exception e) {
            FsLogger.error(ErrorCode.API_CALL_FAILED, "【飞书表格】 获取Sheet元数据异常！错误信息：" + e.getMessage(), "getSheetMeta", e);
            throw new FsHelperException("【飞书表格】 获取Sheet元数据异常！");
        }
    }

    public static void setTableStyle(CustomCellService.StyleCellsBatchBuilder styleCellsBatchBuilder, FeishuClient client, String spreadsheetToken) {
        FsLogger.debug("【飞书表格】 写入表格样式参数：{}", gson.toJson(styleCellsBatchBuilder));

        try {
            CustomCellService.CellBatchUpdateRequest batchUpdateRequest = CustomCellService.CellBatchUpdateRequest.newBuilder()
                    .addRequest(styleCellsBatchBuilder.build())
                    .build();

            ApiResponse apiResponse = client.customCells().cellsBatchUpdate(spreadsheetToken, batchUpdateRequest);
            if (!apiResponse.success()) {
                FsLogger.warn("【飞书表格】 写入表格样式数据异常！参数：{}，错误信息：{}", styleCellsBatchBuilder, apiResponse.getMsg());
                throw new FsHelperException("【飞书表格】 写入表格样式数据异常！");
            }
        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 写入表格样式异常！参数：{}，错误信息：{}", styleCellsBatchBuilder, e.getMessage());
            throw new FsHelperException("【飞书表格】 写入表格样式异常！");
        }
    }

    public static void mergeCells(CustomCellService.CellRequest cellRequest, FeishuClient client, String spreadsheetToken) {
        try {
            CustomCellService.CellBatchUpdateRequest batchMergeRequest = CustomCellService.CellBatchUpdateRequest.newBuilder()
                    .addRequest(cellRequest)
                    .build();

            ApiResponse batchMergeResp = client.customCells().cellsBatchUpdate(spreadsheetToken, batchMergeRequest);

            if (!batchMergeResp.success()) {
                FsLogger.warn("【飞书表格】 合并单元格请求异常！参数：{}，错误信息：{}", cellRequest.toString(), batchMergeResp.getMsg());
                throw new FsHelperException("【飞书表格】 合并单元格请求异常！");
            }
        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 合并单元格异常！参数：{}，错误信息：{}", cellRequest.toString(), e.getMessage(), e);
            throw new FsHelperException("【飞书表格】 合并单元格异常！");
        }
    }

    /**
     * 获取根目录Token
     *
     * 调用飞书开放平台API获取当前租户的根目录token，用于后续的文件夹和文件操作
     * API接口: GET https://open.feishu.cn/open-apis/drive/v1/files/root_folder/meta
     *
     * @param client 飞书客户端
     * @return 根目录token，获取失败时抛出异常
     */
    public static String getRootFolderToken(FeishuClient client) {
        try {
            // 使用自定义文件服务获取根目录元数据
            RootFolderMetaResponse response = client.customFiles().getRootFolderMeta();

            if (response.isSuccess() && response.hasValidData()) {
                String rootFolderToken = response.getData().getToken();
                FsLogger.info("【飞书表格】 获取根目录Token成功！Token: {}", rootFolderToken);
                return rootFolderToken;
            } else {
                FsLogger.warn("【飞书表格】 获取根目录Token失败！错误码：{}，错误信息：{}",
                        response.getCode(), response.getMsg());
                throw new FsHelperException("【飞书表格】 获取根目录Token失败！错误信息：" + response.getMsg());
            }
        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 获取根目录Token异常！错误信息：{}", e.getMessage(), e);
            throw new FsHelperException("【飞书表格】 获取根目录Token异常！");
        }
    }

    public static CreateFolderFileRespBody createFolder(String folderName, String folderToken, FeishuClient client) {
        try {
            // 创建请求对象
            CreateFolderFileReq req = CreateFolderFileReq.newBuilder()
                    .createFolderFileReqBody(CreateFolderFileReqBody.newBuilder()
                            .name(folderName)
                            .folderToken(folderToken)
                            .build())
                    .build();

            // 发起请求
            CreateFolderFileResp resp = client.drive().v1().file().createFolder(req, client.getCloseOfficialPool()
                    ? RequestOptions.newBuilder().headers(m).build() : null);
            if (resp.success()) {
                FsLogger.info("【飞书表格】 创建文件夹成功！ {}", gson.toJson(resp));
                return resp.getData();
            } else {
                FsLogger.warn("【飞书表格】 创建文件夹失败！参数：{}，错误信息：{}", String.format("folderName: %s, folderToken: %s", folderName, folderToken), resp.getMsg());
                throw new FsHelperException("【飞书表格】 创建文件夹失败！");
            }
        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 创建文件夹异常！参数：{}，错误信息：{}", String.format("folderName: %s, folderToken: %s", folderName, folderToken), e.getMessage(), e);
            throw new FsHelperException("【飞书表格】 创建文件夹异常！");
        }
    }

    public static CreateSpreadsheetRespBody createTable(String tableName, String folderToken, FeishuClient client) {
        try {
            CreateSpreadsheetReq req = CreateSpreadsheetReq.newBuilder()
                    .spreadsheet(Spreadsheet.newBuilder()
                            .title(tableName)
                            .folderToken(folderToken)
                            .build())
                    .build();

            CreateSpreadsheetResp resp = client.sheets().v3().spreadsheet().create(req, client.getCloseOfficialPool()
                    ? RequestOptions.newBuilder().headers(m).build() : null);
            if (resp.success()) {
                FsLogger.info("【飞书表格】 创建表格成功！ {}", gson.toJson(resp));
                return resp.getData();
            } else {
                FsLogger.warn("【飞书表格】 创建表格失败！错误信息：{}", gson.toJson(resp));
                throw new FsHelperException("【飞书表格】 创建表格异常！");
            }
        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 创建表格异常！参数：{}，错误信息：{}", String.format("tableName:%s, folderToken:%s", tableName, folderToken), e.getMessage(), e);
            throw new FsHelperException("【飞书表格】 创建表格异常！");
        }
    }

    public static String createSheet(String title, FeishuClient client, String spreadsheetToken) {
        String sheetId = null;
        try {
            // 创建 sheet
            CustomSheetService.SheetBatchUpdateRequest addSheetRequest = CustomSheetService.SheetBatchUpdateRequest.newBuilder()
                    .addRequest(CustomSheetService.SheetRequest.addSheet()
                            .title(title)
//                            .index(0)  // 在第一个位置添加
                            .build())
                    .build();

            ApiResponse addResp = client.customSheets().sheetsBatchUpdate(spreadsheetToken, addSheetRequest);

            if (addResp.success()) {
                FsLogger.info("【飞书表格】 创建 sheet 成功！ {}", gson.toJson(addResp));

                JsonObject jsObj = gson.fromJson(gson.toJson(addResp.getData()), JsonObject.class);
                JsonArray replies = jsObj.getAsJsonArray("replies");
                JsonObject jsonObject = replies.get(0).getAsJsonObject();
                // 使用已有的Reply类
                Reply reply = gson.fromJson(jsonObject, Reply.class);
                sheetId = reply.getAddSheet().getProperties().getSheetId();
                if (sheetId == null || sheetId.isEmpty()) {
                    FsLogger.warn("【飞书表格】 创建 sheet 失败！");
                    throw new FsHelperException("【飞书表格】创建 sheet 异常！SheetId返回为空！");
                }
            } else {
                FsLogger.warn("【飞书表格】 创建 sheet 失败！错误信息：{}", gson.toJson(addResp));
                throw new FsHelperException("【飞书表格】 创建 sheet 异常！");
            }
        } catch (Exception e) {
            String message = e.getMessage();
            FsLogger.warn("【飞书表格】 创建 sheet 异常！错误信息：{}", message);

            throw new FsHelperException(message != null && message.contains("403")? "请按照上方操作，当前智投无法操作对应文档哦" : "【飞书表格】 创建 sheet 异常！");
        }
        return sheetId;
    }


    public static String copySheet(String sourceSheetId, String title, FeishuClient client, String spreadsheetToken) {
        String sheetId = null;
        try {
            CustomSheetService.SheetBatchUpdateRequest copyRequest = CustomSheetService.SheetBatchUpdateRequest.newBuilder()
                    .addRequest(CustomSheetService.SheetRequest.copySheet()
                            .sourceSheetId(sourceSheetId)
                            .destinationTitle(title)
                            .build())
                    .build();

            ApiResponse copyResp = client.customSheets().sheetsBatchUpdate(spreadsheetToken, copyRequest);

            if (copyResp.success()) {
                FsLogger.info("【飞书表格】 复制 sheet 成功！ {}", gson.toJson(copyResp));

                JsonObject jsObj = gson.fromJson(gson.toJson(copyResp.getData()), JsonObject.class);
                JsonArray replies = jsObj.getAsJsonArray("replies");
                JsonObject jsonObject = replies.get(0).getAsJsonObject();
                // 使用已有的Reply类
                Reply reply = gson.fromJson(jsonObject, Reply.class);
                CopySheet copySheet = reply.getCopySheet();
                sheetId = copySheet.getProperties().getSheetId();
                if (sheetId == null || sheetId.isEmpty()) {
                    throw new FsHelperException("【飞书表格】 复制模版异常！SheetID 为空！");
                }
            }
        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 复制模版异常！错误信息：{}", e.getMessage());
            throw new FsHelperException("【飞书表格】 复制模版异常！");
        }
        return sheetId;
    }

    public static void setDateType(String sheetId, FeishuClient client, String spreadsheetToken, String conf, Integer headLine) {
        JsonObject confObj = gson.fromJson(conf, JsonObject.class);
        String position = confObj.get("position").getAsString();
        String formatter = confObj.get("formatter").getAsString();
        if (position == null || position.trim().isEmpty()) return;

        try {
            CustomCellService.CellBatchUpdateRequest batchStyleRequest = CustomCellService.CellBatchUpdateRequest.newBuilder()
                    .addRequest(CustomCellService.CellRequest.styleCells()
                            .sheetId(sheetId)
                            .startPosition(position + headLine)
                            .endPosition(position + DEFAULT_ROW_NUM)
                            .formatter(formatter)
                            .build())
                    .build();

            ApiResponse batchStyleResp = client.customCells().cellsBatchUpdate(spreadsheetToken, batchStyleRequest);

            if (!batchStyleResp.success()) {
                FsLogger.warn("【飞书表格】 写入表格样式数据异常！参数：{}，错误信息：{}", conf, batchStyleResp.getMsg());
            }
        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 写入表格样式数据异常！{}", e.getMessage());
        }
    }

    public static void setOptions(String sheetId, FeishuClient client, String spreadsheetToken, boolean isMulti,
                                  String startPosition, String endPosition, List<String> result) {

        try {
            // 创建设置下拉列表请求
            CustomDataValidationService.DataValidationRequest listRequest = CustomDataValidationService.DataValidationRequest.listValidation()
                    .range(sheetId, startPosition, endPosition) // 设置范围
                    .addValues(result) // 添加下拉选项
                    .multipleValues(isMulti) // 设置支持多选
                    .build();

            // 添加到批量请求中
            CustomDataValidationService.DataValidationBatchUpdateRequest batchRequest = CustomDataValidationService.DataValidationBatchUpdateRequest.newBuilder()
                    .addRequest(listRequest)
                    .build();

            // 执行请求
            ApiResponse response = client.customDataValidations().dataValidationBatchUpdate(spreadsheetToken, batchRequest);

            if (!response.success()) {
                FsLogger.warn("设置下拉列表失败， sheetId:{}, startPosition:{}, endPosition: {}, 返回信息:{}", sheetId, startPosition, endPosition, gson.toJson(response));
            }
        } catch (Exception e) {
            FsLogger.warn("设置下拉列表失败，sheetId:{}", sheetId);
        }
    }

    public static void removeSheet(String sheetId, FeishuClient client, String spreadsheetToken) {
        try {
            CustomSheetService.SheetBatchUpdateRequest deleteRequest = CustomSheetService.SheetBatchUpdateRequest.newBuilder()
                    .addRequest(CustomSheetService.SheetRequest.deleteSheet()
                            .sheetId(sheetId)
                            .build())
                    .build();

            ApiResponse deleteResp = client.customSheets().sheetsBatchUpdate(spreadsheetToken, deleteRequest);

            if (!deleteResp.success()) {
                FsLogger.warn("【飞书表格】 删除 sheet 失败！参数：{}，错误信息：{}", sheetId, deleteResp.getMsg());
            }
        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 删除 sheet 异常！参数：{}，错误信息：{}", sheetId, e.getMessage());
        }
    }

    /**
     * 下载素材
     */
    public static void downloadMaterial(String fileToken, String outputPath, FeishuClient client, String extra) {
        try {
            DownloadMediaReq req = DownloadMediaReq.newBuilder()
                    .fileToken(fileToken)
//                    .extra("无")
                    .build();

            // 发起请求
            DownloadMediaResp resp = client.drive().v1().media().download(req, client.getCloseOfficialPool()
                    ? RequestOptions.newBuilder().headers(m).build() : null);

            // 处理服务端错误
            if (resp.success()) {
                resp.writeFile(outputPath);
            }

        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 下载素材异常！参数：{}，错误信息：{}", fileToken, e.getMessage());
            throw new FsHelperException("【飞书表格】 下载素材异常！");
        }
    }

    public static String downloadTmpMaterialUrl(String fileToken,  FeishuClient client) {
        String tmpUrl = "";
        try {
            BatchGetTmpDownloadUrlMediaReq req = BatchGetTmpDownloadUrlMediaReq.newBuilder()
                    .fileTokens(new String[]{fileToken})
                    .build();

            BatchGetTmpDownloadUrlMediaResp resp = client.drive().v1().media().batchGetTmpDownloadUrl(req, client.getCloseOfficialPool()
                    ? RequestOptions.newBuilder().headers(m).build() : null);

            if (resp.success()) {
                return resp.getData().getTmpDownloadUrls()[0].getTmpDownloadUrl();
            } else {
                FsLogger.warn("【飞书表格】 获取临时下载地址失败！参数：{}，错误信息：{}", fileToken, gson.toJson(resp));
            }
        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 获取临时下载地址异常！参数：{}，错误信息：{}", fileToken, e.getMessage());
        }
        return tmpUrl;
    }

    /**
     * 写入表头
     */
    public static Object writeTableHeaders(String sheetId, String spreadsheetToken, List<String> headers, int titleRow, FeishuClient client) {
        CustomValueService.ValueRequest.BatchPutValuesBuilder batchPutValuesBuilder = CustomValueService.ValueRequest.batchPutValues();

        String position = FsTableUtil.getColumnNameByNuNumber(headers.size());
        batchPutValuesBuilder.addRange(sheetId + "!A" + titleRow + ":" + position + titleRow);
        batchPutValuesBuilder.addRow(headers.toArray());

        Object putValues = FsApiUtil.putValues(spreadsheetToken, batchPutValuesBuilder.build(), client);

        String[] positionArr = {"A" + titleRow, position + titleRow};
        MapSheetConfig config = MapSheetConfig.createDefault();

        CustomCellService.StyleCellsBatchBuilder defaultTableStyle = FsTableUtil.getDefaultTableStyle(sheetId, positionArr,
                config.getHeadBackColor(), config.getHeadFontColor());

        FsApiUtil.setTableStyle(defaultTableStyle, client, spreadsheetToken);
        return putValues;
    }

    /**
     * 写入表头
     */
    public static Object writeTableHeaders(String sheetId, String spreadsheetToken, List<String> headers, FeishuClient client) {
         return writeTableHeaders(sheetId, spreadsheetToken, headers, 1, client);
    }

    public static Object putValues(String spreadsheetToken, CustomValueService.ValueRequest putValuesBuilder, FeishuClient client) {
        FsLogger.debug("【飞书表格】 putValues 开始写入数据！参数：{}", gson.toJson(putValuesBuilder));

        // 添加到批量请求中
        CustomValueService.ValueBatchUpdateRequest putDataRequest = CustomValueService.ValueBatchUpdateRequest.newBuilder()
                .addRequest(putValuesBuilder)
                .build();

        try {
            ApiResponse putResp = client.customValues().valueBatchUpdate(spreadsheetToken, putDataRequest);
            if (putResp.success()) {
                return putResp.getData();
            } else {
                FsLogger.warn("【飞书表格】 写入表格数据失败！参数：{}，错误信息：{}", putValuesBuilder, putResp.getMsg());
                throw new FsHelperException("【飞书表格】 写入表格数据失败！");
            }
        } catch (IOException e) {
            FsLogger.warn("【飞书表格】 写入表格数据异常！参数：{}，错误信息：{}", spreadsheetToken, e.getMessage());
            throw new FsHelperException("【飞书表格】 写入表格数据异常！");
        }
    }

    public static Object batchPutValues(String sheetId, String spreadsheetToken,
                                        CustomValueService.ValueRequest batchPutRequest, FeishuClient client) {

        FsLogger.info("【飞书表格】 batchPutValues 开始写入数据！参数：{}", gson.toJson(batchPutRequest));

        try {
            CustomValueService.ValueBatchUpdateRequest batchPutDataRequest =
                    CustomValueService.ValueBatchUpdateRequest.newBuilder()
                            .addRequest(batchPutRequest)
                            .build();

            ApiResponse batchPutResp = client.customValues().valueBatchUpdate(spreadsheetToken, batchPutDataRequest);
            if (batchPutResp.success()) {
                return batchPutResp.getData();
            } else {
                FsLogger.warn("【飞书表格】 批量写入数据失败！参数：{}，错误信息：{}", sheetId, gson.toJson(batchPutResp));
                throw new FsHelperException("【飞书表格】 批量写入数据失败！");
            }
        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 批量写入数据异常！参数：{}，错误信息：{}", sheetId, e.getMessage());
            throw new FsHelperException("【飞书表格】 批量写入数据异常！");
        }
    }

    public static Object addRowColumns(String sheetId, String spreadsheetToken, String type, int length,FeishuClient client) {

        CustomDimensionService.DimensionBatchUpdateRequest batchRequest = CustomDimensionService.DimensionBatchUpdateRequest.newBuilder()
                .addRequest(CustomDimensionService.DimensionRequest.addDimension()
                        .sheetId(sheetId)
                        .majorDimension(type)
                        .length(length).build())
                .build();

        try {
            ApiResponse batchResp = client.customDimensions().dimensionsBatchUpdate(spreadsheetToken, batchRequest);
            if (batchResp.success()) {
                return batchResp.getData();
            } else {
                FsLogger.warn("【飞书表格】 添加行列失败！参数：{}，错误信息：{}", sheetId, gson.toJson(batchResp));
                throw new FsHelperException("【飞书表格】 添加行列失败！");
            }
        } catch (IOException e) {
            FsLogger.warn("【飞书表格】 添加行列异常！参数：{}，错误信息：{}", sheetId, e.getMessage());
            throw new FsHelperException("【飞书表格】 添加行列异常！");
        }
    }

    public static Object getTableInfo(String sheetId, String spreadsheetToken, FeishuClient client) {
        try {
            // 创建请求对象
            GetSpreadsheetReq req = GetSpreadsheetReq.newBuilder()
                    .spreadsheetToken(spreadsheetToken)
                    .build();

            // 发起请求
            GetSpreadsheetResp resp = client.sheets().v3().spreadsheet().get(req, client.getCloseOfficialPool()
                    ? RequestOptions.newBuilder().headers(m).build() : null);

            // 处理服务端错误
            if (resp.success()) {
                return resp.getData();
            } else {
                FsLogger.warn("【飞书表格】 获取表格信息失败！参数：{}，错误信息：{}", sheetId, resp.getMsg());
            }
        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 获取表格信息异常！参数：{}，错误信息：{}", sheetId, e.getMessage());
        }
        return null;
    }

    /**
     *  字符串类型： formatter: "@"
     */
    public static void setCellType(String sheetId, String formatter, String startPosition, String endPosition, FeishuClient client, String spreadsheetToken) {
        try {
            CustomCellService.CellBatchUpdateRequest batchUpdateRequest = CustomCellService.CellBatchUpdateRequest.newBuilder()
                    .addRequest(CustomCellService.CellRequest.styleCells()
                            .formatter(formatter).sheetId(sheetId).startPosition(startPosition).endPosition(endPosition)
                            .backColor("#ffffff")
                            .bold(false)
                            .build())
                    .build();

            ApiResponse apiResponse = client.customCells().cellsBatchUpdate(spreadsheetToken, batchUpdateRequest);
            if (!apiResponse.success()) {
                FsLogger.warn("【飞书表格】 设置单元格类型失败！参数：{}，错误信息：{}", sheetId, apiResponse.getMsg());
                throw new FsHelperException("【飞书表格】 批量设置单元格类型失败！");
            }
        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 设置单元格类型失败！参数：{}，错误信息：{}", sheetId, e.getMessage());
            throw new FsHelperException("【飞书表格】 批量设置单元格类型异常！");
        }
    }

    public static Object imageUpload(byte[] imageData, String fileName, String position ,String sheetId, String spreadsheetToken, FeishuClient client) {
        try {

            CustomValueService.ValueRequest imageRequest = CustomValueService.ValueRequest.imageValues()
                    .range(sheetId, position)
                    .image(imageData)
                    .name(fileName)
                    .build();

            CustomValueService.ValueBatchUpdateRequest imageWriteRequest = CustomValueService.ValueBatchUpdateRequest.newBuilder()
                    .addRequest(imageRequest)
                    .build();

            ApiResponse imageResp = client.customValues().valueBatchUpdate(spreadsheetToken, imageWriteRequest);

            if (!imageResp.success()) {
                FsLogger.error(ErrorCode.API_SERVER_ERROR, "【飞书表格】 文件上传失败！" + gson.toJson(imageResp));
            }
            return imageResp.getData();
        } catch (Exception e) {
            FsLogger.error(ErrorCode.API_SERVER_ERROR,"【飞书表格】 文件上传异常！" + e.getMessage(), fileName, e);
        }

        return null;
    }
}