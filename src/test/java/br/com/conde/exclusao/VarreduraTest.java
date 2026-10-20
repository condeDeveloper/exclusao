package br.com.conde.exclusao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** O motor da varredura e o juiz: o que eles prometem antes de qualquer protocolo entrar. */
class VarreduraTest {

    @Test
    @DisplayName("o estado inicial tem os dois fora, a memória do protocolo e nenhuma volta")
    void inicialEstaZerado() {
        Protocolo protocolo = new Protocolos.Peterson();

        Estado inicial = Varredura.inicial(protocolo);

        assertThat(inicial.pc).containsExactly(0, 0);
        assertThat(inicial.mem).isEqualTo(protocolo.memoriaInicial());
        assertThat(inicial.voltas).containsExactly(0, 0);
    }

    @Test
    @DisplayName("na seção não crítica cada processo tem duas saídas: querer entrar ou ir embora")
    void naoCriticaTemDuasSaidas() {
        Protocolo protocolo = new Protocolos.Peterson();

        List<Estado> seguintes = Varredura.sucessores(protocolo, Varredura.inicial(protocolo));

        assertThat(seguintes).hasSize(4);

        assertThat(seguintes).anyMatch(e -> e.pc[0] == 1).anyMatch(e -> e.pc[0] == Estado.PARADO)
                .anyMatch(e -> e.pc[1] == 1).anyMatch(e -> e.pc[1] == Estado.PARADO);
    }

    @Test
    @DisplayName("um processo parado não dá mais nenhum passo")
    void paradoNaoAnda() {
        Protocolo protocolo = new Protocolos.Peterson();

        Estado parado = new Estado(new int[] {Estado.PARADO, Estado.PARADO},
                protocolo.memoriaInicial(), new int[] {1, 1});

        assertThat(Varredura.sucessores(protocolo, parado)).isEmpty();
    }

    @Test
    @DisplayName("a varredura termina e dá o mesmo grafo toda vez")
    void aVarreduraEDeterminista() {
        Protocolo protocolo = new Protocolos.Dekker();

        Varredura.Grafo primeira = Varredura.explorar(protocolo);
        Varredura.Grafo segunda = Varredura.explorar(protocolo);

        assertThat(primeira.tamanho()).isEqualTo(segunda.tamanho());
        assertThat(primeira.arestas()).isEqualTo(segunda.arestas());
    }

    @Test
    @DisplayName("nenhum processo passa do número de voltas do teto")
    void oTetoDeVoltasVale() {
        for (Protocolo protocolo : Protocolos.todos())
            for (Estado e : Varredura.explorar(protocolo).estados)
                for (int p = 0; p < Protocolo.PROCESSOS; p++)
                    assertThat(e.voltas[p]).as(protocolo.nome())
                            .isBetween(0, Varredura.VOLTAS);
    }

    @Test
    @DisplayName("quem completou o teto de voltas está parado")
    void quemAcabouAsVoltasPara() {
        for (Protocolo protocolo : Protocolos.todos())
            for (Estado e : Varredura.explorar(protocolo).estados)
                for (int p = 0; p < Protocolo.PROCESSOS; p++)
                    if (e.voltas[p] == Varredura.VOLTAS)
                        assertThat(e.pc[p]).as(protocolo.nome()).isEqualTo(Estado.PARADO);
    }

    @Test
    @DisplayName("todo estado do grafo é alcançável pelo caminho que a varredura guardou")
    void oCaminhoGuardadoEValido() {
        Protocolo protocolo = new Protocolos.Padaria();

        Varredura.Grafo g = Varredura.explorar(protocolo);

        for (int alvo : new int[] {1, 7, 50, g.tamanho() - 1}) {
            int[] caminho = Varredura.caminho(g, alvo);

            assertThat(caminho[0]).as("comeca no inicial").isZero();
            assertThat(caminho[caminho.length - 1]).isEqualTo(alvo);

            for (int passo = 1; passo < caminho.length; passo++) {
                List<Estado> seguintes =
                        Varredura.sucessores(protocolo, g.estados.get(caminho[passo - 1]));

                assertThat(seguintes).contains(g.estados.get(caminho[passo]));
            }
        }
    }

    @Test
    @DisplayName("o juiz da exclusão olha o contador de programa, não a memória")
    void oJuizDaExclusaoOlhaOsDois() {
        Protocolo protocolo = new Protocolos.TrocaAtomica();

        Varredura.Grafo g = Varredura.explorar(protocolo);

        assertThat(Varredura.violacao(g)).isNegative();

        for (Estado e : g.estados) {
            int dentro = 0;

            for (int p = 0; p < Protocolo.PROCESSOS; p++) if (protocolo.naCritica(e.pc[p])) dentro++;

            assertThat(dentro).isLessThanOrEqualTo(1);
        }
    }

    @Test
    @DisplayName("as componentes do subgrafo cobrem cada estado de entrada uma vez só")
    void asComponentesParticionamOSubgrafo() {
        Protocolo protocolo = new Protocolos.FlagComRecuo();

        Varredura.Grafo g = Varredura.explorar(protocolo);

        int esperado = 0;

        for (Estado e : g.estados) if (protocolo.querEntrar(e.pc[0])) esperado++;

        int contados = 0;

        for (int[] componente : Varredura.componentes(g, 0)) contados += componente.length;

        assertThat(contados).isEqualTo(esperado);
    }

    @Test
    @DisplayName("quem anda entre dois estados vizinhos é um processo só")
    void umProcessoPorPasso() {
        Protocolo protocolo = new Protocolos.Dekker();

        Varredura.Grafo g = Varredura.explorar(protocolo);

        for (int i = 0; i < g.tamanho(); i++)
            for (int destino : g.saidas.get(i)) {
                if (destino == i) continue;

                Estado de = g.estados.get(i);
                Estado para = g.estados.get(destino);

                int mudaram = 0;

                for (int p = 0; p < Protocolo.PROCESSOS; p++) if (de.pc[p] != para.pc[p]) mudaram++;

                assertThat(mudaram).isEqualTo(1);
            }
    }
}
