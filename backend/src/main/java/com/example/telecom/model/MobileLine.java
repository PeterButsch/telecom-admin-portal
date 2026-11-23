package com.example.telecom.model;

import com.example.telecom.api.Times;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A mobile subscription: one MSISDN with its SIM, plan, contract and promotion.
 */
@Entity
@Table(name = "mobile_lines")
@SuppressWarnings("PMD.TooManyFields")
public class MobileLine {

    public enum SimType {
        PHYSICAL, ESIM
    }

    public enum Status {
        PENDING, ACTIVE, SUSPENDED, BARRED, TERMINATED
    }

    public enum Operator {
        M1, STARHUB, SIMBA, CIRCLES_LIFE, OTHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "msisdn", nullable = false)
    private String msisdn;

    @Enumerated(EnumType.STRING)
    @Column(name = "sim_type", nullable = false)
    private SimType simType;

    @Column(name = "iccid", nullable = false)
    private String iccid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "status_reason")
    private String statusReason;

    @Column(name = "user_name")
    private String userName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(name = "promotion_ends_on")
    private LocalDate promotionEndsOn;

    @Column(name = "contract_start")
    private LocalDate contractStart;

    @Column(name = "contract_end")
    private LocalDate contractEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "port_in_from")
    private Operator portInFrom;

    @Enumerated(EnumType.STRING)
    @Column(name = "port_out_to")
    private Operator portOutTo;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "terminated_at")
    private Instant terminatedAt;

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

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public SimType getSimType() {
        return simType;
    }

    public void setSimType(SimType simType) {
        this.simType = simType;
    }

    public String getIccid() {
        return iccid;
    }

    public void setIccid(String iccid) {
        this.iccid = iccid;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public void setPromotion(Promotion promotion) {
        this.promotion = promotion;
    }

    public LocalDate getPromotionEndsOn() {
        return promotionEndsOn;
    }

    public void setPromotionEndsOn(LocalDate promotionEndsOn) {
        this.promotionEndsOn = promotionEndsOn;
    }

    public LocalDate getContractStart() {
        return contractStart;
    }

    public void setContractStart(LocalDate contractStart) {
        this.contractStart = contractStart;
    }

    public LocalDate getContractEnd() {
        return contractEnd;
    }

    public void setContractEnd(LocalDate contractEnd) {
        this.contractEnd = contractEnd;
    }

    public Operator getPortInFrom() {
        return portInFrom;
    }

    public void setPortInFrom(Operator portInFrom) {
        this.portInFrom = portInFrom;
    }

    public Operator getPortOutTo() {
        return portOutTo;
    }

    public void setPortOutTo(Operator portOutTo) {
        this.portOutTo = portOutTo;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(Instant activatedAt) {
        this.activatedAt = activatedAt;
    }

    public Instant getTerminatedAt() {
        return terminatedAt;
    }

    public void setTerminatedAt(Instant terminatedAt) {
        this.terminatedAt = terminatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
