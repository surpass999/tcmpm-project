package cn.gemrun.base.module.declare.controller.admin.hospital.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 医院导入 Response VO
 *
 * @author Gemini
 */
@Schema(description = "管理后台 - 医院导入 Response VO")
@Data
@Builder
public class HospitalImportRespVO {

    @Schema(description = "创建成功的医院名称数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> createHospitalNames;

    @Schema(description = "更新成功的医院名称数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> updateHospitalNames;

    @Schema(description = "导入失败的医院集合，key 为医院名称，value 为失败原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> failureHospitals;

}
