package com.test;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class TPCParticipant {
	static String decision = "";
	static DataInputStream inStream = null;
	static DataOutputStream outStream = null;
	static Socket pSocket = null;
	static String clienName = null;
	static String currentState = "INIT";
	static String data = "";
	static Timer timer = null;

	public static void main(String[] args) {
		try {
			pSocket = new Socket("localhost", 9800);

			inStream = new DataInputStream(pSocket.getInputStream());
			outStream = new DataOutputStream(pSocket.getOutputStream());

			TPCParticipantThread thr = new TPCParticipant().new TPCParticipantThread();
			thr.start();
			
			System.out.println("Enter the client name :");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			clienName = br.readLine();

			outStream.writeUTF("STARTED::CLIENT::" + clienName);

			System.out.println("You are now connected");

			timer = new Timer();

			/*
			 * initializing the timer task to check whether the voting request
			 * is received or not
			 */
			TimerTask tt = new TimerTask() {

				@Override
				public void run() {
					if (data.equals("") && currentState.equals("INIT")) {
						System.out.println("No Voting request received, Local Abort...!\n");
						currentState = "ABORT";
					}
				}
			};

			timer.schedule(tt, 60000);
			

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public class TPCParticipantThread extends Thread {
		@Override
		public void run() {
			try {
				String strLine = null;
				String array[] = null;
				String msgCommand = null;
				while (true) {

					strLine = inStream.readUTF();

					array = strLine.split("::");

					msgCommand = array[1];

					if (msgCommand.equalsIgnoreCase("GLOBAL_COMMIT")) {
						decision = "GC";
						currentState = "COMMIT";
						System.out.println("Received global commit, so commiting the transaction");
						data = "";
					} else if (msgCommand.equalsIgnoreCase("GLOBAL_ABORT")) {

						System.out.println("Received Global Abort, so aborting the transaction.\n");

						currentState = "ABORT";
						decision = "GA";

						data = "";

					} else {
						data = array[1];
						System.out.println("Give a VOTE to COMMIT OR ABORT the message : " + data + "\n");
						BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
						String opt = br.readLine();

						if ("COMMIT".equalsIgnoreCase(opt)) {
							
							outStream.writeUTF(opt);
							currentState = "READY";
							decision = "";
							
							timer = new Timer();
							
							/*initiating timer to check of whether the response has received or not*/
							TimerTask timerTask2 = new TimerTask() {
								
								@Override
								public void run() {
									/*check to see whether decision has arrived or not*/
									if(!(decision.equals("GC")||decision.equals("GA"))) {
										System.out.println("No Response from the coordinator, so local ABORT !\n");
										data = "";
										currentState = "ABORT";
									}
								}
							};
							//scheduling to execute after 90 seconds
							timer.schedule(timerTask2, 90000);
							
						} else if ("ABORT".equalsIgnoreCase(opt)) {
							if(!"".equalsIgnoreCase(data)){
								outStream.writeUTF(opt);
								currentState = "ABORT";
								data="";
							}
						}

						timer.cancel();
						timer.purge();
					}
					System.out.println("currentState :: "+currentState);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
