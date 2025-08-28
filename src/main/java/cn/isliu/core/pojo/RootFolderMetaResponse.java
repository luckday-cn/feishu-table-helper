package cn.isliu.core.pojo;

import com.google.gson.annotations.SerializedName;

/**
 * 飞书API获取根目录元数据的响应模型类
 * 
 * 对应飞书API返回的JSON格式：
 * {
 *   "code": 0,
 *   "msg": "success",
 *   "data": {
 *     "token": "fldbc0k5Zws8AQBpfzlFMKCpN4z",
 *     "id": "fldbc0k5Zws8AQBpfzlFMKCpN4z",
 *     "user_id": "ou_xxxxx",
 *     "name": "我的空间"
 *   }
 * }
 * 
 * @author FsHelper
 * @since 1.0
 */
public class RootFolderMetaResponse {

    /**
     * 响应状态码
     * 0表示成功，非0表示失败
     */
    @SerializedName("code")
    private int code;

    /**
     * 响应消息
     * 通常成功时为"success"，失败时包含错误描述
     */
    @SerializedName("msg")
    private String msg;

    /**
     * 根目录元数据
     */
    @SerializedName("data")
    private RootFolderMeta data;

    /**
     * 默认构造函数
     */
    public RootFolderMetaResponse() {
    }

    /**
     * 完整构造函数
     * 
     * @param code 响应状态码
     * @param msg 响应消息
     * @param data 根目录元数据
     */
    public RootFolderMetaResponse(int code, String msg, RootFolderMeta data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 获取响应状态码
     * 
     * @return 响应状态码，0表示成功
     */
    public int getCode() {
        return code;
    }

    /**
     * 设置响应状态码
     * 
     * @param code 响应状态码
     */
    public void setCode(int code) {
        this.code = code;
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
     * 设置响应消息
     * 
     * @param msg 响应消息
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * 获取根目录元数据
     * 
     * @return 根目录元数据
     */
    public RootFolderMeta getData() {
        return data;
    }

    /**
     * 设置根目录元数据
     * 
     * @param data 根目录元数据
     */
    public void setData(RootFolderMeta data) {
        this.data = data;
    }

    /**
     * 检查响应是否成功
     * 
     * @return true表示API调用成功，false表示失败
     */
    public boolean isSuccess() {
        return code == 0;
    }

    /**
     * 检查是否包含有效的根目录数据
     * 
     * @return true表示包含有效的根目录数据
     */
    public boolean hasValidData() {
        return isSuccess() && 
               data != null && 
               data.getToken() != null && 
               !data.getToken().trim().isEmpty();
    }

    /**
     * 根目录元数据内部类
     */
    public static class RootFolderMeta {
        
        /**
         * 文件夹token
         */
        @SerializedName("token")
        private String token;

        /**
         * 文件夹ID
         */
        @SerializedName("id")
        private String id;

        /**
         * 用户ID
         */
        @SerializedName("user_id")
        private String userId;

        /**
         * 文件夹名称
         */
        @SerializedName("name")
        private String name;

        /**
         * 默认构造函数
         */
        public RootFolderMeta() {
        }

        /**
         * 完整构造函数
         * 
         * @param token 文件夹token
         * @param id 文件夹ID
         * @param userId 用户ID
         * @param name 文件夹名称
         */
        public RootFolderMeta(String token, String id, String userId, String name) {
            this.token = token;
            this.id = id;
            this.userId = userId;
            this.name = name;
        }

        /**
         * 获取文件夹token
         * 
         * @return 文件夹token
         */
        public String getToken() {
            return token;
        }

        /**
         * 设置文件夹token
         * 
         * @param token 文件夹token
         */
        public void setToken(String token) {
            this.token = token;
        }

        /**
         * 获取文件夹ID
         * 
         * @return 文件夹ID
         */
        public String getId() {
            return id;
        }

        /**
         * 设置文件夹ID
         * 
         * @param id 文件夹ID
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * 获取用户ID
         * 
         * @return 用户ID
         */
        public String getUserId() {
            return userId;
        }

        /**
         * 设置用户ID
         * 
         * @param userId 用户ID
         */
        public void setUserId(String userId) {
            this.userId = userId;
        }

        /**
         * 获取文件夹名称
         * 
         * @return 文件夹名称
         */
        public String getName() {
            return name;
        }

        /**
         * 设置文件夹名称
         * 
         * @param name 文件夹名称
         */
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "RootFolderMeta{" +
                    "token='" + token + '\'' +
                    ", id='" + id + '\'' +
                    ", userId='" + userId + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "RootFolderMetaResponse{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
}
