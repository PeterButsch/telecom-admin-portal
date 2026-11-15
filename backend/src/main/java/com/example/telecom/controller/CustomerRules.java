package com.example.telecom.controller;

import com.example.telecom.controller.CustomerController.CustomerCreateRequest;
import com.example.telecom.controller.CustomerController.CustomerUpdateRequest;
import com.example.telecom.error.FieldValidationException;
import com.example.telecom.model.Customer;
import com.example.telecom.model.IdentityNumbers;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Validation rules for creating and changing customers. */
@SuppressWarnings("PMD.GodClass")
final class CustomerRules {

    static final int MINIMUM_AGE = 18;
    /**
     * Business (8 digits + letter), local company (9 digits + letter) and
     * other-entity UEN formats.
     */
    private static final Pattern UEN = Pattern.compile("^(\\d{8}[A-Z]|\\d{9}[A-Z]|[TSR]\\d{2}[A-Z]{2}\\d{4}[A-Z])$");
    private static final Pattern PASSPORT = Pattern.compile("^[A-Z0-9]{6,12}$");
    private static final String REQUIRED = "is required";
    private static final String ID_NUMBER = "idNumber";

    private CustomerRules() {
    }

    static Customer newCustomer(CustomerCreateRequest request) {
        Customer customer = new Customer();
        customer.setType(request.type());
        customer.setEmail(request.email().trim());
        customer.setPhone(request.phone().trim());
        customer.setAddressLine(CustomerController.trimToNull(request.addressLine()));
        customer.setPostalCode(CustomerController.trimToNull(request.postalCode()));
        if (request.type() == Customer.Type.BUSINESS) {
            applyBusiness(customer, request);
        } else {
            applyIndividual(customer, request);
        }
        return customer;
    }

    private static void applyBusiness(Customer customer, CustomerCreateRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        String company = CustomerController.trimToNull(request.companyName());
        String uen = CustomerController.upper(request.uen());
        if (company == null) {
            errors.put("companyName", REQUIRED);
        }
        if (uen == null) {
            errors.put("uen", REQUIRED);
        } else if (!UEN.matcher(uen).matches()) {
            errors.put("uen", "must be a valid UEN, for example 201912345K");
        }
        if (request.idNumber() != null || request.idType() != null) {
            errors.put(ID_NUMBER, "is not used for business customers; record the UEN instead");
        }
        throwIfAny(errors);
        customer.setCompanyName(company);
        customer.setUen(uen);
        customer.setContactName(CustomerController.trimToNull(request.contactName()));
    }

    private static void applyIndividual(Customer customer, CustomerCreateRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        String first = CustomerController.trimToNull(request.firstName());
        String last = CustomerController.trimToNull(request.lastName());
        String idNumber = IdentityNumbers.normalize(CustomerController.trimToNull(request.idNumber()));
        if (first == null) {
            errors.put("firstName", REQUIRED);
        }
        if (last == null) {
            errors.put("lastName", REQUIRED);
        }
        if (request.idType() == null) {
            errors.put("idType", REQUIRED);
        }
        if (idNumber == null) {
            errors.put(ID_NUMBER, REQUIRED);
        } else if (request.idType() != null) {
            String problem = identityProblem(request.idType(), idNumber);
            if (problem != null) {
                errors.put(ID_NUMBER, problem);
            }
        }
        LocalDate dob = request.dateOfBirth();
        if (dob == null) {
            errors.put("dateOfBirth", REQUIRED);
        } else if (Period.between(dob, LocalDate.now(Clock.systemUTC())).getYears() < MINIMUM_AGE) {
            errors.put("dateOfBirth", "account holders must be at least " + MINIMUM_AGE + " years old");
        }
        throwIfAny(errors);
        customer.setFirstName(first);
        customer.setLastName(last);
        customer.setIdType(request.idType());
        customer.setIdNumber(idNumber);
        customer.setDateOfBirth(dob);
    }

    /**
     * Returns why the ID number is invalid for its type, or null when it is valid.
     */
    static String identityProblem(Customer.IdType type, String idNumber) {
        return switch (type) {
            case NRIC -> IdentityNumbers.isValidNricOrFin(idNumber) && IdentityNumbers.isNricPrefix(idNumber)
                    ? null
                    : "must be a valid NRIC (S or T, 7 digits and the check letter)";
            case FIN -> IdentityNumbers.isValidNricOrFin(idNumber) && !IdentityNumbers.isNricPrefix(idNumber)
                    ? null
                    : "must be a valid FIN (F, G or M, 7 digits and the check letter)";
            case PASSPORT -> PASSPORT.matcher(idNumber).matches() ? null : "must be 6 to 12 letters or digits";
        };
    }

    static void applyUpdate(Customer customer, CustomerUpdateRequest request) {
        boolean business = customer.getType() == Customer.Type.BUSINESS;
        Map<String, String> errors = new LinkedHashMap<>();
        if (business && (request.firstName() != null || request.lastName() != null)) {
            errors.put("firstName", "business customers have a company name instead");
        }
        if (!business && request.companyName() != null) {
            errors.put("companyName", "only business customers have a company name");
        }
        throwIfAny(errors);
        applyNames(customer, request);
        applyContact(customer, request);
    }

    private static void applyNames(Customer customer, CustomerUpdateRequest request) {
        if (request.firstName() != null) {
            customer.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            customer.setLastName(request.lastName().trim());
        }
        if (request.companyName() != null) {
            customer.setCompanyName(request.companyName().trim());
        }
        if (request.contactName() != null) {
            customer.setContactName(CustomerController.trimToNull(request.contactName()));
        }
    }

    private static void applyContact(Customer customer, CustomerUpdateRequest request) {
        if (request.email() != null) {
            customer.setEmail(request.email().trim());
        }
        if (request.phone() != null) {
            customer.setPhone(request.phone().trim());
        }
        if (request.addressLine() != null) {
            customer.setAddressLine(CustomerController.trimToNull(request.addressLine()));
        }
        if (request.postalCode() != null) {
            customer.setPostalCode(request.postalCode());
        }
        if (request.status() != null) {
            customer.setStatus(request.status());
        }
    }

    private static void throwIfAny(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
    }
}
