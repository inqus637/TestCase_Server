package server;

import org.apache.log4j.Logger;

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
import org.xembly.Directives;
import org.xembly.ImpossibleModificationException;
import org.xembly.Xembler;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class Server {
    final static Logger logger = Logger.getLogger(Server.class);
    /**
     * Специальная "обёртка" для ArrayList, которая обеспечивает доступ к массиву
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
            server = new ServerSocket(4444);
            System.out.print("Сервер запущен");
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
            logger.error(e + " неудолось использовать socket");
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
            // synchronized {} необходим для правильного доступа к данным
            synchronized (connections) {
                Iterator<Connection> iter = connections.iterator();
                while (iter.hasNext()) {
                    iter.next().close();
                }
            }
        } catch (Exception e) {
            logger.error(e + " Потоки не были закрыты!");
            //System.err.println("Потоки не были закрыты!");
        }
    }
    /**
     * Класс содержит данные, относящиеся к конкретному подключению
     * имя пользователя
     * сокет
     * входной поток BufferedReader
     * выходной поток PrintWriter<
     * <p>
     * Расширяет Thread и в методе run() получает информацию от пользователя и
     * пересылает её другим
     */
    private class Connection extends Thread {
        private BufferedReader in;
        private PrintWriter out;
        private Socket socket;
        /**
         * Инициализируются поля объекта
         */
        public Connection(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                logger.error(e + " не удалось подключится");
                e.printStackTrace();
                close();
            }
        }

        /**
         *
         *
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                logger.info("Пользователь подключен");
                out.println("Welcome to SberTest server");
                String str = "";
                // Отправляем всем клиентам сообщение о том, что зашёл новый пользователь
                boolean xmlTest = false;
                String xmlDoc = "";
                while (true) {
                    str = in.readLine();  
                    if (str.equals("exit")) {
                        break;
                    }
                    if (str.equals("start")) {
                        logger.info("получение xml");
                        xmlTest = true;
                        while (xmlTest = true) {
                            str = in.readLine();
                            if (str.equals("end")) {
                                logger.info("xml получен");
                                out.println(xmlServerMessage(xmlDoc));
								System.out.print(xmlDoc);
								System.out.print(xmlServerMessage(xmlDoc));
                                logger.info("ответный xml отправлен");
                                break;
                            }
                            if (!str.equals("start")) {
                            xmlDoc += str + "\n";}
                        }
                        xmlDoc="";

                    }
                    // Отправляем всем клиентам очередное сообщение
                }

            } catch (IOException | ImpossibleModificationException | JDOMException e) {
                logger.error(e);
                //e.printStackTrace();
            } finally {
                close();
            }
        }
        /**
         * Формирует из полученного xml ответ пользователю
         */
        public String xmlServerMessage(String xmlIn) throws ImpossibleModificationException, JDOMException {
            String name = null;
            try {
                SAXBuilder builder = new SAXBuilder();
                Document document = builder.build(new StringReader(xmlIn));
                Element rootNode = document.getRootElement();
                List list = rootNode.getChildren("user");
                Element node = (Element) list.get(0);
                name = node.getChildText("name");
            } catch (IOException io) {
                logger.error(io.getMessage());
            } catch (JDOMException jdomex) {
                logger.error(jdomex.getMessage());
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
            logger.info("ответный xml для " + name + " создан");
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
                logger.info(e + "ошибка при выключении сервера");
                //System.err.println("Потоки не были закрыты!");
            }
        }
    }
}
