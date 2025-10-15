package com.br.rmm.javak8s.service;
import org.slf4j.Logger;

import com.br.rmm.javak8s.models.AppResponse;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public final class KubeServiceImpl implements KubeService {
    Logger logger  = LoggerFactory.getLogger(KubeServiceImpl.class);

    @Override
    public AppResponse validateApp() {
        var response = new AppResponse("::Hello Java Kube =) v4");
        logger.info("Response: " + response);
        return response;
    }
}
