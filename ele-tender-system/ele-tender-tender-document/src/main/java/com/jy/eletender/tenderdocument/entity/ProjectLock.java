package com.jy.eletender.tenderdocument.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eletender.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("td_project_lock")
public class ProjectLock extends BaseEntity {

    private String projectId;
    private String ownerUserId;
    private String ownerUserName;
    private String ownerEnterpriseId;
    private String ownerEnterpriseName;
    private String ownerEnterpriseCode;
    private String ownerAppKey;
    private Date lockTime;
}
