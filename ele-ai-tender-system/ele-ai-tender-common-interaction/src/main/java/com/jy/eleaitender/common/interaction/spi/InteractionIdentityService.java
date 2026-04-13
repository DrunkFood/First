package com.jy.eleaitender.common.interaction.spi;

import com.jy.eleaitender.common.interaction.dto.IdentityContext;
import com.jy.eleaitender.common.interaction.dto.IdentityQueryResponse;

/**
 * 身份查询SPI
 */
public interface InteractionIdentityService {

    IdentityQueryResponse queryCurrentIdentity(IdentityContext context);
}
