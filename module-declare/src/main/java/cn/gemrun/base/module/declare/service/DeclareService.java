package cn.gemrun.base.module.declare.service;

import cn.gemrun.base.module.declare.controller.admin.vo.DeclareCreateReqVO;
import cn.gemrun.base.module.declare.dal.DeclareDO;

/**
 * Declare service 接口
 */
public interface DeclareService {

    Long createDeclare(DeclareCreateReqVO createReqVO);

    DeclareDO getDeclare(Long id);
}

