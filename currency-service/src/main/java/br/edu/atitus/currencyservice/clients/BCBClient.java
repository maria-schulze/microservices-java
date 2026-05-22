package br.edu.atitus.currencyservice.clients;

import br.edu.atitus.currencyservice.dtos.BCBCurrencyDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "bcb-client",
        url = "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/CotacaoMoedaDia(moeda=@moeda,dataCotacao=@dataCotacao)",
        fallback = BCBClientFallback.class
)
public interface BCBClient {

    @GetMapping("/CotacaoMoedaDia(moeda=@moeda,dataCotacao=@dataCotacao)")
    BCBCurrencyDTO getCotacaoMoedaDia(
            @RequestParam("@moeda") String moeda,
            @RequestParam("@dataCotacao") String dataCotacao,
            @RequestParam("$format") String format
    );
}