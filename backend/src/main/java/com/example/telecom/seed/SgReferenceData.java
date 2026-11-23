package com.example.telecom.seed;

import java.util.List;

/**
 * Static Singapore reference data used to generate realistic, entirely
 * fictional demo records.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class SgReferenceData {

    static final List<String> CHINESE_SURNAMES = List.of("Tan", "Lim", "Lee", "Ng", "Ong", "Wong", "Goh", "Chua",
            "Chan", "Koh", "Teo", "Ang", "Yeo", "Tay", "Ho", "Low", "Toh", "Sim", "Chong", "Chia", "Seah", "Foo",
            "Leong", "Quek", "Heng");
    static final List<String> CHINESE_GIVEN = List.of("Wei Ming", "Hui Min", "Jia Hui", "Zhi Hao", "Xin Yi", "Kai Wen",
            "Mei Ling", "Jun Jie", "Shu Fen", "Yong Sheng", "Li Ting", "Wen Jie", "Pei Shan", "Jia Wei", "Siew Ling",
            "Boon Keng", "Ai Ling", "Kok Leong", "Hui Ying", "Chee Keong", "Rachel", "Daniel", "Joanne", "Marcus",
            "Cheryl", "Bryan", "Valerie", "Ryan", "Germaine", "Darren");
    static final List<String> MALAY_MALE = List.of("Muhammad Faizal", "Ahmad Firdaus", "Muhammad Hafiz",
            "Mohamed Irfan", "Syed Azman", "Muhammad Danial", "Khairul Anwar", "Hairul Nizam", "Iskandar", "Zulkifli");
    static final List<String> MALAY_FEMALE = List.of("Nur Aisyah", "Siti Nurhaliza", "Nurul Huda", "Farah Nadia",
            "Aishah", "Nur Syafiqah", "Zarina", "Noraini", "Puteri Balqis", "Hidayah");
    static final List<String> MALAY_FATHERS = List.of("Ahmad", "Rahman", "Ismail", "Hassan", "Osman", "Yusof", "Salleh",
            "Abdullah", "Kassim", "Ibrahim");
    static final List<String> INDIAN_MALE = List.of("Rajesh", "Suresh", "Arun", "Vikram", "Karthik", "Ganesh",
            "Pradeep", "Harish", "Naveen", "Sanjay");
    static final List<String> INDIAN_FEMALE = List.of("Priya", "Kavitha", "Lakshmi", "Deepa", "Meena", "Anitha",
            "Shalini", "Revathi", "Divya", "Nirmala");
    static final List<String> INDIAN_FATHERS = List.of("Kumar", "Raj", "Pillai", "Nair", "Subramaniam", "Krishnan",
            "Ramasamy", "Govindasamy", "Menon", "Naidu");
    static final List<String> OTHER_FIRST = List.of("James", "Sarah", "Oliver", "Emma", "Lucas", "Sophie", "Hiroshi",
            "Yuki", "Min-jun", "Ji-woo", "Arjun", "Ananya", "Thomas", "Charlotte", "Nguyen", "Maria", "Jose", "Ana",
            "Liam", "Chloe");
    static final List<String> OTHER_LAST = List.of("Smith", "Brown", "Williams", "Taylor", "Fernandez", "Santos",
            "Reyes", "Tanaka", "Sato", "Kim", "Park", "Sharma", "Gupta", "Nguyen", "Tran", "Müller", "Martin",
            "De Souza", "Wilson", "Clarke");

    static final List<String> EMAIL_DOMAINS = List.of("gmail.com", "yahoo.com.sg", "hotmail.com", "singnet.com.sg",
            "outlook.com", "icloud.com");

    static final List<Estate> ESTATES = List.of(new Estate("Ang Mo Kio Avenue 3", "56"),
            new Estate("Bedok North Street 1", "46"), new Estate("Tampines Street 21", "52"),
            new Estate("Jurong West Street 41", "64"), new Estate("Woodlands Drive 14", "73"),
            new Estate("Yishun Ring Road", "76"), new Estate("Punggol Field", "82"),
            new Estate("Sengkang East Way", "54"), new Estate("Toa Payoh Lorong 1", "31"),
            new Estate("Clementi Avenue 2", "12"), new Estate("Hougang Avenue 8", "53"),
            new Estate("Bukit Batok West Avenue 6", "65"), new Estate("Choa Chu Kang Avenue 4", "68"),
            new Estate("Pasir Ris Drive 6", "51"), new Estate("Queenstown Road", "16"),
            new Estate("Bishan Street 12", "57"), new Estate("Serangoon Avenue 4", "55"));

    static final List<String> COMPANY_PREFIXES = List.of("Lion City", "Merlion", "Straits", "Harbourfront", "Orchid",
            "Kallang", "Raffles Place", "Changi", "Jurong", "Marina", "Tanjong", "Bukit Timah", "Temasek Ridge",
            "Sentosa", "Keppel Bay", "Novena", "Bugis", "Tiong Bahru", "Dhoby", "Paya Lebar");
    static final List<String> COMPANY_TRADES = List.of("Logistics", "Engineering", "Holdings", "Trading",
            "Facilities Management", "F&B Group", "Clinic Group", "Construction", "Security Services", "Tech", "Retail",
            "Property Management", "Marine Services", "Education Centre", "Cleaning Services");
    static final List<String> COST_CENTRES = List.of("Head Office", "Sales", "Operations", "Field Service", "Drivers",
            "Management");
    static final List<String> BUSINESS_ADDRESSES = List.of("1 Raffles Place #20-01", "80 Robinson Road #10-02",
            "10 Anson Road #15-08", "3 Changi Business Park Crescent #04-01", "2 Jurong East Street 21 #03-11",
            "9 Tampines Grande #06-03", "1 Kaki Bukit View #05-22", "18 Woodlands Loop #02-10");
    static final List<String> BUSINESS_POSTAL_CODES = List.of("048616", "068898", "079903", "486025", "609601",
            "528735", "415941", "738100");

    static final List<DeviceModel> DEVICE_MODELS = List.of(new DeviceModel("Apple", "iPhone 17 Pro", "35391812"),
            new DeviceModel("Apple", "iPhone 17", "35391913"), new DeviceModel("Apple", "iPhone 16", "35284716"),
            new DeviceModel("Samsung", "Galaxy S25 Ultra", "35073422"),
            new DeviceModel("Samsung", "Galaxy S25", "35073523"),
            new DeviceModel("Samsung", "Galaxy Z Flip7", "35112925"),
            new DeviceModel("Samsung", "Galaxy A56", "35221326"), new DeviceModel("Google", "Pixel 10 Pro", "35677830"),
            new DeviceModel("Google", "Pixel 10", "35677931"), new DeviceModel("Google", "Pixel 9a", "35612432"),
            new DeviceModel("OPPO", "Find X8", "86401533"), new DeviceModel("Xiaomi", "15T Pro", "86812134"),
            new DeviceModel("Huawei", "Pura 80", "86953235"));

    static final List<CaseTemplate> CASE_TEMPLATES = List.of(
            new CaseTemplate("BILLING", "Disputed charges on latest bill",
                    "Customer does not recognise some charges on this month's bill."),
            new CaseTemplate("BILLING", "GIRO deduction failed",
                    "GIRO deduction was rejected by the bank; customer wants to update payment details."),
            new CaseTemplate("NETWORK", "Weak 5G signal at home",
                    "Customer reports weak indoor 5G coverage and frequent fallback to 4G."),
            new CaseTemplate("NETWORK", "No mobile data in MRT tunnel",
                    "Data drops between stations on the North East Line."),
            new CaseTemplate("DEVICE", "Handset not charging", "Device under contract stopped charging after a week."),
            new CaseTemplate("DEVICE", "Screen repair request", "Customer requests a repair under device protection."),
            new CaseTemplate("PLAN_CHANGE", "Upgrade to a bigger data plan",
                    "Customer keeps exceeding the data bundle and wants to upgrade."),
            new CaseTemplate("PLAN_CHANGE", "Recontract with new phone",
                    "Contract ends soon; customer wants to recontract with a new handset."),
            new CaseTemplate("PORTING", "Number port-in pending",
                    "Port-in from the previous operator has not completed after two working days."),
            new CaseTemplate("SIM_REPLACEMENT", "Lost SIM card", "Customer lost the SIM and needs a replacement."),
            new CaseTemplate("SIM_REPLACEMENT", "Move number to eSIM",
                    "Customer wants to convert the physical SIM to an eSIM on a new phone."),
            new CaseTemplate("SCAM_REPORT", "Scam SMS impersonating a bank",
                    "Customer received an SMS with a phishing link pretending to be a local bank."),
            new CaseTemplate("SCAM_REPORT", "Suspicious call claiming to be from ICA",
                    "Caller asked for personal details and claimed to be from a government agency."),
            new CaseTemplate("ROAMING", "Roaming not working in Malaysia",
                    "Customer cannot use data while roaming in Johor Bahru."),
            new CaseTemplate("ROAMING", "High roaming charges",
                    "Customer was charged pay-per-use roaming instead of the roaming pack."),
            new CaseTemplate("GENERAL", "Update contact details", "Customer wants to change the registered email."),
            new CaseTemplate("GENERAL", "Request for itemised bill", "Customer asked for an itemised bill by email."));

    static final List<String> EMPLOYEE_ROLES = List.of("Driver", "Technician", "Sales", "Manager", "Supervisor",
            "Engineer", "Admin", "Security");

    private SgReferenceData() {
    }

    /** HDB town, main road and the two-digit postal sector of the town. */
    record Estate(String road, String sector) {
    }

    /**
     * Manufacturer, model and the 8-digit TAC prefix used for IMEIs of that model.
     */
    record DeviceModel(String manufacturer, String model, String tac) {
    }

    /** Case subject and description templates per case category name. */
    record CaseTemplate(String category, String subject, String description) {
    }
}
