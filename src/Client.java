import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.DefaultCaret;


public class Client extends JFrame implements ActionListener, KeyListener, MouseListener{
	
	JScrollPane jScrollPane1 = new JScrollPane();
	JTextArea mainMessage = new JTextArea();
	JTextField messageTF = new JTextField();
	JButton sendB = new JButton();
	JLabel usernameL = new JLabel();
	JTextField usernameTF = new JTextField();
	JButton connectB = new JButton();
	JScrollPane jScrollPane2 = new JScrollPane();
	String[] onlinePeople = {""};
	DefaultListModel<String> model = new DefaultListModel<String>();
	JList<String> onlineList = new JList<String>(model);
	JLabel onlineLabel = new JLabel();
	DefaultCaret caret;
	
	AESEncryption aes = new AESEncryption();
	Socket serverSocket;
	String salt = "LGHQGu695sTyW718";
	String username;
	String previousUsernames = "none";
	boolean connected = true;
	boolean hasPartner = false;
    OutputStream output;
    OutputStreamWriter outputWriter;
    BufferedWriter bw;
    InputStream input;
    InputStreamReader inputReader;
    BufferedReader br;
	
	
	public Client(){
		this.initComponents();
		
		try {
			serverSocket = new Socket("69.119.193.0", 25500);
			connected = true;
		} catch (IOException e) {
			System.out.println("Could not connect to server. Server probably not running.");
			connected = false;
		}
		
		new Thread(){
			public void run(){
				while(connected){
					dealWithMessages();
				}
			}
		}.start();
	}
	
	public void dealWithMessages(){
		String message = readMessages();
		try{
			if(message.substring(0, 9).equals("serverxx ")){
				serverMessages(message.substring(9));
			}else if(message.substring(message.indexOf(",") + 1, message.indexOf(",") + 10).equals("messagex ")){
				clientMessages(message.substring(message.indexOf(",") + 10), message.substring(0, message.indexOf(",")));
			}
		}catch(NullPointerException e){
			disconnected();
		}
	}
	
	public void disconnected(){
		connected = false;
		printMessage("***Can't Connect To Server***");
	}
	
	public void serverMessages(String message){
		if(message.equals("Username Set")){
			username = usernameTF.getText();
			usernameTF.setEnabled(false);
			connectB.setEnabled(false);
			printMessage("SERVER - Username set to: " + username);
		}else if(message.contains("listall")){
			if(message.length() > 7)
				updatePeopleOnline(message.substring(7));
		}else if(message.contains("Connected to ")){
			hasPartner = true;
			printMessage("SERVER - " + message);
		}else if(message.contains(" has disconnected")){
			hasPartner = false;
			printMessage("SERVER - " + message);
		}
		else{
			printMessage("SERVER - " + message);
		}
	}
	
	public void updatePeopleOnline(String usernames){
		if(!previousUsernames.equals(usernames)){
			previousUsernames = usernames;
			onlinePeople = usernames.split(",");
			model.removeAllElements();
			for(String names : onlinePeople){
				model.addElement(names);
			}
		}
	}
	
	public void clientMessages(String message, String username){
		printMessage(username + " - " + message);
	}
	
	public void printMessage(String message){
		if(mainMessage.getText().isEmpty()){
			mainMessage.setText(message);
		}else{
			mainMessage.append("\n" + message);
		}
	}
	
