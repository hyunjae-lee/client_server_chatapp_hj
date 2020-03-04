package com.tmaxsoft.hj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Client {

    private static final int portNumber = 5001;

    SocketChannel socketChannel;

    // Client 시작
    void startClient(){
        Thread thread = new Thread(){
            @Override
            public void run(){
                try{
                    InetSocketAddress connection = new InetSocketAddress(
                            InetAddress.getByName(
                                    "www.hyunjae.test.client.server.com"),
                            portNumber);

                    socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(true);
                    socketChannel.connect(connection);
                } catch (Exception e){
                    if(socketChannel.isOpen()){
                        stopClient();
                    }
                }
            }
        };

        thread.start();
    }

    // Client 종료
    void stopClient(){
        try{
            if(socketChannel != null && socketChannel.isOpen()){
                socketChannel.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    // Server로 부터 data 받음.
    boolean receive(){
        while(true){
            try{
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

                int byteCount = socketChannel.read(byteBuffer);

                if(byteCount == -1) {
                    throw new IOException();
                }

                byteBuffer.flip();
                Charset charset = StandardCharsets.UTF_8;
                String data = charset.decode(byteBuffer).toString();

                System.out.println("RECEIVED: " + data);

                if(data.equals("quit")){
                    return true;
                }
                else{
                    return false;
                }
            } catch (Exception e){
                stopClient();
                break;
            }
        }
        return false;
    }

    // Server로 data 전송.
    void send(String data){
        Thread thread = new Thread(){

            @Override
            public void run(){
                try{
                    Charset charset = StandardCharsets.UTF_8;
                    ByteBuffer byteBuffer = charset.encode(data);
                    socketChannel.write(byteBuffer);
                } catch (Exception e){
                    stopClient();
                }
            }
        };

        thread.start();
    }

    public static void main (String[] args) throws Exception {
        Client client = new Client();
        client.startClient();

        BufferedReader input = new BufferedReader(new
                InputStreamReader(System.in));

        while(true){
            System.out.print("Type a message (type quit to stop): ");
            String msg = input.readLine();

            client.send(msg);

            if (msg.equalsIgnoreCase("quit")) {
                if(client.receive()){
                    client.stopClient();
                }
                break;
            }
        }
    }
}