package com.test;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class TPCCoordinator {
	private static final String COORDINATOR_NAME = "TWOPC_COORDINATOR";
	static Socket coSoccket = null;
	List<String> clientNames = new ArrayList<String>();
	List<String> toRemove = new ArrayList<String>();
	static Map<String, String> mapVote = new HashMap<String, String>();
	static DataInputStream inStream;
	static DataOutputStream outStream;
	static boolean allVoted = false;
	static Timer timer = null;
	public static void main(String[] args) {
		try {
			coSoccket = new Socket("localhost", 9800);

			/*
			 * Streams for data sending and receiving data through client and
			 * server sockets.
			 */
			inStream = new DataInputStream(coSoccket.getInputStream());
			outStream = new DataOutputStream(coSoccket.getOutputStream());

			outStream.writeUTF("STARTED::COORDINATOR::" + COORDINATOR_NAME);
			TPCCoordinatorThread thr = new TPCCoordinator().new TPCCoordinatorThread();
			thr.start();
			
			System.out.println("Please Enter a message for voting :");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String message = br.readLine();
			outStream.writeUTF(message + "::VOTING_REQUEST");
			allVoted = false;
			
			timer = new Timer();

			TimerTask tt1 = new TimerTask() {

				@Override
				public void run() {

					int votecount = 0;
					for (String client : mapVote.keySet()) {
						if (mapVote.get(client).equals("COMMIT")) {
							votecount++;
						} else {
							break;
						}
					}
					/* checking all participants have voted or not */
					if (votecount != mapVote.size()) {
						System.out.println("All Participants not responded , sending GLOBAL_ABORT");

						try {
							outStream.writeUTF("GLOBAL_ABORT");
						} catch (IOException e) {
							e.printStackTrace();
						}

						/*
						 * for (String client : votes.keySet()) {
						 * votes.put(client, ""); }
						 */

					}

				}
			};
			timer.schedule(tt1, 60000);
						
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public class TPCCoordinatorThread extends Thread {
		@Override
		public void run() {
			try {
				String strLine = null;
				String array[] = null;
				String inMsg = null;
				while ((strLine = inStream.readUTF()) != null) {

					array = strLine.split("::");

					inMsg = array[0];
					String command = null;
					command = array[1];
					String client = null;

					if (inMsg.equals("CONNECTED")) {

						client = array[1];

						if (!(client.equalsIgnoreCase(COORDINATOR_NAME))) {
							clientNames.add(client);

							mapVote.put(client, "");

							System.out.println("A New participant : " + client + " registered \n");
						}

					}
					/* participant client has voted to abort */
					else if (command.equals("ABORT")) {
						client = array[2];
						mapVote.put(client, "ABORT");
						System.out.println("Voted  'ABORT' by the client: " + client + "\n");
						System.out.println("Initiating the GLOBAL ABORT !\n");
						timer.cancel();
						timer.purge();

						outStream.writeUTF("GLOBAL_ABORT");

						toRemove.clear();

						for (String cl : mapVote.keySet()) {
							mapVote.put(cl, "");
						}
					}

					else if (command.equals("COMMIT")) {

						client = array[2];
						mapVote.put(client, "COMMIT");
						System.out.println("Voted  'COMMIT' by the client: " + client + "\n");

						int counter = 0;
						for (String key : mapVote.keySet()) {
							if (mapVote.get(key).equals("COMMIT")) {
								counter++;
							} else {
								break;
							}
						}
						
						if (counter == (mapVote.size())) {

							timer.cancel();
							timer.purge();

							System.out.println(
									"All the participant clients voted to commit, so initiating global commit.\n");
							Thread.sleep(2000);

							outStream.writeUTF("GLOBAL_COMMIT");

							for (String cl : mapVote.keySet()) {
								mapVote.put(cl, "");
							}

						}
					}

					else if (command.equals("PARTICIPANT_LIST")) {

						String userList = array[1].replace("[", "").replace("]", "").trim().replaceAll(", ", ",");
						clientNames.addAll(Arrays.asList(userList.split(",")));
						for (String cl : clientNames) {
							mapVote.put(cl, "");
						}

					}

				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
