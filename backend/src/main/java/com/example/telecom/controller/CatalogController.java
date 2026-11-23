package com.example.telecom.controller;

import com.example.telecom.api.Times;
import com.example.telecom.model.AddOn;
import com.example.telecom.model.Promotion;
import com.example.telecom.repository.AddOnRepository;
import com.example.telecom.repository.PromotionRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only add-on and promotion catalogues. */
@RestController
@RequestMapping("/api")
public class CatalogController {

    private final AddOnRepository addOnRepository;
    private final PromotionRepository promotionRepository;

    public CatalogController(AddOnRepository addOnRepository, PromotionRepository promotionRepository) {
        this.addOnRepository = addOnRepository;
        this.promotionRepository = promotionRepository;
    }

    @GetMapping("/add-ons")
    public List<AddOnResponse> getAddOns() {
        return addOnRepository.findAll(Sort.by("category", "monthlyPrice")).stream().map(AddOnResponse::from).toList();
    }

    @GetMapping("/promotions")
    public List<PromotionResponse> getPromotions() {
        return promotionRepository.findAll(Sort.by("code")).stream().map(PromotionResponse::from).toList();
    }

    public record AddOnResponse(Long id, String code, String name, String category, BigDecimal monthly_price,
            boolean is_active) {
        static AddOnResponse from(AddOn a) {
            return new AddOnResponse(a.getId(), a.getCode(), a.getName(), a.getCategory().name(),
                    CustomerController.toMoney(a.getMonthlyPrice()), a.isActive());
        }
    }

    public record PromotionResponse(Long id, String code, String name, String discount_type, BigDecimal discount_value,
            int duration_months, String valid_from, String valid_to, boolean is_active) {
        static PromotionResponse from(Promotion p) {
            return new PromotionResponse(p.getId(), p.getCode(), p.getName(), p.getDiscountType().name(),
                    CustomerController.toMoney(p.getDiscountValue()), p.getDurationMonths(),
                    Times.iso(p.getValidFrom()), Times.iso(p.getValidTo()), p.isActive());
        }
    }
}