	public String readMessages(){
		try {
			input = serverSocket.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		inputReader = new InputStreamReader(input);
		br = new BufferedReader(inputReader);
		
		String message = null;
		try {
			message = br.readLine();
		} catch (IOException e) {
			System.err.println("Disconnected From Server");
			disconnected();
		}
		try {
			message = aes.decrypt(message, salt);
		} catch (Exception e) {
			System.err.println("Decryption Failed");
		}
		return message;
	}
	
	public void userSendMessage(String message){
		if(connected){
			try {
				output = serverSocket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			outputWriter = new OutputStreamWriter(output);
			bw = new BufferedWriter(outputWriter);
			
			try {
				message = aes.encrypt("messagex " + message, salt);
			} catch (Exception e1) {
				System.err.println("Encryption Failed");
			}
			
			try {
				bw.write(message);
				bw.newLine();
				bw.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else{
			printMessage("Please enter a username first");
		}
	}
	
	public void clientSendMessage(String message){
		if(connected){
			try {
				output = serverSocket.getOutputStream();
			} catch (IOException e) {
				disconnected();
			} catch (NullPointerException e){
				disconnected();
			}
			
			outputWriter = new OutputStreamWriter(output);
			bw = new BufferedWriter(outputWriter);
			
			try {
				message = aes.encrypt("username " + message, salt);
			} catch (Exception e1) {
				System.err.println("Encryption Failed");
			}
			
			try {
				bw.write(message);
				bw.newLine();
				bw.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else{
			System.out.println("here");
			disconnected();
		}
	}
	
	public void userSend(){
		userSendMessage(messageTF.getText());
		if(!messageTF.getText().substring(0, 1).equals("/") && connected && username != null && hasPartner){
			printMessage(username + " - " + messageTF.getText());
		}else if(username == null){
			printMessage("Please enter a username first");
		}else if(!hasPartner && !messageTF.getText().substring(0, 1).equals("/")){
			printMessage("You must connect to someone");
		}
		messageTF.setText("");
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		if(e.getSource() == onlineList){
			if(e.getClickCount() == 2){
				userSendMessage("/connect " + onlineList.getSelectedValue());
			}
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == connectB){
			clientSendMessage(usernameTF.getText());
		}else if(e.getSource() == sendB){
			if(!messageTF.getText().isEmpty()){
				userSend();
			}
		}
		
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if(e.getKeyCode() == 10 && e.getSource() == messageTF){
			if(!messageTF.getText().isEmpty()){
				userSend();
			}
		}else if(e.getKeyCode() == 10 && e.getSource() == usernameTF){
			if(!usernameTF.getText().isEmpty()){
				clientSendMessage(usernameTF.getText());
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		
	}
	
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setBackground(new java.awt.Color(102, 102, 102));
        setVisible(true);
        
        mainMessage.setBackground(new java.awt.Color(51, 51, 51));
        mainMessage.setColumns(20);
        mainMessage.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 14)); // NOI18N
        mainMessage.setForeground(new java.awt.Color(255, 255, 255));
        mainMessage.setRows(5);
        mainMessage.setEditable(false);
        mainMessage.setLineWrap(true);
        mainMessage.setWrapStyleWord(true);
        caret = (DefaultCaret) mainMessage.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        jScrollPane1.setViewportView(mainMessage);

        messageTF.setBackground(new java.awt.Color(51, 51, 51));
        messageTF.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 14)); // NOI18N
        messageTF.setForeground(new java.awt.Color(255, 255, 255));
        messageTF.addKeyListener(this);
        
        sendB.setBackground(new java.awt.Color(51, 51, 51));
        sendB.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 14)); // NOI18N
        sendB.setForeground(new java.awt.Color(255, 255, 255));
        sendB.setText("Send");
        sendB.addActionListener(this);

        usernameL.setFont(new java.awt.Font("Microsoft Sans Serif", 1, 12)); // NOI18N
        usernameL.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        usernameL.setText("Username:");
        
        usernameTF.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 14)); // NOI18N
        usernameTF.addKeyListener(this);
        
        connectB.setBackground(new java.awt.Color(51, 51, 51));
        connectB.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 18)); // NOI18N
        connectB.setForeground(new java.awt.Color(255, 255, 255));
        connectB.setText("Connect");
        connectB.addActionListener(this);
        
        onlineList.setBackground(new java.awt.Color(51, 51, 51));
        onlineList.setForeground(new java.awt.Color(255, 255, 255));
        onlineList.addMouseListener(this);
        jScrollPane2.setViewportView(onlineList);

        onlineLabel.setFont(new java.awt.Font("Microsoft Sans Serif", 1, 18)); // NOI18N
        onlineLabel.setForeground(new java.awt.Color(255, 255, 255));
        onlineLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        onlineLabel.setText("People Online");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(onlineLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(usernameL, javax.swing.GroupLayout.DEFAULT_SIZE, 71, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(usernameTF, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(connectB, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(48, 48, 48)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 493, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(messageTF, javax.swing.GroupLayout.PREFERRED_SIZE, 398, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sendB, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(onlineLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane2)
                        .addGap(43, 43, 43)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(usernameTF, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(usernameL, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(connectB, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(20, 20, 20))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 347, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(messageTF)
                    .addComponent(sendB, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }          
    
	@Override
	public void mousePressed(MouseEvent e) {

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		
	}
	
}
