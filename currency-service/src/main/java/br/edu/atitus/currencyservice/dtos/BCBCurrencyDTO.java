package br.edu.atitus.currencyservice.dtos;

import java.util.List;

public record BCBCurrencyDTO(List<BCBCotacaoDTO> value) {

    public record BCBCotacaoDTO(
            Double cotacaoCompra,
            Double cotacaoVenda,
            String dataHoraCotacao
    ) {}
}
