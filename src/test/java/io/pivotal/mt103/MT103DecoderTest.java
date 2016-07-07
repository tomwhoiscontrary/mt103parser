package io.pivotal.mt103;

import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;

public class MT103DecoderTest {

    private String rawInstruction = "{1:F01CATEGB21XXXX0000000000}{2:I103RBOSGB2LXGPLN2020}{4:\r\n" +
            ":20:160216000141234\r\n" +
            ":23B:CRED\r\n" +
            ":32A:160228GBP512345678,25\r\n" +
            ":33B:GBP512345678,25\r\n" +
            ":50K:/16571053811234\r\n" +
            "AAAANTAGE LTD\r\n" +
            "AAAABROOK ROAD\r\n" +
            "AAAARD\r\n" +
            "TN17 4LZ\r\n" +
            ":59:/30929900362940\r\n" +
            "AAAA Kumar\r\n" +
            "GRENOBLE\r\n" +
            ":70:/RFB/Expenses\r\n" +
            ":71A:OUR\r\n" +
            ":72:/FDP/\r\n" +
            "-}";

    @Test
    public void parsesInstruction() throws Exception {
        Map<String, Object> instruction = MT103Decoder.parse(rawInstruction);

        assertThat(instruction, hasEntry("bank", "CATEGB21XXXX"));

        assertThat(instruction, hasEntry("sponsorBank", "RBOSGB2LXGPL"));

        assertThat(instruction, hasEntry("bankUrn", "160216000141234"));

        assertThat(instruction, hasEntry("amount", 512345678.25));

        assertThat(instruction, hasEntry("currency", "GBP"));

        assertThat(instruction, hasEntry("dateForValue", "2016-02-28"));

        assertThat(instruction, hasKey("remitter"));
        @SuppressWarnings("unchecked")
        Map<String, Object> remitter = (Map<String, Object>) instruction.get("remitter");
        assertThat(remitter, hasEntry("name", "AAAANTAGE LTD"));
        assertThat(remitter, hasEntry("address", "AAAABROOK ROADAAAARDTN17 4LZ"));
        assertThat(remitter, hasEntry("bankAccount", "53811234"));
        assertThat(remitter, hasEntry("sortCode", "165710"));

        assertThat(instruction, hasKey("beneficiary"));
        @SuppressWarnings("unchecked")
        Map<String, Object> beneficiary = (Map<String, Object>) instruction.get("beneficiary");
        assertThat(beneficiary, hasEntry("sortCode", "309299"));
        assertThat(beneficiary, hasEntry("bankAccount", "00362940"));
        assertThat(beneficiary, hasEntry("name", "AAAA Kumar"));
        assertThat(beneficiary, hasEntry("address", "GRENOBLE"));
        assertThat(beneficiary, hasEntry("beneficiaryReference", "/RFB/Expenses"));

        assertThat(instruction, hasEntry("charges", "OUR"));

        assertThat(instruction, hasEntry("transactionType", "/FDP/"));
    }

}
