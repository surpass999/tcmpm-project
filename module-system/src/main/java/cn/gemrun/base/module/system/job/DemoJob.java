package cn.gemrun.base.module.system.job;

import cn.gemrun.base.framework.quartz.core.handler.JobHandler;
import cn.gemrun.base.module.system.dal.dataobject.user.AdminUserDO;
import cn.gemrun.base.module.system.dal.mysql.user.AdminUserMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class DemoJob implements JobHandler {

    @Resource
    private AdminUserMapper adminUserMapper;

    @Override
    public String execute(String param) {
        System.out.println("当前租户：单租户模式");
        List<AdminUserDO> users = adminUserMapper.selectList();
        return "用户数量：" + users.size();
    }

}
