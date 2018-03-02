### 1、核心包～common.dao,service,web,mq.
(1)、我们先从dao开始吧，这里也可以是web。熟悉的来了，IBaseMapper<T> 还是定义基础接口，但与中不同的是这个用泛型<T>修饰，为什么呢？先看一张图。

<img src="https://i.imgur.com/ofLp6zS.jpg" width = "300" height = "200"/>

图片+代码可以说明一切问题。反射+泛型。很重。要是还不懂，再用文字描述
- 1、泛型类，是在实例化类的时候指明泛型的具体类型；支持创建可以按类型进行参数化的类
- 2、泛型方法，是在调用方法的时候指明具体的类型。`public List<T> findAll() {}`
- 3、泛型接口，JDK，Spring中大量运用泛型。

总结来说泛型可以提高Java程序的类型安全，所有的类型转换都是自动和隐式的。为优化，性能带来收益。

IBaseDao接口：
```java
/**
 * 持久层通用接口
 */
public interface IBaseDao<T> {
   void save(T entity);
	 void delete(T entity);
	 void update(T entity);
	 T findById(Serializable id);
	 List<T> findAll();
}
```
BaseDaoImpl<T>实现类：
```java
/**
 * 持久层通用实现
 */
public class BaseDaoImpl<T> extends HibernateDaoSupport implements IBaseDao<T> {
	//代表的是某个实体的类型
	private Class<T> entityClass;

	@Resource//根据类型注入spring工厂中的会话工厂对象sessionFactory
	public void setMySessionFactory(SessionFactory sessionFactory){
		super.setSessionFactory(sessionFactory);
	}

	//在父类（BaseDaoImpl）的构造方法中动态获得entityClass
	public BaseDaoImpl() {
		ParameterizedType superclass = (ParameterizedType) this.getClass().getGenericSuperclass();
		//获得父类上声明的泛型数组
		Type[] actualTypeArguments = superclass.getActualTypeArguments();
		entityClass = (Class<T>) actualTypeArguments[0];
	}
	public void save(T entity) {
		this.getHibernateTemplate().save(entity);
	}

	public T findById(Serializable id) {
		return this.getHibernateTemplate().get(entityClass, id);
	}

	public List<T> findAll() {
		String hql = "FROM " + entityClass.getSimpleName();
		return (List<T>) this.getHibernateTemplate().find(hql);
	}
}
```
(2)、接下来，再看本项目中定义IBaseMapper<T>

- @SelectProvider注解用于生成查询用的sql语句，有别于@Select注解，@SelectProvide指定一个Class及其方法，并且通过调用Class上的这个方法来获得sql语句。
- @ResultMap注解用于从查询结果集RecordSet中取数据然后拼装实体bean。
- @SelectProvide方法，如果参数使用了@Param注解，那么参数在Map中以@Param的值为key

在这里使用了注解的形式，但是也可以在XMl方法配置。
```java
public interface IBaseMapper<T> {
    @SelectProvider(type = MapperProvider.class, method = "dynamicSQL")
    T selectOne(T record);
    @SelectProvider(type = MapperProvider.class, method = "dynamicSQL")
    T selectByPrimaryKey(Object key);

    @InsertProvider(type = MapperProvider.class, method = "dynamicSQL")
    int insert(T record);
    ...........................忽略了几个.......................
    @DeleteProvider(type = MapperProvider.class, method = "dynamicSQL")
    int delete(T record);
    @DeleteProvider(type = MapperProvider.class, method = "dynamicSQL")
    int deleteByPrimaryKey(Object key);
    @DeleteProvider(type = MapperProvider.class, method = "dynamicSQL")
    int deleteByExample(Object example);
    @UpdateProvider(type = MapperProvider.class, method = "dynamicSQL")
    int updateByExample(@Param("record") T record, @Param("example") Object example);

    List<T> getAllByPage(RowBounds rowBounds);       //这个是用于分页的。

}
```
------
(3)、中间插一个RabbitMQ MSG序列化JSON转换器，主要作用是客户端和服务端需要传输Json格式的数据包，所以需要进行转换。这个工具包也是必备的之一。友情提示，安装MQ时，一定要以系统管理员运行CMD。

RabbitMQ已经实现了Jackson的消息转换（Jackson2JsonMessageConverter），由于考虑到效率，如下使用Gson实现消息转换。

如下消息的转换类的接口MessageConverter，Jackson2JsonMessageConverter的父类AbstractJsonMessageConverter针对json转换的基类。

