package chatroom;

import chatroomUtils.ArgsParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public class Server {
    List<ServerThread> threads = new CopyOnWriteArrayList<ServerThread>(); //使用CopyOnWriteArrayList 可避免 java.util.ConcurrentModificationException
    int count=0;
    int serverPort=8888;
    public static int maxClients=3;
    protected PrintStream ps;
    public void print(String msg){
        System.out.println(msg);
        ps.println(msg);
    }
    void showHelp(){
        System.out.println("Usage:");
        System.out.println("    -h,--help");
        System.out.println("        Show this help");
        System.out.println("    -p,--port <port>");
        System.out.println("        Set listening port");
        System.out.println("    -m,--maxclients <client_limit>");
        System.out.println("        Set max clients limit(0~99).0 means no limit.");
    }
    void parseArgs(String[] args){
        try{
            Map<String, List<String>> params = ArgsParser.parse(args);
            List<String> optionkeys = new ArrayList<String>(params.keySet());
            List<String> options=null;
            if(params.get("h")!=null || params.get("-help")!=null){
                showHelp();
                System.exit(0);
            }
            else{
                if((options=params.get("p"))!=null || (options=params.get("-port"))!=null){
                    if(options.size()==1){
                        String customport=options.get(0);
                        String portPattern = "^([0-9]|[1-9]\\d|[1-9]\\d{2}|[1-9]\\d{3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])$";
                        if(!Pattern.matches(portPattern, customport))
                            throw new IllegalArgumentException("Invalid value: -p or --port");
                        System.out.println("[*]port:"+customport);
                        serverPort=Integer.parseInt(customport);
                        optionkeys.remove("p");
                        optionkeys.remove("-port");
                    }
                    else{
                        throw new IllegalArgumentException("Error at argument: -p or --port");
                    }
                }else{
                    System.out.println("[*]port:8888 (default)");
                }
                if((options=params.get("m"))!=null || (options=params.get("-maxclients"))!=null){
                    if(options.size()==1){
                        String customlimit=options.get(0);
                        String limitPattern = "^[0-9]|[1-9][0-9]$";
                        if(!Pattern.matches(limitPattern, customlimit))
                            throw new IllegalArgumentException("Invalid value: -m or --maxclients");
                        System.out.println("[*]max clients limit:"+(customlimit.equals("0")?"never":customlimit));
                        maxClients=Integer.parseInt(customlimit);
                        optionkeys.remove("m");
                        optionkeys.remove("-maxclients");
                    }
                    else{
                        throw new IllegalArgumentException("Error at argument: -m or --maxclients");
                    }
                }else{
                    System.out.println("[*]max clients limit:3 (default)");
                }
            }
            if(!optionkeys.isEmpty()){
                System.out.print("[!]Ignored arguments:");
                for(String optionkey:optionkeys){
                    System.out.print(" "+optionkey);
                }
                System.out.println();
            }

        }catch (IllegalArgumentException e){
            System.err.println("[-]"+e.getMessage());
            showHelp();
            System.exit(1);
        }
    }
    public Server(String[] args){
        parseArgs(args);
        ServerSocket serverSocket=null;
        String logpath = this.getClass().getResource("/").getPath()+"serverlog";
        try{
            File logfile=new File(logpath);
            if (!logfile.exists()) {
                if (logfile.createNewFile()) {
                    ps = new PrintStream(new FileOutputStream(logpath, true));//true可以在写入文件时附加在末尾而不覆盖
                    print("[+]Log created successfully");
                    print("[*]Log path: "+logpath);
                }
            }
            else{
                ps = new PrintStream(new FileOutputStream(logpath, true));//true可以在写入文件时附加在末尾而不覆盖
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                print("[*]Server ends running at: "+Calendar.getInstance().getTime());
                ps.close();//关闭日志文件的写入
                for(ServerThread st: threads) {
                    st.notify("Server closed.Exiting...");
                    st.rawpush("$6");//断开客户端的连接
                }                
            }
        });
        try {
            serverSocket = new ServerSocket(serverPort);
            Socket socket = null;
            print("[*]Server start running on port "+serverPort+" at: "+Calendar.getInstance().getTime());
            while (true){
                socket = serverSocket.accept();
                ServerThread serverThread = new ServerThread(socket);//创建一个新的线程
                threads.add(serverThread);
                serverThread.start();//启动线程
                count ++;
                print("[+]A new Client connected from:"+socket.getRemoteSocketAddress());
                print("[*]Running Threads:" + count);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            try {
                serverSocket.close();
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }
    public static void main(String[] args) {
        new Server(args);
    }



    class ServerThread extends Thread {
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        OutputStream os = null;
        PrintWriter pw = null;
        boolean passed=false;
        String username=null;
        boolean silent=false;

        Socket socket = null;//和本线程相关的socket
        SocketAddress addr = null;

        public ServerThread(Socket socket){
            this.socket = socket;
        }
        boolean vertify(String info){
            username=info.split("\\+")[0];
            print(time()+"Login request:"+username);
            File db = getFile("db");
            String line = null;
            try {

                BufferedReader bReader = new BufferedReader(new FileReader(db));
                while ((line = bReader.readLine())!=null) {
                    if(line.equals(info)){
                        bReader.close();
                        return true;
                    }
                }
                bReader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            print("[-]Login request of "+username+" is rejected");
            username=null;
            return false;
        }
        boolean register(String un,String pwd){
            File db = getFile("db");
            String line = null;
            print(time()+"Register request:"+un);
            try {
                BufferedReader bReader = new BufferedReader(new FileReader(db));
                while ((line = bReader.readLine())!=null) {
                    if(line.split("\\+")[0].equals(un)){                                            //equals老是忘
                        bReader.close();
                        print("[-]Register request of "+un+" is rejected");
                        return false;
                    }
                }
                bReader.close();
                FileWriter fWriter = new FileWriter(db,true);
                fWriter.write(un+"+"+pwd+'\n');
                fWriter.flush();
                fWriter.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            print("[+]"+un+" registered successfully.");
            return true;
        }
        File getFile(String fileName) {
            String path = this.getClass().getResource("/").getPath()+fileName;//保存文件于classpath路径
            File file = new File(path);
            try {
                if (!file.exists()) {
                    if (file.createNewFile()) {
                        print("[+]Database created successfully");
                        print("[*]Database path: "+path);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return file;
        }
        void chat() throws IOException {//接受信息并广播到各个用户客户端
            showUsers();
            
            String msg = null;
            while ((msg = br.readLine()) != null){
                print(time()+username+":"+msg);
                for(ServerThread st: threads) {
                    if(st.username!=null)
                        st.push(username,msg);
                }

            }

        }
        void push(String un,String msg){//带时间、用户名发送信息
            if(un==username)//可以用==，判断是否是用户自己
                un="You";
            pw.write(time()+un+":"+msg+'\n');
            pw.flush();
        }
        void notify(String msg){//带时间发送信息
            pw.write(time()+msg+'\n');
            pw.flush();
        }
        void rawpush(String msg){//直接发送信息
            pw.write(msg+'\n');
            pw.flush();
        }
        boolean checkLimit(){//检查连接数是否已达上限
            if(maxClients==0)
                return true;
            int n=0;
            for(ServerThread st: threads) {
                if(st.username!=null){
                    n++;
                }
            }
            if(n >= maxClients){
                return false;
            }
            return true;
                
        }
        void showUsers(){
            String payload="";
            for(ServerThread st: threads) {
                if(st.username!=null && (!st.username.equals(username) || st.username==username)){//若允许多地登录，这样判断可以使一个用户名只出现一次
                    payload+=st.username;
                    payload+="+";
                }
            }
            payload="$5@"+payload;
            if(payload.endsWith("+")){
                payload=payload.substring(0,payload.length()-1);
            }
            for(ServerThread st: threads) {
                if(st.username!=null){
                    st.rawpush(payload);
                }
            }

        }
        String time(){
            Calendar c = Calendar.getInstance();//可以对每个时间域单独提取
            String hour = String.format("%02d",c.get(Calendar.HOUR_OF_DAY));
            String minute = String.format("%02d",c.get(Calendar.MINUTE));
            String second = String.format("%02d",c.get(Calendar.SECOND));
            return("["+hour+":"+minute+":"+second+"]");
        }
        @Override
        public void run() {
            try {
                addr = socket.getRemoteSocketAddress();//获取连接的客户端的ip地址与端口
                is = socket.getInputStream();//获取字节输入流
                isr = new InputStreamReader(is);//将字节流包装成字符流
                br = new BufferedReader(isr);//为字符流添加缓冲
                os = socket.getOutputStream();//获取字节输出流
                pw = new PrintWriter(os);//将输出流包装为打印流
                String info = null;

                while ((info = br.readLine()) != null){//循环获取客户端信息
                    if(info.charAt(0)=='$'){
                        if(register(info.substring(1).split("\\+")[0],info.substring(1).split("\\+")[1])){
                            rawpush("$3");
                        }
                        else{
                            rawpush("$4");
                        }
                    }
                    else{
                        if(checkLimit()){//检查连接数是否已达上限
                            if(vertify(info)){
                                rawpush("$1");
                                passed=true;
                                break;
                            }
                            else{
                                rawpush("$2");
                            }
                        }
                        else{
                            print("[-]Client amount meet the limit("+maxClients+").Closing the thread...");
                            rawpush("$6");
                            forceClose();
                        }
                    }
                }

                if(passed){
                    boolean newlogin = true;
                    for(ServerThread st: threads) {
                        if(st.username != null && st.username.equals(username) && st.username!=username){//若有同用户名的旧线程则通知其关闭，并且不需要视为新登录而广播
                            newlogin = false;
                            print("[*]"+username+" requests to login with a new client.");
                            st.notify("A new Login at "+addr+".Exiting...");
                            st.rawpush("$6");
                            st.silent=true;
                            st.forceClose();
                        }
                    }
                    if(newlogin){
                        print("[+]"+username+" logged in.");
                        for(ServerThread st: threads) {
                            if(st.username!=null){
                                st.notify(username+" joined the chat!");
                            }
                        }
                    }
                    print("[*]"+addr+" => "+username);
                    chat();
                }


                socket.shutdownInput();//关闭输入流
                socket.shutdownOutput();

            }catch(SocketException e){
                if(username != null)
                    print("[-]client "+username+"("+addr+")"+":"+e.getMessage());
                else
                    print("[-]client (non-logged)"+"("+addr+"):"+e.getMessage());
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                //关闭资源
                try {
                    if(pw!=null)
                        pw.close();
                    if(os!=null)
                        os.close();
                    if(br!=null)
                        br.close();
                    if(isr!=null)
                        isr.close();
                    if(is!=null)
                        is.close();
                    if(socket!=null && socket.isClosed()==false)
                        socket.close();

                    threads.remove(this);
                    count--;
                    if(username != null && !silent){
                        print(time()+username+" exited.");
                        for(ServerThread st: threads) {
                            if(st.username!=null){
                                st.notify(username+" left the chat.Bye!");
                            }
                        }
                        showUsers();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally{
                    print("[*]Running Threads:"+count);
                }
            }

        }
        void forceClose(){
            try {
                if(socket!=null){//socket.shutdownInput();socket.shutdownOutput();
                    socket.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}