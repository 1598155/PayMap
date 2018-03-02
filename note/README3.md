### 3、sps包～dao.domain,mapper

为了方便就只展示SQL语句，如果有需要的可以到GitHub上找。这里只是记录

首先定义三个接口

```java
public interface MemberMapper extends IBaseMapper<Member> {
    public Member findByUsername(String username);
}

public interface PayMapMapper extends IBaseMapper<PayMap> {
}

 public interface SysConfigMapper extends IBaseMapper<SysConfig> {
 
     public Integer updateSysConfig(@Param("key") String key, @Param("sysValue") String sysValue);
 
     public List<SysConfig> getAll();
 
 }

```

用户信息表:
```sql
CREATE TABLE `sps_member` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(200) DEFAULT '''' COMMENT '用户名',
  `userCode` varchar(200) DEFAULT '''' COMMENT '用户唯一推广编码',
  `userLabel` int(11) DEFAULT '1' COMMENT '用户标签',
  `password` varchar(1000) DEFAULT '''' COMMENT '用户密码',
  `salt` varchar(100) DEFAULT '''' COMMENT '密码加密的盐',
  `status` int(11) DEFAULT '1' COMMENT '用户状态：1 正常 /0 禁用',
  `grade` int(11) DEFAULT '1' COMMENT '会员级别',
  `realName` varchar(100) DEFAULT '''' COMMENT '真实姓名',
  `nickName` varchar(100) DEFAULT '''' COMMENT '昵称',
  `showImage` varchar(500) DEFAULT '''' COMMENT '用户头像',
  `birthday` bigint(20) DEFAULT '0' COMMENT '生日',
  `birthdayStr` varchar(100) DEFAULT '0' COMMENT '生日字符串',
  `sex` int(11) DEFAULT '0' COMMENT '性别:0 女 / 1 男',
  `email` varchar(100) DEFAULT '''' COMMENT '邮箱',
  `emailStatus` int(11) NOT NULL DEFAULT '0' COMMENT '邮箱校验状态：0 未校验 / 1 校验通过',
  `cellphone` varchar(50) DEFAULT '''' COMMENT '手机号',
  `phoneStatus` int(11) NOT NULL DEFAULT '0' COMMENT '手机号校验状态：0 未校验 / 1 校验通过',
  `telephone` varchar(50) DEFAULT '''' COMMENT '电话号码',
  `province` varchar(50) DEFAULT '''' COMMENT '所在省份',
  `city` varchar(100) DEFAULT '''' COMMENT '所在城市',
  `country` varchar(200) DEFAULT '''' COMMENT '所在区县',
  `address` varchar(1000) DEFAULT '''' COMMENT '具体详细地址',
  `otherInfo` text COMMENT '更多其他信息',
  `createTime` bigint(20) DEFAULT '0' COMMENT '添加日期:Unix时间戳',
  `modifyTime` bigint(20) DEFAULT '0' COMMENT '最后更新日期：Unix时间戳',
  `idCardNo` varchar(50) DEFAULT '''' COMMENT '证件号码',
  `referee` bigint(20) DEFAULT '0' COMMENT '介绍人ID',
  `amount` decimal(18,2) DEFAULT '0.00' COMMENT '现金账户余额',
  `points` int(11) DEFAULT '0' COMMENT '剩余积分',
  `orderCount` int(11) DEFAULT '0' COMMENT '交易次数',
  `odooId` int(20) DEFAULT '0' COMMENT '对应odoo的id',
  `orderAmount` decimal(10,2) DEFAULT '0.00' COMMENT '所购订单总金额',
  `gradeAmount` decimal(10,2) DEFAULT '0.00' COMMENT '当前等级下花费的总金额',
  `gradeChangeTime` bigint(20) DEFAULT '0' COMMENT '会员等级改变时间',
  `memberMark` int(11) DEFAULT '0' COMMENT '会员标记,0-普通会员,1-特殊关注会员',
  `memberLabel` varchar(100) DEFAULT '0' COMMENT '会员标签',
  `labelReason` varchar(100) DEFAULT '0' COMMENT '标记原因',
  `phoneUpdateTime` bigint(20) DEFAULT '0' COMMENT '手机更改时间',
  `emailUpdateTime` bigint(20) DEFAULT '0' COMMENT '邮箱更改时间',
  `del` bit(1) DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `cellphone` (`cellphone`),
  KEY `email` (`email`),
  KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户信息表';
```
订单表：
```sql
CREATE TABLE sps_pay_map(
  id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  orderId BIGINT(20) NOT NULL COMMENT '所属订单ID',
  orderCode VARCHAR(200) NOT NULL COMMENT '所属订单code',
  tempPayCode VARCHAR(200) NOT NULL COMMENT '临时支付号ID',
  platform VARCHAR(200) DEFAULT NULL COMMENT '所属平台',
  payParams VARCHAR(3500) DEFAULT NULL COMMENT '支付所生成的请求信息',
  retMsg VARCHAR(800) DEFAULT NULL COMMENT '支付后回调时的详细信息',
  retMsg2 VARCHAR(800) DEFAULT NULL COMMENT '备用消息',
  isPaid CHAR(1) DEFAULT NULL COMMENT '是否已支付0否；1是',
  remark VARCHAR(200) DEFAULT NULL,
  swiftNumber VARCHAR(60) DEFAULT NULL COMMENT '交易流水号',
  payPurpose VARCHAR(30) DEFAULT NULL COMMENT '交易意图：支付订单，补齐差价',
  idBelongsTo VARCHAR(60) DEFAULT NULL COMMENT 'orderId 所在的表 ',
  cashAmt DECIMAL(18, 2) DEFAULT NULL COMMENT '本次支付所使用的现金账户金额，sps_order表中有该字段，所以普通订单支付时此字段为空',
  remark2 VARCHAR(200) DEFAULT NULL,
  notify_time BIGINT(20) DEFAULT NULL COMMENT '通知回调时间',
  requestBiz VARCHAR(200) DEFAULT NULL COMMENT '支付请求业务来源',
  PRIMARY KEY (id),
  INDEX orderCode (orderCode),
  INDEX orderId (orderId)
)
ENGINE = INNODB
CHARACTER SET utf8
COLLATE utf8_general_ci
COMMENT = '订单表';
```
支付表：
```sql
CREATE TABLE sps_payment(
  id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  payName VARCHAR(100) DEFAULT NULL COMMENT '支付名称',
  payKey VARCHAR(100) DEFAULT NULL COMMENT '支付关键字',
  payData VARCHAR(500) DEFAULT NULL COMMENT '支付加密数据',
  payURI VARCHAR(500) DEFAULT NULL COMMENT '回调URI',
  payLogoPath VARCHAR(500) DEFAULT NULL COMMENT '支付对应logo地址',
  pcPayLogoPath VARCHAR(500) DEFAULT NULL COMMENT '支付对应logo地址',
  shiftPayLogoPath VARCHAR(500) DEFAULT NULL COMMENT '支付对应logo地址',
  isPlat INT(3) DEFAULT NULL COMMENT '是否支付平台1：支付平台；0：银行',
  isWebOn INT(3) DEFAULT NULL COMMENT '是否web可用 ： 0：否；1是',
  isAppOn INT(3) DEFAULT NULL COMMENT '是否app可用',
  PRIMARY KEY (id)
)
ENGINE = INNODB
CHARACTER SET utf8
COLLATE utf8_general_ci
COMMENT = '支付表';
```
系统参数表：
```sql
CREATE TABLE sps_sys_config(
  sysKey VARCHAR(200) NOT NULL COMMENT '参数名',
  sysValue TEXT DEFAULT NULL COMMENT '参数值',
  modifyTime BIGINT(20) DEFAULT 0 COMMENT '修改日期',
  accountId BIGINT(20) DEFAULT 0 COMMENT '修改人',
  isCache TINYINT(1) DEFAULT 0 COMMENT '是否缓存，true为是，false不是，默认false',
  description VARCHAR(200) DEFAULT '' COMMENT '参数描述',
  operator CHAR(4) DEFAULT '1111' COMMENT '字典对应的操作',
  PRIMARY KEY (sysKey)
)
ENGINE = INNODB
CHARACTER SET utf8
COLLATE utf8_general_ci
COMMENT = '系统参数表';
```
 MemberMapper文件：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.guo.sps.dao.MemberMapper">
    <resultMap id="BaseResultMap" type="com.guo.sps.dao.domain.Member">
        <id column="id" jdbcType="BIGINT" property="id"/>
        <result column="username" jdbcType="VARCHAR" property="username"/>
        <result column="userCode" jdbcType="VARCHAR" property="userCode"/>
        <result column="userLabel" jdbcType="INTEGER" property="userLabel"/>
        <result column="password" jdbcType="VARCHAR" property="password"/>
        <result column="salt" jdbcType="VARCHAR" property="salt"/>
        <result column="status" jdbcType="INTEGER" property="status"/>
        <result column="grade" jdbcType="INTEGER" property="grade"/>
        <result column="realName" jdbcType="VARCHAR" property="realName"/>
        <result column="nickName" jdbcType="VARCHAR" property="nickName"/>
        <result column="showImage" jdbcType="VARCHAR" property="showImage"/>
        <result column="birthday" jdbcType="BIGINT" property="birthday"/>
        <result column="birthdayStr" jdbcType="VARCHAR" property="birthdayStr"/>
        <result column="sex" jdbcType="INTEGER" property="sex"/>
        <result column="email" jdbcType="VARCHAR" property="email"/>
        <result column="emailStatus" jdbcType="INTEGER" property="emailStatus"/>
        <result column="cellphone" jdbcType="VARCHAR" property="cellphone"/>
        <result column="phoneStatus" jdbcType="INTEGER" property="phoneStatus"/>
        <result column="telephone" jdbcType="VARCHAR" property="telephone"/>
        <result column="province" jdbcType="VARCHAR" property="province"/>
        <result column="city" jdbcType="VARCHAR" property="city"/>
        <result column="country" jdbcType="VARCHAR" property="country"/>
        <result column="address" jdbcType="VARCHAR" property="address"/>
        <result column="createTime" jdbcType="BIGINT" property="createTime"/>
        <result column="modifyTime" jdbcType="BIGINT" property="modifyTime"/>
        <result column="otherInfo" jdbcType="LONGVARCHAR" property="otherInfo"/>
        <result column="IdCardNo" jdbcType="VARCHAR" property="IdCardNo"/>
        <result column="referee" jdbcType="BIGINT" property="referee"/>
        <result column="amount" jdbcType="DECIMAL" property="amount"/>
        <result column="points" jdbcType="INTEGER" property="points"/>
        <result column="orderCount" jdbcType="INTEGER" property="orderCount"/>
        <result column="odooId" jdbcType="INTEGER" property="odooId"/>
        <result column="orderAmount" jdbcType="DECIMAL" property="orderAmount"/>
        <result column="memberMark" jdbcType="INTEGER" property="memberMark"/>
        <result column="memberLabel" jdbcType="VARCHAR" property="memberLabel"/>
        <result column="labelReason" jdbcType="VARCHAR" property="labelReason"/>
        <result column="del" jdbcType="BIT" property="del"/>
        <result column="gradeAmount" jdbcType="DECIMAL" property="gradeAmount"/>
        <result column="gradeChangeTime" jdbcType="BIGINT" property="gradeChangeTime"/>
        <result column="phoneUpdateTime" jdbcType="BIGINT" property="phoneUpdateTime"/>
        <result column="emailUpdateTime" jdbcType="BIGINT" property="emailUpdateTime"/>
    </resultMap>

    <select id="findByUsername" parameterType="String" resultType="com.guo.sps.dao.domain.Member">
        SELECT * FROM sps_member where username=#{username} and del=0 GROUP BY createTime ASC limit 0,1
    </select>

