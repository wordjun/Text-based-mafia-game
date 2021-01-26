
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

class Player implements Runnable{
	private CyclicBarrier barrier;
	public String role;
	public int playerNumber;
	//constructor
	public Player(CyclicBarrier barrier, String role, int playerNumber) {
		this.barrier = barrier;
		this.role = role;
		this.playerNumber = playerNumber;
	}
	
	@Override
	public void run() {
		try {
			//System.out.println("���ο� �÷��̾ �濡 �����߽��ϴ�.");
			barrier.await();//���...8���� ��� �����ϸ� barrier.await���� ����ϴٰ� Ǯ������(8�� ���)
			Thread.sleep(1000);
		} catch (InterruptedException | BrokenBarrierException e) {
			e.printStackTrace();
		}
		//System.out.println("Game start");//8�� ������, �� 8�����
	}
}

class Day implements Runnable{
	private	CountDownLatch latch;
	private int delay;
	public boolean isDay = false;
	
	public Day(CountDownLatch latch, int delay, boolean isDay) {
		//constructor
		this.latch = latch;
		this.delay = delay;
		this.isDay = isDay;
	}

	@Override
	public void run() {
		//���⼭ ���� ���� ����
		try {
			if(isDay) {
				this.isDay = false;//��нð� ���� ��ǥ���ϰ� ��� false��
				Thread.sleep(delay*1000);
				System.out.println("\n��ǥ�� �ð��� �Ǿ����ϴ�. ��ǥ ��ɾ�([!vote #])�� ���� ��ǥ�� �Ͻʽÿ�.");
				System.out.println("(��ǥ 30�� �� ����)");
				this.isDay = true;//��ǥ�ð� �ٵż��� �ٽ� true
				Thread.sleep(30000);
				latch.countDown();//latch�� count�� 1���ҽ�Ŵ. 
				//���� count�� 0�� �ȴٸ� ��ٸ��� �ִ� ��� thread���� release�ϰ� �ȴ�.
			}
			else {
				Thread.sleep(delay*1000);
				System.out.println("�ɷ��� ����� �ð��� �Ǿ����ϴ�. ���� ��ɾ�([!point #])�� ���� �ɷ��� ���� ����� �����Ͻʽÿ�.");
				System.out.println("(�ɷ� ��� 30�� �� ����)");
				Thread.sleep(30000);
				latch.countDown();//
			}
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}


public class Server implements Runnable{
	private static Socket clientSocket;
	private static Socket policeSocket;
	private static Player player;
	public static int totalNumOfPlayers = 0;//�� �÷��̾�Դ� 1~8 ������ ������ �־�����
	public static int currentPlayerNum = 0;
	//game variables
	public static int day = 0;
	public static int numOfCitizens = 0;
	public static int numOfMafias = 0;
	public static int mafia1 = 0, mafia2 = 0;
	
	//�� �迭�� ������ �������� �ο��ϱ� ���� �� ���� ���۸��� ���̰� �ȴ�.
	public static String[] jobs = {"Citizen", "Police", "Mafia", "Citizen", "Mafia", "Citizen", "Doctor", "Citizen"};
	//���� �� ��ǥ�ð��� ���� ���� ��ǥ���� ������ �迭
	public static int[] playerVotes = {0, 0, 0, 0, 0, 0, 0, 0};
	public static boolean[] isAlive= {true, true, true, true, true, true, true, true};
	public static int healedPlayerNum = 0;
	public static int targetedPlayerNum = 0;
	public static int investigatedPlayerNum = 0;
	public static Day today;
	public static CountDownLatch mafiaLatch = new CountDownLatch(2);
	//constructor
	public Server(Socket clientSocket,Socket policeSocket, Player player) {
		this.clientSocket = clientSocket;
		this.policeSocket = policeSocket;
		this.player = player;
	}
	
	public static String generateRole() {
		String job = null;
		//generate random jobs
		//shuffle
		return jobs[currentPlayerNum];
	}
	
	public static void showVotes() throws InterruptedException {
		int maxVotes = -1, maxPNum = 0;
		int count = 0;
		System.out.println();
		for(int i = 0;i<8;i++) {
			System.out.println("[PLAYER"+ (i + 1) +"]�� ��ǥ��: " + playerVotes[i]);
			if(maxVotes < playerVotes[i]) {
				maxVotes = playerVotes[i];
				maxPNum = i + 1;
			}
		}
		for(int vote : playerVotes) {
			if(maxVotes == vote)
				count++;
		}
		if(count == 1) {//�ִ� ��ǥ���� ���� ����� 1���� ���
			System.out.println("\n���� ���� ǥ�� ���� �÷��̾�� " + maxVotes+ "ǥ�� ���� [PLAYER"+maxPNum+"]�Դϴ�.");
			Thread.sleep(3000);
			System.out.println("==========[PLAYER" + maxPNum + "]��(��) ó�����߽��ϴ�...==========");
			isAlive[maxPNum - 1] = false;
			if(jobs[maxPNum - 1].equals("Mafia")) {
				numOfMafias--;//���Ǿ� 1�� ����
			}
			else {//�ù��� ���� ���
				numOfCitizens--;//�ù� 1�� ����
			}
		}
		else {//�ִ� ��ǥ���� ���� ����� 1�� �̻��� ���
			System.out.println("\n���� ���� ǥ�� ���� ����� "+ count +"�� �Դϴ�.");
			Thread.sleep(3000);
			System.out.println("���� ���� ���� �ƹ��� ���� �ʽ��ϴ�.");
		}
		
		for(int i = 0;i<8;i++)//��� ��ǥ�� �ʱ�ȭ
			playerVotes[i] = 0;
		
		Thread.sleep(2000);
		System.out.println("Remaining players: " + (numOfCitizens +numOfMafias));
		Thread.sleep(3000);
	}
	
	public static void startGame() throws InterruptedException {
		while(numOfMafias < numOfCitizens || numOfMafias != 0) { //������ ������ ���� �� �ΰ���(���Ǿ� �� OR �ù� ��)
			CountDownLatch dayLatch  = new CountDownLatch(1);//�ѹ��� �ϳ��� ����(daytime����, nighttime ���� ����)
			CountDownLatch nightLatch = new CountDownLatch(1);
			
			day++;
			System.out.println("\n==========" + day + "��°���� �Ǿ����ϴ�.==========");
			
			//Daytime
			Thread.sleep(2000);
			System.out.println("\n������ ���Դϴ�. ����� �����ϼ���.");
			today = new Day(dayLatch, 30, true);
			new Thread(today).start();
			dayLatch.await();//��ǥ����� �Բ� ��ǥ�� ����
			showVotes();
			
			//���� ���Ῡ�� Ȯ��
			System.out.println();
			for(int i = 0;i<8;i++) {
				if(isAlive[i])
					System.out.println("[PLAYER" + (i + 1) + "]: Alive");
				else
					System.out.println("[PLAYER" + (i + 1) + "]: Dead");
			}
			if(numOfMafias == 0) {//�ù��� �̱���
				//��ǥ���� ���Ǿ� �� Ȯ��
				break;
			}
			if(numOfMafias == numOfCitizens)//���Ǿư� �̱���
				break;
			
			//isDaytime = false;
			//Night time
			Thread.sleep(3000);
			System.out.println("\n���� �Ǿ����ϴ�.");
			today = new Day(nightLatch, 3, false);
			//isNighttime = true;
			new Thread(today).start();
			nightLatch.await();//�ɷ� ���Ϸ� �� �����¿��� ���
			
			//�ɷ� �����
			Thread.sleep(3000);
			System.out.println("\n��ħ�� ��ҽ��ϴ�.");
			Thread.sleep(3000);
			if(healedPlayerNum == targetedPlayerNum && healedPlayerNum != 0) {//�ǻ簡 ġ���� ��밡 ���ǾƵ��� ���Ŵ���̾��ٸ� �� Ÿ���� �����Ѵ�
				System.out.println("���ǾƵ��� �ϻ쿡 �����߽��ϴ�.");
				Thread.sleep(2000);
				System.out.println("[PLAYER" + healedPlayerNum + "]��(��) �ǻ��� ġ�Ḧ �ް� ������ ���߳½��ϴ�!");
			}
			else if(healedPlayerNum != targetedPlayerNum) {//���ǾƵ��� �ϻ쿡 ������ ���
				System.out.println("���ǾƵ��� ���ݿ� �����߽��ϴ�.");
				Thread.sleep(2000);
				System.out.println("==========[PLAYER" + targetedPlayerNum + "]��(��) ����߽��ϴ�.==========");
				isAlive[targetedPlayerNum - 1] = false;
				
				//���Ǿư� ���θ� ����� ��쵵 ���� �� ����
				if(jobs[targetedPlayerNum - 1].equals("Mafia")) {
					numOfMafias--;//���Ǿ� 1�� ����
				}
				else {//�ù��� ���� ���
					numOfCitizens--;//�ù� 1�� ����
				}
				
				if(numOfCitizens == numOfMafias) {
					//��ǥ���� ���Ǿ� �� Ȯ��
					break;
				}
				else if(numOfMafias == 0)//�� ���� ���Ǿư� ���θ� ����ϰų� �ڰ��� ����̴�.
					break;
			}
			else if(targetedPlayerNum == 0) {
				System.out.println("�ƹ� �ϵ� �Ͼ�� �ʾҽ��ϴ�.");
			}
			
			
			//System.out.println("�÷��̾� �Ѹ��� ���ŵǾ����ϴ�.");
			Thread.sleep(3000);
			System.out.println();
			for(int i = 0;i<8;i++) {
				if(isAlive[i])
					System.out.println("[PLAYER" + (i + 1) + "]: Alive");
				else
					System.out.println("[PLAYER" + (i + 1) + "]: Dead");
			}
			System.out.println("Remaining players: " + (numOfCitizens +numOfMafias));
			//�ʱ�ȭ
			healedPlayerNum = 0;
			targetedPlayerNum = 0;
			investigatedPlayerNum = 0;
			//isNighttime = false;
			Thread.sleep(3000);
		}
		
		if(numOfMafias == 0) {
			System.out.println("\n��� ���ǾƵ��� ���� ����� ������ �������ϴ�.");
			Thread.sleep(2000);
			System.out.println("�١١١١ٽù��� �¸�!�١١١١�\n");
		}
		else if(numOfCitizens == numOfMafias){
			System.out.println("\n�ù����� �ο����� ���Ǿ����� �ο����� ������ ������ �������ϴ�.");
			Thread.sleep(2000);
			System.out.println("�ڡڡڡڡڸ��Ǿ��� �¸�!�ڡڡڡڡ�\n");
		}
		Thread.sleep(2000);
		//��������
		for(int i = 0;i<8;i++)
			System.out.println("[PLAYER" + (i + 1) + "]: " + jobs[i]);
		Thread.sleep(3000);
	}
	public static void main(String[] args) throws InterruptedException {
		ExecutorService eService = Executors.newFixedThreadPool(8);//������ Ǯ�� ����Ѵ�
		CyclicBarrier barrier = new CyclicBarrier(8);//�庮 ����(Player.run())�� �۾� runnable
		//������ 8���� ���ڴٴ� ��(8���� ���)
		Collections.shuffle(Arrays.asList(jobs));//���� ����Ʈ�� �̸� ����
		String job = null;
		System.out.println("���� ��� �� ......");
		try(ServerSocket sSocket = new ServerSocket(10000);
				ServerSocket pSocket = new ServerSocket(10001);){
			while(true) {
				clientSocket = sSocket.accept();
				policeSocket = pSocket.accept();
				//����(8��)���� ��� client����
				job = generateRole();
				totalNumOfPlayers++;
				
				Player player = new Player(barrier, job, totalNumOfPlayers);
				Server server = new Server(clientSocket,policeSocket, player);
				eService.submit(server);
				
				Thread.sleep(2000);
				if(totalNumOfPlayers == 8) {
					System.out.println("\n\n**********������ �����մϴ�!**********");
					startGame();//���ӽ��� ��ƾ
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		Thread.sleep(2000);
		System.out.println("���Ǿ� ������ �����մϴ�.");
		eService.shutdown();
	}

	@Override
	public void run() {
		try(BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
				PrintWriter policeLine = new PrintWriter(policeSocket.getOutputStream(), true);
			){
			String inputLine, temp = null;
			String job = this.player.role;
			currentPlayerNum = this.player.playerNumber;
			Thread.currentThread().setName("PLAYER" + currentPlayerNum);
			String playerName = Thread.currentThread().getName();
			System.out.println(currentPlayerNum + "�� �÷��̾ ���ӿ� �����߽��ϴ�.");//+ 3/8 ������ ���� �ο� �� ��Ÿ����	
			//System.out.println("��� ������ �÷��̾��: " + job + "�Դϴ�.");
			
			if(job == "Mafia") {
				numOfMafias++;
			}
			else {
				numOfCitizens++;
			}
			
			if(totalNumOfPlayers < 8)
				System.out.println("Waiting for more players...[" + totalNumOfPlayers + "/8] players waiting");
			else if(totalNumOfPlayers == 8)
				System.out.println("All players have entered the room...[" + totalNumOfPlayers + "/8] players");
			
			//send totalNumOfPlayers to Client
			out.println(currentPlayerNum);
			out.println(job);
			
			if(job.equals("Mafia")) {
				if(numOfMafias == 1 && mafia1 == 0) {
					mafia1 = currentPlayerNum;
					mafiaLatch.countDown();
				}
				else if(numOfMafias == 2 && mafia2 == 0) {
					mafia2 = currentPlayerNum;
					mafiaLatch.countDown();
				}
				
				try {
					mafiaLatch.await();
					out.println("���Ǿ� ��: [PLAYER" + mafia1 + "], [PLAYER" + mafia2 + "]");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			
			while((inputLine = br.readLine()) != null) {//&& isAlive[this.player.playerNumber - 1]
				
				if(inputLine.startsWith("!")) {
					//��ɾ�� parseó��
					inputLine = inputLine.substring(1);//����ǥ�� ����
					String[] input = inputLine.split(" ");//���ڿ��� ��ɾ�� ������ ���� ����
					String command = input[0];
					int target = Integer.parseInt(input[1]);
					//System.out.println(target);
					//System.out.println(command + ", " + target);
					if(command.equals("vote")) {
						if(today.isDay) {//������ ��ǥ����
							playerVotes[target - 1]++;
							//System.out.println("[" + playerName + "]��(��) "+ target +"�� �÷��̾�� ��ǥ�߽��ϴ�.");
						}
						else {
							System.out.println("���� ��ǥ�ð��� �ƴմϴ�.");
						}
						if(job.equals("Police"))
							policeLine.println("ignore");
					}
					else if(command.equals("point")) {
						if(!today.isDay) {//�㿡�� �ڽ��� ������ ����� �ɷ��� ��밡��
							//System.out.println("[" + playerName + "]�� " +target + "�� �÷��̾ �����߽��ϴ�.");
							if(job.equals("Doctor")) {
								healedPlayerNum = target;
								System.out.println("�ǻ簡 �������� ġ���߽��ϴ�.");
							}
							else if(job.equals("Police")){
								investigatedPlayerNum = target;
								System.out.println("������ �������� �����߽��ϴ�.");
								//������ ���系���� �ڽŸ��� �� �� �����Ƿ�, ���� �ٸ� �÷��̾��� ��ȭ�� ���� ����� �˷��� �� �ִ�.
								if(jobs[investigatedPlayerNum - 1].equals("Mafia")) {
									policeLine.println("����� ������ ����� ���Ǿư� �½��ϴ�.");
								}
								else if(jobs[investigatedPlayerNum - 1].equals("Citizen") ||
										jobs[investigatedPlayerNum - 1].equals("Doctor")){
									policeLine.println("����� ������ ����� ���Ǿư� �ƴմϴ�.");
								}
							}
							else if(job.equals("Mafia")){
								targetedPlayerNum = target;
								System.out.println("���Ǿ����� �������� ��ǥ�� ��ҽ��ϴ�.");
							}
							else {
								//�Ϲ� �ù��� Ư�� �ɷ��� �����ϴ�.
								System.out.println("�Ϲ� �ù��� Ÿ���� ������ �� �����ϴ�.");
							}
						}
						else {
							System.out.println("���� ���� ���� �ʾҽ��ϴ�.");
						}
					}
					else {
						System.out.println("�߸��� ��ɾ��Դϴ�.");
						if(job.equals("Police"))
							policeLine.println("ignore");
					}
					//policeLine.println("ignore");
				}
				else {
					//inputLine�� �� Client���� ���� �޽���
					System.out.println("[" + playerName + "]: " + inputLine);
					if(job.equals("Police"))
						policeLine.println("ignore");
				}
				
//				else
//					out.println("ignore");
			}
			
			System.out.println(Thread.currentThread().getName() + "��(��) ������ �����߽��ϴ�.");
			
			//����߿��� ������ ���
			//currentPlayerNum--;
			//System.out.println("Waiting for more players...[" + totalNumOfPlayers + "/8] players waiting");
		}catch(IOException ex) {
			ex.printStackTrace();
		}
	}
}