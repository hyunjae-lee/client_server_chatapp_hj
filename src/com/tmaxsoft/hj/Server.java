package com.tmaxsoft.hj;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    public int portNumber = 5001;

    ExecutorService executorService;
    ServerSocketChannel serverSocketChannel;
    List<Client> connections = new Vector<Client>();

    // Server 시작
    void startServer(){

        // Thread pool 생성
        executorService = Executors.newFixedThreadPool
                (Runtime.getRuntime().availableProcessors());

        try{
            InetSocketAddress connection = new InetSocketAddress(portNumber);

            // portNumber 포트에서 client의 연결을 수락하는 ServerSocketChannel 생성
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(true);
            serverSocketChannel.bind(connection);
            System.out.println("Listening on port " + portNumber);
        } catch (Exception e) {
            if (serverSocketChannel.isOpen()){
                stopServer();
            }
        }

        // connection accept 작업을 runnable 객체로 만들고
        // thread pool의 작업 thread로 실행시킨다.
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        // thread는 accpet()에서 client 연결 수락을 위해 blocking 된다.
                        SocketChannel socketChannel = serverSocketChannel.accept();

                        String acceptMsg = "[ Accept: " + socketChannel.getRemoteAddress() + ": "
                                + Thread.currentThread().getName() + "]";

                        System.out.println(acceptMsg);

                        Client client = new Client(socketChannel);

                        connections.add(client);

                        // data 통신
                        while(true){
                            if(client.socketChannel.isOpen()){
                                client.receive();
                            }else{
                                break;
                            }
                        }

                    } catch (Exception e){
                        if (serverSocketChannel.isOpen()){
                            stopServer();
                        }
                        break;
                    }
                }
            }
        };

        executorService.submit(runnable);
    }

    // Server 종료
    void stopServer(){

        try{
            Iterator<Client> iterator = connections.iterator();

            while (iterator.hasNext()){
                Client client = iterator.next();
                client.socketChannel.close();
                iterator.remove();
            }

            if (serverSocketChannel != null && serverSocketChannel.isOpen()){
                serverSocketChannel.close();
            }

            if (executorService != null && executorService.isShutdown()){
                executorService.shutdown();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    // 데이터 통신 코드
    class Client {

        SocketChannel socketChannel;

        public Client(SocketChannel socketChannel) {
            this.socketChannel = socketChannel;
        }

        // client로 부터 데이터 받기
        void receive() {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try{
                        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

                        // 클라이언트가 비정상 종료를 했을 경우 IOException 발생
                        int byteCount = socketChannel.read(byteBuffer);

                        // 클라이언트가 정상적으로 SocketChannel의 close()를 호출했을 경우
                        if(byteCount == -1){
                            throw new IOException();
                        }

                        String msg = "[ MSG: " + socketChannel.getRemoteAddress() + ": "
                                + Thread.currentThread().getName() + "]";
                        System.out.println(msg);

                        byteBuffer.flip();
                        Charset charset = Charset.forName("UTF-8");

                        String data = charset.decode(byteBuffer).toString();
                        System.out.println(data);

                        if(data.equals("quit")){
                            String log = "[ TRY TO DISCONNECT: " + socketChannel.getRemoteAddress() + ": "
                                    + Thread.currentThread().getName() + "]";
                            connections.remove(Client.this);
                            socketChannel.close();
                            System.out.println(log);
                        }else {
                            for (Client client : connections) {
                                client.send(data);
                            }
                        }

                    } catch (Exception e) {

                        try{
                            connections.remove(Client.this);
                            socketChannel.close();
                        } catch (IOException e2){
                            e2.printStackTrace();
                        }
                    }
                }
            };

            executorService.submit(runnable);
        }

        // 데이터를 client로 전송
        void send(String data){
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try{
                        Charset charset = Charset.forName("UTF-8");
                        ByteBuffer byteBuffer = charset.encode(data);
                        socketChannel.write(byteBuffer);
                    } catch (Exception e){
                        try{
                            String log = "[ UNCONNECTED: " + socketChannel.getRemoteAddress() + ": "
                                    + Thread.currentThread().getName() + "]";
                            connections.remove(Client.this);
                            socketChannel.close();

                            System.out.println(log);

                        }catch(IOException e2){
                            e2.printStackTrace();
                        }
                    }
                }
            };

            executorService.submit(runnable);
        }
    }

    public static void main(String[] args) throws UnknownHostException {
        Server server = new Server();
        server.startServer();
    }
}