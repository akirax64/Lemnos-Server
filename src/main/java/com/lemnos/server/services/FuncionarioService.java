package com.lemnos.server.services;

import com.lemnos.server.exceptions.auth.TokenNotValidOrExpiredException;
import com.lemnos.server.exceptions.cadastro.CadastroCpfAlreadyInUseException;
import com.lemnos.server.exceptions.entidades.funcionario.FuncionarioNotFoundException;
import com.lemnos.server.exceptions.global.UpdateNotValidException;
import com.lemnos.server.models.SpecificationBuilder;
import com.lemnos.server.models.dtos.requests.FuncionarioFiltroRequest;
import com.lemnos.server.models.dtos.requests.FuncionarioRequest;
import com.lemnos.server.models.dtos.responses.EnderecoResponse;
import com.lemnos.server.models.entidades.FuncionarioSpecification;
import com.lemnos.server.models.enums.Codigo;
import com.lemnos.server.models.enums.Situacao;
import com.lemnos.server.models.entidades.Funcionario;
import com.lemnos.server.models.dtos.responses.FuncionarioResponse;
import com.lemnos.server.repositories.cadastro.CadastroRepository;
import com.lemnos.server.repositories.entidades.FuncionarioRepository;
import com.lemnos.server.utils.Util;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FuncionarioService extends Util {
    private final FuncionarioRepository funcionarioRepository;
    private final CadastroRepository cadastroRepository;

    @Cacheable("allFuncionarios")
    public ResponseEntity<List<FuncionarioResponse>> getAll() {
        List<FuncionarioResponse> response = funcionarioRepository.findAll()
                .stream()
                .map(this::getFuncionarioResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<List<FuncionarioResponse>> filterByName(FuncionarioFiltroRequest filtro) {
        Specification<Funcionario> specification = new SpecificationBuilder<Funcionario>()
                .addIf(StringUtils::isNotBlank, filtro.nome(), FuncionarioSpecification::hasNome)
                .build();

        int page = (filtro.page() != null && filtro.page() > 0) ? filtro.page() : 0;
        int size = (filtro.size() != null && filtro.size() > 0) ? filtro.size() : 5;
        Pageable pageable = PageRequest.of(page, size);

        List<FuncionarioResponse> funcionarioResponse = funcionarioRepository.findAll(specification, pageable)
                .stream()
                .map(this::getFuncionarioResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(funcionarioResponse);
    }

    public ResponseEntity<FuncionarioResponse> getOne(JwtAuthenticationToken token) {
        verificarToken(token);
        Funcionario funcionario = getOneFuncionarioByEmail(token.getName());
        return ResponseEntity.ok(getFuncionarioResponse(funcionario));
    }

    public ResponseEntity<FuncionarioResponse> getOneByEmail(String email) {
        Funcionario funcionario = getOneFuncionarioByEmail(email);
        FuncionarioResponse record = getFuncionarioResponse(funcionario);
        return ResponseEntity.ok(record);
    }

    public ResponseEntity<Void> updateFuncionario(String email, FuncionarioRequest funcionarioRequest) {
        Funcionario updatedFuncionario = insertData(email, funcionarioRequest);
        funcionarioRepository.save(updatedFuncionario);

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Void> ativarOuDesativar(List<String> emails) {
        emails.stream()
                .map(this::getOneFuncionarioByEmail)
                .forEach(funcionario -> {
                    Situacao novaSituacao = funcionario.getSituacao().getSituacao().equals(Situacao.ATIVO.getSituacao()) ? Situacao.INATIVO : Situacao.ATIVO;
                    funcionario.setSituacao(novaSituacao);
                    funcionarioRepository.save(funcionario);
                });
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Void> deleteByEmail(String email) {
        Funcionario funcionarioDeletado = getOneFuncionarioByEmail(email);

        if (funcionarioDeletado.getSituacao() == Situacao.ATIVO) {
            funcionarioDeletado.setSituacao(Situacao.INATIVO);
            funcionarioRepository.save(funcionarioDeletado);
        }
        return ResponseEntity.noContent().build();
    }

    private FuncionarioResponse getFuncionarioResponse(Funcionario funcionario) {
        return new FuncionarioResponse(
                funcionario.getNome(),
                funcionario.getDataNascimento(),
                funcionario.getDataAdmissao(),
                funcionario.getTelefone(),
                funcionario.getCpf(),
                funcionario.getCadastro().getEmail(),
                funcionario.getSituacao().toString(),
                getEnderecoRecords(funcionario)
        );
    }

    private void verificarToken(JwtAuthenticationToken token) {
        if (token == null) throw new TokenNotValidOrExpiredException();
    }

    private Funcionario getOneFuncionarioByEmail(String email) {
        return funcionarioRepository.findByCadastro(
                cadastroRepository.findByEmail(email.replace("%40", "@")).orElseThrow(FuncionarioNotFoundException::new)
        ).orElseThrow(FuncionarioNotFoundException::new);
    }

    private static List<EnderecoResponse> getEnderecoRecords(Funcionario funcionario) {
        return funcionario.getEnderecos().stream()
                .map(funcionarioEndereco -> new EnderecoResponse(
                        funcionarioEndereco.getEndereco().getCep(),
                        funcionarioEndereco.getEndereco().getLogradouro(),
                        funcionarioEndereco.getNumeroLogradouro(),
                        funcionarioEndereco.getComplemento(),
                        funcionarioEndereco.getEndereco().getCidade().getCidade(),
                        funcionarioEndereco.getEndereco().getBairro(),
                        funcionarioEndereco.getEndereco().getEstado().getUf()
                ))
                .collect(Collectors.toList());
    }

    private Funcionario insertData(String email, FuncionarioRequest funcionarioEnviado) {
        Funcionario funcionarioEncontrado = getOneFuncionarioByEmail(email);

        Date dataNasc;
        Date dataAdmi;

        if (StringUtils.isBlank(funcionarioEnviado.nome()) && StringUtils.isBlank(funcionarioEnviado.cpf()) && StringUtils.isBlank(funcionarioEnviado.dataAdmissao()) && StringUtils.isBlank(funcionarioEnviado.dataNascimento()) && StringUtils.isBlank(funcionarioEnviado.telefone())) {
            throw new UpdateNotValidException("Funcionário");
        }
        if (StringUtils.isBlank(funcionarioEnviado.nome())) {
            funcionarioEnviado = funcionarioEnviado.setNome(funcionarioEncontrado.getNome());
        }
        if (StringUtils.isBlank(funcionarioEnviado.cpf())) {
            funcionarioEnviado = funcionarioEnviado.setCpf(funcionarioEncontrado.getCpf().toString());
        }
        Long cpf = convertStringToLong(funcionarioEnviado.cpf(), Codigo.CPF);
        if (StringUtils.isBlank(funcionarioEnviado.dataNascimento())) {
            dataNasc = funcionarioEncontrado.getDataNascimento();
        } else {
            dataNasc = convertData(funcionarioEnviado.dataNascimento());
        }
        if (StringUtils.isBlank(funcionarioEnviado.dataAdmissao())) {
            dataAdmi = funcionarioEncontrado.getDataAdmissao();
        } else {
            dataAdmi = convertData(funcionarioEnviado.dataAdmissao());
        }
        if (StringUtils.isBlank(funcionarioEnviado.telefone())) {
            funcionarioEnviado = funcionarioEnviado.setTelefone(funcionarioEncontrado.getTelefone().toString());
        }
        Long telefone = convertStringToLong(funcionarioEnviado.telefone(), Codigo.TELEFONE);

        Optional<Funcionario> funcionarioOptional = funcionarioRepository.findByCpf(cpf);
        if (funcionarioOptional.isPresent() && !Objects.equals(funcionarioOptional.get().getId(), funcionarioEncontrado.getId()))
            throw new CadastroCpfAlreadyInUseException();

        Funcionario updatedFuncionario = new Funcionario();
        updatedFuncionario.setId(funcionarioEncontrado.getId());
        updatedFuncionario.setNome(funcionarioEnviado.nome());
        updatedFuncionario.setCpf(cpf);
        updatedFuncionario.setDataNascimento(dataNasc);
        updatedFuncionario.setDataAdmissao(dataAdmi);
        updatedFuncionario.setTelefone(telefone);
        updatedFuncionario.setCadastro(funcionarioEncontrado.getCadastro());
        updatedFuncionario.setEnderecos(funcionarioEncontrado.getEnderecos());
        updatedFuncionario.setRole(funcionarioEncontrado.getRole());

        return updatedFuncionario;
    }
}