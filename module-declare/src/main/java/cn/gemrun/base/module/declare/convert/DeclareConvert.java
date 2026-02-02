package cn.gemrun.base.module.declare.convert;

import cn.gemrun.base.module.declare.api.DeclareCreateReqVO;
import cn.gemrun.base.module.declare.dal.DeclareDO;

/**
 * 转换类示例
 */
public class DeclareConvert {

    public static DeclareDO toDeclareDO(DeclareCreateReqVO reqVO) {
        if (reqVO == null) {
            return null;
        }
        DeclareDO d = new DeclareDO();
        d.setTitle(reqVO.getTitle());
        d.setStatus(1);
        return d;
    }
}

