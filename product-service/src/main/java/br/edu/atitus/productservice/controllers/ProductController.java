package br.edu.atitus.productservice.controllers;

import br.edu.atitus.productservice.clients.CurrencyClient;
import br.edu.atitus.productservice.clients.CurrencyResponse;
import br.edu.atitus.productservice.dtos.ProductDTO;
import br.edu.atitus.productservice.entities.ProductEntity;
import br.edu.atitus.productservice.repositories.ProductRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("products")
public class ProductController {

    private final ProductRepository repository;
    private final CurrencyClient currencyClient;

    public ProductController(ProductRepository repository, CurrencyClient currencyClient) {
        this.repository = repository;
        this.currencyClient = currencyClient;
    }

    @Value("${server.port}")
    private String port;

    @GetMapping("/{id}")
    @Cacheable(value = "products", key = "#id + '-' + #targetCurrency")
    @CircuitBreaker(name = "currency-service", fallbackMethod = "getProductFallback")
    @Retry(name = "currency-service")
    public ResponseEntity<ProductDTO> getProduct(
            @PathVariable Long id,
            @RequestParam String targetCurrency) throws Exception {
        targetCurrency = targetCurrency.toUpperCase();

        ProductEntity entity = repository.findById(id)
                .orElseThrow(() -> new Exception("Product not found!"));

        Double convertedPrice = null;
        String environment = "Product-service running on port: " + port;
        String requestCurrency = targetCurrency;

        if (targetCurrency.equals(entity.getCurrency())) {
            convertedPrice = entity.getPrice();
        } else {
            CurrencyResponse currency = currencyClient.getCurrency(entity.getCurrency(), targetCurrency);
            convertedPrice = entity.getPrice() * currency.conversionRate();
            environment = environment + " - " + currency.environment();
        }

        ProductDTO dto = new ProductDTO(
                entity.getId(),
                entity.getDescription(),
                entity.getBrand(),
                entity.getModel(),
                entity.getCurrency(),
                entity.getPrice(),
                entity.getStock(),
                convertedPrice,
                environment,
                requestCurrency
        );

        return ResponseEntity.ok(dto);
    }

    public ResponseEntity<ProductDTO> getProductFallback(
            Long id, String targetCurrency, Throwable t) {
        return repository.findById(id).map(entity -> {
            ProductDTO dto = new ProductDTO(
                    entity.getId(),
                    entity.getDescription(),
                    entity.getBrand(),
                    entity.getModel(),
                    entity.getCurrency(),
                    entity.getPrice(),
                    entity.getStock(),
                    entity.getPrice(),
                    "Product-service running on port: " + port + " - fallback (currency-service unavailable)",
                    entity.getCurrency()
            );
            return ResponseEntity.ok(dto);
        }).orElse(ResponseEntity.notFound().build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        String message = e.getMessage().replace("/r/n", "");
        return ResponseEntity.badRequest().body(message);
    }

}
