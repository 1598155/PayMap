package com.guo.sps.dao;

import com.guo.core.dao.IBaseMapper;
import com.guo.sps.dao.domain.SysConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created by guo on 3/2/2018.
 */
public interface SysConfigMapper extends IBaseMapper<SysConfig> {
    public Integer updateSysConfig(@Param("key") String key, @Param("sysValue") String sysValue);

    public List<SysConfig> getAll();
}
