package com.zosh.controller;


import com.zosh.exception.ResourceNotFoundException;
import com.zosh.modal.SubscriptionPlan;
import com.zosh.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/super-admin/subscription-plans", "/api/subscription-plans"})
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    /**
     * ➕ Create a new subscription plan
     * @param plan SubscriptionPlan entity
     */
    @PostMapping
    public SubscriptionPlan createPlan(@RequestBody SubscriptionPlan plan) {
        return subscriptionPlanService.createPlan(plan);
    }

    /**
     * 🔄 Update an existing subscription plan by ID
     * @param id SubscriptionPlan ID
     * @param plan Updated plan data
     */
    @PutMapping("/{id}")
    public SubscriptionPlan updatePlan(
            @PathVariable Long id,
            @RequestBody SubscriptionPlan plan
    ) throws ResourceNotFoundException {
        return subscriptionPlanService.updatePlan(id, plan);
    }

    /**
     * 📦 Get all subscription plans
     */
    @GetMapping
    public List<SubscriptionPlan> getAllPlans() {
        return subscriptionPlanService.getAllPlans();
    }

    /**
     * 🔍 Get a single subscription plan by ID
     */
    @GetMapping("/{id}")
    public SubscriptionPlan getPlanById(@PathVariable Long id) throws ResourceNotFoundException {
        return subscriptionPlanService.getPlanById(id);
    }

    /**
     * ❌ Delete a subscription plan by ID
     */
    @DeleteMapping("/{id}")
    public void deletePlan(@PathVariable Long id) throws ResourceNotFoundException {
        subscriptionPlanService.deletePlan(id);
    }
}
