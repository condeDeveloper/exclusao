# exclusao

Oito protocolos de exclusão mútua verificados pela **varredura de todos os entrelaçamentos
possíveis**, não por rodar threads num laço e torcer.

Um teste com threads que passa mil vezes não disse nada sobre a milésima primeira. Aqui cada
algoritmo vira uma máquina de passos atômicos, a varredura constrói o grafo inteiro dos estados
alcançáveis, e a resposta é fechada: **o que o grafo não contém não acontece**.

```
mvn -q exec:java -Dexec.mainClass=br.com.conde.exclusao.Medidor
```

## A decisão que faz o resto funcionar

Cada linha do algoritmo é um passo numerado, e um passo é a unidade que não pode ser partida.
Escrever assim deixa a atomicidade **visível**, em vez de escondida na linguagem. É isso que
permite pôr lado a lado dois protocolos que são o mesmo algoritmo palavra por palavra, mudando
só quantos passos uma operação ocupa, e medir o que essa diferença faz.

São dois processos, que é onde moram os algoritmos clássicos. O teto de **2 voltas** por
processo existe porque sem ele o grafo é infinito: a padaria tira senhas que crescem para
sempre. Duas voltas bastam para todo defeito deste repositório aparecer.

## O juiz

Quatro perguntas, respondidas sobre o grafo inteiro. Nenhuma delas olha o código do protocolo:
todas olham os estados alcançáveis.

- **exclusão**: existe estado alcançável com os dois na seção crítica?
- **travamento**: existe estado em que os dois querem entrar e nenhum sai do lugar?
- **bloqueio**: existe estado de onde um processo nunca mais entra, por caminho nenhum?
- **giro**: existe ciclo em que um processo anda para sempre, no protocolo de entrada, sem
  nunca entrar?

## 1. As quatro perguntas, respondidas

| protocolo | estados | exclusão | travamento | bloqueio | giro |
|---|---|---|---|---|---|
| alternância estrita | 51 | ok | ok | **FALHA** | ok |
| só a bandeira | 153 | ok | **FALHA** | **FALHA** | ok |
| bandeira com recuo | 201 | ok | ok | ok | **FALHA** |
| trava lida e escrita em 2 passos | 353 | **FALHA** | ok | ok | ok |
| trava atômica (1 passo) | 105 | ok | ok | ok | ok |
| peterson | 238 | ok | ok | ok | ok |
| dekker | 433 | ok | ok | ok | ok |
| padaria de lamport | 617 | ok | ok | ok | ok |

**Nenhuma das quatro implica a outra**, e a tabela inteira existe para mostrar isso. Dá para
garantir exclusão e travar. Dá para nunca travar e girar para sempre. E dá para andar sempre,
nunca travar, nunca girar, e mesmo assim deixar os dois entrarem juntos.

## 2. O único que deixa os dois entrarem juntos

Os dois protocolos abaixo são o **mesmo algoritmo**. A única diferença é que um lê a trava e a
marca em dois passos, e o outro faz isso num passo só.

| protocolo | passos | exclusão |
|---|---|---|
| trava atômica (1 passo) | 4 | ok |
| trava lida e escrita em 2 passos | 6 | **FALHA** |

O caminho mais curto até os dois dentro tem **8 passos**:

```
 1. p0: passo 0 -> 1   mem=[0, 0, 0]
 2. p0: passo 1 -> 2   mem=[0, 0, 0]     p0 le a trava: esta livre
 3. p0: passo 2 -> 3   mem=[0, 0, 0]     p0 decide entrar
 4. p1: passo 0 -> 1   mem=[0, 0, 0]
 5. p1: passo 1 -> 2   mem=[0, 0, 0]     p1 le a trava: ainda esta livre
 6. p0: passo 3 -> 4   mem=[1, 0, 0]     p0 marca a trava e ENTRA
 7. p1: passo 2 -> 3   mem=[1, 0, 0]     p1 decide entrar pelo que leu no passo 5
 8. p1: passo 3 -> 4   mem=[1, 0, 0]     p1 marca de novo e ENTRA JUNTO
```

A janela é o intervalo entre o passo 5 e o passo 6: p1 já leu, p0 ainda não escreveu.

São **4 estados em 353** com os dois dentro, contra 92 com um só. E é exatamente por serem
poucos que um teste com threads não acha: a falha mora em 4 configurações, e nada garante que
o escalonador vá produzir justamente uma delas enquanto você observa.

## 3. A alternância estrita exclui, nunca trava, e mesmo assim prende

| exclusão | travamento | bloqueio |
|---|---|---|
| ok | ok | **FALHA** |

O caminho até o processo p0 não poder mais entrar tem **6 passos**:

```
 1. p0: passo 0 -> 1   mem=[0]
 2. p0: passo 1 -> 2   mem=[0]    p0 entra (a vez era dele)
 3. p0: passo 2 -> 3   mem=[0]
 4. p0: passo 3 -> 0   mem=[1]    p0 sai e passa a vez para p1
 5. p0: passo 0 -> 1   mem=[1]    p0 quer entrar de novo
 6. p1: passo 0 -> fim mem=[1]    p1 vai embora, e leva a vez junto
```

