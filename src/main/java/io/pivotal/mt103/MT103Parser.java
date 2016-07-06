package io.pivotal.mt103;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class MT103Parser {

    public static Map<String, Object> parse(String instructionString) throws IOException, ParseException {
        Map<String, Object> instructionData = ISO15022Parser.parse(instructionString);

        HashMap<String, Object> instruction = new HashMap<>();

        String basicHeader = getEntry(instructionData, "1");
        String sourceTerminalAddress = basicHeader.substring(3, 15);

        String applicationHeader = getEntry(instructionData, "2");
        String destinationTerminalAddress = applicationHeader.substring(4, 16);

        Map<String, Object> message = getMapEntry(instructionData, "4");

        String bankUrn = getEntry(message, "20");

        String settlement = getEntry(message, "32A");
        LocalDate valueDate = parseSwiftDate(settlement.substring(0, 6));
        String currency = settlement.substring(6, 9);
        double interbankSettledAmount = parseSwiftDouble(settlement.substring(9));

        Customer orderingCustomer = parseCustomer(getEntry(message, "50K"));

        Customer beneficiaryCustomer = parseCustomer(getEntry(message, "59"));

        String remittanceInformation = getEntry(message, "70");

        String detailsOfCharges = getEntry(message, "71A");
        String senderToReceiverInformation = getEntry(message, "72");

        instruction.put("bank", sourceTerminalAddress);
        instruction.put("sponsorBank", destinationTerminalAddress);
        instruction.put("bankUrn", bankUrn);
        instruction.put("amount", interbankSettledAmount);
        instruction.put("currency", currency);
        instruction.put("dateForValue", valueDate.toString());

        instruction.put("remitter", customerMap(orderingCustomer));

        Map<String, Object> beneficiary = customerMap(beneficiaryCustomer);
        beneficiary.put("beneficiaryReference", remittanceInformation);
        instruction.put("beneficiary", beneficiary);

        instruction.put("charges", detailsOfCharges);
        instruction.put("transactionType", senderToReceiverInformation);

        return instruction;
    }

    private static String getEntry(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new NoSuchElementException(key);
        }
        if (!(value instanceof String)) {
            throw new ClassCastException("value '" + value + "' of key '" + key + "' should be a string but was a " + value.getClass());
        }
        return (String) value;
    }

    private static Map<String, Object> getMapEntry(Map<String, Object> instructionData, String key) {
        Object value = instructionData.get(key);
        if (value == null) {
            throw new NoSuchElementException(key);
        }
        if (!(value instanceof Map)) {
            throw new ClassCastException("value '" + value + "' of key '" + key + "' should be a map but was a " + value.getClass());
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        return map;
    }

    private static Customer parseCustomer(String customerString) {
        String[] splitCustomerString = customerString.split("\n", 3);

        return new Customer(
                splitCustomerString[0].substring(1, 7),
                splitCustomerString[0].substring(7),
                splitCustomerString[1],
                splitCustomerString[2]);
    }

    private static LocalDate parseSwiftDate(String substring) {
        return LocalDate.parse(substring, DateTimeFormatter.ofPattern("yyMMdd"));
    }

    private static double parseSwiftDouble(String substring) {
        return Double.parseDouble(substring.replace(',', '.'));
    }

    private static class Customer {

        private final String sortCode, accountNumber, name, address;

        private Customer(String sortCode, String accountNumber, String name, String address) {
            this.sortCode = sortCode;
            this.accountNumber = accountNumber;
            this.name = name;
            this.address = address;
        }

    }

    private static Map<String, Object> customerMap(Customer customer) {
        Map<String, Object> map = new HashMap<>();
        map.put("sortCode", customer.sortCode);
        map.put("bankAccount", customer.accountNumber);
        map.put("name", customer.name);
        map.put("address", customer.address.replace("\n", ""));
        return map;
    }

}
