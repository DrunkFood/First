package com.jy.eletender.tenderdocument.support.generation;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * 生成纯系统内部使用的招标文件唯一编码。
 */
@Component
public class TenderDocumentUniqueCodeGenerator {

    public String generate() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }
}
