package br.com.conde.exclusao;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A VARREDURA DE TODOS OS ENTRELAÇAMENTOS POSSÍVEIS.
 *
 * <p>Não há sorteio, não há repetição de teste e não há thread de verdade. A varredura constrói
 * o grafo inteiro dos estados alcançáveis: de cada estado, cada processo que pode dar um passo
 * dá, e cada resultado vira um nó. O que o grafo não contém não acontece, e o que ele contém
 * acontece em alguma execução real.
 *
 * <p>É isso que separa este repositório de rodar duas threads num laço e torcer: um teste com
 * threads que passa mil vezes não provou nada sobre a milésima primeira. Aqui o domínio é
 * finito e a resposta é fechada.
 */
public final class Varredura {

    private Varredura() {}

    /**
     * QUANTAS VOLTAS CADA PROCESSO PODE COMPLETAR.
     *
     * <p>Sem um teto o grafo é infinito: a padaria, por exemplo, tira senhas que crescem para
     * sempre. Duas voltas bastam para todo defeito deste repositório aparecer, e o teto está
     * aqui em vez de escondido em cada protocolo, para ser o mesmo para todos.
     */
    public static final int VOLTAS = 2;

    /** O grafo pronto, com o caminho de volta ao início guardado para contar a história. */
    public static final class Grafo {

        public final Protocolo protocolo;
        public final List<Estado> estados;
        public final List<int[]> saidas;
        public final int[] anterior;
        public final int[] processoDoPasso;

        Grafo(Protocolo protocolo, List<Estado> estados, List<int[]> saidas, int[] anterior,
                int[] processoDoPasso) {
            this.protocolo = protocolo;
            this.estados = estados;
            this.saidas = saidas;
            this.anterior = anterior;
            this.processoDoPasso = processoDoPasso;
        }

        public int tamanho() {
            return estados.size();
        }

        public long arestas() {
            long total = 0;

            for (int[] saida : saidas) total += saida.length;

            return total;
        }
    }

    /** O estado inicial: os dois na seção não crítica, memória zerada, nenhuma volta dada. */
    public static Estado inicial(Protocolo protocolo) {
        return new Estado(new int[Protocolo.PROCESSOS], protocolo.memoriaInicial(),
                new int[Protocolo.PROCESSOS]);
    }

    /**
     * OS SUCESSORES DE UM ESTADO: um por processo que pode avançar.
     *
     * <p>Na seção não crítica o processo tem <b>duas</b> escolhas, e as duas entram no grafo:
     * querer entrar, ou nunca mais querer. A segunda não é detalhe. É ela que revela o defeito
     * da alternância estrita, onde quem parou de querer entrar prende quem ainda quer.
     */
    public static List<Estado> sucessores(Protocolo protocolo, Estado atual) {
        List<Estado> saida = new ArrayList<>();

        for (int p = 0; p < Protocolo.PROCESSOS; p++) {
            int pc = atual.pc[p];

            if (pc == Estado.PARADO) continue;

            if (pc == 0) {
                saida.add(com(atual, p, 1));
                saida.add(com(atual, p, Estado.PARADO));

                continue;
            }

            Estado seguinte = protocolo.passo(atual, p);

            if (seguinte == atual) {
                saida.add(atual);

                continue;
            }

            saida.add(contarVolta(seguinte, p, pc));
        }

        return saida;
    }

    private static Estado com(Estado atual, int p, int novoPc) {
        int[] pc = atual.pc.clone();

        pc[p] = novoPc;

        return new Estado(pc, atual.mem, atual.voltas);
    }

    /** Voltar à seção não crítica conta uma volta, e a última volta encerra o processo. */
    private static Estado contarVolta(Estado seguinte, int p, int pcAnterior) {
        if (seguinte.pc[p] != 0 || pcAnterior == 0) return seguinte;

        int[] voltas = seguinte.voltas.clone();
        int[] pc = seguinte.pc.clone();

        voltas[p]++;

        if (voltas[p] >= VOLTAS) pc[p] = Estado.PARADO;

        return new Estado(pc, seguinte.mem, voltas);
    }

