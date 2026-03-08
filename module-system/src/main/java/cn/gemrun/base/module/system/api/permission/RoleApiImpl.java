package cn.gemrun.base.module.system.api.permission;

import cn.gemrun.base.module.system.dal.dataobject.permission.RoleDO;
import cn.gemrun.base.module.system.service.permission.RoleService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色 API 实现类
 *
 * @author 芋道源码
 */
@Service
public class RoleApiImpl implements RoleApi {

    @Resource
    private RoleService roleService;

    @Override
    public void validRoleList(Collection<Long> ids) {
        roleService.validateRoleList(ids);
    }

    @Override
    public Set<Long> getRoleIdsByCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return roleService.getRoleList().stream()
                .filter(role -> codes.contains(role.getCode()))
                .map(RoleDO::getId)
                .collect(Collectors.toSet());
    }
}
