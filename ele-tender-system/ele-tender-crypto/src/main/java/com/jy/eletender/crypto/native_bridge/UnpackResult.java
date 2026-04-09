package com.jy.eletender.crypto.native_bridge;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UnpackResult {

    private List<byte[]> encryptedSegments = new ArrayList<>();

    private byte[] encryptedProjectInfoRsa;

    private String formatVersion;

    private byte[] rawFileBytes;
}
