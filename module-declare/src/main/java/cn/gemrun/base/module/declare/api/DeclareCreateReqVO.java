package cn.gemrun.base.module.declare.api;

import javax.validation.constraints.NotBlank;

/**
 * 创建请求 DTO
 */
public class DeclareCreateReqVO {

    @NotBlank
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

