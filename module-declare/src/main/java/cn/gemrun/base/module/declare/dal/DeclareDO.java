package cn.gemrun.base.module.declare.dal;

import java.io.Serializable;

/**
 * 数据对象：Declare
 */
public class DeclareDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private Integer status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}

