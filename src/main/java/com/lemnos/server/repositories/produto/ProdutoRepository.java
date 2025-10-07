package com.lemnos.server.repositories.produto;

import com.lemnos.server.models.produto.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, UUID>, JpaSpecificationExecutor<Produto> {
    List<Produto> findByDescontoValorDescontoNot(String porcentagemDesconto);
}
