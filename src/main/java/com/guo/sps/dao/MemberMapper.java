package com.guo.sps.dao;

import com.guo.core.dao.IBaseMapper;
import com.guo.sps.dao.domain.Member;

/**
 * Created by guo on 3/2/2018.
 */
public interface MemberMapper extends IBaseMapper<Member> {
    public Member findByUsername(String username);
}
