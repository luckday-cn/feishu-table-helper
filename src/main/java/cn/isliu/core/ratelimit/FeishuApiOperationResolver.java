package cn.isliu.core.ratelimit;

import okhttp3.HttpUrl;
import okhttp3.Request;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 根据请求信息推断 API 操作类型
 */
public final class FeishuApiOperationResolver {

    private static final Pattern SPREADSHEET_TOKEN_PATTERN =
            Pattern.compile("/sheets/(?:v2|v3)/spreadsheets/([^/]+)");

    private FeishuApiOperationResolver() {
    }

    public static ApiOperation resolve(Request request) {
        if (request == null || request.url() == null) {
            return ApiOperation.GENERIC_OPERATION;
        }
        String path = request.url().encodedPath();
        String method = request.method().toUpperCase(Locale.ROOT);

        if (path == null) {
            return ApiOperation.GENERIC_OPERATION;
        }

        if (path.contains("/values_prepend")) {
            return ApiOperation.INSERT_DATA;
        }
        if (path.contains("/values_append")) {
            return ApiOperation.APPEND_DATA;
        }
        if (path.contains("/values_image")) {
            return ApiOperation.WRITE_IMAGE;
        }
        if (path.contains("/values_batch_update")) {
            return ApiOperation.WRITE_MULTI_RANGE;
        }
        if (path.contains("/values_batch_get")) {
            return ApiOperation.READ_MULTI_RANGE;
        }
        if (path.contains("/values/") && "GET".equals(method)) {
            return ApiOperation.READ_SINGLE_RANGE;
        }
        if (path.endsWith("/values") && "PUT".equals(method)) {
            return ApiOperation.WRITE_SINGLE_RANGE;
        }
        if (path.contains("/merge_cells")) {
            return ApiOperation.MERGE_CELLS;
        }
        if (path.contains("/unmerge_cells")) {
            return ApiOperation.SPLIT_CELLS;
        }
        if (path.contains("/styles_batch_update")) {
            return ApiOperation.BATCH_SET_CELL_STYLE;
        }
        if (path.endsWith("/style")) {
            return ApiOperation.SET_CELL_STYLE;
        }
        if (path.contains("/sheets_batch_update")) {
            return ApiOperation.SHEET_OPERATION;
        }
        if (path.contains("/dimension_range")) {
            if ("POST".equals(method)) {
                return ApiOperation.ADD_ROWS_COLUMNS;
            }
            if ("PUT".equals(method)) {
                return ApiOperation.UPDATE_ROWS_COLUMNS;
            }
            if ("DELETE".equals(method)) {
                return ApiOperation.DELETE_ROWS_COLUMNS;
            }
        }
        if (path.contains("/insert_dimension_range")) {
            return ApiOperation.INSERT_ROWS_COLUMNS;
        }
        if (path.contains("/dataValidation")) {
            if ("GET".equals(method)) {
                return ApiOperation.QUERY_DROPDOWN;
            }
            if ("POST".equals(method)) {
                return ApiOperation.SET_DROPDOWN;
            }
            if ("PUT".equals(method)) {
                return ApiOperation.UPDATE_DROPDOWN;
            }
            if ("DELETE".equals(method)) {
                return ApiOperation.DELETE_DROPDOWN;
            }
        }
        if (path.contains("/protected_dimension")) {
            return ApiOperation.ADD_PROTECTED_RANGE;
        }

        return ApiOperation.GENERIC_OPERATION;
    }

    public static String extractSpreadsheetToken(Request request) {
        if (request == null) {
            return null;
        }
        HttpUrl url = request.url();
        if (url == null) {
            return null;
        }
        Matcher matcher = SPREADSHEET_TOKEN_PATTERN.matcher(url.encodedPath());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}

