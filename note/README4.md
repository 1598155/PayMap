### 4、sps.realm、services。

在Dao层定义了domian、mapper映射文件，想了解的可以去看看。有助于理解整个系统。[点这里]()


(1)、我们首先来看消息摘要生成器,原谅我看不懂。先看个大概，回头慢慢看。

```java
/**
 * 消息摘要生成器
 */
public class HmacSHA256Utils {
    private static Logger logger = LoggerFactory.getLogger(HmacSHA256Utils.class);
    private HmacSHA256Utils(){}
    public static String digest(String key, String content) {
        try {
            logger.info("key:{};content:{}",key,content);
            Mac mac = Mac.getInstance("HmacSHA256");
            byte[] secretByte = key.getBytes("utf-8");
            byte[] dataBytes = content.getBytes("utf-8");

            SecretKey secret = new SecretKeySpec(secretByte, "HmacSHA256");
            mac.init(secret);

            byte[] doFinal = mac.doFinal(dataBytes);
            // byte[] hexB = new Hex().encode(doFinal);
            String oss= Base64.encodeBase64String(mac.doFinal(dataBytes));
            logger.info("加密后的字符串：{}" , oss);
            return oss;

            // return new String(hexB, "utf-8");
        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }

    public static String digest(String key, Map<String, ?> map) {
        StringBuilder s = new StringBuilder();
        for(Object values : map.values()) {
            if(values instanceof String[]) {
                for(String value : (String[])values) {
                    s.append(value);
                }
            } else if(values instanceof List) {
                for(String value : (List<String>)values) {
                    s.append(value);
                }
            } else {
                s.append(values);
            }
        }
        return digest(key, s.toString());
    }
}
```

还是看realm包下的`StatelessAuthRealm`,登录用户的验证和授权

