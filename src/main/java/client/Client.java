package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.lang.Thread.sleep;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

import org.xembly.Directives;
import org.xembly.ImpossibleModificationException;
import org.xembly.Xembler;

/**
 * Обеспечивает работу программы в режиме клиента
 *
 * @author Влад
 */
public class Client {

    private BufferedReader in;
    private PrintWriter out;
    private Socket socket;

    /**
     * Запрашивает у пользователя ник и организовывает обмен сообщениями с
     * сервером
     */
    public Client() throws FileNotFoundException {
        Scanner scan = new Scanner(System.in);
        Scanner sc = new Scanner(new File("./src/main/resources/config.ini"));
        String ip;
        String port;
        ip = sc.nextLine();
        port = sc.nextLine();
        ip = ip.replace("ServerIp=", "");
        port = port.replace("ServerPort=", "");
        int portInt = Integer.parseInt(port);
        try {
            // Подключаемся в серверу и получаем потоки(in и out) для передачи сообщений
            socket = new Socket(ip, portInt);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
           
            // Запускаем вывод всех входящих сообщений в консоль
            Resender resend = new Resender();
            resend.start();
            out.print("connect");
            sleep(1000);
            out.println("start");
            
            out.println(xmlClientMessage());
            out.println("end");
            System.out.println("тест строка2");
            // Пока пользователь не введёт "exit" отправляем на сервер всё, что
            // введено из консоли
            String str = "";
            while (!str.equals("exit")) {
                str = scan.nextLine();
                out.println(str);
            }
            resend.setStop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }
     public static String xmlClientMessage() throws ImpossibleModificationException {
        Locale loc = new Locale("es", "ES");
        Scanner scan = new Scanner(System.in, "UTF-8");
        scan.useLocale(loc);
        System.out.println("Введите свое имя:");
        String name = scan.nextLine();
        System.out.println("Введите свою Фамилию:");
        String secondname = scan.nextLine();
        System.out.println("Введите сообщение:");
        String message = scan.nextLine();
        SimpleDateFormat dt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        Date date = new Date();
        String dateString = dt.format(date);
        String xml = new Xembler(
                new Directives()
                        .add("root")
                        .add("user")
                        .add("name")
                        .set(name)
                        .up()
                        .add("secondname")
                        .set(secondname)
                        .up()
                        .add("message")
                        .set(message)
                        .up()
                        .add("date")
                        .set(dateString)
                        .up()
        ).xml();
        System.out.println("для отправки данных на сервер нажмите Enter");
        scan.nextLine();
        //System.out.println(xml);
        return xml;
    }

    public static String xmlSrverMessage(String name, String message) throws ImpossibleModificationException {
        SimpleDateFormat dt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        Date date = new Date();
        String dateString = dt.format(date);
        String xml = new Xembler(
                new Directives()
                        .add("response")
                        .add("message")
                        .set("Добрый день, " + name + ", Ваше сообщение успешно обработано!")
                        .up()
                        .add("date")
                        .set(dateString)
                        .up()
        ).xml();
        System.out.println(xml);
        return xml;
    }

    /**
     * Закрывает входной и выходной потоки и сокет
     */
    private void close() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (Exception e) {
            System.err.println("Потоки не были закрыты!");
        }
    }

    /**
     * Класс в отдельной нити пересылает все сообщения от сервера в консоль.
     * Работает пока не будет вызван метод setStop().
     *
     * @author Влад
     */
    private class Resender extends Thread {

        private boolean stoped;

        /**
         * Прекращает пересылку сообщений
         */
        public void setStop() {
            stoped = true;
        }

        /**
         * Считывает все сообщения от сервера и печатает их в консоль.
         * Останавливается вызовом метода setStop()
         *
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                while (!stoped) {
                    String str = in.readLine();
                    System.out.println(str);
                }
            } catch (IOException e) {
                System.err.println("Ошибка при получении сообщения.");
                e.printStackTrace();
            }
        }
    }

}
