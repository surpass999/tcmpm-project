package cn.gemrun.base.module.bpm.api;

import cn.gemrun.base.module.bpm.api.dto.BpmActionRespDTO;
import cn.gemrun.base.module.bpm.enums.BpmAction;
import cn.gemrun.base.module.bpm.enums.BpmActionDef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BPM 按钮定义 Api 实现
 *
 * @author Gemini
 */
@Slf4j
@Service
public class BpmActionApiImpl implements BpmActionApi {

    @Override
    public List<BpmActionRespDTO> getAllActions() {
        return Arrays.stream(BpmActionDef.values())
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BpmActionRespDTO getActionByKey(String key) {
        BpmActionDef actionDef = BpmActionDef.fromKey(key);
        return actionDef != null ? convertToDTO(actionDef) : null;
    }

    /**
     * 将枚举转换为 DTO
     */
    private BpmActionRespDTO convertToDTO(BpmActionDef actionDef) {
        BpmActionRespDTO dto = new BpmActionRespDTO();
        dto.setKey(actionDef.getKey());
        dto.setLabel(actionDef.getLabel());
        dto.setBizStatus(actionDef.getBizStatus());
        dto.setBizStatusLabel(actionDef.getBizStatusLabel());
        BpmAction bpmAction = actionDef.getBpmAction();
        dto.setBpmAction(bpmAction != null ? bpmAction.getValue() : null);
        return dto;
    }

}
