package com.test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TPCServer {
	static ServerSocket serverSocket;
	static Socket client;
	static DataOutputStream dos;
	List<String> clientNames = new ArrayList<>();
	static List<DataOutputStream> streamList = new ArrayList<>();
	static DataOutputStream coStream = null;

	public static void main(String[] args) {
		try {
			//Initiating the server on port 9800
			serverSocket = new ServerSocket(9800);
			System.out.println("Server Started ...!!!");
			while (true) {
				Socket socket = serverSocket.accept();
				TPCThread thr = new TPCServer().new TPCThread();
				thr.socket = socket;
				thr.dopStream = new DataOutputStream(socket.getOutputStream());
				streamList.add(thr.dopStream);

				thr.start();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public class TPCThread extends Thread {
		Socket socket = null;
		DataOutputStream dopStream = null;
		DataInputStream dinStream = null;

		@Override
		public void run() {
			String strLine = null;
			String array[] = null;
			String name = null;
			try {
				dinStream = new DataInputStream(socket.getInputStream());

				while ((strLine = dinStream.readUTF()) != null) {
					System.out.println("strLine ::: "+strLine);
					
					if (strLine.contains("::")) {
						array = strLine.split("::");
						if(array.length==3){
							name = array[2];	
						}
					}

					if (strLine.startsWith("STARTED")) {
						if ("COORDINATOR".equalsIgnoreCase(array[1])) {
							if (streamList.size() == 1) {
								coStream = new DataOutputStream(streamList.get(0));
								streamList.remove(0);
							} else {
								coStream = new DataOutputStream(streamList.get(streamList.size() - 1));
								streamList.remove(streamList.size() - 1);
								coStream.writeUTF("PARTICIPANT_LIST::" + streamList.toString());
							}
						} else {
							clientNames.add(name);
							coStream.writeUTF("CONNECTED::" + name);

						}
					} else if (strLine.contains("VOTING_REQUEST")) {
						sendMessageToParticipants("MESSAGE::"+array[0]+"::VOTING_REQUEST");
					} else if (strLine.contains("GLOBAL_ABORT")) {
						sendMessageToParticipants("MESSAGE::GLOBAL_ABORT");
					} else if (strLine.contains("GLOBAL_COMMIT")) {
						sendMessageToParticipants("MESSAGE::GLOBAL_COMMIT");
					} else if (strLine.contains("COMMIT")) {
						coStream.writeUTF("MESSAGE::COMMIT::" + name);
					} else if (strLine.contains("ABORT")) {
						coStream.writeUTF("MESSAGE::ABORT::" + name);
					} 
				}
				System.out.println("exittt");

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		// to send message to all the participant clients
		public void sendMessageToParticipants(String data) {

			try {
				for (DataOutputStream dataOutputStream : streamList) {

					dataOutputStream.writeUTF(data);

				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
