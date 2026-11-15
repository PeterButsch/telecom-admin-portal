package com.example.telecom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.telecom.model.IdentityNumbers;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CustomerApiTest extends IntegrationTestSupport {

    private static String individual(String idType, String idNumber, String dateOfBirth, String email) {
        return """
                {"type":"INDIVIDUAL","firstName":"Siti","lastName":"binte Rahman","idType":"%s","idNumber":"%s",
                 "dateOfBirth":"%s","email":"%s","phone":"+65 8765 4321","postalCode":"520123"}
                """.formatted(idType, idNumber, dateOfBirth, email);
    }

    private static String validNric() {
        return "T0312345" + IdentityNumbers.checkLetter('T', "0312345");
    }

    @Test
    void createsAnIndividualAndOnlyReturnsTheMaskedId() throws Exception {
        String nric = validNric();
        postJson("/api/customers", individual("NRIC", nric.toLowerCase(), "1990-01-01", "siti@example.com"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.customer_number").value("C1000003"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.id_number_masked").value("*****345" + nric.charAt(8)))
                .andExpect(jsonPath("$.id_number").doesNotExist())
                .andExpect(jsonPath("$.display_name").value("Siti binte Rahman"));
    }

    @Test
    void rejectsAnNricWithTheWrongCheckLetter() throws Exception {
        String nric = validNric();
        char wrong = nric.charAt(8) == 'A' ? 'B' : 'A';
        postJson("/api/customers", individual("NRIC", nric.substring(0, 8) + wrong, "1990-01-01", "siti@example.com"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.idNumber").exists());
    }

    @Test
    void rejectsAFinRecordedAsAnNric() throws Exception {
        postJson("/api/customers", individual("NRIC", "M1234567K", "1990-01-01", "siti@example.com"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.idNumber").exists());
        postJson("/api/customers", individual("FIN", "M1234567K", "1990-01-01", "siti@example.com"))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsAccountHoldersUnder18() throws Exception {
        String seventeen = LocalDate.now().minusYears(17).toString();
        postJson("/api/customers", individual("NRIC", validNric(), seventeen, "siti@example.com"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.dateOfBirth").exists());
    }

    @Test
    void duplicateIdentityOrEmailIsAConflict() throws Exception {
        postJson("/api/customers", individual("NRIC", ADA_NRIC, "1990-01-01", "other@example.com"))
                .andExpect(status().isConflict());
        postJson("/api/customers", individual("NRIC", validNric(), "1990-01-01", "ADA.TAN@example.com"))
                .andExpect(status().isConflict());
    }

    @Test
    void businessCustomersNeedAValidUenAndNoPersonalId() throws Exception {
        postJson("/api/customers", """
                {"type":"BUSINESS","companyName":"Merlion Tech Pte. Ltd.","uen":"12AB","idType":"NRIC",
                 "idNumber":"S1234567D","email":"it@merlion.sg","phone":"+65 6222 3333"}
                """).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.uen").exists())
                .andExpect(jsonPath("$.errors.idNumber").exists());
        postJson("/api/customers", """
                {"type":"BUSINESS","companyName":"Merlion Tech Pte. Ltd.","uen":"202312345k",
                 "email":"it@merlion.sg","phone":"+65 6222 3333"}
                """).andExpect(status().isCreated()).andExpect(jsonPath("$.uen").value("202312345K"))
                .andExpect(jsonPath("$.display_name").value("Merlion Tech Pte. Ltd."));
    }

    @Test
    void searchFindsMobileNumbersInAnyFormatButNeverIdNumbers() throws Exception {
        mockMvc.perform(get("/api/customers").param("q", "+65 9123 4567")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(1));
        mockMvc.perform(get("/api/customers").param("q", "lion city")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2));
        mockMvc.perform(get("/api/customers").param("q", ADA_NRIC)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listIncludesLineAndAccountSummaries() throws Exception {
        mockMvc.perform(get("/api/customers").param("type", "INDIVIDUAL")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].line_count").value(2))
                .andExpect(jsonPath("$.content[0].active_line_count").value(1))
                .andExpect(jsonPath("$.content[0].account_count").value(1))
                .andExpect(jsonPath("$.content[0].date_of_birth").doesNotExist());
        mockMvc.perform(get("/api/customers/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.date_of_birth").value("1990-05-17"));
    }

    @Test
    void patchKeepsFieldsThatAreNotSent() throws Exception {
        patchJson("/api/customers/1", "{\"phone\":\"+65 9000 1111\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+65 9000 1111")).andExpect(jsonPath("$.first_name").value("Ada"))
                .andExpect(jsonPath("$.email").value("ada.tan@example.com"));
        patchJson("/api/customers/1", "{\"companyName\":\"Ada Pte. Ltd.\"}").andExpect(status().isBadRequest());
    }

    @Test
    void customerWithAccountsCannotBeDeleted() throws Exception {
        mockMvc.perform(delete("/api/customers/1")).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/customers/404")).andExpect(status().isNotFound());
    }

    @Test
    void customerWithoutHistoryCanBeDeleted() throws Exception {
        postJson("/api/customers", individual("NRIC", validNric(), "1990-01-01", "siti@example.com"))
                .andExpect(status().isCreated());
        Long id = jdbc.queryForObject("SELECT id FROM customers WHERE email = 'siti@example.com'", Long.class);
        mockMvc.perform(delete("/api/customers/" + id)).andExpect(status().isNoContent());
    }
}
