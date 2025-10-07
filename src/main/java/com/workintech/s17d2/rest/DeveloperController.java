package com.workintech.s17d2.rest;

import com.workintech.s17d2.model.*;
import com.workintech.s17d2.tax.Taxable;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class DeveloperController {

    public Map<Integer, Developer> developers;
    private final Taxable taxable;

    // DI: Testte @WebMvcTest ile mock/real enjekte senaryolarına uyumlu
    public DeveloperController(Taxable taxable) {
        this.taxable = taxable;
    }

    @PostConstruct
    public void init() {
        developers = new ConcurrentHashMap<>();
    }

    // [GET] /developers -> tüm developer'lar (JSON array)
    @GetMapping("/developers")
    public List<Developer> findAll() {
        return new ArrayList<>(developers.values());
    }

    // [GET] /developers/{id} -> tek developer
    @GetMapping("/developers/{id}")
    public ResponseEntity<Developer> findById(@PathVariable int id) {
        Developer dev = developers.get(id);
        return dev != null ? ResponseEntity.ok(dev) : ResponseEntity.notFound().build();
    }

    // [POST] /developers -> body: Developer (id, name, salary, experience)
    // experience'a göre uygun tipten nesne oluştur, vergiyi düş, kaydet
    @PostMapping("/developers")
    public ResponseEntity<Developer> save(@RequestBody Developer body) {
        if (body == null || body.getExperience() == null) {
            return ResponseEntity.badRequest().build();
        }

        int id = body.getId();
        String name = body.getName();
        double baseSalary = body.getSalary();
        Experience exp = body.getExperience();

        Developer created;

        switch (exp) {
            case JUNIOR -> {
                double net = applyTax(baseSalary, taxable.getSimpleTaxRate());
                created = new JuniorDeveloper(id, name, net);
            }
            case MID -> {
                double net = applyTax(baseSalary, taxable.getMiddleTaxRate());
                created = new MidDeveloper(id, name, net);
            }
            case SENIOR -> {
                double net = applyTax(baseSalary, taxable.getUpperTaxRate());
                created = new SeniorDeveloper(id, name, net);
            }
            default -> {
                return ResponseEntity.badRequest().build();
            }
        }

        developers.put(id, created);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // [PUT] /developers/{id} -> body'deki değerle güncelle
    @PutMapping("/developers/{id}")
    public ResponseEntity<Developer> update(@PathVariable int id, @RequestBody Developer updated) {
        if (updated == null) return ResponseEntity.badRequest().build();
        developers.put(id, updated);
        return ResponseEntity.ok(updated);
    }

    // [DELETE] /developers/{id} -> sil
    @DeleteMapping("/developers/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        developers.remove(id);
        return ResponseEntity.ok().build();
    }

    // helper: salary - salary * (rate/100)
    private double applyTax(double salary, double ratePercent) {
        return salary - (salary * (ratePercent / 100.0));
    }
}
