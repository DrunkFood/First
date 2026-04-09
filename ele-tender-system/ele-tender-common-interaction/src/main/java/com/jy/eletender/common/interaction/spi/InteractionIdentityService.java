package com.jy.eletender.common.interaction.spi;

import com.jy.eletender.common.interaction.dto.IdentityContext;
import com.jy.eletender.common.interaction.dto.IdentityQueryResponse;

/**
 * 身份查询SPI
 */
public interface InteractionIdentityService {

    IdentityQueryResponse queryCurrentIdentity(IdentityContext context);
}
