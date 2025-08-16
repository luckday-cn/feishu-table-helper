package cn.isliu.core.utils;

import cn.isliu.core.CopySheet;
import cn.isliu.core.Reply;
import cn.isliu.core.Sheet;
import cn.isliu.core.SheetMeta;
import cn.isliu.core.ValuesBatch;
import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.exception.FsHelperException;
import cn.isliu.core.logging.FsLogger;
import cn.isliu.core.pojo.ApiResponse;
import cn.isliu.core.service.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lark.oapi.service.drive.v1.model.BatchGetTmpDownloadUrlMediaReq;
import com.lark.oapi.service.drive.v1.model.BatchGetTmpDownloadUrlMediaResp;
import com.lark.oapi.service.drive.v1.model.DownloadMediaReq;
import com.lark.oapi.service.drive.v1.model.DownloadMediaResp;
import com.lark.oapi.service.sheets.v3.model.GetSpreadsheetReq;
import com.lark.oapi.service.sheets.v3.model.GetSpreadsheetResp;
import com.lark.oapi.service.sheets.v3.model.QuerySpreadsheetSheetReq;
import com.lark.oapi.service.sheets.v3.model.QuerySpreadsheetSheetResp;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import cn.isliu.core.logging.FsLogger;
import cn.isliu.core.enums.ErrorCode;


/**
 * 飞书API工具类
 * 
 * 封装了与飞书表格API交互的各种操作方法，包括数据读取、元数据获取、单元格合并等操作
 */
public class FsApiUtil {

    private static final Gson gson = new Gson();
    // 使用统一的FsLogger替代java.util.logging.Logger
    private static final String REQ_TYPE = "JSON_STR";
    public static final int DEFAULT_ROW_NUM = 1000;

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

            // 发起请求
            QuerySpreadsheetSheetResp resp = client.sheets().v3().spreadsheetSheet().query(req);

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

    /**
     * 合并单元格
     * 
     * 在指定工作表中合并指定范围的单元格
     * 
     * @param cell 合并范围（如"A1:B2"）
     * @param sheetId 工作表ID
     * @param client 飞书客户端
     * @param spreadsheetToken 电子表格Token
     */
    public static void mergeCells(String cell, String sheetId, FeishuClient client, String spreadsheetToken) {
        try {
            CustomCellService.CellBatchUpdateRequest batchMergeRequest = CustomCellService.CellBatchUpdateRequest.newBuilder()
                    .addRequest(CustomCellService.CellRequest.mergeCells().setReqType(REQ_TYPE)
                            .setReqParams(cell.replaceAll("%SHEET_ID%", sheetId)).build())
                    .build();

            ApiResponse batchMergeResp = client.customCells().cellsBatchUpdate(spreadsheetToken, batchMergeRequest);

            if (!batchMergeResp.success()) {
                FsLogger.warn("【飞书表格】 合并单元格请求异常！参数：{}，错误信息：{}", cell, batchMergeResp.getMsg());
                throw new FsHelperException("【飞书表格】 合并单元格请求异常！");
            }
        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 合并单元格异常！参数：{}，错误信息：{}", cell, e.getMessage());
            throw new FsHelperException("【飞书表格】 合并单元格异常！");
        }
    }

    public static void createTemplateHead(String head, String sheetId, FeishuClient client, String spreadsheetToken) {
        try {
            // 批量操作数据值（在一个请求中同时执行多个数据操作）
            CustomValueService.ValueBatchUpdateRequest batchValueRequest = CustomValueService.ValueBatchUpdateRequest.newBuilder()
                    // 在指定范围前插入数据
                    .addRequest(CustomValueService.ValueRequest.batchPutValues()
                            .setReqType(REQ_TYPE)
                            .setReqParams(head.replaceAll("%SHEET_ID%", sheetId))
                            .build())
                    .build();

            ApiResponse apiResponse = client.customValues().valueBatchUpdate(spreadsheetToken, batchValueRequest);
            if (!apiResponse.success()) {
                FsLogger.warn("【飞书表格】 写入表格头数据异常！错误信息：{}", apiResponse.getMsg());
                throw new FsHelperException("【飞书表格】 写入表格头数据异常！");
            }
        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 写入表格头异常！错误信息：{}", e.getMessage());
            throw new FsHelperException("【飞书表格】 写入表格头异常！");
        }
    }

    public static void setTableStyle(String style, String sheetId, FeishuClient client, String spreadsheetToken) {
        try {
            CustomCellService.CellBatchUpdateRequest batchUpdateRequest = CustomCellService.CellBatchUpdateRequest.newBuilder()
                    .addRequest(CustomCellService.CellRequest.styleCellsBatch().setReqType(REQ_TYPE)
                            .setParams(style.replaceAll("%SHEET_ID%", sheetId))
                            .build())
                    .build();

            ApiResponse apiResponse = client.customCells().cellsBatchUpdate(spreadsheetToken, batchUpdateRequest);
            if (!apiResponse.success()) {
                FsLogger.warn("【飞书表格】 写入表格样式数据异常！参数：{}，错误信息：{}", style, apiResponse.getMsg());
                throw new FsHelperException("【飞书表格】 写入表格样式数据异常！");
            }
        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 写入表格样式异常！参数：{}，错误信息：{}", style, e.getMessage());
            throw new FsHelperException("【飞书表格】 写入表格样式异常！");
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
            DownloadMediaResp resp = client.drive().v1().media().download(req);

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

            BatchGetTmpDownloadUrlMediaResp resp = client.drive().v1().media().batchGetTmpDownloadUrl(req);

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

    public static Object putValues(String spreadsheetToken, CustomValueService.ValueRequest putValuesBuilder, FeishuClient client) {
        FsLogger.info("【飞书表格】 putValues 开始写入数据！参数：{}", gson.toJson(putValuesBuilder));

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
            GetSpreadsheetResp resp = client.sheets().v3().spreadsheet().get(req);

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

    public static Object imageUpload(String filePath, String fileName, String position ,String sheetId, String spreadsheetToken, FeishuClient client) {
        try {
            byte[] imageData = FileUtil.getImageData(filePath);

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
                FsLogger.warn("【飞书表格】 图片上传失败！参数：{}，错误信息：{}", filePath, gson.toJson(imageResp));
            }
            return imageResp.getData();
        } catch (Exception e) {
            FsLogger.warn("【飞书表格】 图片上传异常！参数：{}，错误信息：{}", filePath, e.getMessage());
        }

        return null;
    }
}