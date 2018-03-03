### 6、sps.controller.base,front.

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


首先来看BaseController

```java
public class BaseController {

    private Logger logger = LoggerFactory.getLogger(BaseController.class);

    /**
     * 获取用户ID，用户ID可能为NULL,需自行判断
     */
    protected Long getUserId(HttpServletRequest request) {
        String sId = request.getHeader("userId");
        if (!StringUtil.isEmpty(sId)) {
            try {
                Long userId = Long.parseLong(sId);
                return userId;
            } catch (NumberFormatException e) {
                logger.warn("请求头userId参数格式错误:{}", sId);
            }
        }
        return null;
    }

    /**
     * 获取用户ID,当userId为空的时候抛出异常
     */
    protected Long getNotNullUserId(HttpServletRequest request) throws BusinessException {
        Long userId = getUserId(request);
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        return userId;
    }

    /**
     * 获取请求来源类型
     */
    protected RequestFrom getRequestFrom(HttpServletRequest request) throws BusinessException {
        String from = request.getHeader("from");
        if (StringUtil.isEmpty(from)) {
            throw new BusinessException("请求头错误未包含来源字段");
        }
        try {
            int iFom = Integer.parseInt(from);
            return RequestFrom.getById(iFom);
        } catch (NumberFormatException e) {
            throw new BusinessException("请求头来源字段类型错误");
        }

    }

    /**
     * 获取移动端请求头信息
     */
    protected MobileInfo getMobileInfo(HttpServletRequest request) throws BusinessException {
        String appVersion = request.getHeader("appVersion");
        String systemVersion = request.getHeader("appSystemVersion");
        String deviceId = request.getHeader("appDeviceId");
        Integer width = null;
        Integer height = null;
        int night = 0;
        try {
            width = Integer.parseInt(request.getHeader("appDeviceWidth"));
            height = Integer.parseInt(request.getHeader("appDeviceHeight"));
            if (request.getHeader("nightMode") != null) {
                night = Integer.parseInt(request.getHeader("nightMode"));
            }
        } catch (NumberFormatException e) {
            throw new BusinessException("移动端请求头不符合约定");
        }
        if (StringUtil.isEmpty(appVersion) || width == null || height == null) {
            throw new BusinessException("移动端请求头不符合约定");
        }
        return new MobileInfo(appVersion, systemVersion, deviceId, width, height, night != 0);
    }

}
```

控制层异常统一处理

```java

/**
 * 控制层异常统一处理
 */
public class RestErrorHandler {
    private static Logger logger = LoggerFactory.getLogger(RestErrorHandler.class);

    @ExceptionHandler(BindException.class)
    @ResponseBody
    public AjaxResult handleBindException(BindException exception) {
        AjaxResult result = AjaxResult.getError(ResultCode.ParamException);
        Set<ValidationError> errors = new HashSet<ValidationError>();
        for (FieldError er : exception.getFieldErrors()) {
            errors.add(new ValidationError(er.getObjectName(), er.getField(), er.getDefaultMessage()));
        }
        result.setData(errors);
        logger.warn("参数绑定错误:{}", exception.getObjectName());
        return result;
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public AjaxResult handleBusinessException(BusinessException exception) {
        AjaxResult result = AjaxResult.getError(ResultCode.BusinessException);
        result.setMessage(exception.getMessage());
        logger.warn("业务错误:{}", exception.getMessage());
        return result;
    }

    @ExceptionHandler(SystemException.class)
    @ResponseBody
    public AjaxResult handleSystemException(SystemException exception) {
        AjaxResult result = AjaxResult.getError(ResultCode.SystemException);
        result.setMessage("系统错误");
        logger.error("系统错误:{}", exception);
        return result;
    }

    @ExceptionHandler(DBException.class)
    @ResponseBody
    public AjaxResult handleDBException(DBException exception) {
        AjaxResult result = AjaxResult.getError(ResultCode.DBException);
        result.setMessage("数据库错误");
        logger.error("数据库错误:{}", exception);
        return result;
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public AjaxResult handleException(Exception exception) {
        AjaxResult result = AjaxResult.getError(ResultCode.UnknownException);
        result.setMessage("服务器错误");
        logger.error("服务器错误:{}", exception);
        return result;
    }
}

```

