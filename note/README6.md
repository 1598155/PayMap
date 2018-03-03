### 6、sps.services.及impls。

## 目录
- 2、core.dao，service，web(重点是接口的设计)[点这里](https://github.com/guoxiaoxu/PayMap/blob/master/note/README1.md)

- 3、sps包～dto、enums、mq[点这里](https://github.com/guoxiaoxu/PayMap/blob/master/note/README2.md)

- 4、在Dao层定义了domian、mapper映射文件，想了解的可以去看看。有助于理解整个系统。[点这里](https://github.com/guoxiaoxu/PayMap/blob/master/note/README4.md)

- 5、pay.util.app。 pay.stratege,支付工具类，和支付策略[点这里](https://github.com/guoxiaoxu/PayMap/blob/master/note/README5.md)

- 6、sps.service及实现类(**重点**)[点这里](https://github.com/guoxiaoxu/PayMap/blob/master/note/README5.md)

首先看下支付的一些相关知识点

(1)、支付方案：

<img src="https://i.imgur.com/5ZW6e9h.jpg" width = "300" height = "200"/>

(2)、支付流程：

![](https://i.imgur.com/LqAxdIC.png)

一般流程说明：    [原作者](http://hugnew.com/?p=868)

- 用户在商户网站选定商品并下单，在商户支付页面选择对应支付平台图标，进行支付；
- 商户按照文档提交支付请求接口组织报文，向支付平台发送支付请求；
- 如果是PC端，会跳转到对应支付平台的支付页面，如果是移动端，会唤起对应的支付工具，用户在支付平台输入支付信息，提交支付；
- 支付平台将支付结果通知商户；
- 若支付成功，则支付平台将交易结果异步发送给商户；
- 商户若未收到交易结果，则商户按照文档查询接口向支付平台发请求查询该交易，以确定消费交易的状态，支付平台收到 查询请求时，将同步返回该笔消费交易的交易结果；
- 商户若收到交易结果，如果未向支付平台反馈已收到交易结果，支付平台会重复发送交易结果。

在这提一点，由于网络原因等异常情况支付平台可能出现多次发送支付结果的情况，通知回调接口商户要注意做好接口幂等，其余的不再多说。

在线支付过程：
- 01）创建合法的商业购物网站，且在易宝支付平台申请成为商家，并提供商家银行卡号，等待审查

- 02）如果审查通过，和易宝支付签约，得向易宝支付一定的接口使用费或每笔交易的手续费，通常是交易额的1%左右

- 03）付款后，易宝会给每个商家一个唯一的商家编号，密钥，和接口文档和jar包

- 04）当用户请求来了，你得获取客户的相关信息，例如：订单号，金额，支付银行等14个信息
    注意：其中hmac是由客户订单信息和商家密钥生成，通过易宝提供的工具包自动生成

- 05）用表单的方式，以POST或GET请求，使用GBK或GB2312向易宝发出支付请求，请求中带有14个参数

- 06）易宝如果验成功，即客户发送过来的信息与易宝生成的信息相同的话，易宝认为是合法用户请求,否则非法用户请求
    注意：验证是否成功，主要通过hmac和密钥作用

- 07）如果是合法用户的支付请求的话，易宝再将请求转发到客户指定的银行，例如：招商银行
    注意：易宝必须支持招商银行在线支付

- 08）凡是转账，查询，等等都由银行后台操作完成，是全封闭的，与易宝没有任何关系，千万不要认为是易宝在处理资金结算

- 09）银行处理完毕后，将响应结果转发到易宝在线支付平台

- 10）易宝在线支付经过加工处理后，再将结果响应到用户指定的外网可以访问的Servlet或Jsp页面

- 11）商家网站可以用GET方式接收易宝的响应数据，经过验证合法后，再将付款结果，显示在用户的浏览器
    注意：验证是否成功，主要通过hmac和密钥作用




**终于终于到了重点内容了。首先看几个重要的接口**

```java
/**
 * 支付宝通知业务接口
 */
public interface IAlipayNotifyService {

    /**
     * 国内支付宝app通知（这里暂时不考虑国际app，所以不贴那个接口）

     */
    void alipayNotifyMainApp(HttpServletRequest request, HttpServletResponse response);
}
--------------------------------------------------------------------------------
public interface IPayRouteService {

    /**
     * 组装支付请求报文（http入口）
     */
    public Map<String, Object> getPayRetMap(PayRequestParam payRequestParam);
    /**
     * 组装支付请求报文（MQ入口）
     */
    public Boolean getPayRetMap4MQ(PayRequestParam payRequestParam);
}

------------------------------------------------------------------------------
/**
 * 交易流水业务接口
 */
public interface IPayMapService extends IBaseService<PayMap> {
    /**
     * 支付通知更新交易记录
     */
    PayMap updatePayMapByPayCode(String tempPayCode, String msg, String msg2, PlatformType platformType, String ssn, String remark2);

}
--------------------------------------------------------------------------------
/**
 * 字典业务接口
 */
public interface ISysConfigService extends IBaseService<SysConfig> {
    /**
     * 获取字典数据
     */
    public List<DDDetails> getDictionary(String sysKey, String id);
    //增删改查忽略
}

--------------------------------------------------------------------------------
/**
 * 缓存业务接口
 * Created by Martin on 2016/7/01.
 */
public interface ICacheService {
    /**
     * 获取指定缓存对象,不能用作获取缓存List对象-List用getListCacheByKey方法
     * @param key   the key
     * @param clazz the clazz 不能传集合类型
     * @return cache by key
     */
    public <T> T getCacheByKey(String key, Class<T> clazz);
    //省略。。大部分

```

现在来看具体的实现类。

支付请求路由：
**
 * 支付请求路由业务
 * Created by Martin on 2016/7/01.
 */
@Service
public class PayRouteService implements IPayRouteService {

    private static Logger logger = LoggerFactory.getLogger(PayRouteService.class);
    @Resource
    private RabbitTemplate amqpTemplate;
    @Autowired
    private PayMapMapper payMapMapper;

    public PayType dealPayType(String onLineStyle, String browseType) {
        PayType payType = null;
        switch (onLineStyle) {
            case "alipay":
                switch (browseType) {
                    case "web":
                        payType = PayType.ALIPAY_WEB;
                        break;
                    case "wap":
                        payType = PayType.ALIPAY_WAP;
                        break;
                    case "app":
                        payType = PayType.ALIPAY_APP;
                        break;
                    default:
                        payType = null;
                }
                break;
            //略
        return payType;
    }

    private void savePayRecord(String payRequsetMsg, PayType payType, PayRequestParam payRequestParam) {
        PayMap payMap = new PayMap();
        payMap.setOrderId(payRequestParam.getOrderID());
        payMap.setOrderCode(payRequestParam.getOrderCode());
        PlatformType type = PlatformType.getPlatform(payType.value());
        payMap.setPlatform(type.value());
        payMap.setTempPayCode(payRequestParam.getPayCode());
        payMap.setPayParams(payRequsetMsg);
        payMap.setRequestBiz(payRequestParam.getRequestBiz());
        payMapMapper.insertSelective(payMap);
    }

    @Override
    public Map<String, Object> getPayRetMap(PayRequestParam payRequestParam) {
        Map<String, Object> retMap = assembleRetMap(payRequestParam);
        return retMap;
    }

    @Override
    public Boolean getPayRetMap4MQ(PayRequestParam payRequestParam) {
        Map<String, Object> retMap = assembleRetMap(payRequestParam);
        amqpTemplate.convertAndSend("payRequestCallback." + payRequestParam.getRequestBiz() + payRequestParam.getOrderCode(), JSON.toJSONString(retMap));
        return true;
    }

    private Map<String, Object> assembleRetMap(PayRequestParam payRequestParam) {
        Map<String, Object> paramsToPass = new HashMap<>();
        if (StringUtils.isNotBlank(payRequestParam.getRetUrl())) {
            paramsToPass.put("retUrl", payRequestParam.getRetUrl());
        }
        paramsToPass.put("toPay", payRequestParam.getToPay());
        paramsToPass.put("payCode", payRequestParam.getPayCode());
        paramsToPass.put("sellerCode", payRequestParam.getSellerCode());
        paramsToPass.put("sellerName", payRequestParam.getSellerName());
        paramsToPass.put("orderCode", payRequestParam.getOrderCode());
        paramsToPass.put("invalidTime", payRequestParam.getInvalidTime());
        paramsToPass.put("orderCreateTime", payRequestParam.getCreateTime());
        PayType payType = dealPayType(payRequestParam.getOnLineStyle(), payRequestParam.getBrowseType());
        StrategyContext context = new StrategyContext();
        String payRequsetMsg = context.generatePayParams(payType, paramsToPass);
        if(logger.isDebugEnabled()){
            logger.debug("订单code为{}的支付请求参数生成信息：{}", new Object[]{payRequestParam.getOrderCode(), payRequsetMsg});
        }
        savePayRecord(payRequsetMsg, payType, payRequestParam);
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("payData", payRequsetMsg);
        retMap.put("payment", payRequestParam.getOnLineStyle());
        retMap.put("orderID", payRequestParam.getOrderID());
        retMap.put("errorCode", "0710");
        retMap.put("message", "请去第三方平台支付~~");
        return retMap;
    }

}

支付宝通知业务：
```java
/**
 * 支付宝通知业务
 */
@Service
public class AlipayNotifyService implements IAlipayNotifyService {

    private static Logger logger = LoggerFactory.getLogger(AlipayNotifyService.class);
    @Autowired
    private IPayMapService payMapService;
    @Resource
    private RabbitTemplate amqpTemplate;

    @Override
    public void alipayNotifyMainApp(HttpServletRequest request, HttpServletResponse response) {
        //商户订单号
        String out_trade_no = null;
        //交易状态
        String trade_status = null;
        Map params = getRequestParams(request);
        if (com.guo.sps.services.pay.util.app.ali.main.util.AlipayNotify.verify(params)) {//验证成功
            try {
                out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"), "UTF-8");
                trade_status = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                logger.error("request getParameter error:{}",e.getMessage());
                throw new SystemException(e);
            }
            String platformCode= PayPlatform.ALIPAY_COMMON.getCode();
            if (trade_status.equals("TRADE_FINISHED")) {
            } else if (trade_status.equals("TRADE_SUCCESS")) {
                pay2NextBiz(response, out_trade_no, params, platformCode);
            }
        } else {//验证失败
            try {
                response.getOutputStream().println("fail");
            } catch (IOException e) {
                logger.error("response IO error:{}", e.getMessage());
                throw new SystemException(e);
            }
        }
    }

    private Map getRequestParams(HttpServletRequest request) {
        Map requestParams = request.getParameterMap();
        Map params = new HashMap();
        for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }
        if(logger.isDebugEnabled()){
            logger.debug("订单orderCode：{}所对应的支付宝通知交易码为{}返回参数列表：{}", new Object[]{params.get("out_trade_no"), params.get("trade_no"), params.toString()});
        }
        return params;
    }

    private void pay2NextBiz(HttpServletResponse response, String out_trade_no, Map params, String platformCode) {
        if(logger.isDebugEnabled()){
            logger.debug("订单orderCode：{}所对应的支付宝通知交易码为:{}支付成功", new Object[]{params.get("out_trade_no"), params.get("trade_no")});
        }
        PayMap payMap = payMapService.updatePayMapByPayCode(out_trade_no, params.toString(), null, PlatformType.TB, String.valueOf(params.get("trade_no")), platformCode);
        amqpTemplate.convertAndSend("payNotify." + payMap.getRequestBiz()+payMap.getOrderCode(), JSON.toJSONString(payMap));
        try {
            //向支付平台回写本地处理成功消息，让支付平台不再继续发送通知消息
            response.getWriter().println("success");
        } catch (IOException e) {
            logger.error("response IO error:{}", e.getMessage());
            throw new SystemException(e);
        }
    }
}

```

交易流水业务：

```java
/**
 * 交易流水业务
 * Created by Martin on 2016/7/01.
 */
@Service
public class PayMapService extends BaseService<PayMap> implements IPayMapService {

    private static Logger logger = LoggerFactory.getLogger(PayMapService.class);
    @Autowired
    private PayMapMapper payMapMapper;

    @Override
    public IBaseMapper<PayMap> getBaseMapper() {
        return payMapMapper;
    }

    @Override
    public PayMap updatePayMapByPayCode(String tempPayCode, String msg, String msg2, PlatformType platformType, String ssn, String remark2) {
        PayMap param = new PayMap();
        param.setTempPayCode(tempPayCode);
        param.setPlatform(platformType.value());
        List<PayMap> payMaps = payMapMapper.select(param);
        Assert.notNull(payMaps);
        if (payMaps != null && !payMaps.isEmpty()) {
            PayMap payMap = payMaps.get(0);
            payMap.setRetMsg(msg);
            payMap.setRetMsg2(msg2);
            payMap.setSwiftNumber(ssn);
            payMap.setIsPaid("1");
            payMap.setNotifyTime(DateUtils.getUnixTimestamp());
            if (StringUtils.isNotBlank(remark2)) {
                payMap.setRemark2(remark2);
            }
            payMapMapper.updateByPrimaryKeySelective(payMap);
            return payMap;
        } else {
            throw new BusinessException("数据库异常，交易记录查询为Null");
        }
    }
}
```

缓存业务：
```java
/**
 * 缓存业务
 *这里只贴了部分。                                      
 */
@Service
public class CacheService implements ICacheService {

    private static Logger logger = LoggerFactory.getLogger(CacheService.class);
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public <T> T getCacheByKey(String key, Class<T> clazz) {
        ValueOperations valueOper = redisTemplate.opsForValue();
        return JSON.parseObject((String) valueOper.get(key), clazz);
    }

    @Override
    public List<DDDetails> getDictEntriesByKey(String dictKey) {
        HashOperations<String, Object, Object> opsForHash = redisTemplate.opsForHash();
        Map<Object, Object> entries = opsForHash.entries(dictKey);
        List<DDDetails> ddDetailsList = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            DDDetails ddDetails = new DDDetails();
            ddDetails.setId(entry.getKey().toString());
            ddDetails.setName((String) entry.getValue());
            ddDetailsList.add(ddDetails);
        }
        return ddDetailsList;
    }


    @Override
    public boolean keyExistInHashTable(String tableName, String key) {
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        return hashOperations.hasKey(tableName, key);
    }

    @Override
    public void delKeyFromRedis(String key) {
        if (StringUtil.isEmpty(key)) {
            throw new BusinessException("参数不能为空");
        }
        redisTemplate.delete(key);
    }

}
```