```java
/**
 * 登录用户的验证和授权
 * Created by Martin on 2016/7/01.
 */
public class StatelessAuthRealm extends AuthorizingRealm {

    private static Logger logger = LoggerFactory.getLogger(StatelessAuthRealm.class);

    //1、首先注入用户信息
    @Autowired
    public MemberMapper memberMapper;

    @Override
    public boolean supports(AuthenticationToken token) {
        //仅支持StatelessToken类型的Token
        return token instanceof StatelessAuthToken;
    }

    /**
     * 授权
     * @param principals
     * @return
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String username = (String) principals.getPrimaryPrincipal();
        //2、查询用户
        Member user = memberMapper.findByUsername(username);
        if (user != null) {
          //3、在这里进行授权
            SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
            return authorizationInfo;
        } else {
            logger.info("授权失败");
            throw new IncorrectCredentialsException();
        }
    }
------------------------- --------------------------------------------------------
    /**
     * 验证
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        StatelessAuthToken statelessToken = (StatelessAuthToken) token;
        String userId = statelessToken.getUserId();
        String username=statelessToken.getUsername();
        String key = null;
        try {
            key = this.getKey(userId,username);
        } catch (Exception e) {
            logger.info("获取加密key失败{}",e);
            throw new BusinessException("获取加密key失败{}",e);
        }
        //在服务器端生成客户端参数消息摘要
        String serverDigest = HmacSHA256Utils.digest(key, statelessToken.getParams());
        logger.info("服务器端的randomKey：{}，time:{},url:{},username:{},digest:{}",statelessToken.getParams().get("randomKey"),
                statelessToken.getParams().get("time"),statelessToken.getParams().get("url"),
                statelessToken.getParams().get("username"),serverDigest);
        //然后进行客户端消息摘要和服务器端消息摘要的匹配
        return new SimpleAuthenticationInfo(
                username,
                serverDigest,
                getName());
    }

    /**
     * 根据用户名获取密码
     * @param username 用户名
     * @return  密码
     */
    private String getKey(String userId,String username){
        // 访客模式，密码为visit$13password经过MD5加密后的字符串
        return "abc";
    }

```
拦截器：
```java
/**
 *  shiro无状态的web权限拦截器
 */
public class StatelessAuthcFilter extends AccessControlFilter{
    private static Logger logger = LoggerFactory.getLogger(StatelessAuthcFilter.class);

    @Override
    protected boolean isAccessAllowed(ServletRequest servletRequest, ServletResponse servletResponse, Object o) throws Exception {
        return false;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest servletRequest, ServletResponse servletResponse) throws Exception {
        //获取请求头的参数
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String userId=httpServletRequest.getHeader("userId");
        String authentication = httpServletRequest.getHeader("Authentication");
        String uri=httpServletRequest.getRequestURI();
        String queryString=httpServletRequest.getQueryString();
        String url="";
        if(queryString==null){
            url=uri;
        }else {
            url=uri+"?"+queryString;
        }
        String time = httpServletRequest.getHeader("Time");
        if (StringUtil.isEmpty(authentication) || StringUtil
                .isEmpty(url) || StringUtil.isEmpty(time)) {
            logger.warn("shiro获取验证失败，请求的参数有空值");
            throw new BusinessException("获取验证失败，请求的参数有空值");
        }
        //验证时间的合法性
        if (!this.isAccessTime(time)) {
            logger.warn("shiro获取验证失败，非法的请求时间");
            throw new BusinessException("获取验证失败，非法的请求时间");
        }
        //从字符串参数中获取各个参数值
        Map<String, String> parms = this.getParms(authentication, url, time);

        String username = parms.get("username");
        String clientDigest = parms.get("clientDigest");
        parms.remove("clientDigest");

        //验证随机数的合法性
        String randomKey = parms.get("randomKey");
        logger.info("随机数重复验证:{}",randomKey);

        //委托给realm验证digest的合法性
        StatelessAuthToken token = new StatelessAuthToken(username, parms, clientDigest,userId);
        try {
            logger.info("客户端的randomKey：{}，time:{},url:{},username:{},digest:{}",randomKey,time,url,username,clientDigest);
            getSubject(servletRequest, servletResponse).login(token);
        } catch (Exception e) {
            logger.warn("shiro获取验证失败，消息摘要错误");
            throw new BusinessException("获取验证失败，消息摘要错误");

        }
        return true;
    }
----------------------------------------------------------------------------------------
    /**
     * 根据客户端发来的参数来组合成加密参数
     *
     * @param authentication 客户端传来的加密信息
     * @param url            url
     * @param time           客户端时间
     * @return 加密参数Map parms
     */
    private Map<String, String> getParms(String authentication, String url, String time)  {
        String authentications[] = authentication.split(":");
        if(authentications.length<3){
            logger.warn("shiro验证失败，验证参数错误");
            throw new BusinessException("验证失败，验证参数错误");
        }
        String username = authentications[0];
        String randomData = authentications[1];
        String clientDigest = authentications[2];
        Map<String, String> maps = new HashMap<>();
        maps.put("username", username);
        maps.put("randomKey", randomData);
        maps.put("url", url);
        maps.put("time", time);
        maps.put("clientDigest", clientDigest);
        return maps;
    }

    /**
     * @param time 客户端传来的时间
     * @return 该时间是否为可通过的时间
     * @throws ParseException 格式转换异常
     */
    private boolean isAccessTime(String time) throws ParseException {
        long serverTimeStamp = DateUtils.getUnixTimestamp();
        long clientTimeStamp = DateUtils.getUnixTimestamp(time,DateUtils.standard);
        Long deadTime =600L;
        long timeStampTemp = Math.abs(serverTimeStamp - clientTimeStamp);
        if (timeStampTemp > deadTime) {
            return false;
        } else {
            return true;
        }
    }
}
```

唉，表示看不懂那。。MQ监听支付请求入口消息

```java
/**
 * MQ监听支付请求入口消息
 */
public class PayRequestQueueListener implements ChannelAwareMessageListener {
    private static Logger logger = LoggerFactory.getLogger(PayRequestQueueListener.class);
    @Autowired
    private IPayRouteService payRouteService;
    @Autowired
    private Gson2JsonMessageConverter messageConverter;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        channel.basicQos(100);
        PayRequestParam queueObject = (PayRequestParam)messageConverter.fromMessage(message);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        if(payRouteService.getPayRetMap4MQ(queueObject)){
            logger.trace("success processed pay request:{}", JSON.toJSONString(queueObject));
        }else{
            logger.error("error processed pay request:{}",JSON.toJSONString(queueObject));
            //补偿机制忽略
        }
    }
}
```

配置文件

```xml
    <!--statelessReealm-->
    <bean id="statelessRealm" class="com.guo.sps.realm.StatelessAuthRealm">
        <property name="cachingEnabled" value="false"/>
    </bean>
    <!--statelessFilter-->
    <bean id="statelessFilter" class="com.guo.sps.realm.filters.StatelessAuthcFilter"></bean>
    
    <bean id="payRequestQueueListener" class="com.guo.sps.mq.PayRequestQueueListener"/>
```

web.xml

```xml
    <filter>
        <filter-name>loggingFilter</filter-name>
        <filter-class>com.guo.core.web.system.filters.LoggingFilter</filter-class>
    </filter>

    <filter-mapping>
        <filter-name>loggingFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

```
