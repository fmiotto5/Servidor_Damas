package servidor;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Principal extends JFrame implements Runnable{
    private ServerSocket serverSocket;
    private Tabuleiro tabuleiro;
    private Socket socketPlayer1;
    private Socket socketPlayer2;
    ObjectOutputStream outputPlayer1, outputPlayer2;
    ObjectInputStream inputPlayer1, inputPlayer2;
    private boolean gameOver = false;

    public Principal(){
        this.tabuleiro = new Tabuleiro();
    }

    public Principal(Socket socketPlayer1, Socket socketPlayer2, ObjectOutputStream outputPlayer1,ObjectOutputStream outputPlayer2){
        this.socketPlayer1 = socketPlayer1;
        this.socketPlayer2 = socketPlayer2;
        this.outputPlayer1 = outputPlayer1;
        this.outputPlayer2 = outputPlayer2;
    }

    private void iniciaConexao() {
        try {
            servidor.Principal principal = new servidor.Principal();
            principal.criaServerSocket(5000);
            System.out.println("Esperando conexões...");

            socketPlayer1 = principal.esperaConexao();
            Mensagem msg = new Mensagem();
            outputPlayer1 = new ObjectOutputStream(socketPlayer1.getOutputStream());
            msg.setIdJogador(0); //manda mensagem para o primeiro jogador que conectou, avisando que ele é o jogador de id 0
            outputPlayer1.writeObject(msg);
            outputPlayer1.flush();
            System.out.println("Jogador 1 conectou\nEsperando jogador 2...");

            socketPlayer2 = principal.esperaConexao();
            outputPlayer2 = new ObjectOutputStream(socketPlayer2.getOutputStream());
            msg.setIdJogador(1);
            outputPlayer2.writeObject(msg);
            outputPlayer2.flush();
            System.out.println("Jogador 2 conectou");

            Thread t1 = new Thread(new Principal(socketPlayer1,socketPlayer2,outputPlayer1,outputPlayer2));
            t1.setName("Player 1");
            t1.start();
            Thread t2 = new Thread(new Principal(socketPlayer1,socketPlayer2,outputPlayer1,outputPlayer2));
            t2.setName("Player 2");
            t2.start();


            while(!gameOver){

            }
            System.out.println("Jogo finalizado");
        } catch (IOException e) {

        }
    }

    private void criaServerSocket(int porta) {
        try {
            serverSocket = new ServerSocket(porta);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Socket esperaConexao() throws IOException {
        Socket socket = serverSocket.accept();
        return socket;
    }

    private void fechaSocket(Socket socket) throws IOException {
        socket.close();
        gameOver = true;
    }

    public void run(){
        Socket socket;

        ObjectOutputStream output;
        ObjectInputStream input;
        ObjectOutputStream outputAdversario = null;

        if(Thread.currentThread().getName().equals("Player 1")){
            socket = socketPlayer1;
            outputAdversario = outputPlayer2;

            output = outputPlayer1;
        } else{
            socket = socketPlayer2;
            outputAdversario = outputPlayer1;

            output = outputPlayer2;
        }

        try {
            input = new ObjectInputStream(socket.getInputStream());
            while(!gameOver) {

                Mensagem msg = null;

                msg = (Mensagem) input.readObject();
                msg = trataJogada(msg);

                if(msg.getTabuleiro().getQtdeBrancas() == 0){
                    msg.setS(Status.VITORIA_JOGADOR2);
                } else if (msg.getTabuleiro().getQtdePretas() == 0){
                    msg.setS(Status.VITORIA_JOGADOR1);
                }

                output.writeObject(msg);
                output.flush();

                outputAdversario.writeObject(msg);
                outputAdversario.flush();

            }
            input.close();
            output.close();
            gameOver = true;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Cliente desconectado");
            gameOver = true;
        } finally {
            try {
                fechaSocket(socket);
            } catch (IOException e) {
                e.printStackTrace();
                gameOver = true;
            }
        }
    }

    private Mensagem trataJogada(Mensagem m) {
        //verifica a jogada do usuário em relação à situação atual do tabuleiro
        Tabuleiro t = m.getTabuleiro();
        int iOrig, iDest, jOrig, jDest;
        iOrig = m.getiOrig();
        iDest = m.getiDest();
        jOrig = m.getjOrig();
        jDest = m.getjDest();
        Peca p = t.getPeca(iOrig, jOrig);

        if(p == null){
            m.setS(Status.PECA_INEXISTENTE);
            return m;
        }

        if(m.getIdJogador() == 0 && p.getCor().equals(Cor.PRETA)){
            m.setS(Status.PECA_ADVERSARIO);
            return m;
        }

        if(m.getIdJogador() == 1 && p.getCor().equals(Cor.BRANCA)){
            m.setS(Status.PECA_ADVERSARIO);
            return m;
        }

        if(iDest < 0 || iDest > 7 || jDest < 0 || jDest > 7){
            m.setS(Status.JOGADA_INVALIDA);
            return m;
        }

        if(t.tabuleiro[iDest][jDest] != null){
            m.setS(Status.JOGADA_INVALIDA);
            return m;
        }

        ArrayList<int[]> pecasObrigatorias = verificaObrigatoriedadeCaptura(m.getIdJogador(),t); //verifica se há alguma jogada obrigatória

        if(pecasObrigatorias.size() > 0){
            for (int[] pos : pecasObrigatorias){
                if(iDest == pos[0] && jDest == pos[1]){
                    if(iOrig == pos[2] && jOrig == pos[3]) {
                        if(m.getIdJogador() == 0 && iDest == 0)
                            p.setKing(true);
                        else if(m.getIdJogador() == 1 && iDest == 7)
                            p.setKing(true);

                        t.tabuleiro[iOrig][jOrig] = null;
                        t.tabuleiro[iDest][jDest] = p;
                        t.tabuleiro[pos[4]][pos[5]] = null;
                        if(m.getIdJogador() == 0)
                            t.setQtdePretas(t.getQtdePretas() - 1);
                        else
                            t.setQtdeBrancas((t.getQtdeBrancas() - 1));
                        m.setTabuleiro(t);
                        m.setS(Status.OK);
                        return m;
                    }
                }
            }

                m.setS(Status.CAPTURA_OBRIGATORIA);
                return m;
        }

        if(m.getIdJogador() == 0){
            if(p.isKing()){
                if((iDest == (iOrig-1) && (jDest == (jOrig-1) || jDest == (jOrig+1))) || (iDest == (iOrig+1) && (jDest == (jOrig-1) || jDest == (jOrig+1)))) {
                    t.tabuleiro[iOrig][jOrig] = null;
                    t.tabuleiro[iDest][jDest] = p;
                    m.setTabuleiro(t);
                    m.setS(Status.OK);
                    return m;
                }
            }else if(iDest == (iOrig-1) && (jDest == (jOrig-1) || (jDest == (jOrig+1)))){ //se for peça branca, se movimenta diagonalmente para a esquerda ou diagonalmente para a direita
                //se atingiu o lado oposto do tabuleiro, vira rei
                if(iDest == 0)
                    p.setKing(true);

                t.tabuleiro[iOrig][jOrig] = null;
                t.tabuleiro[iDest][jDest] = p;
                m.setTabuleiro(t);
                m.setS(Status.OK);
                return m;
            } else{
                m.setS(Status.JOGADA_INVALIDA);
                return m;
            }
        } else{
            if(p.isKing()){
                if((iDest == (iOrig-1) && (jDest == (jOrig-1) || jDest == (jOrig+1))) || (iDest == (iOrig+1) && (jDest == (jOrig-1) || jDest == (jOrig+1)))) {
                    t.tabuleiro[iOrig][jOrig] = null;
                    t.tabuleiro[iDest][jDest] = p;
                    m.setTabuleiro(t);
                    m.setS(Status.OK);
                    return m;
                }
            }else if(iDest == (iOrig+1) && (jDest == (jOrig-1) || (jDest == (jOrig+1)))){ //se for peça preta, se movimenta diagonalmente para a esquerda ou diagonalmente para a direita
                if(iDest == 7)
                    p.setKing(true);

                t.tabuleiro[iOrig][jOrig] = null;
                t.tabuleiro[iDest][jDest] = p;
                m.setTabuleiro(t);
                m.setS(Status.OK);
                return m;
            } else{
                m.setS(Status.JOGADA_INVALIDA);
                return m;
            }
        }
        return new Mensagem();
    }

    public ArrayList verificaObrigatoriedadeCaptura(int idJogador,Tabuleiro t){
        ArrayList posicoes = new ArrayList<int[]>();
        int[] pos;
        int cont = 0;

        if(idJogador == 0) {
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    if (t.tabuleiro[i][j] != null && (t.tabuleiro[i][j].getCor().equals(Cor.BRANCA))) {
                        if (j > 1 && j < 6 && i > 1) {
                            if(t.tabuleiro[i-1][j-1] != null && t.tabuleiro[i-1][j-1].getCor().equals(Cor.PRETA)){
                                if(t.tabuleiro[i-2][j-2] == null){
                                    pos = new int[6];
                                    pos[0] = i-2; //i destino obrigatório
                                    pos[1] = j-2; //j destino obrigatório
                                    pos[2] = i; //i origem obriagtório
                                    pos[3] = j; //j origem obrigatório
                                    pos[4] = i-1; //i da peça que vai ser removida
                                    pos[5] = j-1; //j da peça que vai ser removida
                                    posicoes.add(pos);
                                }
                            }

                            if(t.tabuleiro[i-1][j+1] != null && t.tabuleiro[i-1][j+1].getCor().equals(Cor.PRETA)){
                                if(t.tabuleiro[i-2][j+2] == null){
                                    pos = new int[6];
                                    pos[0] = i-2;
                                    pos[1] = j+2;
                                    pos[2] = i;
                                    pos[3] = j;
                                    pos[4] = i-1;
                                    pos[5] = j+1;
                                    posicoes.add(pos);
                                }
                            }
                        } else if((j == 0 || j == 1) && i > 1){
                            if(t.tabuleiro[i-1][j+1] != null && t.tabuleiro[i-1][j+1].getCor().equals(Cor.PRETA)){
                                if(t.tabuleiro[i-2][j+2] == null){
                                    pos = new int[6];
                                    pos[0] = i-2;
                                    pos[1] = j+2;
                                    pos[2] = i;
                                    pos[3] = j;
                                    pos[4] = i-1;
                                    pos[5] = j+1;
                                    posicoes.add(pos);
                                }
                            }
                        } else if((j == 6 || j == 7) && i > 1){
                            if(t.tabuleiro[i-1][j-1] != null && t.tabuleiro[i-1][j-1].getCor().equals(Cor.PRETA)){
                                if(t.tabuleiro[i-2][j-2] == null){
                                    pos = new int[6];
                                    pos[0] = i-2;
                                    pos[1] = j-2;
                                    pos[2] = i;
                                    pos[3] = j;
                                    pos[4] = i-1;
                                    pos[5] = j-1;
                                    posicoes.add(pos);
                                }
                            }
                        }

                        if(t.tabuleiro[i][j].isKing()){
                            if (j > 1 && j < 6 && i < 6) {
                                if(t.tabuleiro[i+1][j-1] != null && t.tabuleiro[i+1][j-1].getCor().equals(Cor.PRETA)){
                                    if(t.tabuleiro[i+2][j-2] == null){
                                        pos = new int[6];
                                        pos[0] = i+2;
                                        pos[1] = j-2;
                                        pos[2] = i;
                                        pos[3] = j;
                                        pos[4] = i+1;
                                        pos[5] = j-1;
                                        posicoes.add(pos);
                                    }
                                }

                                if(t.tabuleiro[i+1][j+1] != null && t.tabuleiro[i+1][j+1].getCor().equals(Cor.PRETA)){
                                    if(t.tabuleiro[i+2][j+2] == null){
                                        pos = new int[6];
                                        pos[0] = i+2;
                                        pos[1] = j+2;
                                        pos[2] = i;
                                        pos[3] = j;
                                        pos[4] = i+1;
                                        pos[5] = j+1;
                                        posicoes.add(pos);
                                    }
                                }
                            } else if((j == 0 || j == 1) && i < 5){
                                if(t.tabuleiro[i+1][j+1] != null && t.tabuleiro[i+1][j+1].getCor().equals(Cor.PRETA)){
                                    if(t.tabuleiro[i+2][j+2] == null){
                                        pos = new int[6];
                                        pos[0] = i+2;
                                        pos[1] = j+2;
                                        pos[2] = i;
                                        pos[3] = j;
                                        pos[4] = i+1;
                                        pos[5] = j+1;
                                        posicoes.add(pos);
                                    }
                                }
                            } else if((j == 6 || j == 7) && i < 6){
                                if(t.tabuleiro[i+1][j-1] != null && t.tabuleiro[i+1][j-1].getCor().equals(Cor.PRETA)){
                                    if(t.tabuleiro[i+2][j-2] == null){
                                        pos = new int[6];
                                        pos[0] = i+2;
                                        pos[1] = j-2;
                                        pos[2] = i;
                                        pos[3] = j;
                                        pos[4] = i+1;
                                        pos[5] = j-1;
                                        posicoes.add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else{
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    if (t.tabuleiro[i][j] != null && t.tabuleiro[i][j].getCor().equals(Cor.PRETA)) {
                        if (j > 1 && j < 6 && i < 6) {
                            if(t.tabuleiro[i+1][j-1] != null && t.tabuleiro[i+1][j-1].getCor().equals(Cor.BRANCA)){
                                if(t.tabuleiro[i+2][j-2] == null){
                                    pos = new int[6];
                                    pos[0] = i+2;
                                    pos[1] = j-2;
                                    pos[2] = i;
                                    pos[3] = j;
                                    pos[4] = i+1;
                                    pos[5] = j-1;
                                    posicoes.add(pos);
                                }
                            }

                            if(t.tabuleiro[i+1][j+1] != null && t.tabuleiro[i+1][j+1].getCor().equals(Cor.BRANCA)){
                                if(t.tabuleiro[i+2][j+2] == null){
                                    pos = new int[6];
                                    pos[0] = i+2;
                                    pos[1] = j+2;
                                    pos[2] = i;
                                    pos[3] = j;
                                    pos[4] = i+1;
                                    pos[5] = j+1;
                                    posicoes.add(pos);
                                }
                            }
                        } else if((j == 0 || j == 1) && i < 5){
                            if(t.tabuleiro[i+1][j+1] != null && t.tabuleiro[i+1][j+1].getCor().equals(Cor.BRANCA)){
                                if(t.tabuleiro[i+2][j+2] == null){
                                    pos = new int[6];
                                    pos[0] = i+2;
                                    pos[1] = j+2;
                                    pos[2] = i;
                                    pos[3] = j;
                                    pos[4] = i+1;
                                    pos[5] = j+1;
                                    posicoes.add(pos);
                                }
                            }
                        } else if((j == 6 || j == 7) && i < 6){
                            if(t.tabuleiro[i+1][j-1] != null && t.tabuleiro[i+1][j-1].getCor().equals(Cor.BRANCA)){
                                if(t.tabuleiro[i+2][j-2] == null){
                                    pos = new int[6];
                                    pos[0] = i+2;
                                    pos[1] = j-2;
                                    pos[2] = i;
                                    pos[3] = j;
                                    pos[4] = i+1;
                                    pos[5] = j-1;
                                    posicoes.add(pos);
                                }
                            }
                        }

                        if(t.tabuleiro[i][j].isKing()){
                            if (j > 1 && j < 6 && i > 1) {
                                if(t.tabuleiro[i-1][j-1] != null && t.tabuleiro[i-1][j-1].getCor().equals(Cor.BRANCA)){
                                    if(t.tabuleiro[i-2][j-2] == null){
                                        pos = new int[6];
                                        pos[0] = i-2; //i destino obrigatório
                                        pos[1] = j-2; //j destino obrigatório
                                        pos[2] = i; //i origem obriagtório
                                        pos[3] = j; //j origem obrigatório
                                        pos[4] = i-1; //i da peça que vai ser removida
                                        pos[5] = j-1; //j da peça que vai ser removida
                                        posicoes.add(pos);
                                    }
                                }

                                if(t.tabuleiro[i-1][j+1] != null && t.tabuleiro[i-1][j+1].getCor().equals(Cor.BRANCA)){
                                    if(t.tabuleiro[i-2][j+2] == null){
                                        pos = new int[6];
                                        pos[0] = i-2;
                                        pos[1] = j+2;
                                        pos[2] = i;
                                        pos[3] = j;
                                        pos[4] = i-1;
                                        pos[5] = j+1;
                                        posicoes.add(pos);
                                    }
                                }
                            } else if((j == 0 || j == 1) && i > 1){
                                if(t.tabuleiro[i-1][j+1] != null && t.tabuleiro[i-1][j+1].getCor().equals(Cor.BRANCA)){
                                    if(t.tabuleiro[i-2][j+2] == null){
                                        pos = new int[6];
                                        pos[0] = i-2;
                                        pos[1] = j+2;
                                        pos[2] = i;
                                        pos[3] = j;
                                        pos[4] = i-1;
                                        pos[5] = j+1;
                                        posicoes.add(pos);
                                    }
                                }
                            } else if((j == 6 || j == 7) && i > 1){
                                if(t.tabuleiro[i-1][j-1] != null && t.tabuleiro[i-1][j-1].getCor().equals(Cor.BRANCA)){
                                    if(t.tabuleiro[i-2][j-2] == null){
                                        pos = new int[6];
                                        pos[0] = i-2;
                                        pos[1] = j-2;
                                        pos[2] = i;
                                        pos[3] = j;
                                        pos[4] = i-1;
                                        pos[5] = j-1;
                                        posicoes.add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return posicoes;
    }

    public static void main(String[] args) throws IOException {
        Principal principal = new Principal();
        principal.iniciaConexao();
    }
}

