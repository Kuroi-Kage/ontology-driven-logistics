package com.flexchain.controller;

import com.flexchain.dto.dashboard.DashboardOverviewDto;
import com.flexchain.service.DashboardAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@CrossOrigin("*")
public class DashboardController {

    private final DashboardAggregationService service;

    @GetMapping("/overview")
    public DashboardOverviewDto overview() {
        return service.overview();
    }
}