支付通知入口：

```java
/**
 * 支付通知入口
 * Created by Martin on 2016/7/01.
 */
@RequestMapping(value = "/open/payNotify")
public class PayNotifyController extends BaseController {

    private static Logger logger = LoggerFactory.getLogger(PayNotifyController.class);


    /**
     * 国内支付宝app通知回调
     * @param request
     * @param response
     * @throws SystemException
     * @throws BusinessException
     */
    @RequestMapping(value = "/alipayNotifyMainApp", method = RequestMethod.POST)
    public void alipayNotifyMainApp(HttpServletRequest request, HttpServletResponse response) throws SystemException, BusinessException {
        alipayNotifyService.alipayNotifyMainApp(request, response);
    }
    /**
     * 国内支付宝web通知回调
     * @param request
     * @param response
     * @throws SystemException
     * @throws BusinessException
     */
    @RequestMapping(value = "/alipayNotifyMain", method = RequestMethod.POST)
    public void alipayNotifyMain(HttpServletRequest request, HttpServletResponse response) throws SystemException, BusinessException {
        alipayNotifyService.alipayNotifyMain(request, response);
    }
    /**
     * 国际支付宝app通知回调
     * @param request
     * @param response
     * @throws SystemException
     * @throws BusinessException
     */
    @RequestMapping(value = "alipayNotifyGlobalApp", method = RequestMethod.POST)
    public void alipayNotifyGlobalApp(HttpServletRequest request, HttpServletResponse response) throws SystemException, BusinessException {
        alipayNotifyService.alipayNotifyGlobalApp(request, response);
    }
}

```

支付请求相关接口

```java
/**
 * 支付请求相关接口
 */
@Controller
@RequestMapping("/app/payRequest")
public class PayRequestController extends BaseController {

    private static Logger logger = LoggerFactory.getLogger(PayRequestController.class);
    @Autowired
    private IPayRouteService payRouteService;

    /**
     * 组装支付请求报文
     * @param payRequestParam
     * @return
     * @throws BusinessException
     * @throws SystemException
     */
    @ResponseBody
    @RequestMapping(value = "/getPayParams", method = RequestMethod.POST)
    public AjaxResult getPayParams(@RequestBody PayRequestParam payRequestParam) throws BusinessException, SystemException {
        return AjaxResult.getOK(payRouteService.getPayRetMap(payRequestParam));
    }

}
```
------------------------------------------------------------------

接下来在看看配置文件，支付相关的暂时省略，因为俺没有。

