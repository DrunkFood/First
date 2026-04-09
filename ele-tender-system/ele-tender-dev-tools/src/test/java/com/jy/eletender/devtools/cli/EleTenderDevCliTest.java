package com.jy.eletender.devtools.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EleTenderDevCliTest {

    @Test
    void run_noArgs_returns1() {
        assertEquals(1, EleTenderDevCli.run(new String[]{}));
    }

    @Test
    void run_nullArgs_returns1() {
        assertEquals(1, EleTenderDevCli.run(null));
    }

    @Test
    void run_unknownCommand_returns1() {
        assertEquals(1, EleTenderDevCli.run(new String[]{"unknown-command"}));
    }

    @Test
    void run_generateTenderNoArgs_succeeds() {
        // generate-tender 无参时使用默认输出目录，应能成功（生成文件在 ./dev-output）
        int exitCode = EleTenderDevCli.run(new String[]{"generate-tender"});
        assertEquals(0, exitCode);
    }

    @Test
    void run_generateBidNoArgs_succeeds() {
        int exitCode = EleTenderDevCli.run(new String[]{"generate-bid"});
        assertEquals(0, exitCode);
    }

    @Test
    void run_decryptTenderMissingRequired_returns1() {
        assertEquals(1, EleTenderDevCli.run(new String[]{"decrypt-tender"}));
    }

    @Test
    void run_decryptBidMissingRequired_returns1() {
        assertEquals(1, EleTenderDevCli.run(new String[]{"decrypt-bid"}));
    }
}
