package nunettine.game;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import kr.co.shineware.util.common.model.Pair;

public class ServerApp {

  User user1;
  User user2;
  Komoran komoran = new Komoran(DEFAULT_MODEL.LIGHT);

  public ServerApp() {

  }

  public static void main(String[] args) throws IOException {
    ServerApp serverApp = new ServerApp();
    serverApp.execute();

  }

  public static void printSystem(String message) {
    System.out.println("[System] " + message);
  }

  public void execute() throws IOException {
    ServerSocket serverSocket = new ServerSocket(8888);

    user1 = new User(serverSocket.accept());
    printSystem("첫 번째 사용자가 연결되었습니다.");
    printSystem("유저 이름 : " + user1.getName());
    printSystem("유저 IP : " + user1.getAddress());

    user2 = new User(serverSocket.accept());
    printSystem("두 번째 사용자가 연결되었습니다.");
    printSystem("유저 이름 : " + user2.getName());
    printSystem("유저 IP : " + user2.getAddress());

    gameStart(user1, user2);

  }

  private void gameStart(User user1, User user2) {
    boolean gameEnd = false;
    boolean firstTurn = true;
    boolean timeOver = false;
    boolean wrongWord = false;
    boolean isBeforeWord = false;
    User playUser = user1;
    User waitUser = user2;
    String newWord = new String();
    String beforeWord = new String();
    List<String> wordList = new ArrayList<>();

    while (!gameEnd) {
      playUser.send("play");
      waitUser.send("wait");

      long startTime = System.currentTimeMillis();
      newWord = playUser.receive();
      wordList.add(newWord);
      waitUser.send(playUser.getName() + "의 단어 >> " + newWord);
      waitUser.send(getWordList(wordList));
      long endTime = System.currentTimeMillis();
      long timeElapsed = endTime - startTime;

      if (timeElapsed > 30000) {
        timeOver = true;
      }

      KomoranResult analyzeResult = komoran.analyze(newWord);
      List<Pair<String, String>> resultList = analyzeResult.getList();
      Pair<String, String> resultPair = resultList.get(0);

      System.out.println(resultList);
      System.out.println(resultList.size());
      System.out.println(resultPair.getSecond());

      if (resultList.size() != 1
          || (!resultPair.getSecond().equals("NNG") && !resultPair.getSecond().equals("NNP"))) {
        wrongWord = true;
      }

      if (wordList.contains(newWord)) {
        isBeforeWord = true;
      }

      if (!firstTurn) {
        if (beforeWord.charAt(beforeWord.length() - 1) != newWord.charAt(0) || timeOver
            || wrongWord) {
          System.out.println(beforeWord.charAt(beforeWord.length() - 1));
          System.out.println(newWord.charAt(0));
          playUser.send("lose");
          waitUser.send("win");

          StringBuilder message = new StringBuilder();
          if (timeOver) {
            message.append("시간 초과!!!");
          }
          if (wrongWord) {
            message.append("올바르지 않은 단어입니다!!! (명사형 X or 두 개의 단어)");
          }
          if (isBeforeWord) {
            message.append("이전에 사용한 단어입니다!!!");
          }

          playUser.send(message.toString());
          waitUser.send(message.toString());
          System.out.println("playUser의 " + playUser.receive());
          System.out.println("waitUser의 " + waitUser.receive());
          playUser.send(waitUser.getName() + "에게 패배하였습니다.");
          waitUser.send(playUser.getName() + "(으)로부터 승리하였습니다.");

          return;
        }
      }

      beforeWord = newWord;
      firstTurn = false;

      if (playUser == user1) {
        playUser = user2;
        waitUser = user1;
      } else if (playUser == user2) {
        playUser = user1;
        waitUser = user2;
      } else {
        System.out.println("Error 발생 !!!");
        return;
      }

    }
  }

  private String getWordList(List<String> wordList) {
    StringBuilder stringBuilder = new StringBuilder();

    for (String word : wordList) {
      stringBuilder.append(word);
      if (word.equals(wordList.getLast())) {
        return stringBuilder.toString();
      } else {
        stringBuilder.append(" - ");
      }

    }
    return stringBuilder.toString();
  }

}

class UserSocket {

  PrintStream printStream;
  Scanner scanner;
  InetAddress inetAddress;
  Socket socket;

  public UserSocket(Socket socket) {
    this.socket = socket;

    try {
      scanner = new Scanner(this.socket.getInputStream());
      printStream = new PrintStream(this.socket.getOutputStream());
      inetAddress = this.socket.getLocalAddress();
    } catch (IOException e) {
      System.out.println("사용자 연결 중 오류 발생!!!");
      e.printStackTrace();
    }
  }

  public Socket getSocket() {
    return socket;
  }

  public void setSocket(Socket socket) {
    this.socket = socket;
  }

  public PrintStream getPrintStream() {
    return printStream;
  }

  public void setPrintStream(PrintStream printStream) {
    this.printStream = printStream;
  }

  public Scanner getScanner() {
    return scanner;
  }

  public void setScanner(Scanner scanner) {
    this.scanner = scanner;
  }

  public InetAddress getInetAddress() {
    return inetAddress;
  }

  public void setInetAddress(InetAddress inetAddress) {
    this.inetAddress = inetAddress;
  }
}

class User {

  String name;
  String address;
  UserSocket userSocket;

  public User(Socket socket) {
    this.userSocket = new UserSocket(socket);
    this.name = userSocket.scanner.nextLine();
    this.address = userSocket.inetAddress.getHostAddress();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    User user = (User) o;
    return Objects.equals(name, user.name) && Objects.equals(address,
        user.address) && Objects.equals(userSocket, user.userSocket);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, address, userSocket);
  }

  public void send(String message) {
    PrintStream printStream = userSocket.getPrintStream();
    printStream.println(message);
  }

  public String receive() {
    Scanner scanner = userSocket.getScanner();
    return scanner.nextLine();
  }

  public UserSocket getUserSocket() {
    return userSocket;
  }

  public void setUserSocket(UserSocket userSocket) {
    this.userSocket = userSocket;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