    /** Constrói o grafo inteiro, em largura, a partir do estado inicial. */
    public static Grafo explorar(Protocolo protocolo) {
        List<Estado> estados = new ArrayList<>();
        Map<Estado, Integer> indice = new HashMap<>();
        List<int[]> saidas = new ArrayList<>();
        List<Integer> anterior = new ArrayList<>();
        List<Integer> processoDoPasso = new ArrayList<>();

        Estado raiz = inicial(protocolo);

        estados.add(raiz);
        indice.put(raiz, 0);
        saidas.add(null);
        anterior.add(-1);
        processoDoPasso.add(-1);

        Deque<Integer> fila = new ArrayDeque<>();

        fila.add(0);

        while (!fila.isEmpty()) {
            int atual = fila.remove();

            List<Estado> seguintes = sucessores(protocolo, estados.get(atual));

            int[] destino = new int[seguintes.size()];

            for (int i = 0; i < seguintes.size(); i++) {
                Estado e = seguintes.get(i);

                Integer ja = indice.get(e);

                if (ja == null) {
                    ja = estados.size();

                    estados.add(e);
                    indice.put(e, ja);
                    saidas.add(null);
                    anterior.add(atual);
                    processoDoPasso.add(quemAndou(estados.get(atual), e));

                    fila.add(ja);
                }

                destino[i] = ja;
            }

            saidas.set(atual, destino);
        }

        return new Grafo(protocolo, estados, saidas, inteiros(anterior), inteiros(processoDoPasso));
    }

    private static int quemAndou(Estado de, Estado para) {
        for (int p = 0; p < Protocolo.PROCESSOS; p++) if (de.pc[p] != para.pc[p]) return p;

        return -1;
    }

    private static int[] inteiros(List<Integer> lista) {
        int[] saida = new int[lista.size()];

        for (int i = 0; i < saida.length; i++) saida[i] = lista.get(i);

        return saida;
    }

    /* ===================== o juiz ===================== */

    /**
     * EXCLUSÃO MÚTUA: existe algum estado alcançável com os dois na seção crítica?
     *
     * <p>Isto é a definição, não um algoritmo. O juiz não sabe o que é bandeira, turno ou senha:
     * ele olha o contador de programa dos dois processos em cada um dos estados alcançáveis.
     */
    public static int violacao(Grafo g) {
        for (int i = 0; i < g.tamanho(); i++) {
            Estado e = g.estados.get(i);

            int dentro = 0;

            for (int p = 0; p < Protocolo.PROCESSOS; p++)
                if (g.protocolo.naCritica(e.pc[p])) dentro++;

            if (dentro > 1) return i;
        }

        return -1;
    }

    /**
     * ESTADO TRAVADO: os dois querem entrar e nenhum dos dois consegue sair do lugar.
     *
     * <p>Exigir que os <b>dois</b> queiram entrar é o que separa travamento de bloqueio. Um
     * processo sozinho esperando para sempre não está travado por outro: ele está esperando
     * alguém que foi embora, e isso é a terceira pergunta, não esta.
     */
    public static boolean travado(Grafo g, int i) {
        Estado e = g.estados.get(i);

        int querendo = 0;

        for (int p = 0; p < Protocolo.PROCESSOS; p++)
            if (g.protocolo.querEntrar(e.pc[p])) querendo++;

        if (querendo < Protocolo.PROCESSOS) return false;

        for (int destino : g.saidas.get(i)) if (destino != i) return false;

        return true;
    }

    public static int impasse(Grafo g) {
        for (int i = 0; i < g.tamanho(); i++) if (travado(g, i)) return i;

        return -1;
    }

