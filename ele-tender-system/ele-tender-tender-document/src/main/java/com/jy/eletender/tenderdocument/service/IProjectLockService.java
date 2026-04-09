package com.jy.eletender.tenderdocument.service;

import com.jy.eletender.tenderdocument.entity.ProjectLock;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;

public interface IProjectLockService {

    ProjectLock verifyOrCreateLock(String projectId, TenderDocumentUserContext userContext);
}
