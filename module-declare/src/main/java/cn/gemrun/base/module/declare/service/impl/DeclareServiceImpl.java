package cn.gemrun.base.module.declare.service.impl;

import cn.gemrun.base.module.declare.controller.admin.vo.DeclareCreateReqVO;
import cn.gemrun.base.module.declare.dal.DeclareDO;
import cn.gemrun.base.module.declare.dal.mapper.DeclareMapper;
import cn.gemrun.base.module.declare.service.DeclareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Declare service 实现（示例）。
 */
@Service
public class DeclareServiceImpl implements DeclareService {

    @Autowired
    private DeclareMapper declareMapper;

    @Override
    public Long createDeclare(DeclareCreateReqVO createReqVO) {
        DeclareDO record = new DeclareDO();
        record.setTitle(createReqVO.getTitle());
        record.setStatus(1);
        declareMapper.insert(record);
        return record.getId();
    }

    @Override
    public DeclareDO getDeclare(Long id) {
        return declareMapper.selectById(id);
    }
}

