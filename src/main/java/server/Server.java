package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xembly.Directives;
import org.xembly.ImpossibleModificationException;
import org.xembly.Xembler;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Обеспечивает работу программы в режиме сервера
 *
 * @author Влад
 */
public class Server {

    /**
     * Специальная "обёртка" для ArrayList, которая обеспечивает доступ к
     * массиву из разных нитей
     */
    private List<Connection> connections
            = Collections.synchronizedList(new ArrayList<Connection>());
    private ServerSocket server;

    /**
     * Конструктор создаёт сервер. Затем для каждого подключения создаётся
     * объект Connection и добавляет его в список подключений.
     */
    public Server() {
        try {
            server = new ServerSocket(Const.Port);

            while (true) {
                Socket socket = server.accept();

                // Создаём объект Connection и добавляем его в список
                Connection con = new Connection(socket);
                connections.add(con);

                // Инициализирует нить и запускает метод run(),
                // которая выполняется одновременно с остальной программой
                con.start();

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeAll();
        }
    }

    /**
     * Закрывает все потоки всех соединений а также серверный сокет
     */
    private void closeAll() {
        try {
            server.close();

            // Перебор всех Connection и вызов метода close() для каждого. Блок
            // synchronized {} необходим для правильного доступа к одним данным
            // их разных нитей
            synchronized (connections) {
                Iterator<Connection> iter = connections.iterator();
                while (iter.hasNext()) {
                    ((Connection) iter.next()).close();
                }
            }
        } catch (Exception e) {
            System.err.println("Потоки не были закрыты!");
        }
    }

    /**
     * Класс содержит данные, относящиеся к конкретному подключению:
     * <ul>
     * <li>имя пользователя</li>
     * <li>сокет</li>
     * <li>входной поток BufferedReader</li>
     * <li>выходной поток PrintWriter</li>
     * </ul>
     * Расширяет Thread и в методе run() получает информацию от пользователя и
     * пересылает её другим
     *
     * @author Влад
     */
    private class Connection extends Thread {

        private BufferedReader in;
        private PrintWriter out;
        private Socket socket;

        /**
         * Инициализирует поля объекта и получает имя пользователя
         *
         * @param socket сокет, полученный из server.accept()
         */
        public Connection(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
                close();
            }
        }

        /**
         * Запрашивает имя пользователя и ожидает от него сообщений. При
         * получении каждого сообщения, оно вместе с именем пользователя
         * пересылается всем остальным.
         *
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                //System.out.println("тест строка3");
                String str = "";
                //int duplo = connections.size() - 1;
                //System.out.println("sssss " + connections + " ALOO" + duplo);
                str = in.readLine();
                out.println("Добро пожаловать на сервер SberTest");
                // Отправляем всем клиентам сообщение о том, что зашёл новый пользователь

                boolean xmlTest = false;
                String xmlDoc = "";
                while (true) {
                    //System.out.println("тест строка2");
                    str = in.readLine();
                    while (xmlTest = true) {
                        str = in.readLine();
                        //System.out.println("тест строка1");
                        if (str.equals("end")) {
                            out.println(xmlSrverMessage(xmlDoc));
                            //System.out.println(xmlDoc);
                            xmlTest = false;
                            //System.out.println(xmlDoc);
                            break;
                        }
                        xmlDoc += "\n" + str;
                    }
                    if (str.equals("exit")) {
                        break;
                    }
                    if (str.equals("start")) {
                        xmlTest = true;
                    }

                    // Отправляем всем клиентам очередное сообщение
                    // sync
                }

                // sync
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ImpossibleModificationException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JDOMException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                close();
            }
        }

        public String xmlSrverMessage(String xmlIn) throws ImpossibleModificationException, JDOMException {
            String name = null;
            String secondname = null;
            String message = null;
            String dateIn = null;
            try {
                SAXBuilder builder = new SAXBuilder();
                Document document = (Document) builder.build(new StringReader(xmlIn));
                Element rootNode = document.getRootElement();
                List list = rootNode.getChildren("user");
                Element node = (Element) list.get(0);
                name = node.getChildText("name");
                secondname = node.getChildText("secondname");
                message = node.getChildText("message");
                dateIn = node.getChildText("date");
                System.out.println("");
            } catch (IOException io) {
                System.out.println(io.getMessage());
            } catch (JDOMException jdomex) {
                System.out.println(jdomex.getMessage());
            }
            SimpleDateFormat dt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            Date dateOut = new Date();
            String dateString = dt.format(dateOut);
            String xmlOut = new Xembler(
                    new Directives()
                            .add("response")
                            .add("message")
                            .set("Добрый день, " + name + ", Ваше сообщение успешно обработано!")
                            .up()
                            .add("date")
                            .set(dateString)
                            .up()
            ).xml();
            //System.out.println(xmlOut);
            return xmlOut;
        }

        /**
         * Закрывает входной и выходной потоки и сокет
         */
        public void close() {
            try {
                in.close();
                out.close();
                socket.close();

                // Если больше не осталось соединений, закрываем всё, что есть и
                // завершаем работу сервера
                connections.remove(this);
                if (connections.size() == 0) {
                    Server.this.closeAll();
                    System.exit(0);
                }
            } catch (Exception e) {
                System.err.println("Потоки не были закрыты!");
            }
        }
    }
}
