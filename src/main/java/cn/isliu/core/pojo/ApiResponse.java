package cn.isliu.core.pojo;

/**
 * API响应基类
 *
 * @param <T> 响应数据类型
 */
public class ApiResponse<T> {

    private int code;
    private String msg;
    private T data;

    /**
     * 无参构造函数
     */
    public ApiResponse() {}

    /**
     * 构造函数
     *
     * @param code 响应码
     * @param msg 响应消息
     * @param data 响应数据
     */
    public ApiResponse(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 获取响应码
     *
     * @return 响应码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取响应消息
     *
     * @return 响应消息
     */
    public String getMsg() {
        return msg;
    }

    /**
     * 获取响应数据
     *
     * @return 响应数据
     */
    public T getData() {
        return data;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setData(T data) {
        this.data = data;
    }

    /**
     * 判断请求是否成功
     *
     * @return true表示成功，false表示失败
     */
    public boolean success() {
        return code == 0;
    }

}
