package com.jy.eletender.crypto.service;

import com.jy.eletender.common.interaction.dto.EnvelopeJoinRequest;
import com.jy.eletender.common.interaction.dto.EnvelopeJoinResponse;

public interface IEnvelopeService {

    /**
     * 将 CA 解密后的各层 hashKeyD 按 order 拼接，还原 bidderPwdStr。
     */
    EnvelopeJoinResponse join(EnvelopeJoinRequest request);
}
