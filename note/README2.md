### 3、sps包～dto、enums、mq



**题外话：**

- 1、最好到Google　Play上下载。某些应用模块没有，比如：微信的游戏，jD的金融。有些功能会增强，但到了国内就变广告了。

- 2、到[GitHub](https://github.com/getlantern/lantern/releases/tag/latest)下载lantern。还有下载一个Go安装器。更新下Google服务。一般需要root。原生系统更新到7.1.1.自带的。

- 3、尽量用安卓原生系统吧，比如诺基亚，港货。台货。

1、要支付首先你得有个手机，在安装几个APP。我们正式开始吧，
```java
/**
 * APP端请求头所带的信息
 */
public class MobileInfo implements Serializable {
    /**
     * app版本
     */
    private String appVersion;
    /**
     * app系统版本
     */
    private String appSystemVersion;
    /**
     * app设备ID
     */
    private String appDeviceId;
    /**
     * app设备宽
     */
    private Integer appDeviceWidth;
    /**
     * app设备高
     */
    private Integer appDeviceHeight;
    /**
     * 是否开启夜间模式
     */
    private Boolean nightMode;
  //  Setter、Getter、Constrctor略...
}
```
数据字典中关于id和name的json串处理DTO

```java
public class DDDetails implements Serializable {
    /**
     * The Id.
     */
    private String id;
    /**
     * The Name.
     */
    private String name;
}

```
支付请求参数(重点理解)
```java
public class PayRequestParam {
    /**
     * 通知回调跳转页面
     */
    private String retUrl;
    /**
     * 订单ID
     */
    private Long orderID;
    /**
     * 订单编号
     */
    private String orderCode;
    /**
     * 支付编号
     */
    private String payCode;
    /**
     * 支付平台
     */
    private String onLineStyle;
    /**
     * 支付入口
     */
    private String browseType;
    /**
     * 支付金额
     */
    private BigDecimal toPay;
    /**
     * 商家
     */
    private String sellerName;
    /**
     * 商家编号
     */
    private String sellerCode;
    /**
     * 创建时间
     */
    private Long createTime;
    /**
     * 有效时间
     */
    private Long invalidTime;
    /**
     * 请求业务来源
     */
    private String requestBiz;
```
--------------
(2)、一些枚举类，定义支付平台、支付类型、支付平台类新，请求来源。

支付平台：
```java
public enum  PayPlatform {
    UNION_PC("101", "银联(onlinePay)"),
    UNION_APP("102", "银联(手机)"),
    CEB_GATEWAY("201", "光大网关"),
    CEB("202", "光大网页"),
    ALIPAY_GLOBAL("301", "支付宝(国际)"),
    ALIPAY_COMMON("302", "支付宝(普通)"),
    WECHAT_APP("401", "微信支付(开放平台)"),
    WECHAT_WAP("402", "微信支付(公众平台)"),
    ACCOUNT_COMMON("501", "现金账户"),
    PSBC("601", "邮政银行");

    private String code;
    private String label;
    public static PayPlatform getByCode(String code) {
        for (PayPlatform o : PayPlatform.values()) {
            if (o.getCode().equals(code)) {
                return o;
            }
        }
        throw new IllegalArgumentException("Not exist "
                + PayPlatform.class.getName() + " for code " + code);
    }
}
```
三方支付：
```java
public enum PayType {
    ALIPAY_WEB(1, "支付宝网页支付"),
    ALIPAY_WAP(2, "支付宝手机网页支付"),
    UNION_WEB(3, "银联网页支付"),
    UNION_WAP(4, "银联手机网页支付"),
    UNION_APP(5, "银联手机APP支付"),
    ALIPAY_APP(6, "支付宝手机APP支付"),
    PSBC_WEB(7, "邮局网页支付"),
    PSBC_WAP(8, "邮局手机网页支付"),
    CEB_WEB(9, "光大银行网页支付"),
    CEB_WAP(10, "光大银行手机网页支付"),
    WECHAT_APP(11, "微信app支付"),//微信只支持app
    CEB_GATEWAY_WEB(12, "光大银行网关支付"),
    CEB_GATEWAY_WAP(13, "光大银行手机支付");

    private Integer value;
    private String desc;
    private String name;

    public static PayType valueOf(int value) {

        for (PayType type : PayType.values()) {
            if (type.value() == value) {
                return type;
            }
        }
        return null;
    }
```

支付平台类型：
```java
public enum  PlatformType {
    DD("DD", "当当"),
    TB("TB", "淘宝"),
    JD("JD", "京东"),
    ICBC("ICBC", "工行"),
    UNIONPAY("UNIONPAY", "银联支付"),
    CEB("CEB", "光大银行"),
    PSBC("PSBC", "邮政储蓄"),
    CCB("CCB", "建行"),
    HZBSQ("HZBSQ", "杭州保税区"),
    NBBSQ("NBBSQ", "宁波保税区"),
    WECHAT("WECHAT", "微信支付"),
    YHD("YHD", "一号店");
    private String value;
    private String desc;

    PlatformType(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static PlatformType getPlatform(Integer value) {
        if (1 == value || 2 == value || 6 == value) {
            return PlatformType.TB;
        } else if (3 == value || 4 == value || 5 == value) {
            return PlatformType.UNIONPAY;
        } else if (7 == value || 8 == value) {
            return PlatformType.PSBC;
        } else if (9 == value || 10 == value || 12 == value) {
            return PlatformType.CEB;
        } else if (11 == value) {
            return PlatformType.WECHAT;
        }
        return null;
    }
}
```

请求来源：

```java
/**
 * Created by guo on 3/2/2018.
 * 请求来源
 */
public enum RequestFrom {
    PC(1, "PC"),
    WAP(2, "WAP"),
    WEIXIN(3, "微信"),
    ANDROID(4, "安卓"),
    IOS(5, "苹果");

    private int _id;

    private String _name;

    public static RequestFrom getById(int id) throws BusinessException {
        RequestFrom result = null;

        for(RequestFrom requestFrom : RequestFrom.values()){
            if(requestFrom._id == id){
                result = requestFrom;
            }
        }
        if(result == null){
            throw new BusinessException("系统中未包含此请求来源类型");
        }else {
            return result;
        }
    }

    public boolean isApp(){
        if(_id==IOS._id||_id==ANDROID._id){
            return true;
        }
        return false;
    }
    public boolean isIOS(){
        if(_id==IOS._id){
            return true;
        }
        return false;
    }
    public boolean isMobile(){
        return _id != PC._id;
    }
    public int transToImageSizeType(){
        return this.equals(PC) ? 0 : 1;
    }
}
```

---------------------------------
接下来是Dao gogogo.
