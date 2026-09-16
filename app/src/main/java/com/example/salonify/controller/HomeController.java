package com.example.salonify.controller;

import com.example.salonify.entity.Salon;
import com.example.salonify.entity.SalonVisibility;
import com.example.salonify.repository.SalonRepository;
import com.example.salonify.service.SalonQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final SalonRepository salons;
    private final SalonQueryService salonQuery;

    public HomeController(SalonRepository salons, SalonQueryService salonQuery) {
        this.salons = salons;
        this.salonQuery = salonQuery;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<Salon> latest = salons.findByVisibilityOrderByCreatedAtDesc(SalonVisibility.PUBLIC);
        if (latest.size() > 6) latest = latest.subList(0, 6);
        model.addAttribute("cards", salonQuery.toCards(latest));
        return "home";
    }
}
