package com.example.telecom.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SearchTermsTest {

    @Test
    void blankQueriesMatchEverything() {
        assertNull(SearchTerms.of(null).text());
        assertNull(SearchTerms.of("   ").digits());
    }

    @Test
    void phoneNumbersMatchWithoutTheCountryCode() {
        SearchTerms terms = SearchTerms.of("+65 9123-4567");
        assertEquals("%91234567%", terms.digits());
        assertEquals("%+65 9123-4567%", terms.text());
    }

    @Test
    void identityNumbersAndNamesDoNotTurnIntoNumberSearches() {
        assertNull(SearchTerms.of("S1234567D").digits());
        assertNull(SearchTerms.of("Tower 2").digits());
        assertNull(SearchTerms.of("12").digits());
    }

    @Test
    void likeWildcardsAreEscapedToMatchThemselves() {
        assertEquals("%a!%b!_c%", SearchTerms.of("a%b_c").text());
        assertEquals("%50!! off%", SearchTerms.of("50! off").text());
    }
}