![](https://i.imgur.com/hvpdUT5.jpg)

我们实现Gson2JsonMessageConverter转换类也继承AbstractJsonMessageConverter。

为了节约地方，代码放[Gist](https://gist.github.com/guoxiaoxu/d37cf16b65d8724ebb316fd51ec04cee)了，需要的时候直接去找。

```java
/**
 * MQ MSG序列化JSON转换器
 */
public class Gson2JsonMessageConverter extends AbstractJsonMessageConverter {

    private static Logger logger = LoggerFactory.getLogger(Gson2JsonMessageConverter.class);
    private static ClassMapper classMapper = new DefaultClassMapper();
    private static Gson gson = new Gson();

    @Override
    protected Message createMessage(Object object, MessageProperties messageProperties) {
        byte[] bytes = null;
        try {
            String jsonString = gson.toJson(object);
            jsonString.getBytes(getDefaultCharset());
        }
        catch (IOException e) {
            new MessageConversionException("Failed to convert Mesage context",e);
        }
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setContentEncoding(getDefaultCharset());
        if (bytes != null) {
            messageProperties.setContentLength(bytes.length);
        }
        classMapper.fromClass(object.getClass(),messageProperties);
        return new Message(bytes,messageProperties);
    }
    @Override
    public ClassMapper getClassMapper() {
        return new DefaultClassMapper();
    }
}
```
(4)、接下来就是定义基础IBaseService以及实现类
```java
public interface IBaseService<T> {
  /**
   * 根据主键查询指定实体
   */
    T getId(Object id) ;
    List<T> getByEntiry(T entity);
    PageInfo<T> getByPage(RowBounds rowBounds);
    int save(T entity);
    int update(T entity);
    int delete(Object id);
    int saveSelective(T entity) throws DBException;
    int updateSelective(T entity);
}
```
IBaseService:[全部代码在这GIst](https://gist.github.com/guoxiaoxu/4b5aab5c15e4bf20e195efaf5cde133f)

```java
/**
 * Created by guo on 3/2/2018.
 */
public abstract class BaseService<T> implements IBaseService<T> {
    private static Logger logger = LoggerFactory.getLogger(BaseService.class);
    @Resource
    protected RabbitTemplate amqpTemplate;
    @Autowired
    protected RedisTemplate redisTemplate;

    public abstract IBaseMapper<T> getBaseMapper();

    /**
     * 根据主键查询指定实体
     * @param id
     * @return
     */
    @Override
    public T getId(Object id) {
        return this.getBaseMapper().selectByPrimaryKey(id);
    }
    /**
     * 获取分页数据
     */
    @Override
    public PageInfo<T> getByPage(RowBounds rowBounds) {
        List<T> list = this.getBaseMapper().getAllByPage(rowBounds);
        return new PageInfo<T>(list);
    }
    /**
     * 保存对象，保存所有属性
     */
    @Override
    public int save(T entity) {
        return this.getBaseMapper().insert(entity);
    }
    /**
     * 删除指定数据
     */
    @Override
    public int delete(Object id) {
        return this.getBaseMapper().deleteByPrimaryKey(id);
    }
    /**
     * 更新对象，值更新对象中不为Null的属性，主键不能为NULL
     */
    @Override
    public int updateSelective(T entity) {
        return this.getBaseMapper().updateByPrimaryKeySelective(entity);
    }
}
```

----
(5)、接下来就是web包中的内容，涉及监听器和过滤器。


```java
/**
 * 系统初始化监听器,在系统启动时运行,进行一些初始化工作
 */
public class InitListener implements javax.servlet.ServletContextListener {

    private static Logger logger = LoggerFactory.getLogger(InitListener.class);
    public static ApplicationContext context;

    public void contextDestroyed(ServletContextEvent arg0) {
    }

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        context = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContextEvent.getServletContext());
        //加载银联upop配置文件
        SDKConfig.getConfig().loadPropertiesFromSrc();
        String proPath = servletContextEvent.getServletContext().getRealPath("/");
        SDKConfig config = SDKConfig.getConfig();
        config.setSignCertDir(proPath + config.getSignCertDir());
        config.setSignCertPath(proPath + config.getSignCertPath());
        config.setValidateCertDir(proPath + config.getValidateCertDir());
        //缓存初始化忽略
    }
}
```

过滤器：

我们先看关于日志的，真心看不懂，后面有一大堆。[代码地址](https://gist.github.com/guoxiaoxu/c9a985eb47937a7e6ca88e5793a91337)
```java
/**
 * request response log记录过滤器
 */
public class LoggingFilter extends OncePerRequestFilter {

    protected static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);
    private static final String REQUEST_PREFIX = "Request: ";
    private static final String RESPONSE_PREFIX = "Response: ";
    private AtomicLong id = new AtomicLong(1);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, final FilterChain filterChain) throws ServletException, IOException {
        if (logger.isDebugEnabled()) {
            long requestId = id.incrementAndGet();
            request = new RequestWrapper(requestId, request);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (logger.isDebugEnabled()) {
                logRequest(request);
            }
        }
    }
    //...
}
```

还有一个请求包装类，和响应类。。[代码地址](https://gist.github.com/guoxiaoxu/c9a985eb47937a7e6ca88e5793a91337)

```java
public class RequestWrapper extends HttpServletRequestWrapper {
    private final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    private long id;

    public RequestWrapper(Long requestId, HttpServletRequest request) {
        super(request);
        this.id = requestId;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new ServletInputStream() {
            private TeeInputStream tee = new TeeInputStream(RequestWrapper.super.getInputStream(), bos);

            @Override
            public int read() throws IOException {
                return tee.read();
            }
        };
    }

    public byte[] toByteArray() {
        return bos.toByteArray();
    }
}

---------------------------------------------------------------------------------
public class ResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    private PrintWriter writer = new PrintWriter(bos);
    private long id;

    public ResponseWrapper(Long requestId, HttpServletResponse response) {
        super(response);
        this.id = requestId;
    }
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStream() {
            private TeeOutputStream tee = new TeeOutputStream(ResponseWrapper.super.getOutputStream(), bos);

            @Override
            public void write(int b) throws IOException {
                tee.write(b);
            }
        };
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return new TeePrintWriter(super.getWriter(), writer);
    }

    public byte[] toByteArray() {
        return bos.toByteArray();
    }
}
```

这一块只是得补补，用到的时候再看，还有一个`TeePrintWriter`

核心包的东东算是完了，重点是在IBaseMapper、IBaseService的设计。这里用到了泛型，还有Mybatis3.X新特性，基于注解的。其实完全可以用XML配置文件。


gogogo 正式进入业务逻辑。
