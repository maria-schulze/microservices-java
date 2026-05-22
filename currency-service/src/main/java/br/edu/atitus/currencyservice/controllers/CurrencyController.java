package br.edu.atitus.currencyservice.controllers;

import br.edu.atitus.currencyservice.clients.BCBClient;
import br.edu.atitus.currencyservice.dtos.BCBCurrencyDTO;
import br.edu.atitus.currencyservice.dtos.CurrencyDTO;
import br.edu.atitus.currencyservice.entities.CurrencyEntity;
import br.edu.atitus.currencyservice.repositories.CurrencyRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@RestController
@RequestMapping("currency")
public class CurrencyController {

    private final CurrencyRepository repository;
    private final BCBClient bcbClient;

    @Value("${server.port}")
    private String port;

    public CurrencyController(CurrencyRepository repository, BCBClient bcbClient) {
        this.repository = repository;
        this.bcbClient = bcbClient;
    }

    @GetMapping("/convert")
    @Cacheable(value = "currencies", key = "#source + '-' + #target")
    @CircuitBreaker(name = "bcb-client", fallbackMethod = "getConvertFallback")
    @Retry(name = "bcb-client")
    public ResponseEntity<CurrencyDTO> getConvert(
            @RequestParam String source,
            @RequestParam String target) throws Exception {

        source = source.toUpperCase();
        target = target.toUpperCase();

        String environment = "Currency Service running on port " + port;

        // 1. Tenta buscar taxa de conversão no banco de dados local
        Optional<CurrencyEntity> currencyOpt =
                repository.findBySourceCurrencyAndTargetCurrency(source, target);

        if (currencyOpt.isPresent()) {
            CurrencyEntity currency = currencyOpt.get();
            return ResponseEntity.ok(new CurrencyDTO(
                    currency.getSourceCurrency(),
                    currency.getTargetCurrency(),
                    currency.getConversionRate(),
                    environment
            ));
        }

        // 2. Não encontrado localmente — consulta API do Banco Central do Brasil (PTAX)
        String dataCotacao = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));

        BCBCurrencyDTO bcbResponse = bcbClient.getCotacaoMoedaDia(
                "'" + source + "'",
                "'" + dataCotacao + "'",
                "json"
        );

        if (bcbResponse == null
                || bcbResponse.value() == null
                || bcbResponse.value().isEmpty()) {
            throw new Exception("Currency not found: " + source + " -> " + target);
        }

        BCBCurrencyDTO.BCBCotacaoDTO cotacao = bcbResponse.value().get(0);
        Double conversionRate = cotacao.cotacaoVenda();

        return ResponseEntity.ok(new CurrencyDTO(
                source,
                target,
                conversionRate,
                environment + " - Source: BCB API"
        ));
    }

    public ResponseEntity<CurrencyDTO> getConvertFallback(
            String source, String target, Throwable t) {
        return ResponseEntity.ok(new CurrencyDTO(
                source,
                target,
                1.0,
                "Currency Service - fallback (BCB API unavailable): " + t.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        String message = e.getMessage().replace("/r/n", "");
        return ResponseEntity.badRequest().body(message);
    }
}
