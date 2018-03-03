### 4、sps.services.pay.util.app。 pay.stratege.

首先需要明确的是只研究支付宝APP支付，其他的支付过程大同小异。还是先从工具类开始，

(1、)Base64编码及其作用：
- 1、由于某些系统中只能使用ASCII字符。Base64就是用来将非ASCII字符的数据转换成ASCII字符的一种方法。
- 2、base64其实不是安全领域下的加密解密算法。虽然有时候经常看到所谓的base64加密解密。其实base64只能算是一个编码算法，对数据内容进行编码来适合传输
```java
public final class Base64 {}
```
RSA数字签名的原理及作用:                   [友情提示](http://blog.51cto.com/jianboli/1887611)

- 1、数字签名，就是只有信息的发送者才能产生的别人无法伪造的一段数字串，这段数字串同时也是对信息的发送者发送信息真实性的一个有效证明。
- 2、数字签名是非对称密钥加密技术与数字摘要技术的应用。简单地说,所谓数字签名就是附加在数据单元上的一些数据,或是对数据单元所作的密码变换。
- 3、这种数据或变换允许数据单元的接收者用以确认数据单元的来源和数据单元的完整性并保护数据,防止被人(例如接收者)进行伪造。

具体代码[点这里](https://gist.github.com/guoxiaoxu/5eb344011b4f176f5082acd72a6811b0)
```java

public class RSA {

    public static final String SIGN_ALGORITHMS = "SHA1WithRSA";

    /**
     * RSA签名
     *
     * @param content       待签名数据
     * @param privateKey    商户私钥
     * @param input_charset 编码格式
     * @return 签名值
     */
    public static String sign(String content, String privateKey, String input_charset) {}

    /**
     * RSA验签名检查
     *
     * @param content        待签名数据
     * @param sign           签名值
     * @param ali_public_key 支付宝公钥
     * @param input_charset  编码格式
     * @return 布尔值
     */
    public static boolean verify(String content, String sign, String ali_public_key, String input_charset) {}

    /**
     * 解密
     *
     * @param content       密文
     * @param private_key   商户私钥
     * @param input_charset 编码格式
     * @return 解密后的字符串
     */
    public static String decrypt(String content, String private_key, String input_charset) throws Exception {}


    /**
     * 得到私钥
     *
     * @param key 密钥字符串（经过base64编码）
     * @throws Exception
     */
    public static PrivateKey getPrivateKey(String key) throws Exception {}
}
```

支付宝接口公用函数类[全部代码点这里](https://gist.github.com/guoxiaoxu/3475074e8f74ba6bc2f63723539a98f3)

```java
/* *
 *类名：AlipayFunction
 *功能：支付宝接口公用函数类
 *详细：该类是请求、通知返回两个文件所调用的公用函数核心处理文件，不需要修改
 *版本：3.3
 *日期：2012-08-14
 *说明：
 *以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己网站的需要，按照技术文档编写,并非一定要使用该代码。
 *该代码仅供学习和研究支付宝接口使用，只是提供一个参考。
 */

public class AlipayCore {

    /**
     * 除去数组中的空值和签名参数
     *
     * @param sArray 签名参数组
     * @return 去掉空值与签名参数后的新签名参数组
     */
    public static Map<String, String> paraFilter(Map<String, String> sArray) {}

    /**
     * 把数组所有元素排序，并按照“参数=参数值”的模式用“&”字符拼接成字符串
     *
     * @param params 需要排序并参与字符拼接的参数组
     * @return 拼接后字符串
     */
    public static String createLinkString(Map<String, String> params) {}

    /**
     * 写日志，方便测试（看网站需求，也可以改成把记录存入数据库）
     *
     * @param sWord 要写入日志里的文本内容
     */
    public static void logResult(String sWord) {}

    /**
     * 生成文件摘要
     *
     * @param strFilePath      文件路径
     * @param file_digest_type 摘要算法
     * @return 文件摘要结果
     */
    public static String getAbstract(String strFilePath, String file_digest_type) throws IOException {}
}
```
支付宝通知处理类:[具体代码点这里](https://gist.github.com/guoxiaoxu/7d2034285142792cc06a4f717e8cf67f)
```java
/* *
 *类名：AlipayNotify
 *功能：支付宝通知处理类
 *详细：处理支付宝各接口通知返回
 *版本：3.3
 *日期：2012-08-17
 *说明：
 *以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己网站的需要，按照技术文档编写,并非一定要使用该代码。
 *该代码仅供学习和研究支付宝接口使用，只是提供一个参考

 *************************注意*************************
 *调试通知返回时，可查看或改写log日志的写入TXT里的数据，来检查通知返回是否正常
 */
public class AlipayNotify {

    /**
     * 支付宝消息验证地址
     */
    private static final String HTTPS_VERIFY_URL = "https://mapi.alipay.com/gateway.do?service=notify_verify&";

    /**
     * 验证消息是否是支付宝发出的合法消息
     *
     * @param params 通知返回来的参数数组
     * @return 验证结果
     */
    public static boolean verify(Map<String, String> params) {}

    /**
     * 根据反馈回来的信息，生成签名结果
     *
     * @param Params 通知返回来的参数数组
     * @param sign   比对的签名结果
     * @return 生成的签名结果
     */
    private static boolean getSignVeryfy(Map<String, String> Params, String sign) {}

    /**
     * 获取远程服务器ATN结果,验证返回URL
     *
     * @param notify_id 通知校验ID
     * @return 服务器ATN结果
     * 验证结果集：
     * invalid命令参数不对 出现这个错误，请检测返回处理中partner和key是否为空
     * true 返回正确信息
     * false 请检查防火墙或者是服务器阻止端口问题以及验证时间是否超过一分钟
     */
    private static String verifyResponse(String notify_id) {}

    /**
     * 获取远程服务器ATN结果
     *
     * @param urlvalue 指定URL路径地址
     * @return 服务器ATN结果
     * 验证结果集：
     * invalid命令参数不对 出现这个错误，请检测返回处理中partner和key是否为空
     * true 返回正确信息
     * false 请检查防火墙或者是服务器阻止端口问题以及验证时间是否超过一分钟
     */
    private static String checkUrl(String urlvalue) {}
}
```

支付宝各接口请求提交类[具体代码看这里](https://gist.github.com/guoxiaoxu/b96d694610d7ea24fd2966530481b758)

```java
/* *
 *类名：AlipaySubmit
 *功能：支付宝各接口请求提交类
 *详细：构造支付宝各接口表单HTML文本，获取远程HTTP数据
 *版本：3.3
 *日期：2012-08-13
 *说明：
 *以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己网站的需要，按照技术文档编写,并非一定要使用该代码。
 *该代码仅供学习和研究支付宝接口使用，只是提供一个参考。
 */

public class AlipaySubmit {

    /**
     * 支付宝提供给商户的服务接入网关URL(新)
     */
    private static final String ALIPAY_GATEWAY_NEW = "https://mapi.alipay.com/gateway.do?";

    /**
     * 生成签名结果
     *
     * @param sPara 要签名的数组
     * @return 签名结果字符串
     */
    public static String buildRequestMysign(Map<String, String> sPara) {}

    /**
     * 生成要请求给支付宝的参数数组
     *
     * @param sParaTemp 请求前的参数数组
     * @return 要请求的参数数组
     */
    private static Map<String, String> buildRequestPara(Map<String, String> sParaTemp) {}

    /**
     * 建立请求，以表单HTML形式构造（默认）
     *
     * @param sParaTemp     请求参数数组
     * @param strMethod     提交方式。两个值可选：post、get
     * @param strButtonName 确认按钮显示文字
     * @return 提交表单HTML文本
     */
    public static String buildRequest(Map<String, String> sParaTemp, String strMethod, String strButtonName) {}

    /**
     * 建立请求，以表单HTML形式构造（默认）
     *
     * @param sParaTemp 请求参数数组
     * @return 提交表单所需的参数
     */
    public static String buildRequestParams(Map<String, String> sParaTemp) {
        //待请求参数数组
        Map<String, String> sPara = buildRequestPara(sParaTemp);
        StringBuffer toRet = new StringBuffer();
        for (Map.Entry<String, String> entry : sPara.entrySet()) {
            toRet.append(entry.getKey()).append("=").append("\"").append(entry.getValue()).append("\"").append("&");
        }

        return toRet.substring(0, toRet.length() - 1);
    }

    /**
     * 建立请求，以表单HTML形式构造，带文件上传功能
     *
     * @param sParaTemp       请求参数数组
     * @param strMethod       提交方式。两个值可选：post、get
     * @param strButtonName   确认按钮显示文字
     * @param strParaFileName 文件上传的参数名
     * @return 提交表单HTML文本
     */
    public static String buildRequest(Map<String, String> sParaTemp, String strMethod, String strButtonName, String strParaFileName) {}

    /**
     * 建立请求，以模拟远程HTTP的POST请求方式构造并获取支付宝的处理结果
     * 如果接口中没有上传文件参数，那么strParaFileName与strFilePath设置为空值
     * 如：buildRequest("", "",sParaTemp)
     *
     * @param strParaFileName 文件类型的参数名
     * @param strFilePath     文件路径
     * @param sParaTemp       请求参数数组
     * @return 支付宝处理结果
     * @throws Exception
     */
    public static String buildRequest(String strParaFileName, String strFilePath, Map<String, String> sParaTemp) throws Exception {}

    /**
     * MAP类型数组转换成NameValuePair类型
     *
     * @param properties MAP类型数组
     * @return NameValuePair类型数组
     */
    private static NameValuePair[] generatNameValuePair(Map<String, String> properties) {}

    /**
     * 用于防钓鱼，调用接口query_timestamp来获取时间戳的处理函数
     * 注意：远程解析XML出错，与服务器是否支持SSL等配置有关
     *
     * @return 时间戳字符串
     * @throws IOException
     * @throws DocumentException
     * @throws MalformedURLException
     */
    public static String query_timestamp() throws
            DocumentException, IOException {  }
}

```
httpClient :

```java
/* *
 *类名：HttpRequest
 *功能：Http请求对象的封装
 *详细：封装Http请求
 *版本：3.3
 *日期：2011-08-17
 *说明：
 *以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己网站的需要，按照技术文档编写,并非一定要使用该代码。
 *该代码仅供学习和研究支付宝接口使用，只是提供一个参考。
 */

public class HttpRequest {

    /**
     * HTTP GET method
     */
    public static final String METHOD_GET = "GET";

    /**
     * HTTP POST method
     */
    public static final String METHOD_POST = "POST";

    /**
     * 待请求的url
     */
    private String url = null;

    /**
     * 默认的请求方式
     */
    private String method = METHOD_POST;

    private int timeout = 0;

    private int connectionTimeout = 0;

    /**
     * Post方式请求时组装好的参数值对
     */
    private NameValuePair[] parameters = null;

    /**
     * Get方式请求时对应的参数
     */
    private String queryString = null;

    /**
     * 默认的请求编码方式
     */
    private String charset = "GBK";

    /**
     * 请求发起方的ip地址
     */
    private String clientIp;

    /**
     * 请求返回的方式
     */
    private HttpResultType resultType = HttpResultType.BYTES;

    public HttpRequest(HttpResultType resultType) {
        super();
        this.resultType = resultType;
    }
}

```

```java
/* *
 *类名：HttpResponse
 *功能：Http返回对象的封装
 *详细：封装Http返回信息
 *版本：3.3
 *日期：2011-08-17
 *说明：
 *以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己网站的需要，按照技术文档编写,并非一定要使用该代码。
 *该代码仅供学习和研究支付宝接口使用，只是提供一个参考。
 */

public class HttpResponse {


    /**
     * 返回中的Header信息
     */
    private Header[] responseHeaders;

    /**
     * String类型的result
     */
    private String stringResult;

    /**
     * btye类型的result
     */
    private byte[] byteResult;

}

-----------------------------------------------------
/* *
 *类名：HttpProtocolHandler
 *功能：HttpClient方式访问
 *详细：获取远程HTTP数据
 *版本：3.3
 *日期：2012-08-17
 *说明：
 *以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己网站的需要，按照技术文档编写,并非一定要使用该代码。
 *该代码仅供学习和研究支付宝接口使用，只是提供一个参考。
 */

public class HttpProtocolHandler {

    /**
     * 默认等待HttpConnectionManager返回连接超时（只有在达到最大连接数时起作用）：1秒
     */
    private static final long defaultHttpConnectionManagerTimeout = 3 * 1000;
    private static String DEFAULT_CHARSET = "GBK";
    private static HttpProtocolHandler httpProtocolHandler = new HttpProtocolHandler();
    /**
     * 连接超时时间，由bean factory设置，缺省为8秒钟
     */
    private int defaultConnectionTimeout = 8000;
    /**
     * 回应超时时间, 由bean factory设置，缺省为30秒钟
     */
    private int defaultSoTimeout = 30000;
    /**
     * 闲置连接超时时间, 由bean factory设置，缺省为60秒钟
     */
    private int defaultIdleConnTimeout = 60000;
    private int defaultMaxConnPerHost = 30;
    private int defaultMaxTotalConn = 80;
    /**
     * HTTP连接管理器，该连接管理器必须是线程安全的.
     */
    private HttpConnectionManager connectionManager;
}

--------------------------------------------------------------------------
/* *
 *类名：HttpResultType
 *功能：表示Http返回的结果字符方式
 *详细：表示Http返回的结果字符方式
 *版本：3.3
 *日期：2012-08-17
 *说明：
 *以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己网站的需要，按照技术文档编写,并非一定要使用该代码。
 *该代码仅供学习和研究支付宝接口使用，只是提供一个参考。
 */
public enum HttpResultType {
    /**
     * 字符串方式
     */
    STRING,

    /**
     * 字节数组方式
     */
    BYTES
}
```

支付策略：

```java
/**
 * 支付策略路由
 */
public interface PayStrategy {
    /**
     * 调用对应支付平台组装支付请求报文
     * @param payType 传入需要的支付方式
     * @param params  其他额外需要的参数
     * @return 生成的支付请求
     */
    String generatePayParams(PayType payType, Map<String, Object> params);

}

-------------------------------------------------------------------------------
/**
 * 支付策略工厂
 * Created by Martin on 2016/7/01.
 */
public class StrategyFactory {

    private static Map<Integer, PayStrategy> strategyMap = new HashMap<>();

    static {
        strategyMap.put(PayType.ALIPAY_WEB.value(), new AlipayWebStrategy());
        strategyMap.put(PayType.ALIPAY_APP.value(), new AlipayAppStrategy());
        strategyMap.put(PayType.ALIPAY_WAP.value(), new AlipayWapStrategy());
        //........
        strategyMap.put(PayType.WECHAT_APP.value(), new WechatPayAppStrategy());
    }

    private StrategyFactory() {                  //构造方法私有化
    }

    private static class InstanceHolder {        //这里使用了单例模式内部类的加载机制。
        public static StrategyFactory instance = new StrategyFactory();
    }
    public static StrategyFactory getInstance() {
        return InstanceHolder.instance;
    }

    public PayStrategy creator(Integer type) {
        return strategyMap.get(type);
    }

}

-------------------------------------------------------------------------------
/**
 * 支付策略上下文
 */
public class StrategyContext {

    private PayStrategy payStrategy;

    /**
     * 调用对应支付平台组装支付请求报文
     * @param payType
     * @param params
     * @return
     */
    public String generatePayParams(PayType payType, Map<String, Object> params) {
        payStrategy = StrategyFactory.getInstance().creator(payType.value());
        return payStrategy.generatePayParams(payType, params);
    }

}

```
支付宝app支付（对接移动支付，包含国内、国际）:   具体代码[点这里](https://gist.github.com/guoxiaoxu/8c6b35d85a13129dda04fe6b82c46b30)
```java
/**
 * 支付宝app支付（对接移动支付，包含国内、国际）
 * Created by Martin on 2016/7/01.
 */
public class AlipayAppStrategy implements PayStrategy {

    private static Logger logger = LoggerFactory.getLogger(AlipayAppStrategy.class);

    public String generatePayParams(PayType payType, Map<String, Object> params) {
        String retUrl = null;
        if (params.size() > 0 && null != params.get("retUrl")) {
            retUrl = (String) params.get("retUrl");
        }
        String sellerCode = (String) params.get("sellerCode");
        ICheckSellerService checkSellerService = (ICheckSellerService) InitListener.context.getBean("checkSellerService");
        if (checkSellerService.isUseGlobalPay(sellerCode)) {
            //国际支付
            return makeGlobalParam(params);
        } else {
            //国内支付
            return makeMainParams(params, retUrl);
        }
    }

    private String makeGlobalParam(Map<String, Object> params) {
        StringBuffer sb = new StringBuffer();
        // 签约合作者身份ID
        sb.append("partner=\"").append(PropertiesUtil.getValue("pay.request.alipayAppGlobal.partner")).append("\"");
        // 签约卖家支付宝账号
        sb.append("&seller_id=\"").append(PropertiesUtil.getValue("pay.request.alipayAppGlobal.seller_email")).append("\"");
        // 商户网站唯一订单号
        sb.append("&out_trade_no=\"").append((String) params.get("payCode")).append("\"");
        // 商品名称
        sb.append("&subject=\"").append(params.get("sellerName")).append(params.get("orderCode")).append("\"");
        // 商品详情
        sb.append("&body=\"").append(PropertiesUtil.getValue("pay.request.alipayAppGlobal.body")).append("\"");
        // 商品金额
        sb.append("&rmb_fee=\"").append(String.valueOf(((BigDecimal) params.get("toPay")).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue())).append("\"");
        sb.append("&currency=\"").append(PropertiesUtil.getValue("pay.request.alipayAppGlobal.currency")).append("\"");
        sb.append("&forex_biz=\"").append(PropertiesUtil.getValue("pay.request.alipayAppGlobal.forex_biz")).append("\"");
        // 服务器异步通知页面路径
        sb.append("&notify_url=\"").append(PropertiesUtil.getValue("pay.notify.alipay.globalApp.url")).append("\"");
        // 服务接口名称， 固定值
        sb.append("&service=\"").append(PropertiesUtil.getValue("pay.request.alipayAppGlobal.service")).append("\"");
        // 支付类型， 固定值
        sb.append("&payment_type=\"").append(PropertiesUtil.getValue("pay.request.alipayAppGlobal.payment_type")).append("\"");
        // 参数编码， 固定值
        sb.append("&_input_charset=\"").append(PropertiesUtil.getValue("pay.request.alipayAppGlobal.input_charset")).append("\"");
        sb.append("&appenv=").append("\"").append(PropertiesUtil.getValue("pay.request.alipayAppGlobal.appenv")).append("\"");
        String sign = com.guo.sps.services.pay.util.app.ali.global.sign.RSA.sign(sb.toString(), PropertiesUtil.getValue("pay.request.alipayAppGlobal.private_key"), PropertiesUtil.getValue("pay.request.alipayAppGlobal.input_charset"));
        try {
            sign = URLEncoder.encode(sign, PropertiesUtil.getValue("pay.request.alipayAppGlobal.input_charset"));
        } catch (UnsupportedEncodingException e) {
            logger.error("alipayGlobal app sign encode error,ex:{}",e.getMessage());
        }
        String sHtmlText = sb.append("&sign=\"").append(sign).append("\"&sign_type=\"").append(PropertiesUtil.getValue("pay.request.alipayAppGlobal.sign_type")).append("\"").toString();
        if(logger.isDebugEnabled()){
            logger.debug("alipay参数信息:{}", sHtmlText);
        }
        return sHtmlText;
    }

}
```
