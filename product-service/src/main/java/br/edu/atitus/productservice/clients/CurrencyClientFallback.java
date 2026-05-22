package br.edu.atitus.productservice.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CurrencyClientFallback implements CurrencyClient {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyClientFallback.class);

    @Override
    public CurrencyResponse getCurrency(String source, String target) {
        logger.warn("CurrencyClient fallback acionado para source={} target={} - currency-service indisponível",
                source, target);
        return new CurrencyResponse(source, target, 1.0, "fallback - currency-service unavailable");
    }
}