    /**
     * OS ESTADOS DE ONDE O PROCESSO p AINDA CONSEGUE ENTRAR, por busca ao contrário.
     *
     * <p>Começa nos estados em que p está na seção crítica e anda para trás pelas arestas. O que
     * não for alcançado é estado de onde p nunca mais entra, por nenhum caminho.
     */
    public static boolean[] aindaEntra(Grafo g, int p) {
        List<List<Integer>> entradas = new ArrayList<>();

        for (int i = 0; i < g.tamanho(); i++) entradas.add(new ArrayList<>());

        for (int i = 0; i < g.tamanho(); i++)
            for (int destino : g.saidas.get(i)) entradas.get(destino).add(i);

        boolean[] alcanca = new boolean[g.tamanho()];

        Deque<Integer> fila = new ArrayDeque<>();

        for (int i = 0; i < g.tamanho(); i++)
            if (g.protocolo.naCritica(g.estados.get(i).pc[p])) {
                alcanca[i] = true;

                fila.add(i);
            }

        while (!fila.isEmpty()) {
            int atual = fila.remove();

            for (int anterior : entradas.get(atual))
                if (!alcanca[anterior]) {
                    alcanca[anterior] = true;

                    fila.add(anterior);
                }
        }

        return alcanca;
    }

    /**
     * FALHA DE PROGRESSO: um estado em que p quer entrar e, a partir dali, nunca entra.
     *
     * <p>Cobre o travamento e o laço infinito de uma vez só, porque os dois são a mesma coisa
     * vista do grafo: um lugar de onde a seção crítica deixou de ser alcançável.
     */
    public static int semProgresso(Grafo g, int p) {
        boolean[] alcanca = aindaEntra(g, p);

        for (int i = 0; i < g.tamanho(); i++) {
            if (alcanca[i]) continue;

            if (g.protocolo.querEntrar(g.estados.get(i).pc[p])) return i;
        }

        return -1;
    }

    /**
     * GIRO SEM PROGRESSO: o processo p trabalha e não sai do lugar.
     *
     * <p>Procura um ciclo formado só por estados em que p está no protocolo de entrada e só por
     * passos <b>dados por p</b> que mudam alguma coisa. Um ciclo assim é um processo que executa
     * instruções para sempre sem nunca entrar: não é travamento, porque ele não está parado, e
     * é justamente por isso que trocar o travamento por isso não conserta nada.
     *
     * <p>A espera ocupada parada não conta, porque ela é um laço do estado sobre si mesmo e
     * esses laços ficam de fora.
     */
    public static int giroSemEntrar(Grafo g, int p) {
        for (int[] componente : componentes(g, p)) {
            if (componente.length < 2) continue;

            if (justa(g, componente) && anda(g, componente, p)) return componente[0];
        }

        return -1;
    }

    /** Quem deu o passo entre dois estados vizinhos, ou -1 quando nada mudou. */
    public static int quemAnda(Estado de, Estado para) {
        for (int p = 0; p < Protocolo.PROCESSOS; p++) if (de.pc[p] != para.pc[p]) return p;

        return -1;
    }

    /**
     * EXECUÇÃO JUSTA: nenhum processo fica pronto para andar e nunca anda.
     *
     * <p>Sem esta exigência, qualquer protocolo "falha": basta imaginar que um dos processos
     * parou de ser escalonado para sempre, e aí o outro gira sozinho. Isso não é defeito do
     * protocolo, é defeito do escalonador imaginário. Um ciclo só acusa o protocolo quando todo
     * processo que podia andar dentro dele andou.
     */
    private static boolean justa(Grafo g, int[] componente) {
        boolean[] naComponente = new boolean[g.tamanho()];

        for (int i : componente) naComponente[i] = true;

        boolean[] podia = new boolean[Protocolo.PROCESSOS];
        boolean[] andou = new boolean[Protocolo.PROCESSOS];

        for (int i : componente)
            for (int destino : g.saidas.get(i)) {
                if (destino == i) continue;

                int quem = quemAnda(g.estados.get(i), g.estados.get(destino));

                if (quem < 0) continue;

                podia[quem] = true;

                if (naComponente[destino]) andou[quem] = true;
            }

        for (int q = 0; q < Protocolo.PROCESSOS; q++) if (podia[q] && !andou[q]) return false;

        return true;
    }

    private static boolean anda(Grafo g, int[] componente, int p) {
        boolean[] naComponente = new boolean[g.tamanho()];

        for (int i : componente) naComponente[i] = true;

        for (int i : componente)
            for (int destino : g.saidas.get(i)) {
                if (destino == i || !naComponente[destino]) continue;

                if (quemAnda(g.estados.get(i), g.estados.get(destino)) == p) return true;
            }

        return false;
    }

