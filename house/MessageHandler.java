package house;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import utility.Message;

public class MessageHandler implements Runnable {

    Socket socket;
    OutputStreamWriter output;
    BufferedReader input;

    public MessageHandler(Socket s) {
        socket = s;
        try {
            input = new BufferedReader(new InputStreamReader(s.getInputStream()));
            output = new OutputStreamWriter(s.getOutputStream());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }


    @Override
    public void run() {
        while (true) {
            Message msg = null;
            try {
                msg = new Message(input.readLine());
            } catch (Exception e) {System.err.println(e.getMessage());}
            msg.getHeader();
            //switch (msg.getHeader())
        }
    }


}
