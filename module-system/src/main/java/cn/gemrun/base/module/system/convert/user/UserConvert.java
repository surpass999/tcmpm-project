package cn.gemrun.base.module.system.convert.user;

import cn.gemrun.base.framework.common.util.collection.CollectionUtils;
import cn.gemrun.base.framework.common.util.collection.MapUtils;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.framework.ip.core.Area;
import cn.gemrun.base.framework.ip.core.utils.AreaUtils;
import cn.gemrun.base.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.gemrun.base.module.system.controller.admin.dept.vo.post.PostSimpleRespVO;
import cn.gemrun.base.module.system.controller.admin.permission.vo.role.RoleSimpleRespVO;
import cn.gemrun.base.module.system.controller.admin.user.vo.profile.UserProfileRespVO;
import cn.gemrun.base.module.system.controller.admin.user.vo.user.UserRespVO;
import cn.gemrun.base.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import cn.gemrun.base.module.system.dal.dataobject.dept.DeptDO;
import cn.gemrun.base.module.system.dal.dataobject.dept.PostDO;
import cn.gemrun.base.module.system.dal.dataobject.permission.RoleDO;
import cn.gemrun.base.module.system.dal.dataobject.user.AdminUserDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserConvert {

    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

    default List<UserRespVO> convertList(List<AdminUserDO> list, Map<Long, DeptDO> deptMap) {
        return CollectionUtils.convertList(list, user -> convert(user, deptMap.get(user.getDeptId())));
    }

    default UserRespVO convert(AdminUserDO user, DeptDO dept) {
        UserRespVO userVO = BeanUtils.toBean(user, UserRespVO.class);
        if (dept != null) {
            userVO.setDeptName(dept.getName());
        }
        // 填充地区名称
        fillAreaNames(userVO, user);
        return userVO;
    }

    /**
     * 填充省市名称
     */
    default void fillAreaNames(UserRespVO userVO, AdminUserDO user) {
        if (user.getProvinceId() != null) {
            Area province = AreaUtils.getArea(user.getProvinceId());
            if (province != null) {
                userVO.setProvinceName(province.getName());
            }
        }
        if (user.getCityId() != null) {
            Area city = AreaUtils.getArea(user.getCityId());
            if (city != null) {
                userVO.setCityName(city.getName());
            }
        }
    }

    default List<UserSimpleRespVO> convertSimpleList(List<AdminUserDO> list, Map<Long, DeptDO> deptMap) {
        return CollectionUtils.convertList(list, user -> {
            UserSimpleRespVO userVO = BeanUtils.toBean(user, UserSimpleRespVO.class);
            MapUtils.findAndThen(deptMap, user.getDeptId(), dept -> userVO.setDeptName(dept.getName()));
            return userVO;
        });
    }

    default UserProfileRespVO convert(AdminUserDO user, List<RoleDO> userRoles,
                                      DeptDO dept, List<PostDO> posts) {
        UserProfileRespVO userVO = BeanUtils.toBean(user, UserProfileRespVO.class);
        userVO.setRoles(BeanUtils.toBean(userRoles, RoleSimpleRespVO.class));
        userVO.setDept(BeanUtils.toBean(dept, DeptSimpleRespVO.class));
        userVO.setPosts(BeanUtils.toBean(posts, PostSimpleRespVO.class));
        return userVO;
    }

}