![](https://i.imgur.com/oFrTsDa.jpg)
```xml
<generatorConfiguration>

    <!-- 可以用于加载配置项或者配置文件，在整个配置文件中就可以使用${propertyKey}的方式来引用配置项
    resource：配置资源加载地址，使用resource，MBG从classpath开始找，比如com/myproject/generatorConfig.properties
    url：配置资源加载地质，使用URL的方式，比如file:///C:/myfolder/generatorConfig.properties.
    注意，两个属性只能选址一个;

    另外，如果使用了mybatis-generator-maven-plugin，那么在pom.xml中定义的properties都可以直接在generatorConfig.xml中使用
    <properties resource="" url="" />
    -->
    <properties resource="server_config.properties"/>

       <!--
       context:生成一组对象的环境
       id:必选，上下文id，用于在生成错误时提示
       defaultModelType:指定生成对象的样式
           1，conditional：类似hierarchical；
           2，flat：所有内容（主键，blob）等全部生成在一个对象中；
           3，hierarchical：主键生成一个XXKey对象(key class)，Blob等单独生成一个对象，其他简单属性在一个对象中(record class)
       targetRuntime:
           1，MyBatis3：默认的值，生成基于MyBatis3.x以上版本的内容，包括XXXBySample；
           2，MyBatis3Simple：类似MyBatis3，只是不生成XXXBySample；
       introspectedColumnImpl：类全限定名，用于扩展MBG
       -->

    <context id="Mysql" targetRuntime="MyBatis3Simple" defaultModelType="flat">
        <!-- 自动识别数据库关键字，默认false，如果设置为true，根据SqlReservedWords中定义的关键字列表；
        一般保留默认值，遇到数据库关键字（Java关键字），使用columnOverride覆盖
        -->
        <property name="autoDelimitKeywords" value="false"/>
        <!-- 生成的Java文件的编码 -->
        <property name="javaFileEncoding" value="UTF-8"/>
        <!-- 格式化java代码 -->
        <property name="javaFormatter" value="org.mybatis.generator.api.dom.DefaultJavaFormatter"/>
        <!-- 格式化XML代码 -->
        <property name="xmlFormatter" value="org.mybatis.generator.api.dom.DefaultXmlFormatter"/>

        <!-- beginningDelimiter和endingDelimiter：指明数据库的用于标记数据库对象名的符号，比如ORACLE就是双引号，MYSQL默认是`反引号； -->
        <property name="beginningDelimiter" value="`"/>
        <property name="endingDelimiter" value="`"/>


        <plugin type="com.github.abel533.generator.MapperPlugin">
            <property name="mappers" value="com.github.abel533.mapper.Mapper"/>
        </plugin>
        <!-- 必须要有的，使用这个配置链接数据库 -->
        <jdbcConnection driverClass="com.mysql.jdbc.Driver"
                        connectionURL="${master1.jdbc.url}"
                        userId="${master1.jdbc.username}"
                        password="${master1.jdbc.password}">
        </jdbcConnection>
        <!-- java模型创建器，是必须要的元素
        负责：1，key类（见context的defaultModelType）；2，java类；3，查询类
        targetPackage：生成的类要放的包，真实的包受enableSubPackages属性控制；
        targetProject：目标项目，指定一个存在的目录下，生成的内容会放到指定目录中，如果目录不存在，MBG不会自动建目录
        -->
        <javaModelGenerator targetPackage="${targetModelPackage}" targetProject="${targetJavaProject}"/>

        <!-- 生成SQL map的XML文件生成器，
       注意，在Mybatis3之后，我们可以使用mapper.xml文件+Mapper接口（或者不用mapper接口），
        或者只使用Mapper接口+Annotation，所以，如果 javaClientGenerator配置中配置了需要生成XML的话，这个元素就必须配置
       targetPackage/targetProject:同javaModelGenerator
    -->
        <sqlMapGenerator targetPackage="${targetXMLPackage}" targetProject="${targetResourcesProject}"/>

        <!-- 对于mybatis来说，即生成Mapper接口，注意，如果没有配置该元素，那么默认不会生成Mapper接口
        targetPackage/targetProject:同javaModelGenerator
        type：选择怎么生成mapper接口（在MyBatis3/MyBatis3Simple下）：
        1，ANNOTATEDMAPPER：会生成使用Mapper接口+Annotation的方式创建（SQL生成在annotation中），不会生成对应的XML；
        2，MIXEDMAPPER：使用混合配置，会生成Mapper接口，并适当添加合适的Annotation，但是XML会生成在XML中；
        3，XMLMAPPER：会生成Mapper接口，接口完全依赖XML；
        注意，如果context是MyBatis3Simple：只支持ANNOTATEDMAPPER和XMLMAPPER
         -->

        <javaClientGenerator targetPackage="${targetMapperPackage}" targetProject="${targetJavaProject}"
                             type="XMLMAPPER"/>

    </context>
</generatorConfiguration>

```

数据库配置：

```xml
#MySQL
mysql.jdbc.url=jdbc:mysql://127.0.0.1:3306/sps_db?useUnicode=true&characterEncoding=UTF-8&amp;allowMultiQueries=true
mysql.jdbc.username=root
#连接数据库的密码.
mysql.jdbc.password=root
mysql.jdbc.initialSize=10
#连接池在空闲时刻保持的最大连接数.
mysql.jdbc.minIdle=10
#连接池在同一时刻内所提供的最大活动连接数。
mysql.jdbc.maxActive=100
#当发生异常时数据库等待的最大毫秒数 (当没有可用的连接时).
mysql.jdbc.maxWait=60000
mysql.jdbc.timeBetweenEvictionRunsMillis=60000
mysql.jdbc.minEvictableIdleTimeMillis=300000
mysql.jdbc.removeAbandonedTimeout=7200
mysql.jdbc.validationQuery=SELECT 'x'
mysql.jdbc.testWhileIdle=true
mysql.jdbc.testOnBorrow=false
mysql.jdbc.testOnReturn=false
mysql.jdbc.filters=slf4j
mysql.jdbc.removeAbandoned=true
mysql.jdbc.logAbandoned=true

#Redis
redis.ip=127.0.0.1
redis.port=6379
redis.timeout=6000
#Redis-pool
redis.pool.maxTotal=10000
redis.pool.maxIdle=1000
redis.pool.testOnBorrow=true

#RabbitMQ
rabbitmq.master.ip=127.0.0.1
rabbitmq.master.port=5672
rabbitmq.master.username=guest
rabbitmq.master.password=guest

```

接着再看这几个

![](https://i.imgur.com/gPWcPl9.jpg)

applicationContext:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:context="http://www.springframework.org/schema/context"
       xmlns:aop="http://www.springframework.org/schema/aop" xmlns:tx="http://www.springframework.org/schema/tx"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
         http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
         http://www.springframework.org/schema/context
         http://www.springframework.org/schema/context/spring-context-3.0.xsd
         http://www.springframework.org/schema/tx
         http://www.springframework.org/schema/tx/spring-tx-3.0.xsd
         http://www.springframework.org/schema/aop
         http://www.springframework.org/schema/aop/spring-aop-3.0.xsd">

    <context:component-scan base-package="com.guo.sps">
        <context:exclude-filter type="annotation"
                                expression="org.springframework.stereotype.Controller" />
        <context:exclude-filter type="annotation" expression="org.springframework.web.bind.annotation.ControllerAdvice"/>
    </context:component-scan>

    <bean id="propertyConfigurer"
          class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer">
        <property name="locations">
            <list>
                <value>classpath:server_config.properties</value>
                <value>classpath:sys_config.properties</value>
            </list>
        </property>
    </bean>

    <!-- 启动AspectJ支持 -->
    <aop:aspectj-autoproxy/>

    <!-- jedis客户端连接工厂 -->
    <bean id="jedisPoolConfig" class="redis.clients.jedis.JedisPoolConfig">
        <property name="maxTotal" value="${redis.pool.maxTotal}"/>
        <property name="maxIdle"  value="${redis.pool.maxIdle}" />
        <property name="testOnBorrow"  value="${redis.pool.testOnBorrow}" />
    </bean>

    <bean id="jedisConnectionFactory" class="org.springframework.data.redis.connection.jedis.JedisConnectionFactory">
        <property name="hostName" value="${redis.ip}" />
        <property name="port" value="${redis.port}" />
        <property name="usePool" value="true" />
        <property name="poolConfig" ref="jedisPoolConfig" />
        <property name="timeout" value="${redis.timeout}"/>
    </bean>
    <!-- redisTemplate模板 -->
    <bean id="stringRedisSerializer"
          class="org.springframework.data.redis.serializer.StringRedisSerializer" />
    <bean id="jdkRedisSerializer"
          class="org.springframework.data.redis.serializer.JdkSerializationRedisSerializer" />

    <bean id="redisTemplate" class="org.springframework.data.redis.core.RedisTemplate">
        <property name="connectionFactory" ref="jedisConnectionFactory"/>
        <property name="KeySerializer" ref="stringRedisSerializer" />
        <property name="ValueSerializer" ref="stringRedisSerializer" />
        <property name="hashKeySerializer" ref="stringRedisSerializer"/>
        <property name="hashValueSerializer" ref="jdkRedisSerializer"/>
    </bean>

    <!--mysql datasource-->
    <bean id="dataSourceProxy" class="com.alibaba.druid.pool.DruidDataSource" init-method="init" destroy-method="close">
        <property name="url" value="${mysql.jdbc.url}" />
        <property name="username" value="${mysql.jdbc.username}" />
        <property name="password" value="${mysql.jdbc.password}" />
        <!-- 配置初始化大小、最小、最大 -->
        <property name="initialSize" value="${mysql.jdbc.initialSize}" />
        <property name="minIdle" value="${mysql.jdbc.minIdle}" />
        <property name="maxActive" value="${mysql.jdbc.maxActive}" />
        <!-- 配置获取连接等待超时的时间 -->
        <property name="maxWait" value="${mysql.jdbc.maxWait}" />
        <!-- 配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒 -->
        <property name="timeBetweenEvictionRunsMillis" value="${mysql.jdbc.timeBetweenEvictionRunsMillis}" />
        <!-- 配置一个连接在池中最小生存的时间，单位是毫秒 -->
        <property name="minEvictableIdleTimeMillis" value="${mysql.jdbc.minEvictableIdleTimeMillis}" />
        <property name="validationQuery" value="${mysql.jdbc.validationQuery}" />
        <property name="testWhileIdle" value="${mysql.jdbc.testWhileIdle}" />
        <property name="testOnBorrow" value="${mysql.jdbc.testOnBorrow}" />
        <property name="testOnReturn" value="${mysql.jdbc.testOnReturn}" />
        <property name="filters" value="${mysql.jdbc.filters}" />
        <!-- 打开removeAbandoned功能 -->
        <property name="removeAbandoned" value="${mysql.jdbc.removeAbandoned}" />
        <!-- 7200秒，也就是两个小时 -->
        <property name="removeAbandonedTimeout" value="${mysql.jdbc.removeAbandonedTimeout}" />
        <!-- 关闭abanded连接时输出错误日志 -->
        <property name="logAbandoned" value="${mysql.jdbc.logAbandoned}" />
    </bean>

    <bean id="systemConfig" class="com.guo.core.common.constant.SystemConfig">
        <property name="guestUsername">
            <value>${shiro.guest.username}</value>
        </property>
    </bean>

    <!--事务管理DataSourceTransactionManager -->
    <bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <property name="dataSource" ref="dataSourceProxy" />
    </bean>
    <!-- mybatis session factory -->
    <bean id="sqlSessionFactory" class="com.guo.core.util.SqlSessionFactoryBeanUtil">
        <property name="configLocation" value="classpath:mybatis_config.xml" />
        <property name="dataSource" ref="dataSourceProxy" />
        <property name="mapperLocations" value="classpath*:com/guo/sps/dao/mapper/*.xml" />
    </bean>

    <tx:advice id="txAdvice" transaction-manager="transactionManager">
        <tx:attributes>
            <!--以下方法，如save，update，insert等对数据库进行写入操作的方法，当产生Exception时进行回滚 -->
            <!--<tx:method name="*" propagation="REQUIRED" />-->
            <tx:method name="insert*" propagation="REQUIRED" />
            <tx:method name="update*" propagation="REQUIRED" />
            <tx:method name="save*" propagation="REQUIRED" />
            <tx:method name="add*" propagation="REQUIRED" />
            <tx:method name="create*" propagation="REQUIRED" />
            <tx:method name="delete*" propagation="REQUIRED" />
            <tx:method name="tx*" propagation="REQUIRED" />
        </tx:attributes>
    </tx:advice>

    <aop:config expose-proxy="true">
        <!-- 只对业务逻辑层实施事务 -->
        <aop:pointcut id="txPointcut" expression="execution(public * com.guo.sps.services.*.*.*(..))" />
        <aop:pointcut id="subPointcut" expression="execution(public * com.guo.sps.services.*.*.*.*.*(..))" />
        <aop:pointcut id="basePointcut" expression="execution(public * com.guo.core.service.*.*.*(..))" />
        <aop:advisor advice-ref="txAdvice" pointcut-ref="txPointcut" />
        <aop:advisor advice-ref="txAdvice" pointcut-ref="subPointcut" />
        <aop:advisor advice-ref="txAdvice" pointcut-ref="basePointcut" />
    </aop:config>

    <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
        <property name="basePackage" value="com.guo.sps.dao" />
    </bean>
</beans>
```
mybatis_config

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN" "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>

    <typeAliases>
        <package name="com.guo.sps.dao.domain"/>
    </typeAliases>
    <plugins>
        <plugin interceptor="com.github.pagehelper.PageHelper">
            <property name="dialect" value="mysql"/>
            <!-- 该参数默认为false -->
            <!-- 设置为true时，会将RowBounds第一个参数offset当成pageNum页码使用 -->
            <!-- 和startPage中的pageNum效果一样-->
            <property name="offsetAsPageNum" value="true"/>
            <!-- 该参数默认为false -->
            <!-- 设置为true时，使用RowBounds分页会进行count查询 -->
            <property name="rowBoundsWithCount" value="true"/>
            <!-- 设置为true时，如果pageSize=0或者RowBounds.limit = 0就会查询出全部的结果 -->
            <!-- （相当于没有执行分页查询，但是返回结果仍然是Page类型）-->
            <property name="pageSizeZero" value="true"/>
            <!-- 3.3.0版本可用 - 分页参数合理化，默认false禁用 -->
            <!-- 启用合理化时，如果pageNum<1会查询第一页，如果pageNum>pages会查询最后一页 -->
            <!-- 禁用合理化时，如果pageNum<1或pageNum>pages会返回空数据 -->
            <property name="reasonable" value="false"/>
            <!-- 3.5.0版本可用 - 为了支持startPage(Object params)方法 -->
            <!-- 增加了一个`params`参数来配置参数映射，用于从Map或ServletRequest中取值 -->
            <!-- 可以配置pageNum,pageSize,count,pageSizeZero,reasonable,不配置映射的用默认值 -->
            <property name="params"
                      value="pageNum=start;pageSize=limit;pageSizeZero=zero;reasonable=heli;count=contsql"/>
        </plugin>
        <plugin interceptor="com.github.abel533.mapperhelper.MapperInterceptor">
            <!--================================================-->
            <!--可配置参数说明(一般无需修改)-->
            <!--================================================-->
            <!--UUID生成策略-->
            <!--配置UUID生成策略需要使用OGNL表达式-->
            <!--默认值32位长度:@java.util.UUID@randomUUID().toString().replace("-", "")-->
            <!--<property name="UUID" value="@java.util.UUID@randomUUID().toString()"/>-->
            <!--主键自增回写方法,默认值MYSQL,详细说明请看文档-->
            <property name="IDENTITY" value="MYSQL"/>
            <!--序列的获取规则,使用{num}格式化参数，默认值为{0}.nextval，针对Oracle-->
            <!--可选参数一共3个，对应0,1,2,分别为SequenceName，ColumnName,PropertyName-->
            <property name="seqFormat" value="{0}.nextval"/>
            <!--主键自增回写方法执行顺序,默认AFTER,可选值为(BEFORE|AFTER)-->
            <!--<property name="ORDER" value="AFTER"/>-->
            <!--通用Mapper接口，多个通用接口用逗号隔开-->
            <!-- <property name="mappers" value="com.github.abel533.mapper.Mapper"/>-->
            <property name="mappers" value="com.guo.core.dao.IBaseMapper"/>
            <!--<property name="enableUnderline" value="false"/>-->
        </plugin>
    </plugins>

</configuration>

```

spring_mvc
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:mvc="http://www.springframework.org/schema/mvc" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
      http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
       http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context-3.0.xsd
         http://www.springframework.org/schema/mvc
		http://www.springframework.org/schema/mvc/spring-mvc-3.0.xsd ">

    <!-- 启用spring mvc 注解 -->
    <mvc:annotation-driven/>

    <!-- 设置使用注解的类所在的jar包 -->
    <context:component-scan base-package="com.guo.sps">
        <context:exclude-filter type="annotation" expression="org.springframework.stereotype.Service"/>
    </context:component-scan>

    <!--上传文件解析器-->
    <bean id="multipartResolver"
          class="org.springframework.web.multipart.commons.CommonsMultipartResolver">
        <property name="maxUploadSize" value="104857600"/>
        <property name="maxInMemorySize" value="4096"/>
        <property name="defaultEncoding" value="UTF-8"></property>
    </bean>
    <!-- 对转向页面的路径解析。prefix：前缀， suffix：后缀 -->
    <bean id="viewResolver"
          class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix" value="/jsp/"></property>
        <property name="suffix" value=".jsp"></property>
    </bean>

    <bean class="org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator"
          depends-on="lifecycleBeanPostProcessor">
        <property name="proxyTargetClass" value="true"></property>
    </bean>
    <!--安全管理器-->
    <bean class="org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor">
        <property name="securityManager" ref="securityManager"></property>
    </bean>


</beans>

```
spring_shiro

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:util="http://www.springframework.org/schema/util"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xsi:schemaLocation="
       http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
       http://www.springframework.org/schema/aop  http://www.springframework.org/schema/aop/spring-aop.xsd
       http://www.springframework.org/schema/util http://www.springframework.org/schema/util/spring-util.xsd
       ">

    <!--配置securityManager-->
    <bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
        <property name="realm" ref="statelessRealm"/>
    </bean>
    <!--statelessReealm-->
    <bean id="statelessRealm" class="com.guo.sps.realm.StatelessAuthRealm">
        <property name="cachingEnabled" value="false"/>
    </bean>
    <!--statelessFilter-->
    <bean id="statelessFilter" class="com.guo.sps.realm.filters.StatelessAuthcFilter"></bean>
    <!-- 基于Form表单的身份验证过滤器 -->
    <bean id="formAuthenticationFilter" class="org.apache.shiro.web.filter.authc.FormAuthenticationFilter">
        <property name="usernameParam" value="username"/>
        <property name="passwordParam" value="password"/>
        <property name="rememberMeParam" value="rememberMe"/>
        <property name="loginUrl" value="/jsp/login"/>
    </bean>

    <!--配置shiroFilter-->
    <bean id="shiroFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
        <property name="securityManager" ref="securityManager"/>
        <property name="filters">
            <util:map>
                <entry key="stateless" value-ref="statelessFilter"/>
            </util:map>
        </property>
        <!--过滤链定义-->
        <property name="filterChainDefinitions">
            <value>
                /=anon
                /index.jsp=anon
                /app/**=stateless
            </value>
        </property>
    </bean>
    <!-- Shiro生命周期处理器-->
    <bean id="lifecycleBeanPostProcessor" class="org.apache.shiro.spring.LifecycleBeanPostProcessor"/>

</beans>

```
Spring-rabbitmq
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns:mvc="http://www.springframework.org/schema/mvc"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:rabbit="http://www.springframework.org/schema/rabbit"
       xmlns="http://www.springframework.org/schema/beans"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
      http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
      http://www.springframework.org/schema/mvc
	  http://www.springframework.org/schema/mvc/spring-mvc-3.0.xsd http://www.springframework.org/schema/rabbit http://www.springframework.org/schema/rabbit/spring-rabbit.xsd">

    <mvc:annotation-driven/>

    <rabbit:connection-factory id="connectionFactory" host="${rabbitmq.master.ip}" port="${rabbitmq.master.port}"
                               username="${rabbitmq.master.username}" password="${rabbitmq.master.password}"/>
    <!-- 通用 template声明 -->
    <rabbit:template id="amqpTemplate" connection-factory="connectionFactory"
                     exchange="order_topic_exchange" message-converter="gsonConverter"/>

    <rabbit:admin connection-factory="connectionFactory"/>
    <!-- queue 队列声明-->
    <rabbit:queue name="payRequestQueue" durable="true"/>
    <rabbit:queue name="payRequestCallbackQueue" durable="true"/>
    <rabbit:queue name="payNotifyQueue" durable="true"/>
    <!-- topic-exchange，作为主题模式使用。
   匹配routingkey的模式，这里匹配两个queue
   queue_topic准备匹配key为zhu.q1
   queue_topic2准备匹配key为zhu.q2
    -->
    <rabbit:topic-exchange name="pay_topic_exchange">
        <rabbit:bindings>
            <rabbit:binding queue="payRequestQueue" pattern="payRequest.#"/>
            <rabbit:binding queue="payRequestCallbackQueue" pattern="payRequestCallback.#"/>
            <rabbit:binding queue="payNotifyQueue" pattern="payNotify.#"/>
        </rabbit:bindings>
    </rabbit:topic-exchange>
    <!--监听器-->
    <rabbit:listener-container connection-factory="connectionFactory" acknowledge="manual" concurrency="10">
        <rabbit:listener queues="payRequestQueue" ref="payRequestQueueListener"/>
    </rabbit:listener-container>
    <!--MQ监听支付请求入口消息-->
    <bean id="payRequestQueueListener" class="com.guo.sps.mq.PayRequestQueueListener"/>
    <!--MQ MSG序列化JSON转换器-->
    <bean id="gsonConverter" class="com.guo.core.mq.Gson2JsonMessageConverter"/>

</beans>

```

接下来看最重要的web.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns:web="http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd" xmlns="http://java.sun.com/xml/ns/javaee"
         xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd"
         version="3.0">
    <display-name>Pay Map Service</display-name>
    <context-param>
        <param-name>webAppRootKey</param-name>
        <param-value>PayMap</param-value>
    </context-param>
    <!-- 加载配置文件 -->
    <context-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>classpath:applicationContext.xml,classpath:spring_shiro.xml,classpath:spring_rabbitmq.xml
        </param-value>
    </context-param>

    <!-- LogBack Request MDC Filter -->
    <filter>
        <filter-name>MDCInsertingServletFilter</filter-name>
        <filter-class>
            ch.qos.logback.classic.helpers.MDCInsertingServletFilter
        </filter-class>
    </filter>
    <filter-mapping>
        <filter-name>MDCInsertingServletFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

    <!-- shiroFilter配置-->
    <filter>
        <filter-name>shiroFilter</filter-name>
        <filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>shiroFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

    <!-- 解决post乱码 -->
    <filter>
        <description>字符集过滤器</description>
        <filter-name>encodingFilter</filter-name>
        <filter-class>org.springframework.web.filter.CharacterEncodingFilter</filter-class>
        <init-param>
            <description>字符集编码</description>
            <param-name>encoding</param-name>
            <param-value>UTF-8</param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>encodingFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

    <filter>
        <filter-name>DruidWebStatFilter</filter-name>
        <filter-class>com.alibaba.druid.support.http.WebStatFilter</filter-class>
        <init-param>
            <param-name>exclusions</param-name>
            <param-value>*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*</param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>DruidWebStatFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

    <filter>
        <filter-name>loggingFilter</filter-name>
        <filter-class>com.guo.core.web.system.filters.LoggingFilter</filter-class>
    </filter>

    <filter-mapping>
        <filter-name>loggingFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

    <listener>
        <description>spring监听器</description>
        <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
    </listener>
    <listener>
        <description>Introspector缓存清除监听器</description>
        <listener-class>org.springframework.web.util.IntrospectorCleanupListener</listener-class>
    </listener>
    <listener>
        <description>request监听器</description>
        <listener-class>org.springframework.web.context.request.RequestContextListener</listener-class>
    </listener>
    <listener>                                 c
        <description>系统初始化监听器</description>
        <listener-class>com.guo.core.web.system.listener.InitListener</listener-class>
    </listener>
    <servlet-mapping>
        <servlet-name>default</servlet-name>
        <url-pattern>/static/*</url-pattern>
    </servlet-mapping>
    <servlet>
        <description>spring mvc servlet</description>
        <servlet-name>springMvc</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <init-param>
            <description>spring mvc 配置文件</description>
            <param-name>contextConfigLocation</param-name>
            <param-value>classpath:spring_mvc.xml</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>springMvc</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>
    <servlet>
        <servlet-name>DruidStatView</servlet-name>
        <servlet-class>com.alibaba.druid.support.http.StatViewServlet</servlet-class>
    </servlet>
    <servlet-mapping>
        <servlet-name>DruidStatView</servlet-name>
        <url-pattern>/druid/*</url-pattern>
    </servlet-mapping>

    <filter>
        <filter-name>HiddenHttpMethodFilter</filter-name>
        <filter-class>org.springframework.web.filter.HiddenHttpMethodFilter</filter-class>
    </filter>

    <filter-mapping>
        <filter-name>HiddenHttpMethodFilter</filter-name>
        <servlet-name>springMvc</servlet-name>
    </filter-mapping>

    <!-- session超时设置30分钟 -->
    <session-config>
        <session-timeout>30</session-timeout>
    </session-config>

    <welcome-file-list>
        <welcome-file>index.jsp</welcome-file>
    </welcome-file-list>
</web-app>

```
这只是简单的过来一遍，对大概的流程有个印象。需要配置文件的时候能找到。如果你有幸能看到这段话，那就好好加油吧。gogogo。
