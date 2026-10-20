package br.com.conde.exclusao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AS MEDIDAS: cada protocolo varrido inteiro, julgado pelas quatro perguntas.
 *
 * <p>As quatro são independentes e nenhuma implica a outra, o que é metade do assunto. Dá para
 * garantir exclusão e travar; dá para não travar e girar para sempre; e dá para andar sempre e
 * deixar dois processos entrarem juntos.
 */
public final class Medidas {

    private Medidas() {}

    /**
     * O veredito de um protocolo. Cada campo de defeito guarda o estado em que ele aparece, ou
     * -1 quando não aparece, para o relatório poder contar a sequência exata.
     */
    public record Resultado(String nome, int passos, int estados, long arestas, int violacao,
            int impasse, int bloqueio, int bloqueioDe, int giro, int giroDe) {

        public boolean exclui() {
            return violacao < 0;
        }

        public boolean trava() {
            return impasse >= 0;
        }

        public boolean bloqueia() {
            return bloqueio >= 0;
        }

        public boolean gira() {
            return giro >= 0;
        }

        public boolean certo() {
            return exclui() && !trava() && !bloqueia() && !gira();
        }
    }

    private static final Map<String, Varredura.Grafo> grafos = new LinkedHashMap<>();

    private static List<Resultado> resultados;

    public static Varredura.Grafo grafo(Protocolo protocolo) {
        return grafos.computeIfAbsent(protocolo.nome(), chave -> Varredura.explorar(protocolo));
    }

    public static Resultado medir(Protocolo protocolo) {
        Varredura.Grafo g = grafo(protocolo);

        int bloqueio = -1;
        int bloqueioDe = -1;
        int giro = -1;
        int giroDe = -1;

        for (int p = 0; p < Protocolo.PROCESSOS && bloqueio < 0; p++) {
            int achado = Varredura.semProgresso(g, p);

            if (achado >= 0) {
                bloqueio = achado;
                bloqueioDe = p;
            }
        }

        for (int p = 0; p < Protocolo.PROCESSOS && giro < 0; p++) {
            int achado = Varredura.giroSemEntrar(g, p);

            if (achado >= 0) {
                giro = achado;
                giroDe = p;
            }
        }

        return new Resultado(protocolo.nome(), protocolo.passos(), g.tamanho(), g.arestas(),
                Varredura.violacao(g), Varredura.impasse(g), bloqueio, bloqueioDe, giro, giroDe);
    }

    public static List<Resultado> todos() {
        if (resultados != null) return resultados;

        List<Resultado> saida = new ArrayList<>();

        for (Protocolo protocolo : Protocolos.todos()) saida.add(medir(protocolo));

        resultados = List.copyOf(saida);

        return resultados;
    }

    public static Resultado de(String nome) {
        for (Resultado resultado : todos()) if (resultado.nome().equals(nome)) return resultado;

        throw new IllegalArgumentException("protocolo desconhecido: " + nome);
    }

    /** A sequência de passos que leva os dois à seção crítica ao mesmo tempo. */
    public static List<String> comoQuebra(Protocolo protocolo) {
        Varredura.Grafo g = grafo(protocolo);

        int onde = Varredura.violacao(g);

        if (onde < 0) return List.of();

        return Varredura.narrar(g, Varredura.caminho(g, onde));
    }

    /** A sequência de passos que leva ao estado de onde um processo não entra mais. */
    public static List<String> comoBloqueia(Protocolo protocolo) {
        Resultado resultado = medir(protocolo);

        if (!resultado.bloqueia()) return List.of();

        Varredura.Grafo g = grafo(protocolo);

        return Varredura.narrar(g, Varredura.caminho(g, resultado.bloqueio()));
    }

    /** Quantos estados do grafo são travamento, e não só o primeiro deles. */
    public static int quantosTravados(Protocolo protocolo) {
        Varredura.Grafo g = grafo(protocolo);

        int total = 0;

        for (int i = 0; i < g.tamanho(); i++) if (Varredura.travado(g, i)) total++;

        return total;
    }

    /** Quantos estados têm alguém na seção crítica, e quantos têm os dois. */
    public static int[] ocupacao(Protocolo protocolo) {
        Varredura.Grafo g = grafo(protocolo);

        int umDentro = 0;
        int doisDentro = 0;

        for (Estado e : g.estados) {
            int dentro = 0;

            for (int p = 0; p < Protocolo.PROCESSOS; p++) if (protocolo.naCritica(e.pc[p])) dentro++;

            if (dentro == 1) umDentro++;
            if (dentro == 2) doisDentro++;
        }

        return new int[] {umDentro, doisDentro};
    }
}
