package br.com.conde.exclusao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Os vereditos: o que a varredura respondeu para cada protocolo, travado contra regressão. */
class MedidasTest {

    @Test
    @DisplayName("um protocolo só deixa os dois entrarem juntos: a trava partida em dois passos")
    void soUmQuebraAExclusao() {
        for (Medidas.Resultado linha : Medidas.todos())
            assertThat(linha.exclui()).as(linha.nome())
                    .isEqualTo(!linha.nome().equals("trava lida e escrita em 2 passos"));
    }

    @Test
    @DisplayName("a mesma trava, com leitura e escrita num passo só, não quebra nunca")
    void aAtomicidadeEADiferenca() {
        assertThat(Medidas.de("trava atomica (1 passo)").exclui()).isTrue();
        assertThat(Medidas.de("trava lida e escrita em 2 passos").exclui()).isFalse();
    }

    @Test
    @DisplayName("o contraexemplo é uma execução de verdade, e termina com os dois dentro")
    void oContraexemploSeSustenta() {
        Protocolo protocolo = new Protocolos.TrocaPartida();

        Varredura.Grafo g = Medidas.grafo(protocolo);

        int onde = Varredura.violacao(g);

        assertThat(onde).isNotNegative();

        int[] caminho = Varredura.caminho(g, onde);

        for (int passo = 1; passo < caminho.length; passo++)
            assertThat(Varredura.sucessores(protocolo, g.estados.get(caminho[passo - 1])))
                    .contains(g.estados.get(caminho[passo]));

        Estado fim = g.estados.get(onde);

        assertThat(protocolo.naCritica(fim.pc[0])).isTrue();
        assertThat(protocolo.naCritica(fim.pc[1])).isTrue();

        assertThat(Medidas.comoQuebra(protocolo)).hasSize(8);
    }

    @Test
    @DisplayName("só a bandeira trava, e trava em 4 estados alcançáveis")
    void aBandeiraSozinhaTrava() {
        assertThat(Medidas.de("so a bandeira").trava()).isTrue();

        assertThat(Medidas.quantosTravados(new Protocolos.SoFlag())).isEqualTo(4);

        for (Medidas.Resultado linha : Medidas.todos())
            if (!linha.nome().equals("so a bandeira"))
                assertThat(linha.trava()).as(linha.nome()).isFalse();
    }

    @Test
    @DisplayName("o recuo tira o travamento inteiro e põe um giro no lugar")
    void oRecuoTrocaUmDefeitoPorOutro() {
        Medidas.Resultado comFlag = Medidas.de("so a bandeira");
        Medidas.Resultado comRecuo = Medidas.de("bandeira com recuo");

        assertThat(comFlag.trava()).isTrue();
        assertThat(comFlag.gira()).isFalse();

        assertThat(comRecuo.trava()).isFalse();
        assertThat(comRecuo.gira()).isTrue();
    }

    @Test
    @DisplayName("a alternância estrita exclui e nunca trava, e mesmo assim prende quem ficou")
    void aAlternanciaPrendeSemTravar() {
        Medidas.Resultado linha = Medidas.de("alternancia estrita");

        assertThat(linha.exclui()).isTrue();
        assertThat(linha.trava()).isFalse();
        assertThat(linha.gira()).isFalse();
        assertThat(linha.bloqueia()).isTrue();

        assertThat(Medidas.comoBloqueia(new Protocolos.Alternancia())).hasSize(6);
    }

    @Test
    @DisplayName("o bloqueio da alternância some se ninguém puder ir embora")
    void semIrEmboraAAlternanciaPassaria() {
        Protocolo protocolo = new Protocolos.Alternancia();

        Varredura.Grafo g = Medidas.grafo(protocolo);

        int onde = Varredura.semProgresso(g, 0);

        assertThat(onde).isNotNegative();

        assertThat(g.estados.get(onde).pc[1]).as("o vizinho foi embora")
                .isEqualTo(Estado.PARADO);
    }

    @Test
    @DisplayName("quatro protocolos passam nas quatro perguntas")
    void osQuatroCorretos() {
        List<String> corretos = Medidas.todos().stream().filter(Medidas.Resultado::certo)
                .map(Medidas.Resultado::nome).toList();

        assertThat(corretos).containsExactlyInAnyOrder("trava atomica (1 passo)", "peterson",
                "dekker", "padaria de lamport");
    }

    @Test
    @DisplayName("o giro exige justiça, e sem ela o dekker seria acusado por engano")
    void oGiroExigeJustica() {
        Protocolo dekker = new Protocolos.Dekker();

        Varredura.Grafo g = Medidas.grafo(dekker);

        assertThat(Varredura.giroSemEntrar(g, 0)).isNegative();

        boolean temCicloDoProcessoSozinho = false;

        for (int[] componente : Varredura.componentes(g, 0))
            if (componente.length > 1) temCicloDoProcessoSozinho = true;

        assertThat(temCicloDoProcessoSozinho)
                .as("o ciclo existe no grafo; o que ele nao e, e justo").isTrue();
    }

    @Test
    @DisplayName("o tamanho de cada grafo é o que foi medido")
    void osGrafosTemOTamanhoMedido() {
        assertThat(Medidas.de("alternancia estrita").estados()).isEqualTo(51);
        assertThat(Medidas.de("so a bandeira").estados()).isEqualTo(153);
        assertThat(Medidas.de("bandeira com recuo").estados()).isEqualTo(201);
        assertThat(Medidas.de("trava lida e escrita em 2 passos").estados()).isEqualTo(353);
        assertThat(Medidas.de("trava atomica (1 passo)").estados()).isEqualTo(105);
        assertThat(Medidas.de("peterson").estados()).isEqualTo(238);
        assertThat(Medidas.de("dekker").estados()).isEqualTo(433);
        assertThat(Medidas.de("padaria de lamport").estados()).isEqualTo(617);
    }

    @Test
    @DisplayName("os estados com os dois dentro não são um acidente isolado")
    void aQuebraAconteceEmVariosEstados() {
        int[] ocupacao = Medidas.ocupacao(new Protocolos.TrocaPartida());

        assertThat(ocupacao[0]).isEqualTo(92);
        assertThat(ocupacao[1]).isEqualTo(4);
    }

    @Test
    @DisplayName("protocolo correto não tem estado com os dois dentro, em nenhum lugar do grafo")
    void osCorretosNuncaDeixamOsDoisEntrarem() {
        for (Protocolo protocolo : Protocolos.todos()) {
            if (!Medidas.de(protocolo.nome()).exclui()) continue;

            assertThat(Medidas.ocupacao(protocolo)[1]).as(protocolo.nome()).isZero();
        }
    }
}
