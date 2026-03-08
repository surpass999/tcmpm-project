package cn.gemrun.base.module.declare.api;

import cn.gemrun.base.module.declare.api.dto.ExpertRespDTO;
import cn.gemrun.base.module.declare.dal.dataobject.expert.ExpertDO;
import cn.gemrun.base.module.declare.service.expert.ExpertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 专家信息 Api 实现
 *
 * @author Gemini
 */
@Slf4j
@Service
public class ExpertApiImpl implements ExpertApi {

    @Resource
    private ExpertService expertService;

    @Override
    public ExpertRespDTO getExpertByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        ExpertDO expert = expertService.getExpertByUserId(userId);
        if (expert == null) {
            return null;
        }
        return convertToDTO(expert);
    }

    @Override
    public boolean isExpert(Long userId) {
        return getExpertByUserId(userId) != null;
    }

    @Override
    public List<Long> getExpertUserIds() {
        return expertService.getExpertUserIds();
    }

    /**
     * 将 DO 转换为 DTO
     */
    private ExpertRespDTO convertToDTO(ExpertDO expert) {
        ExpertRespDTO dto = new ExpertRespDTO();
        dto.setId(expert.getId());
        dto.setUserId(expert.getUserId());
        dto.setExpertName(expert.getExpertName());
        dto.setExpertType(expert.getExpertType());
        dto.setStatus(expert.getStatus());
        return dto;
    }

}
