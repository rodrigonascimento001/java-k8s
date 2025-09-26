package com.br.rmm.javak8s.controllers;

import com.br.rmm.javak8s.models.AppResponse;
import com.br.rmm.javak8s.service.KubeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kube")
public class KubeController {
    private final KubeService kubeService;

    public KubeController(KubeService kubeService) {
        this.kubeService = kubeService;
    }

    @GetMapping
    public ResponseEntity<AppResponse> helloKube(){
        return ResponseEntity.ok(kubeService.validateApp());
    }
}
