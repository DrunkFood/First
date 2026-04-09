package com.jy.eletender.common.interaction;

import com.jy.eletender.common.interaction.constant.InteractionApiPaths;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InteractionApiPathsTest {

    @Test
    void shouldExposeFixedControllerPrefix() {
        assertEquals("/api/eleTender/interaction", InteractionApiPaths.BASE);
        assertEquals("/api/eleTender/interaction/identity/current", InteractionApiPaths.IDENTITY_CURRENT);
        assertEquals("/api/eleTender/interaction/ca-keys/query", InteractionApiPaths.CA_KEYS_INFO);
        assertEquals("/api/eleTender/interaction/callbacks/bid-document-result", InteractionApiPaths.CALLBACK_BID_DOCUMENT_RESULT);
        assertEquals("/api/eleTender/interaction/callbacks/bid-decrypt-result", InteractionApiPaths.CALLBACK_BID_DECRYPT_RESULT);
    }
}