</mapper>


```
PayMapMapper文件：
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd" >
<mapper namespace="com.guo.sps.dao.PayMapMapper">
    <resultMap id="BaseResultMap" type="com.guo.sps.dao.domain.PayMap">
        <id column="id" property="id" jdbcType="BIGINT"/>
        <result column="orderId" property="orderId" jdbcType="BIGINT"/>
        <result column="orderCode" property="orderCode" jdbcType="VARCHAR"/>
        <result column="tempPayCode" property="tempPayCode" jdbcType="VARCHAR"/>
        <result column="platform" property="platform" jdbcType="VARCHAR"/>
        <result column="payParams" property="payParams" jdbcType="VARCHAR"/>
        <result column="retMsg" property="retMsg" jdbcType="VARCHAR"/>
        <result column="retMsg2" property="retMsg2" jdbcType="VARCHAR"/>
        <result column="isPaid" property="isPaid" jdbcType="CHAR"/>
        <result column="remark" property="remark" jdbcType="VARCHAR"/>
        <result column="remark2" property="remark2" jdbcType="VARCHAR"/>
        <result column="swiftNumber" property="swiftNumber" jdbcType="VARCHAR"/>
        <result column="payPurpose" property="payPurpose" jdbcType="VARCHAR"/>
        <result column="idBelongsTo" property="idBelongsTo" jdbcType="VARCHAR"/>
        <result column="cashAmt" property="cashAmt" jdbcType="DECIMAL"/>
        <result column="notify_time" property="notifyTime" jdbcType="BIGINT"/>
        <result column="requestBiz" property="requestBiz" jdbcType="VARCHAR"/>
    </resultMap>
</mapper>


```
PaymentMapper:
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd" >
<mapper namespace="com.guo.sps.dao.PaymentMapper">
    <resultMap id="BaseResultMap" type="com.guo.sps.dao.domain.Payment">
        <id column="id" property="id" jdbcType="BIGINT"/>
        <result column="payName" property="payName" jdbcType="VARCHAR"/>
        <result column="payKey" property="payKey" jdbcType="VARCHAR"/>
        <result column="payData" property="payData" jdbcType="VARCHAR"/>
        <result column="payURI" property="payURI" jdbcType="VARCHAR"/>
        <result column="payLogoPath" property="payLogoPath" jdbcType="VARCHAR"/>
        <result column="payLogoPath" property="pcPayLogoPath" jdbcType="VARCHAR"/>
        <result column="payLogoPath" property="shiftPayLogoPath" jdbcType="VARCHAR"/>
        <result column="isPlat" property="isPlat" jdbcType="INTEGER"/>
        <result column="isWebOn" property="isWebOn" jdbcType="INTEGER"/>
        <result column="isAppOn" property="isAppOn" jdbcType="INTEGER"/>
    </resultMap>
</mapper>

```
SysConfigMapper
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd" >
<mapper namespace="com.guo.sps.dao.SysConfigMapper">
    <resultMap id="BaseResultMap" type="com.guo.sps.dao.domain.SysConfig">
        <id column="sysKey" property="sysKey" jdbcType="VARCHAR"/>
        <result column="modifyTime" property="modifyTime" jdbcType="BIGINT"/>
        <result column="accountId" property="accountId" jdbcType="BIGINT"/>
        <result column="sysValue" property="sysValue" jdbcType="LONGVARCHAR"/>
        <result column="description" property="description" jdbcType="LONGVARCHAR"/>
        <result column="operator" property="operator" jdbcType="VARCHAR"/>
        <result column="isCache" property="isCache" jdbcType="BOOLEAN"/>
    </resultMap>

    <update id="updateSysConfig">
        UPDATE sps_sys_config SET sysValue=#{sysValue},modifyTime=unix_timestamp(now()) where sysKey=#{key}
    </update>

    <select id="getAll" resultType="com.guo.sps.dao.domain.SysConfig">
        SELECT * FROM  sps_sys_config WHERE  isCache=1
    </select>

</mapper>
```
