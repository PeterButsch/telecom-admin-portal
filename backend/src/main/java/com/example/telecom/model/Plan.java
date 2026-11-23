package com.example.telecom.model;

import com.example.telecom.api.Times;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "plans")
public class Plan {

    public enum Brand {
        SINGTEL, GOMO, HI_CARD
    }

    /**
     * POSTPAID plans come with a contract; PREPAID and DATA_ONLY lines are counted
     * separately by IMDA rules.
     */
    public enum Category {
        POSTPAID, SIM_ONLY, PREPAID, DATA_ONLY
    }

    public enum Segment {
        CONSUMER, ENTERPRISE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "brand", nullable = false)
    private Brand brand;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "segment", nullable = false)
    private Segment segment;

    @Column(name = "monthly_price", nullable = false)
    private BigDecimal monthlyPrice;

    @Column(name = "data_limit_gb")
    private Integer dataLimitGb;

    @Column(name = "minutes_limit")
    private Integer minutesLimit;

    @Column(name = "sms_limit")
    private Integer smsLimit;

    @Column(name = "contract_months", nullable = false)
    private int contractMonths;

    @Column(name = "network", nullable = false)
    private String network = "5G";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Times.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Brand getBrand() {
        return brand;
    }

    public void setBrand(Brand brand) {
        this.brand = brand;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Segment getSegment() {
        return segment;
    }

    public void setSegment(Segment segment) {
        this.segment = segment;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(BigDecimal monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public Integer getDataLimitGb() {
        return dataLimitGb;
    }

    public void setDataLimitGb(Integer dataLimitGb) {
        this.dataLimitGb = dataLimitGb;
    }

    public Integer getMinutesLimit() {
        return minutesLimit;
    }

    public void setMinutesLimit(Integer minutesLimit) {
        this.minutesLimit = minutesLimit;
    }

    public Integer getSmsLimit() {
        return smsLimit;
    }

    public void setSmsLimit(Integer smsLimit) {
        this.smsLimit = smsLimit;
    }

    public int getContractMonths() {
        return contractMonths;
    }

    public void setContractMonths(int contractMonths) {
        this.contractMonths = contractMonths;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
