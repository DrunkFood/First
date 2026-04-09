package com.jy.eletender.crypto.service.impl;

import com.jy.eletender.common.interaction.dto.EnvelopeJoinRequest;
import com.jy.eletender.common.interaction.dto.EnvelopeJoinResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvelopeServiceImplTest {

    private final EnvelopeServiceImpl service = new EnvelopeServiceImpl();

    private static final byte[] BIDDER_PWD = Base64.getDecoder().decode(
            "SfHrOh8qoWJHlh0rVeRm2czmgltfVD75GvEGZmrKj+8=");

    @Test
    void shouldJoinTwoLayersFromHashKeyList() {
        // split: [0..16) + [16..32)
        byte[] seg1 = Arrays.copyOfRange(BIDDER_PWD, 0, 16);
        byte[] seg2 = Arrays.copyOfRange(BIDDER_PWD, 16, 32);

        EnvelopeJoinRequest.HashKeyItem item1 = new EnvelopeJoinRequest.HashKeyItem();
        item1.setCaId("ca-001");
        item1.setHashKeyE("encrypted-stub");
        item1.setHashKeyD(Base64.getEncoder().encodeToString(seg1));
        item1.setOrder(1);

        EnvelopeJoinRequest.HashKeyItem item2 = new EnvelopeJoinRequest.HashKeyItem();
        item2.setCaId("ca-002");
        item2.setHashKeyE("encrypted-stub");
        item2.setHashKeyD(Base64.getEncoder().encodeToString(seg1));
        item2.setOrder(1); // 同 order 冗余，应被跳过

        EnvelopeJoinRequest.HashKeyItem item3 = new EnvelopeJoinRequest.HashKeyItem();
        item3.setCaId("ca-001");
        item3.setHashKeyE("encrypted-stub");
        item3.setHashKeyD(Base64.getEncoder().encodeToString(seg2));
        item3.setOrder(2);

        EnvelopeJoinRequest request = new EnvelopeJoinRequest();
        request.setHashKeyList(Arrays.asList(item1, item2, item3));

        EnvelopeJoinResponse response = service.join(request);

        byte[] restored = Base64.getDecoder().decode(response.getBidderPwdStr());
        assertThat(restored).isEqualTo(BIDDER_PWD);
    }

    @Test
    void shouldJoinSingleLayer() {
        EnvelopeJoinRequest.HashKeyItem item = new EnvelopeJoinRequest.HashKeyItem();
        item.setCaId("ca-001");
        item.setHashKeyE("encrypted-stub");
        item.setHashKeyD(Base64.getEncoder().encodeToString(BIDDER_PWD));
        item.setOrder(1);

        EnvelopeJoinRequest request = new EnvelopeJoinRequest();
        request.setHashKeyList(List.of(item));

        EnvelopeJoinResponse response = service.join(request);

        byte[] restored = Base64.getDecoder().decode(response.getBidderPwdStr());
        assertThat(restored).isEqualTo(BIDDER_PWD);
    }

    @Test
    void shouldRejectWhenNoValidHashKeyD() {  // method name kept for readability
        EnvelopeJoinRequest.HashKeyItem item = new EnvelopeJoinRequest.HashKeyItem();
        item.setCaId("ca-001");
        item.setHashKeyE("encrypted-stub");
        item.setHashKeyD(null);
        item.setOrder(1);

        EnvelopeJoinRequest request = new EnvelopeJoinRequest();
        request.setHashKeyList(List.of(item));

        assertThatThrownBy(() -> service.join(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hashKeyD");
    }

    @Test
    void shouldRejectWhenTotalLengthNot32() {
        EnvelopeJoinRequest.HashKeyItem item = new EnvelopeJoinRequest.HashKeyItem();
        item.setCaId("ca-001");
        item.setHashKeyE("encrypted-stub");
        item.setHashKeyD(Base64.getEncoder().encodeToString(new byte[16]));
        item.setOrder(1);

        EnvelopeJoinRequest request = new EnvelopeJoinRequest();
        request.setHashKeyList(List.of(item));

        assertThatThrownBy(() -> service.join(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32");
    }
}
