package com.lemnos.server.configurations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lemnos.server.models.dtos.requests.FornecedorRequest;
import com.lemnos.server.models.dtos.requests.ProdutoRequest;
import com.lemnos.server.repositories.entidades.FornecedorRepository;
import com.lemnos.server.repositories.produto.ProdutoRepository;
import com.lemnos.server.services.AuthService;
import com.lemnos.server.services.ProdutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InitialSeeder implements CommandLineRunner {
    private final AuthService authService;
    private final FornecedorRepository fornecedorRepository;
    private final ProdutoService produtoService;
    private final ObjectMapper objectMapper;
    private final ProdutoRepository produtoRepository;

    @Override
    public void run(String... args) throws Exception {
        fornecedorSeeder();
        produtoSeeder();
    }

    private void fornecedorSeeder() throws IOException {
        if (fornecedorRepository.count() > 0) {
            System.out.println("Fornecedores já cadastrados");
            return;
        }

        InputStream jsonStream = getClass().getResourceAsStream("/data/fornecedores.json");
        List<FornecedorRequest> fornecedores = objectMapper.readValue(jsonStream, new TypeReference<>() {});

        for (FornecedorRequest fornecedor : fornecedores) {
            try {
                authService.registerFornecedor(fornecedor);
            }
            catch (Exception e) {
                System.out.println("Falha ao registrar fornecedor: " + fornecedor.nome());
                e.fillInStackTrace();
            }
        }

        System.out.println("Fornecedores inseridos com sucesso!");
    }

    private void produtoSeeder() throws IOException {
        if (produtoRepository.count() > 0) {
            System.out.println("Produtos já cadastrados");
            return;
        }

        InputStream jsonStream = getClass().getResourceAsStream("/data/products.json");
        List<ProdutoRequest> produtos = objectMapper.readValue(jsonStream, new TypeReference<>() {});

        for (ProdutoRequest produto : produtos) {
            try {
                produtoService.register(produto);
            }
            catch (Exception e) {
                System.out.println("Falha ao registrar produto: " + produto.nome());
                e.fillInStackTrace();
            }
        }

        System.out.println("Produtos inseridos com sucesso!");
    }
}
