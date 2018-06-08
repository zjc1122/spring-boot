package cn.zjc.enums;

/**
 * @ClassName : SysUtilCode
 * @Author : zhangjiacheng
 * @Date : 2018/6/8
 * @Description : TODO
 */
public enum SysUtilCode {

    //密码错误
    PASSWORD_ERROR(1,"密码错误"),
    //消息发送失败
    MESSAGE_SEND_ERROR(2,"消息发送失败");

    private Integer code;
    private String desc;

    SysUtilCode(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
