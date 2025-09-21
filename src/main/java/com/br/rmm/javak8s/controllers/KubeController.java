package com.br.rmm.javak8s.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kube")
public class KubeController {
    @GetMapping
    public ResponseEntity<String> helloKube(){
        return ResponseEntity.ok("Hello Java Kube =)");
    }
}
