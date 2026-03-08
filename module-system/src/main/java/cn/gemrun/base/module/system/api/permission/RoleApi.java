package cn.gemrun.base.module.system.api.permission;

import java.util.Collection;
import java.util.Set;

/**
 * 角色 API 接口
 *
 * @author 芋道源码
 */
public interface RoleApi {

    /**
     * 校验角色们是否有效。如下情况，视为无效：
     * 1. 角色编号不存在
     * 2. 角色被禁用
     *
     * @param ids 角色编号数组
     */
    void validRoleList(Collection<Long> ids);

    /**
     * 根据角色编码集合获取角色 ID 集合
     *
     * @param codes 角色编码集合，如 ["PROVINCE", "NATION"]
     * @return 角色 ID 集合
     */
    Set<Long> getRoleIdsByCodes(Collection<String> codes);

}
