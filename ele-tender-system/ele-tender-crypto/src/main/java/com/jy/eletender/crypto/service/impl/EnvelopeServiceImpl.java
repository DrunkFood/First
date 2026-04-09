package com.jy.eletender.crypto.service.impl;

import com.jy.eletender.common.interaction.dto.EnvelopeJoinRequest;
import com.jy.eletender.common.interaction.dto.EnvelopeJoinResponse;
import com.jy.eletender.crypto.service.IEnvelopeService;
import com.jy.eletender.crypto.support.BidderPwdSegmentUtil;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EnvelopeServiceImpl implements IEnvelopeService {

    @Override
    public EnvelopeJoinResponse join(EnvelopeJoinRequest request) {
        // 按 order 分组，每组取第一条有 hashKeyD 的记录
        Map<Integer, byte[]> orderToSegment = new TreeMap<>();

        for (EnvelopeJoinRequest.HashKeyItem item : request.getHashKeyList()) {
            int order = item.getOrder();
            if (orderToSegment.containsKey(order)) {
                continue;
            }
            String hashKeyD = item.getHashKeyD();
            if (hashKeyD != null && !hashKeyD.isEmpty()) {
                orderToSegment.put(order, Base64.getDecoder().decode(hashKeyD));
            }
        }

        if (orderToSegment.isEmpty()) {
            throw new IllegalArgumentException("hashKeyList 中无任何有效的 hashKeyD");
        }

        byte[] bidderPwdBytes = BidderPwdSegmentUtil.join(orderToSegment);

        EnvelopeJoinResponse response = new EnvelopeJoinResponse();
        response.setBidderPwdStr(Base64.getEncoder().encodeToString(bidderPwdBytes));
        return response;
    }
}