Quem ficou cumpre o protocolo direitinho e espera para sempre uma vez que nunca mais chega.

E aqui está o ponto que vale mais que a tabela: **o modelo decide o resultado**. A seção não
crítica tem duas saídas no grafo, querer entrar de novo e nunca mais querer. Sem a segunda,
este protocolo passaria em todas as quatro perguntas. O defeito da alternância estrita não está
no código dela, está na suposição de que todo mundo sempre volta.

## 4. Tirar o travamento não é o mesmo que resolver

| protocolo | travamento | giro | estados |
|---|---|---|---|
| só a bandeira | **FALHA** | ok | 153 |
| bandeira com recuo | ok | **FALHA** | 201 |

A bandeira sozinha trava em **4 estados alcançáveis**: os dois levantaram a bandeira antes de
olhar a do outro, e agora nenhum dos dois abaixa.

Abaixar a bandeira e tentar de novo tira o travamento **inteiro**, e é a correção que todo
mundo escreve na primeira tentativa. Em troca, os dois passam a poder levantar e abaixar a
bandeira em sincronia para sempre, sem nunca entrar.

A diferença entre os dois defeitos não é de grau. No travamento o processo está parado; no giro
ele está trabalhando. Um monitor de carga de CPU mostra o segundo como se estivesse tudo bem.

## 5. O custo de verificar

| protocolo | passos | estados | transições |
|---|---|---|---|
| alternância estrita | 4 | 51 | 95 |
| só a bandeira | 5 | 153 | 280 |
| bandeira com recuo | 6 | 201 | 372 |
| trava lida e escrita em 2 passos | 6 | 353 | 660 |
| trava atômica (1 passo) | 4 | 105 | 188 |
| peterson | 6 | 238 | 436 |
| dekker | 10 | 433 | 810 |
| padaria de lamport | 8 | 617 | 1.156 |

O grafo cresce com o número de passos do protocolo, não com o número de execuções imaginadas.
O protocolo inteiro mais caro de verificar aqui cabe em 617 estados: é por isso que a resposta
é fechada e a varredura termina.

## O que as medidas me corrigiram

**"Qualquer ciclo em que o processo anda sem entrar é um defeito."** A primeira versão do
detector de giro acusou o **Dekker**, que está correto desde 1962. O ciclo existe mesmo no
grafo, e o teste `oGiroExigeJustica` confirma que ele existe: o que ele não é, é percorrível de
verdade. Ele só se sustenta se o outro processo nunca mais for escalonado, e isso não é defeito
do protocolo, é defeito do escalonador imaginário. A correção foi exigir **justiça**: um ciclo
só acusa quando todo processo que podia andar dentro dele andou. Com a exigência, o Dekker
ficou limpo e a bandeira com recuo continuou acusada, que é o resultado certo dos dois lados.

**"Travamento é quando alguém espera para sempre."** Com essa definição a alternância estrita
aparecia travando, e travar é justamente o defeito que ela não tem. São coisas diferentes:
travamento é dois processos se segurando; bloqueio é um processo esperando alguém que não
volta. Separar as duas perguntas foi o que fez a primeira tabela ficar legível.

**Tentei medir espera limitada e não consegui.** Queria uma quinta coluna com quantas vezes o
vizinho passa na frente de quem está esperando. Fiz três versões. Contar as voltas do vizinho
num estado em que o processo quer entrar empatou todo mundo no teto, porque contava voltas
dadas **antes** do pedido. Contar a partir do pedido empatou de novo no teto, porque sem
justiça o vizinho passa quantas vezes quiser se o processo nunca for escalonado, que é o mesmo
erro do giro aparecendo de outro jeito. Contar só enquanto o processo está genuinamente preso
parou de separar a trava atômica, porque ela libera por um instante entre uma volta e outra.
As três mediam coisas diferentes do que eu dizia que mediam, então a medida **saiu do
repositório** em vez de virar uma tabela. Espera limitada precisa de raciocínio sobre execuções
infinitas justas com contagem, e isso é outro problema, não uma coluna a mais nesta.

**"Dois processos entrarem na seção crítica juntos é azar raro."** São 4 estados em 353, e o
caminho mais curto até um deles tem 8 passos a partir do início. Raro e improvável não são a
mesma palavra: a sequência é curta, e o que falta para ela acontecer é só o escalonador
escolher assim uma vez.

**"É só tirar o travamento."** A bandeira com recuo tira os 4 estados travados e não deixa
nenhum. Eu teria publicado isso como conserto se a quarta pergunta não existisse.

## Como está organizado

- `Estado.java` - um instante do sistema inteiro, imutável e com igualdade por conteúdo.
- `Protocolo.java` - o contrato: um algoritmo escrito como passos atômicos numerados.
- `Protocolos.java` - os oito, os que funcionam e os que não funcionam.
- `Varredura.java` - o grafo, as quatro perguntas e as componentes fortemente conexas que a
  pergunta do giro usa.
- `Medidas.java` - os vereditos e os contraexemplos.
- `Medidor.java` - imprime as tabelas deste README.
- `VarreduraTest.java` - o motor e o juiz, incluindo o caminho reconstruído sendo reexecutado.
- `MedidasTest.java` - os vereditos dos oito, travados contra regressão.
