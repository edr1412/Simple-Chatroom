package chatroomGUI;

import chatroom.Server;

import java.util.regex.Pattern;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Container;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
public class ServerGUI extends JFrame implements ActionListener{
    private static final long serialVersionUID = 1L;
    JTextArea ta;
    JMenuBar menuBar;
    JMenu menu;
    JMenuItem menuItem;
    JTextField jt;
    JButton jb;
    JDialog jd;
    JPanel jp;

    ServerGUI(String[] args) {
        super("Server");
        setSize(900, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        ta = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(ta);
        add("Center", scrollPane);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        jb=new JButton("Set");
        jb.addActionListener(this);
        jt = new JTextField(5);
        jp=new JPanel();
        jp.add(jt);
        jp.add(jb);
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        menu=new JMenu("Setting");
        menu.setMnemonic(KeyEvent.VK_S);
        menuBar.add(menu);
        menuItem=new JMenuItem("Max_clients",KeyEvent.VK_M);
        menuItem.addActionListener(this);
        menu.add(menuItem);

        setVisible(true);
        new Server(args){
            public void print(String msg) {
                ta.append(msg+'\n');
                ta.setCaretPosition(ta.getDocument().getLength());
                ps.println(msg);
            }
        };

        
    }
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Max_clients")) {
            //弹出设置窗口
            jd = new JDialog(this,"Setting max clients");
            jd.setSize(240,100);
            Container c =jd.getContentPane();
            c.add(jp);
			jd.setVisible(true);
        }
        else if (e.getActionCommand().equals("Set")) {
            //检查输入，修改Server.maxClients
            String inputMax=null;
            inputMax=jt.getText();
            String limitPattern = "^[0-9]|[1-9][0-9]$";
            if(Pattern.matches(limitPattern, inputMax)){
                Server.maxClients=Integer.parseInt(inputMax);
                ta.append("[*]Max clients limit changed to "+Server.maxClients+"\n");
            }else{
                ta.append("[-]Invalid input!\n");
                ta.append("[!]Max clients limit range is [0,99].0 means no limit.\n");
            }
            
        } 
    }
    public static void main(String[] args){
        new ServerGUI(args);
    }
    
}
