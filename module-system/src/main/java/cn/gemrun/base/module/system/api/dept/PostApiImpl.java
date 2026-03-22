package cn.gemrun.base.module.system.api.dept;

import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.system.api.dept.dto.PostRespDTO;
import cn.gemrun.base.module.system.dal.dataobject.dept.PostDO;
import cn.gemrun.base.module.system.service.dept.PostService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

/**
 * 岗位 API 实现类
 *
 * @author 芋道源码
 */
@Service
public class PostApiImpl implements PostApi {

    @Resource
    private PostService postService;

    @Override
    public void validPostList(Collection<Long> ids) {
        postService.validatePostList(ids);
    }

    @Override
    public List<PostRespDTO> getPostList(Collection<Long> ids) {
        List<PostDO> list = postService.getPostList(ids);
        return BeanUtils.toBean(list, PostRespDTO.class);
    }


    @Override
    public PostRespDTO getPost(Long id) {
        PostDO post = postService.getPost(id);
        return post != null ? BeanUtils.toBean(post, PostRespDTO.class) : null;
    }

}
