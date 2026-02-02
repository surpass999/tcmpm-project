package cn.gemrun.base.module.declare.service;

import cn.gemrun.base.module.declare.dal.DeclareDO;
import cn.gemrun.base.module.declare.api.DeclareCreateReqVO;

/**
 * Declare service 接口
 */
public interface DeclareService {

    Long createDeclare(DeclareCreateReqVO createReqVO);

    DeclareDO getDeclare(Long id);
}

