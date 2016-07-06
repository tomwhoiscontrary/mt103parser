package io.pivotal.mt103;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ISO15022ParserTest {

    /**
     * Taken from 'Parsing a SWIFT Message', John Davies, IONA Technologies, May 2007
     * <p>
     * http://www.immagic.com/eLibrary/ARCHIVES/GENERAL/IONA_IE/I070510D.pdf
     */
    String johnDaviesExample = "{1:F01MIDLGB22AXXX0548034693}{2:I103BKTRUS33XBRDN3}{3:{108:MT103}}{4:\r\n" +
            ":20:8861198-0706\r\n" +
            ":23B:CRED\r\n" +
            ":32A:000612USD5443,99\r\n" +
            ":33B:USD5443,99\r\n" +
            ":50K:GIAN ANGELO IMPORTS\r\n" +
            "NAPLES\r\n" +
            ":52A:BCITITMM500\r\n" +
            ":53A:BCITUS33\r\n" +
            ":54A:IRVTUS3N\r\n" +
            ":57A:BNPAFRPPGRE\r\n" +
            ":59:/20041010050500001M02606\r\n" +
            "KILLY S.A.\r\n" +
            "GRENOBLE\r\n" +
            ":70:/RFB/INVOICE 559661\r\n" +
            ":71A:SHA\r\n" +
            "-}";

    /**
     * Taken from 'Anatomy of a SWIFT message', Petr Stodulka, 10 January 2015
     * <p>
     * http://coding.pstodulka.com/2015/01/10/anatomy-of-a-swift-message/
     */
    String petrStodulkaExample = "{1:F01TESTBIC12XXX0360105154}{2:O5641057130214TESTBIC34XXX26264938281302141757N}{3:{103:CAD}{108:2RDRQDHM3WO}}{4:\r\n" +
            ":16R:GENL\r\n" +
            ":20C::CORP//1234567890123456\r\n" +
            ":20C::SEME//9876543210987654\r\n" +
            ":23G:NEWM\r\n" +
            ":22F::CAEV//INTR\r\n" +
            ":22F::CAMV//MAND\r\n" +
            ":98C::PREP//20220202105733\r\n" +
            ":25D::PROC//ENTL\r\n" +
            ":16S:GENL\r\n" +
            ":16R:USECU\r\n" +
            ":35B:ISIN CH0101010101\r\n" +
            "/XS/232323232\r\n" +
            "FINANCIAL INSTRUMENT ACME\r\n" +
            ":16R:FIA\r\n" +
            ":22F::MICO//A007\r\n" +
            ":16S:FIA\r\n" +
            ":16R:ACCTINFO\r\n" +
            ":97A::SAFE//99999\r\n" +
            ":94F::SAFE//NCSD/TESTBIC0ABC\r\n" +
            ":93B::ELIG//FAMT/500000,\r\n" +
            ":93B::SETT//FAMT/500000,\r\n" +
            ":16S:ACCTINFO\r\n" +
            ":16S:USECU\r\n" +
            ":16R:CADETL\r\n" +
            ":98A::ANOU//20220113\r\n" +
            ":98A::RDTE//20220113\r\n" +
            ":69A::INPE//20220214/20220214\r\n" +
            ":99A::DAAC//360\r\n" +
            ":92K::INTR//UKWN\r\n" +
            ":22F::ADDB//CAPA\r\n" +
            ":16S:CADETL\r\n" +
            ":16R:CAOPTN\r\n" +
            ":13A::CAON//001\r\n" +
            ":22F::CAOP//CASH\r\n" +
            ":11A::OPTN//USD\r\n" +
            ":17B::DFLT//Y\r\n" +
            ":16R:CASHMOVE\r\n" +
            ":22H::CRDB//CRED\r\n" +
            ":22H::CONT//ACTU\r\n" +
            ":97A::CASH//89898\r\n" +
            ":19B::ENTL//USD3333,\r\n" +
            ":19B::TXFR//USD3333,\r\n" +
            ":19B::NETT//USD3333,\r\n" +
            ":98A::PAYD//20220214\r\n" +
            ":98A::VALU//20220214\r\n" +
            ":98A::EARL//20220214\r\n" +
            ":92A::INTP//0,75\r\n" +
            ":92A::TAXR//0,\r\n" +
            ":16S:CASHMOVE\r\n" +
            ":16S:CAOPTN\r\n" +
            ":16R:ADDINFO\r\n" +
            ":70E::ADTX//PAYMENT UPON RECEIPT OF FUNDS - \r\n" +
            "TIMELY PAYMENT EXPECTED\r\n" +
            ":16S:ADDINFO\r\n" +
            "-}{5:{CHK:C77F8E009597}}";

    @Test
    public void parsesEmptyMessage() throws Exception {
        assertThat(ISO15022Parser.parse(""), equalTo(blocks()));
    }

    @Test
    public void parsesEmptyBlock() throws Exception {
        assertThat(ISO15022Parser.parse("{}"), equalTo(blocks(block())));
    }

    @Test
    public void parsesMultipleEmptyBlocks() throws Exception {
        assertThat(ISO15022Parser.parse("{}{}{}"), equalTo(blocks(block(), block(), block())));
    }

    @Test
    public void parsesTagAndValueInBlock() throws Exception {
        assertThat(ISO15022Parser.parse("{tag:value}"), equalTo(blocks(block("tag", "value"))));
    }

    @Test
    public void acceptsEmptyTag() throws Exception {
        assertThat(ISO15022Parser.parse("{:value}"), equalTo(blocks(block("", "value"))));
    }

    @Test
    public void acceptsEmptyValue() throws Exception {
        assertThat(ISO15022Parser.parse("{tag:}"), equalTo(blocks(block("tag", ""))));
    }

    @Test
    public void parsesSubBlocks() throws Exception {
        assertThat(ISO15022Parser.parse("{tag:{foo:bar}{baz:qux}}"), equalTo(blocks(block("tag", blocks(block("foo", "bar"), block("baz", "qux"))))));
    }

    @Test
    public void parsesEmptyMessageBlock() throws Exception {
        assertThat(ISO15022Parser.parse("{tag:\r\n-}"), equalTo(blocks(block("tag", block()))));
    }

    @Test
    public void parsesMessageBlock() throws Exception {
        assertThat(ISO15022Parser.parse("{tag:\r\n:foo:bar\r\n:baz:qux\r\n-}"), equalTo(blocks(block("tag", block(field("foo", "bar"), field("baz", "qux"))))));
    }

    @Test
    public void parsesMultilineField() throws Exception {
        assertThat(ISO15022Parser.parse("{tag:\r\n:foo:bar\r\nbaz\r\nqux\r\n-}"), equalTo(blocks(block("tag", block(field("foo", "bar\nbaz\nqux"))))));
    }

    @Test
    public void parsesJohnDaviesExample() throws Exception {
        assertThat(ISO15022Parser.parse(johnDaviesExample), equalTo(blocks(
                block("1", "F01MIDLGB22AXXX0548034693"),
                block("2", "I103BKTRUS33XBRDN3"),
                block("3", blocks(block("108", "MT103"))),
                block("4", block(
                        field("20", "8861198-0706"),
                        field("23B", "CRED"),
                        field("32A", "000612USD5443,99"),
                        field("33B", "USD5443,99"),
                        field("50K", "GIAN ANGELO IMPORTS\nNAPLES"),
                        field("52A", "BCITITMM500"),
                        field("53A", "BCITUS33"),
                        field("54A", "IRVTUS3N"),
                        field("57A", "BNPAFRPPGRE"),
                        field("59", "/20041010050500001M02606\nKILLY S.A.\nGRENOBLE"),
                        field("70", "/RFB/INVOICE 559661"),
                        field("71A", "SHA")
                ))
        )));
    }

    @Test
    public void parsesPetrStodulkaExample() throws Exception {
        assertThat(ISO15022Parser.parse(petrStodulkaExample), equalTo(blocks(
                block("1", "F01TESTBIC12XXX0360105154"),
                block("2", "O5641057130214TESTBIC34XXX26264938281302141757N"),
                block("3", blocks(block("103", "CAD"), block("108", "2RDRQDHM3WO"))),
                block("4", block(
                        field("20C", ":CORP//1234567890123456"),
                        field("20C", ":SEME//9876543210987654"),
                        field("23G", "NEWM"),
                        field("22F", ":CAEV//INTR"),
                        field("22F", ":CAMV//MAND"),
                        field("98C", ":PREP//20220202105733"),
                        field("25D", ":PROC//ENTL"),
                        field("16R", "GENL"),
                        field("20C", ":CORP//1234567890123456"),
                        field("20C", ":SEME//9876543210987654"),
                        field("23G", "NEWM"),
                        field("22F", ":CAEV//INTR"),
                        field("22F", ":CAMV//MAND"),
                        field("98C", ":PREP//20220202105733"),
                        field("25D", ":PROC//ENTL"),
                        field("16S", "GENL"),
                        field("16R", "USECU"),
                        field("35B", "ISIN CH0101010101\n/XS/232323232\nFINANCIAL INSTRUMENT ACME"),
                        field("16R", "FIA"),
                        field("22F", ":MICO//A007"),
                        field("16S", "FIA"),
                        field("16R", "ACCTINFO"),
                        field("97A", ":SAFE//99999"),
                        field("94F", ":SAFE//NCSD/TESTBIC0ABC"),
                        field("93B", ":ELIG//FAMT/500000,"),
                        field("93B", ":SETT//FAMT/500000,"),
                        field("16S", "ACCTINFO"),
                        field("16S", "USECU"),
                        field("16R", "CADETL"),
                        field("98A", ":ANOU//20220113"),
                        field("98A", ":RDTE//20220113"),
                        field("69A", ":INPE//20220214/20220214"),
                        field("99A", ":DAAC//360"),
                        field("92K", ":INTR//UKWN"),
                        field("22F", ":ADDB//CAPA"),
                        field("16S", "CADETL"),
                        field("16R", "CAOPTN"),
                        field("13A", ":CAON//001"),
                        field("22F", ":CAOP//CASH"),
                        field("11A", ":OPTN//USD"),
                        field("17B", ":DFLT//Y"),
                        field("16R", "CASHMOVE"),
                        field("22H", ":CRDB//CRED"),
                        field("22H", ":CONT//ACTU"),
                        field("97A", ":CASH//89898"),
                        field("19B", ":ENTL//USD3333,"),
                        field("19B", ":TXFR//USD3333,"),
                        field("19B", ":NETT//USD3333,"),
                        field("98A", ":PAYD//20220214"),
                        field("98A", ":VALU//20220214"),
                        field("98A", ":EARL//20220214"),
                        field("92A", ":INTP//0,75"),
                        field("92A", ":TAXR//0,"),
                        field("16S", "CASHMOVE"),
                        field("16S", "CAOPTN"),
                        field("16R", "ADDINFO"),
                        field("70E", ":ADTX//PAYMENT UPON RECEIPT OF FUNDS - \nTIMELY PAYMENT EXPECTED"),
                        field("16S", "ADDINFO")
                )),
                block("5", blocks(block("CHK", "C77F8E009597")))
        )));
    }

    @Ignore
    @Test
    public void parsesRepeatedFields() throws Exception {
        Assert.fail("as 20C in the Petr Stodulka example");
    }

    @Ignore
    @Test
    public void parsesSequencedFields() throws Exception {
        Assert.fail("all that 16R/16S stuff");
    }

    @Ignore
    @Test
    public void parsesNestedSequencedFields() throws Exception {
        Assert.fail("advanced case of 16R/16S stuff");
    }

    @SafeVarargs
    private static List<Map<String, Object>> blocks(Map<String, Object>... blocks) {
        return Arrays.asList(blocks);
    }

    private static Map<String, Object> block() {
        return Collections.emptyMap();
    }

    private static Map<String, Object> block(String tag, Object value) {
        return Collections.singletonMap(tag, value);
    }

    @SafeVarargs
    private static Map<String, Object> block(Map.Entry<String, Object>... entries) {
        HashMap<String, Object> block = new HashMap<>();
        for (Map.Entry<String, Object> entry : entries) {
            block.put(entry.getKey(), entry.getValue());
        }
        return block;
    }

    private static Map.Entry<String, Object> field(String tag, Object value) {
        return new AbstractMap.SimpleImmutableEntry<>(tag, value);
    }

}
