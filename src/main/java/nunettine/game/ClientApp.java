package nunettine.game;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class ClientApp {

  Prompt prompt = new Prompt();

  public ClientApp() {

  }

  public static void main(String[] args) throws IOException {
    ClientApp clientApp = new ClientApp();

    clientApp.execute();
  }

  public void execute() throws IOException {
    String host = prompt.input("Host 주소 >> ");
    int port = prompt.inputInt("Port 번호 >> ");
    String name = prompt.input("User 이름 >> ");

    Socket socket = new Socket(host, port);

    PrintStream printStream = new PrintStream(socket.getOutputStream());
    Scanner scanner = new Scanner(socket.getInputStream());

    printStream.println(name);

    while (true) {
      String command = scanner.nextLine();

      if (command.equals("play")) {
        printStream.println(prompt.input("단어 입력 >> "));
      } else if (command.equals("wait")) {
        System.out.println("...상대방 입력 대기중");
        System.out.println(scanner.nextLine());
        System.out.println(scanner.nextLine());
      } else if (command.equals("lose") || command.equals("win")) {
        System.out.println(scanner.nextLine());
        printStream.println("입력 확인");
        System.out.println(scanner.nextLine());
        return;
      }
    }
  }
}
