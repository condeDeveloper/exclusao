package br.com.conde.exclusao;

import java.util.List;

/** Imprime as tabelas que o README publica. */
public final class Medidor {

    private Medidor() {}

    public static void main(String[] argumentos) {
        String qual = argumentos.length > 0 ? argumentos[0] : "tudo";

        if (qual.equals("tudo") || qual.equals("veredito")) veredito();
        if (qual.equals("tudo") || qual.equals("quebra")) quebra();
        if (qual.equals("tudo") || qual.equals("bloqueio")) bloqueio();
        if (qual.equals("tudo") || qual.equals("giro")) giro();
        if (qual.equals("tudo") || qual.equals("custo")) custo();
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println(texto);
        System.out.println("=".repeat(texto.length()));
        System.out.println();
    }

    private static String sinal(boolean ruim) {
        return ruim ? "FALHA" : "ok";
    }

    private static void veredito() {
        titulo("1. as quatro perguntas, respondidas pela varredura inteira");

        System.out.printf("%-34s %8s %10s %10s %10s %10s%n", "protocolo", "estados", "exclusao",
                "travamento", "bloqueio", "giro");

        for (Medidas.Resultado linha : Medidas.todos())
            System.out.printf("%-34s %8d %10s %10s %10s %10s%n", linha.nome(), linha.estados(),
                    sinal(!linha.exclui()), sinal(linha.trava()), sinal(linha.bloqueia()),
                    sinal(linha.gira()));

        System.out.println();
        System.out.println("exclusao   : existe estado alcancavel com os dois na secao critica?");
        System.out.println("travamento : existe estado de onde ninguem mais sai do lugar?");
        System.out.println("bloqueio   : existe estado de onde um processo nunca mais entra?");
        System.out.println("giro       : existe ciclo em que um processo anda e nunca entra?");

        System.out.println();
        System.out.println("nenhuma das quatro implica a outra, e a tabela mostra isso: ha");
        System.out.println("protocolo que exclui e trava, que nao trava e gira, e que anda");
        System.out.println("sempre e deixa os dois entrarem juntos.");
    }

    private static void quebra() {
        titulo("2. o unico que deixa os dois entrarem juntos, e a sequencia exata");

        Protocolo partida = new Protocolos.TrocaPartida();
        Protocolo atomica = new Protocolos.TrocaAtomica();

        Medidas.Resultado comPartida = Medidas.medir(partida);
        Medidas.Resultado comAtomica = Medidas.medir(atomica);

        System.out.println("os dois protocolos sao o MESMO algoritmo. a unica diferenca e que um");
        System.out.println("le a trava e a marca em dois passos, e o outro faz isso num passo.");
        System.out.println();

        System.out.printf("%-34s %8s %10s%n", "protocolo", "passos", "exclusao");
        System.out.printf("%-34s %8d %10s%n", comAtomica.nome(), comAtomica.passos(),
                sinal(!comAtomica.exclui()));
        System.out.printf("%-34s %8d %10s%n", comPartida.nome(), comPartida.passos(),
                sinal(!comPartida.exclui()));

        List<String> passos = Medidas.comoQuebra(partida);

        System.out.println();
        System.out.println("o caminho mais curto ate os dois dentro tem " + passos.size()
                + " passos:");
        System.out.println();

        for (String linha : passos) System.out.println("  " + linha);

        int[] ocupacao = Medidas.ocupacao(partida);

        System.out.println();
        System.out.printf("estados com um dentro: %d. estados com OS DOIS dentro: %d, de %d.%n",
                ocupacao[0], ocupacao[1], comPartida.estados());
        System.out.println("sao poucos estados, e e exatamente por isso que testar com threads");
        System.out.printf("nao acha: a falha mora em %d configuracoes de %d, e bastam %d passos%n",
                ocupacao[1], comPartida.estados(), passos.size());
        System.out.println("para chegar numa delas a partir do inicio.");
    }

    private static void bloqueio() {
        titulo("3. a alternancia estrita exclui, nunca trava, e mesmo assim prende");

        Protocolo alternancia = new Protocolos.Alternancia();

        Medidas.Resultado resultado = Medidas.medir(alternancia);

        System.out.printf("exclusao: %s, travamento: %s, bloqueio: %s%n",
                sinal(!resultado.exclui()), sinal(resultado.trava()), sinal(resultado.bloqueia()));

        List<String> passos = Medidas.comoBloqueia(alternancia);

        System.out.println();
        System.out.println("o caminho ate o processo p" + resultado.bloqueioDe()
                + " nao poder mais entrar tem " + passos.size() + " passos:");
        System.out.println();

        for (String linha : passos) System.out.println("  " + linha);

        System.out.println();
        System.out.println("o outro processo foi embora na secao nao critica, e levou a vez");
        System.out.println("junto. quem ficou cumpre o protocolo direitinho e espera para");
        System.out.println("sempre uma vez que nunca mais chega.");

        System.out.println();
        System.out.println("por isso a secao nao critica tem DUAS saidas no modelo: querer");
        System.out.println("entrar de novo e nunca mais querer. sem a segunda, este protocolo");
        System.out.println("passaria em todas as perguntas.");
    }

    private static void giro() {
        titulo("4. tirar o travamento nao e o mesmo que resolver");

        Protocolo soFlag = new Protocolos.SoFlag();
        Protocolo recuo = new Protocolos.FlagComRecuo();

        Medidas.Resultado comFlag = Medidas.medir(soFlag);
        Medidas.Resultado comRecuo = Medidas.medir(recuo);

        System.out.printf("%-34s %12s %10s %8s%n", "protocolo", "travamento", "giro", "estados");
        System.out.printf("%-34s %12s %10s %8d%n", comFlag.nome(), sinal(comFlag.trava()),
                sinal(comFlag.gira()), comFlag.estados());
        System.out.printf("%-34s %12s %10s %8d%n", comRecuo.nome(), sinal(comRecuo.trava()),
                sinal(comRecuo.gira()), comRecuo.estados());

        System.out.println();
        System.out.printf("a bandeira sozinha trava em %d estados alcancaveis: os dois levantaram%n",
                Medidas.quantosTravados(soFlag));
        System.out.println("a bandeira antes de olhar a do outro, e agora nenhum dos dois abaixa.");

        System.out.println();
        System.out.println("abaixar a bandeira e tentar de novo tira o travamento inteiro, e e");
        System.out.println("a correcao que todo mundo escreve na primeira tentativa. em troca,");
        System.out.println("os dois passam a poder levantar e abaixar a bandeira em sincronia");
        System.out.println("para sempre, sem nunca entrar. o processo nao esta parado: ele esta");
        System.out.println("trabalhando. so nao esta progredindo.");
    }

    private static void custo() {
        titulo("5. o que cada protocolo custa para ser verificado");

        System.out.printf("%-34s %8s %10s %12s%n", "protocolo", "passos", "estados", "transicoes");

        for (Medidas.Resultado linha : Medidas.todos())
            System.out.printf("%-34s %8d %10d %12d%n", linha.nome(), linha.passos(),
                    linha.estados(), linha.arestas());

        System.out.println();
        System.out.println("o grafo cresce com o numero de passos do protocolo, nao com o numero");
        System.out.println("de execucoes imaginadas. e por isso que a resposta e fechada: a");
        System.out.println("varredura termina, e o que ela nao achou nao existe.");

        System.out.println();
        System.out.println("teto de voltas por processo: " + Varredura.VOLTAS);
    }
}
