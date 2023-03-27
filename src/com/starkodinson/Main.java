package com.starkodinson;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

public class Main {

    public static void main(String[] args) throws IOException {
	// write your code here
        ServerSocket thisServer = new ServerSocket(55555);
        System.out.println(InetAddress.getLocalHost().getHostAddress());
        Thread read = new Thread(new DownloadFiles());
        read.start();

        Socket s = thisServer.accept();
        System.out.println("\n" + s.getInetAddress().getHostAddress());

        Thread t = new Thread(new ReadCommands(s));
        t.start();

    }

}

class DownloadFiles implements Runnable
{

    @Override
    public void run() {
        Scanner s = new Scanner(System.in);
        while (true)
        {
            System.out.print("Enter image URL:  ");
            String urlStr = s.nextLine();
            try {
                URL url = new URL(urlStr);
                Image image = ImageIO.read(url);

                System.out.print("\nSuccesfully located file! Enter name here: ");
                String thisN = s.nextLine();


                try (Stream<Path> files = Files.list(Paths.get("images"))) {
                    long count = files.count();
                    ImageIO.write((RenderedImage) image, "png", new File("images\\" + thisN + ".png"));
                    System.out.print("Saved file succesfully!");
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println();
        }
    }
}

class ReadCommands implements Runnable
{
    private Socket socket;
    private BufferedReader br;
    private PrintWriter p;
    private DataOutputStream dataOS;

    public ReadCommands(Socket s) throws IOException {
        this.socket = s;
        this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        this.p = new PrintWriter(socket.getOutputStream(), true);

        this.dataOS = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        while (!socket.isClosed())
        {
            System.out.println("Running");
            try {
                String thisInput = br.readLine();
                System.out.print(thisInput);
                if (thisInput.equalsIgnoreCase("list"))
                {
                    System.out.println("Received list command!");
                    Stream<Path> files = Files.list(Paths.get("images"));
                    StringBuilder sb = new StringBuilder();

                    files.forEach(f -> sb.append(f.getFileName().toString() + "|"));

                    String finOP = sb.toString();

                    p.println(finOP);
                }
                else if (thisInput.split(" ")[0].equalsIgnoreCase("download"))
                {
                    System.out.println("Received download command!");
                    int bytes = 0;
                    File file = new File("images\\" + thisInput.split(" ")[1]);
                    FileInputStream fIS = new FileInputStream(file);

                    System.out.println("going to send " + file.getAbsolutePath());

                    dataOS.writeLong(file.length());

                    byte[] buffer = new byte[4*1024];

                    System.out.print("Sending " + thisInput.split(" ")[1] + "...\n");

                    while ((bytes=fIS.read(buffer)) != -1)
                    {
                        dataOS.write(buffer, 0, bytes);
                        dataOS.flush();
                    }

                    System.out.print("Succesfully sent file.");

                    fIS.close();
                }
                else if (thisInput.equalsIgnoreCase("quit"))
                {
                    p.println("Received quit command. Closing connection and shutting down...");
                    socket.close();
                    System.exit(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
