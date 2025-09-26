package com.br.rmm.javak8s.service;

import com.br.rmm.javak8s.models.AppResponse;
import org.springframework.stereotype.Service;

public sealed interface KubeService permits KubeServiceImpl {
    AppResponse validateApp();
}
