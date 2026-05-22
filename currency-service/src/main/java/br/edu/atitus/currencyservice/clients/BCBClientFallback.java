package br.edu.atitus.currencyservice.clients;

import br.edu.atitus.currencyservice.dtos.BCBCurrencyDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BCBClientFallback implements BCBClient {

    private static final Logger logger = LoggerFactory.getLogger(BCBClientFallback.class);

    @Override
    public BCBCurrencyDTO getCotacaoMoedaDia(String moeda, String dataCotacao, String format) {
        logger.warn("BCBClient fallback acionado para moeda={} data={} - API do BCB indisponível",
                moeda, dataCotacao);
        return new BCBCurrencyDTO(java.util.Collections.emptyList());
    }
}
