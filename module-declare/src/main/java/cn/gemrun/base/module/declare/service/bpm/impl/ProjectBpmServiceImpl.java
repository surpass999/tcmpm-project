package cn.gemrun.base.module.declare.service.bpm.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 项目流程 BPM 服务
 *
 * @author Gemini
 */
@Service
@Slf4j
public class ProjectBpmServiceImpl {

    public void updateProjectProcessInstance(Long id, String processInstanceId, String projectStatus) {
        log.info("[updateProjectProcessInstance] id={}, processInstanceId={}", id, processInstanceId);
    }
}