    /**
     * AS COMPONENTES FORTEMENTE CONEXAS do subgrafo em que p está no protocolo de entrada.
     *
     * <p>Uma componente com mais de um estado é um conjunto de configurações que o sistema pode
     * percorrer para sempre sem nunca sair, e sair dali é justamente o que p precisa fazer para
     * entrar. Tarjan, iterativo para a pilha da máquina não ser o limite.
     */
    public static List<int[]> componentes(Grafo g, int p) {
        int n = g.tamanho();

        boolean[] dentro = new boolean[n];

        for (int i = 0; i < n; i++) dentro[i] = g.protocolo.querEntrar(g.estados.get(i).pc[p]);

        int[] ordem = new int[n];
        int[] baixo = new int[n];
        int[] proximo = new int[n];
        int[] pilha = new int[n];
        int[] chamada = new int[n];
        boolean[] naPilha = new boolean[n];

        Arrays.fill(ordem, -1);

        List<int[]> saida = new ArrayList<>();

        int contador = 0;

        for (int raiz = 0; raiz < n; raiz++) {
            if (!dentro[raiz] || ordem[raiz] >= 0) continue;

            int topoPilha = 0;
            int topoChamada = 0;

            ordem[raiz] = baixo[raiz] = contador++;
            proximo[raiz] = 0;
            pilha[topoPilha++] = raiz;
            naPilha[raiz] = true;
            chamada[topoChamada] = raiz;

            while (topoChamada >= 0) {
                int atual = chamada[topoChamada];
                int[] vizinhos = g.saidas.get(atual);

                if (proximo[atual] < vizinhos.length) {
                    int destino = vizinhos[proximo[atual]++];

                    if (destino == atual || !dentro[destino]) continue;

                    if (ordem[destino] < 0) {
                        ordem[destino] = baixo[destino] = contador++;
                        proximo[destino] = 0;
                        pilha[topoPilha++] = destino;
                        naPilha[destino] = true;
                        chamada[++topoChamada] = destino;
                    } else if (naPilha[destino]) {
                        baixo[atual] = Math.min(baixo[atual], ordem[destino]);
                    }

                    continue;
                }

                if (baixo[atual] == ordem[atual]) {
                    List<Integer> componente = new ArrayList<>();

                    int retirado;

                    do {
                        retirado = pilha[--topoPilha];
                        naPilha[retirado] = false;

                        componente.add(retirado);
                    } while (retirado != atual);

                    saida.add(inteiros(componente));
                }

                topoChamada--;

                if (topoChamada >= 0) {
                    int pai = chamada[topoChamada];

                    baixo[pai] = Math.min(baixo[pai], baixo[atual]);
                }
            }
        }

        return saida;
    }

    /* ===================== contar a historia ===================== */

    /** O caminho do início até um estado, em passos. */
    public static int[] caminho(Grafo g, int ate) {
        List<Integer> invertido = new ArrayList<>();

        for (int i = ate; i != -1; i = g.anterior[i]) invertido.add(i);

        int[] saida = new int[invertido.size()];

        for (int i = 0; i < saida.length; i++) saida[i] = invertido.get(saida.length - 1 - i);

        return saida;
    }

    /** O caminho escrito em linhas, para o relatório mostrar a sequência exata. */
    public static List<String> narrar(Grafo g, int[] caminho) {
        List<String> linhas = new ArrayList<>();

        for (int passo = 1; passo < caminho.length; passo++) {
            int de = caminho[passo - 1];
            int para = caminho[passo];

            Estado antes = g.estados.get(de);
            Estado depois = g.estados.get(para);

            int p = g.processoDoPasso[para];

            String movimento = p < 0 ? "   (sem mudanca)"
                    : String.format("p%d: passo %s -> %s", p, nome(antes.pc[p]),
                            nome(depois.pc[p]));

            linhas.add(String.format("%2d. %-22s mem=%s", passo, movimento,
                    Arrays.toString(depois.mem)));
        }

        return linhas;
    }

    private static String nome(int pc) {
        return pc == Estado.PARADO ? "fim" : String.valueOf(pc);
    }
}
