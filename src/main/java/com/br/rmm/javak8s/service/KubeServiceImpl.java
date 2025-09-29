package com.br.rmm.javak8s.service;

import com.br.rmm.javak8s.models.AppResponse;
import org.springframework.stereotype.Service;

@Service
public final class KubeServiceImpl implements KubeService {
    @Override
    public AppResponse validateApp() {
        return new AppResponse("::Hello Java Kube =) v2");
    }
}
