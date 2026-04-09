package com.jy.eletender.crypto.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BidderPwdSegmentUtilTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void shouldPassAllSharedVectors() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/native-vectors/bidder-pwd-segment-vectors.json")) {
            JsonNode root = MAPPER.readTree(is);
            byte[] bidderPwdBytes = Base64.getDecoder().decode(root.get("bidderPwdBase64").asText());

            for (JsonNode vector : root.get("vectors")) {
                int encryptOrder = vector.get("encryptOrder").asInt();
                List<byte[]> segments = BidderPwdSegmentUtil.split(bidderPwdBytes, encryptOrder);

                JsonNode expectedArr = vector.get("expectedSegmentsBase64");
                assertThat(segments).hasSize(expectedArr.size());

                for (int i = 0; i < segments.size(); i++) {
                    byte[] expected = Base64.getDecoder().decode(expectedArr.get(i).asText());
                    assertThat(segments.get(i))
                            .as("encryptOrder=%d, segment[%d]", encryptOrder, i)
                            .isEqualTo(expected);
                }
            }
        }
    }

    @Test
    void splitThenJoinShouldRestoreOriginal() {
        byte[] bidderPwd = Base64.getDecoder().decode("SfHrOh8qoWJHlh0rVeRm2czmgltfVD75GvEGZmrKj+8=");

        for (int order = 1; order <= 32; order++) {
            List<byte[]> segments = BidderPwdSegmentUtil.split(bidderPwd, order);

            Map<Integer, byte[]> orderMap = new LinkedHashMap<>();
            for (int i = 0; i < segments.size(); i++) {
                orderMap.put(i + 1, segments.get(i));
            }
            byte[] restored = BidderPwdSegmentUtil.join(orderMap);
            assertThat(restored)
                    .as("encryptOrder=%d roundtrip", order)
                    .isEqualTo(bidderPwd);
        }
    }

    @Test
    void joinShouldWorkWithNonConsecutiveOrders() {
        byte[] bidderPwd = Base64.getDecoder().decode("SfHrOh8qoWJHlh0rVeRm2czmgltfVD75GvEGZmrKj+8=");
        List<byte[]> segments = BidderPwdSegmentUtil.split(bidderPwd, 2);

        Map<Integer, byte[]> orderMap = new LinkedHashMap<>();
        orderMap.put(5, segments.get(0));
        orderMap.put(10, segments.get(1));

        byte[] restored = BidderPwdSegmentUtil.join(orderMap);
        assertThat(restored).isEqualTo(bidderPwd);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 33, 100})
    void splitShouldRejectInvalidEncryptOrder(int invalidOrder) {
        byte[] bidderPwd = new byte[32];
        assertThatThrownBy(() -> BidderPwdSegmentUtil.split(bidderPwd, invalidOrder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("encryptOrder");
    }

    @Test
    void splitShouldRejectInvalidLength() {
        assertThatThrownBy(() -> BidderPwdSegmentUtil.split(new byte[16], 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32");

        assertThatThrownBy(() -> BidderPwdSegmentUtil.split(null, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void joinShouldRejectWrongTotalLength() {
        Map<Integer, byte[]> orderMap = new LinkedHashMap<>();
        orderMap.put(1, new byte[16]);
        orderMap.put(2, new byte[8]);

        assertThatThrownBy(() -> BidderPwdSegmentUtil.join(orderMap))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32");
    }

    @Test
    void joinShouldRejectEmptyMap() {
        assertThatThrownBy(() -> BidderPwdSegmentUtil.join(Collections.emptyMap()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
