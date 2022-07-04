package chatroomGUI;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Client extends JFrame implements ActionListener {
    private static final long serialVersionUID = -2205336255279715713L;// 保持版本兼容性
    JTextField tu, tc;
    JPasswordField tp;
    JTextArea ta;
    JPanel p, p1, p2;
    JLabel lu, lp, lc;
    JButton b1, b2, b3;
    JList<String> listNames;
    JScrollPane scrollPane,scrollPane2;
    JMenuBar menuBar;
    JMenu menu;
    JMenuItem menuItem;

    
    CardLayout c;

    String username=null;

    Socket socket = null;
    OutputStream os = null;
    PrintWriter pw = null;
    InputStream is = null;
    BufferedReader br = null;

    Client() {
        super("Chatroom");
        setSize(900, 720);
        setLocationRelativeTo(null);//set the window to center of the screen
        p = new JPanel();
        p1 = new JPanel();
        p2 = new JPanel();
        lu = new JLabel("nickname");
        lp = new JLabel("password");
        lc = new JLabel(">");
        b1 = new JButton("Register");
        b2 = new JButton("Login");
        b3 = new JButton("Send");
        tu = new JTextField(8);
        tp = new JPasswordField(8);
        tc = new JTextField(35);
        ta = new JTextArea();

        ta.setEditable(false);
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        menu=new JMenu("Setting");
        menu.setMnemonic(KeyEvent.VK_S);
        menuBar.add(menu);
        menuItem=new JMenuItem("Server",KeyEvent.VK_E);
        menuItem.addActionListener(this);
        menu.add(menuItem);

        p1.add(lu);
        p1.add(tu);
        p1.add(lp);
        p1.add(tp);
        p1.add(b1);
        p1.add(b2);
        p2.add(lc);
        p2.add(tc);
        p2.add(b3);
        c = new CardLayout();
        p = new JPanel(c);
        p.add(p1, "login");
        p.add(p2, "chat");
        c.show(p, "login");//默认为登录状态
        Container cc = getContentPane();
        cc.setLayout(new BorderLayout());

        scrollPane2=new JScrollPane(ta);
        add("Center", scrollPane2);
        add("South", p);
        listNames=new JList<String>();
        scrollPane=new JScrollPane(listNames);
        add("East",scrollPane);
        scrollPane.setVisible(false);


        b1.addActionListener(this);
        b2.addActionListener(this);
        b3.addActionListener(this);
        KeyAdapter ka = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if(b2.getText().equals("Login") && b2.isEnabled())
                        login(false);
                    else if(b2.getText().equals("Apply"))
                        connect();
                }
            }
        };
        tu.addKeyListener(ka);
        tp.addKeyListener(ka);
        tc.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    send();
                }
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
                System.exit(0);
            }
        });
        init("localhost", 8888);

        setVisible(true);
        tu.requestFocus();

    }

    public void actionPerformed(ActionEvent e) {//按钮的动作响应实现
        if (e.getActionCommand().equals("Register")) {
            login(true);
        } 
        else if (e.getActionCommand().equals("Login")) {
            login(false);
        } else if (e.getActionCommand().equals("Send")) {
            send();
        }
        else if  (e.getActionCommand().equals("Server")) {
            close();
            configServerui();

        }
        else if  (e.getActionCommand().equals("Default")) {
            tu.setText("localhost");
            tp.setText("8888");
        }
        else if  (e.getActionCommand().equals("Apply")) {
            connect();
        }
    }
    boolean isFormValid(String str){
        String pattern="^\\w{1,15}$";
        if(Pattern.matches(pattern, str))
            return true;
        return false;
    }
    void send(){
        pw.write(tc.getText() + "\n");
        pw.flush();
        tc.setText("");
        tc.requestFocus();
    }
    void connect(){
        String newip = tu.getText();
        String newport = String.valueOf(tp.getPassword());
        String ipPattern = "^([1-9]?\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.([1-9]?\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}|localhost$";
        String portPattern = "^([0-9]|[1-9]\\d|[1-9]\\d{2}|[1-9]\\d{3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])$";

        if(!Pattern.matches(ipPattern, newip))
            showmsg("invalid ip!");
        else if (!Pattern.matches(portPattern, newport))
            showmsg("invalid port!");
        else {
            init(tu.getText(),Integer.parseInt(newport));
            if(b3.isEnabled())      //根据b3可判断是否连接服务器成功
                loginui();
        }
    }
    void configServerui(){//切换至设置连接的界面
        c.show(p, "login");
        scrollPane.setVisible(false);
        b1.setEnabled(true);
        b2.setEnabled(true);
        lu.setText("IP Address");
        tu.setText("");
        tp.setEchoChar('\0');
        tp.setText("");
        lp.setText("port");
        b1.setText("Default");
        b2.setText("Apply");
        tu.requestFocus();
    }
    void loginui(){//切换至登录界面
        c.show(p, "login");
        scrollPane.setVisible(false);
        lu.setText("nickname");
        tu.setText("");
        tp.setEchoChar('*');
        tp.setText("");
        lp.setText("password");
        b1.setText("Register");
        b2.setText("Login");
        tu.requestFocus();
    }
    class ChatThread extends Thread {
        public void run() {
            try {
                String msg;
                while (br !=null && (msg = br.readLine()) != null) {
                    if(msg.startsWith("$")){
                        if(msg.startsWith("$5")){//用户列表信息
                            String[] names = msg.split("@")[1].split("\\+");//提取所有用户名
                            for(int i=0;i<names.length;i++){
                                if(names[i].equals(username)){//标注自己
                                    names[i]+="(me)";
                                }
                            }
                            listNames.setListData(names);//显示在侧边栏的JList上
                        }
                        else if(msg.equals("$6")){
                            close();
                            configServerui();
                        }
                    }
                    else{
                        showmsg(msg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    void showmsg(String msg){
        ta.append(msg + '\n');
        ta.setCaretPosition(ta.getDocument().getLength());  // 设置滚动条始终在最下面
    }
    void init(String ip, int port) {

        try {
            socket = new Socket();//创建客户端Socket
            socket.connect(new InetSocketAddress(ip, port), 5000);//指定服务器地址和端口,设置连接请求超时时间1 s
            os = socket.getOutputStream();// 字节输出流
            pw = new PrintWriter(os);// 将输出流包装为打印流
            is = socket.getInputStream();//获取字节输入流
            br = new BufferedReader(new InputStreamReader(is));//将字节流包装成字符流并添加缓冲
            b1.setEnabled(true);
            b2.setEnabled(true);
            b3.setEnabled(true);
            showmsg("Successfully connected to server "+ip+":"+port);


        } catch (ConnectException conne) {
            showmsg("Connection failed.Is server running?");
            //conne.printStackTrace();
            if(b1.getText().equals("Register"))
                b1.setEnabled(false);
            if(b2.getText().equals("Login"))
                b2.setEnabled(false);
            b3.setEnabled(false);
        } catch(SocketTimeoutException e){
            showmsg(e.getMessage());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    void login(boolean register){
        String usernm = tu.getText();
        String passwd = String.valueOf(tp.getPassword());
        String salt = "ρ|zZ@~bθγ";
        if (!isFormValid(usernm)) {
            showmsg("nickname must be 1~15 long number,letter or underscore");
        }
        else if (!isFormValid(passwd)) {
            showmsg("password must be 1~15 long number,letter or underscore");
        }
        else{
            try {
                passwd=getMD5(passwd+salt);//加盐哈希两次，有效防止密码明文被破解
                passwd=getMD5(passwd+salt);
                if(register){
                    pw.write("$");
                }
                pw.write(usernm+"+"+passwd+'\n');
                pw.flush();

                String reponse = null;
                while((reponse = br.readLine()) != null){
                    if(reponse.equals("$1")){
                        showmsg("Successfully login!");
                        username=usernm;
                        lc.setText(username);
                        
                        c.show(p, "chat");
                        tc.requestFocus();
                        scrollPane.setVisible(true);
                        ChatThread sr = new ChatThread();// 创建新线程监听服务器发来的消息
                        sr.start();
                        break;
                    }
                    else if(reponse.equals("$2")){
                        showmsg("Failed to login.Please check your nickname or password and try again.");
                        break;
                    }
                    else if(reponse.equals("$3")){
                        showmsg("Successfully registered!");
                        break;
                    }
                    else if(reponse.equals("$4")){
                        showmsg("Sorry,this nickname is alreeady registered.");
                        break;
                    }
                    else if(reponse.equals("$6")){
                        showmsg("Server is unavaliable now.please try later or connect another server.");
                        close();
                        configServerui();
                        break;
                    }
                    else{
                        showmsg(reponse);
                    }
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    void close(){//关闭资源
        try {
            if(b3.isEnabled())
                showmsg("Connection closed.");
            if(b1.getText().equals("Register"))
                b1.setEnabled(false);
            if(b2.getText().equals("Login"))
                b2.setEnabled(false);
            b3.setEnabled(false);

            if(socket!=null && socket.isConnected() && socket.isClosed()==false){
                 socket.shutdownInput();
                 socket.shutdownOutput();
            }
            if(br!=null){
                br.close();
                br=null;
            }
            if(is!=null)
                is.close();
            if(pw!=null)
                pw.close();
            if(os!=null)
                os.close();
            if(socket!=null && socket.isClosed()==false){
                socket.close();
                socket=null;
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    String getMD5(String str) {
        byte[] digest = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("md5");
            digest  = md5.digest(str.getBytes("utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        String md5Str = new BigInteger(1, digest).toString(16);
        return md5Str;
    }
    
    public static void main(String[] args) {
        new Client();
    }
}

