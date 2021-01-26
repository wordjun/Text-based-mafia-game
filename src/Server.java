
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
			//System.out.println("새로운 플레이어가 방에 입장했습니다.");
			barrier.await();//대기...8명이 모두 도착하면 barrier.await에서 대기하다가 풀려난다(8명 모두)
			Thread.sleep(1000);
		} catch (InterruptedException | BrokenBarrierException e) {
			e.printStackTrace();
		}
		//System.out.println("Game start");//8개 쓰레드, 총 8번출력
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
		//여기서 낮과 밤을 진행
		try {
			if(isDay) {
				this.isDay = false;//토론시간 때는 투표못하게 잠시 false로
				Thread.sleep(delay*1000);
				System.out.println("\n투표할 시간이 되었습니다. 투표 명령어([!vote #])를 통해 투표를 하십시오.");
				System.out.println("(투표 30초 후 종료)");
				this.isDay = true;//투표시간 다돼서는 다시 true
				Thread.sleep(30000);
				latch.countDown();//latch의 count를 1감소시킴. 
				//만약 count가 0이 된다면 기다리고 있던 모든 thread들을 release하게 된다.
			}
			else {
				Thread.sleep(delay*1000);
				System.out.println("능력을 사용할 시간이 되었습니다. 지목 명령어([!point #])를 통해 능력을 행할 대상을 지목하십시오.");
				System.out.println("(능력 사용 30초 후 종료)");
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
	public static int totalNumOfPlayers = 0;//각 플레이어에게는 1~8 사이의 정수가 주어진다
	public static int currentPlayerNum = 0;
	//game variables
	public static int day = 0;
	public static int numOfCitizens = 0;
	public static int numOfMafias = 0;
	public static int mafia1 = 0, mafia2 = 0;
	
	//이 배열은 직업에 랜덤성을 부여하기 위해 새 게임 시작마다 섞이게 된다.
	public static String[] jobs = {"Citizen", "Police", "Mafia", "Citizen", "Mafia", "Citizen", "Doctor", "Citizen"};
	//매일 낮 투표시간을 통해 얻은 득표수를 저장할 배열
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
			System.out.println("[PLAYER"+ (i + 1) +"]의 득표수: " + playerVotes[i]);
			if(maxVotes < playerVotes[i]) {
				maxVotes = playerVotes[i];
				maxPNum = i + 1;
			}
		}
		for(int vote : playerVotes) {
			if(maxVotes == vote)
				count++;
		}
		if(count == 1) {//최대 득표수를 얻은 사람이 1명인 경우
			System.out.println("\n가장 많은 표를 받은 플레이어는 " + maxVotes+ "표를 받은 [PLAYER"+maxPNum+"]입니다.");
			Thread.sleep(3000);
			System.out.println("==========[PLAYER" + maxPNum + "]이(가) 처형당했습니다...==========");
			isAlive[maxPNum - 1] = false;
			if(jobs[maxPNum - 1].equals("Mafia")) {
				numOfMafias--;//마피아 1명 감소
			}
			else {//시민을 죽인 경우
				numOfCitizens--;//시민 1명 감소
			}
		}
		else {//최대 득표수를 얻은 사람이 1명 이상인 경우
			System.out.println("\n가장 많은 표를 받은 사람이 "+ count +"명 입니다.");
			Thread.sleep(3000);
			System.out.println("따라서 오늘 낮엔 아무도 죽지 않습니다.");
		}
		
		for(int i = 0;i<8;i++)//모든 투표수 초기화
			playerVotes[i] = 0;
		
		Thread.sleep(2000);
		System.out.println("Remaining players: " + (numOfCitizens +numOfMafias));
		Thread.sleep(3000);
	}
	
	public static void startGame() throws InterruptedException {
		while(numOfMafias < numOfCitizens || numOfMafias != 0) { //게임이 끝나는 경우는 단 두가지(마피아 승 OR 시민 승)
			CountDownLatch dayLatch  = new CountDownLatch(1);//한번에 하나만 실행(daytime따로, nighttime 따로 진행)
			CountDownLatch nightLatch = new CountDownLatch(1);
			
			day++;
			System.out.println("\n==========" + day + "번째날이 되었습니다.==========");
			
			//Daytime
			Thread.sleep(2000);
			System.out.println("\n지금은 낮입니다. 토론을 진행하세요.");
			today = new Day(dayLatch, 30, true);
			new Thread(today).start();
			dayLatch.await();//투표종료와 함께 득표수 공개
			showVotes();
			
			//게임 종료여부 확인
			System.out.println();
			for(int i = 0;i<8;i++) {
				if(isAlive[i])
					System.out.println("[PLAYER" + (i + 1) + "]: Alive");
				else
					System.out.println("[PLAYER" + (i + 1) + "]: Dead");
			}
			if(numOfMafias == 0) {//시민이 이긴경우
				//투표이후 마피아 수 확인
				break;
			}
			if(numOfMafias == numOfCitizens)//마피아가 이긴경우
				break;
			
			//isDaytime = false;
			//Night time
			Thread.sleep(3000);
			System.out.println("\n밤이 되었습니다.");
			today = new Day(nightLatch, 3, false);
			//isNighttime = true;
			new Thread(today).start();
			nightLatch.await();//능력 사용완료 후 대기상태에서 깨어남
			
			//능력 사용결과
			Thread.sleep(3000);
			System.out.println("\n아침이 밝았습니다.");
			Thread.sleep(3000);
			if(healedPlayerNum == targetedPlayerNum && healedPlayerNum != 0) {//의사가 치료한 상대가 마피아들의 제거대상이었다면 그 타겟은 생존한다
				System.out.println("마피아들이 암살에 실패했습니다.");
				Thread.sleep(2000);
				System.out.println("[PLAYER" + healedPlayerNum + "]이(가) 의사의 치료를 받고 공격을 버텨냈습니다!");
			}
			else if(healedPlayerNum != targetedPlayerNum) {//마피아들이 암살에 성공한 경우
				System.out.println("마피아들이 공격에 성공했습니다.");
				Thread.sleep(2000);
				System.out.println("==========[PLAYER" + targetedPlayerNum + "]이(가) 사망했습니다.==========");
				isAlive[targetedPlayerNum - 1] = false;
				
				//마피아가 서로를 배신한 경우도 있을 수 있음
				if(jobs[targetedPlayerNum - 1].equals("Mafia")) {
					numOfMafias--;//마피아 1명 감소
				}
				else {//시민을 죽인 경우
					numOfCitizens--;//시민 1명 감소
				}
				
				if(numOfCitizens == numOfMafias) {
					//투표이후 마피아 수 확인
					break;
				}
				else if(numOfMafias == 0)//이 경우는 마피아가 서로를 배신하거나 자결한 경우이다.
					break;
			}
			else if(targetedPlayerNum == 0) {
				System.out.println("아무 일도 일어나지 않았습니다.");
			}
			
			
			//System.out.println("플레이어 한명이 제거되었습니다.");
			Thread.sleep(3000);
			System.out.println();
			for(int i = 0;i<8;i++) {
				if(isAlive[i])
					System.out.println("[PLAYER" + (i + 1) + "]: Alive");
				else
					System.out.println("[PLAYER" + (i + 1) + "]: Dead");
			}
			System.out.println("Remaining players: " + (numOfCitizens +numOfMafias));
			//초기화
			healedPlayerNum = 0;
			targetedPlayerNum = 0;
			investigatedPlayerNum = 0;
			//isNighttime = false;
			Thread.sleep(3000);
		}
		
		if(numOfMafias == 0) {
			System.out.println("\n모든 마피아들이 죽은 관계로 게임이 끝났습니다.");
			Thread.sleep(2000);
			System.out.println("☆☆☆☆☆시민팀 승리!☆☆☆☆☆\n");
		}
		else if(numOfCitizens == numOfMafias){
			System.out.println("\n시민팀의 인원수와 마피아팀의 인원수가 같아져 게임이 끝났습니다.");
			Thread.sleep(2000);
			System.out.println("★★★★★마피아팀 승리!★★★★★\n");
		}
		Thread.sleep(2000);
		//직업공개
		for(int i = 0;i<8;i++)
			System.out.println("[PLAYER" + (i + 1) + "]: " + jobs[i]);
		Thread.sleep(3000);
	}
	public static void main(String[] args) throws InterruptedException {
		ExecutorService eService = Executors.newFixedThreadPool(8);//쓰레드 풀을 사용한다
		CyclicBarrier barrier = new CyclicBarrier(8);//장벽 해제(Player.run())시 작업 runnable
		//쓰레드 8개만 쓰겠다는 뜻(8명의 멤버)
		Collections.shuffle(Arrays.asList(jobs));//직업 리스트는 미리 셔플
		String job = null;
		System.out.println("연결 대기 중 ......");
		try(ServerSocket sSocket = new ServerSocket(10000);
				ServerSocket pSocket = new ServerSocket(10001);){
			while(true) {
				clientSocket = sSocket.accept();
				policeSocket = pSocket.accept();
				//정원(8명)까지 계속 client받음
				job = generateRole();
				totalNumOfPlayers++;
				
				Player player = new Player(barrier, job, totalNumOfPlayers);
				Server server = new Server(clientSocket,policeSocket, player);
				eService.submit(server);
				
				Thread.sleep(2000);
				if(totalNumOfPlayers == 8) {
					System.out.println("\n\n**********게임을 시작합니다!**********");
					startGame();//게임시작 루틴
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		Thread.sleep(2000);
		System.out.println("마피아 게임을 종료합니다.");
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
			System.out.println(currentPlayerNum + "번 플레이어가 게임에 참가했습니다.");//+ 3/8 식으로 현재 인원 수 나타내기	
			//System.out.println("방금 참가한 플레이어는: " + job + "입니다.");
			
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
					out.println("마피아 팀: [PLAYER" + mafia1 + "], [PLAYER" + mafia2 + "]");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			
			while((inputLine = br.readLine()) != null) {//&& isAlive[this.player.playerNumber - 1]
				
				if(inputLine.startsWith("!")) {
					//명령어는 parse처리
					inputLine = inputLine.substring(1);//느낌표는 제거
					String[] input = inputLine.split(" ");//문자열을 명령어와 지목한 상대로 나눔
					String command = input[0];
					int target = Integer.parseInt(input[1]);
					//System.out.println(target);
					//System.out.println(command + ", " + target);
					if(command.equals("vote")) {
						if(today.isDay) {//낮에는 투표가능
							playerVotes[target - 1]++;
							//System.out.println("[" + playerName + "]이(가) "+ target +"번 플레이어에게 투표했습니다.");
						}
						else {
							System.out.println("아직 투표시간이 아닙니다.");
						}
						if(job.equals("Police"))
							policeLine.println("ignore");
					}
					else if(command.equals("point")) {
						if(!today.isDay) {//밤에는 자신의 직업에 기반한 능력을 사용가능
							//System.out.println("[" + playerName + "]가 " +target + "번 플레이어를 지목했습니다.");
							if(job.equals("Doctor")) {
								healedPlayerNum = target;
								System.out.println("의사가 누군가를 치료했습니다.");
							}
							else if(job.equals("Police")){
								investigatedPlayerNum = target;
								System.out.println("경찰이 누군가를 조사했습니다.");
								//경찰은 조사내용을 자신만이 알 수 있으므로, 낮에 다른 플레이어들과 대화를 통해 결과를 알려줄 수 있다.
								if(jobs[investigatedPlayerNum - 1].equals("Mafia")) {
									policeLine.println("당신이 조사한 대상은 마피아가 맞습니다.");
								}
								else if(jobs[investigatedPlayerNum - 1].equals("Citizen") ||
										jobs[investigatedPlayerNum - 1].equals("Doctor")){
									policeLine.println("당신이 조사한 대상은 마피아가 아닙니다.");
								}
							}
							else if(job.equals("Mafia")){
								targetedPlayerNum = target;
								System.out.println("마피아팀이 누군가를 목표로 삼았습니다.");
							}
							else {
								//일반 시민은 특수 능력이 없습니다.
								System.out.println("일반 시민은 타인을 지목할 수 없습니다.");
							}
						}
						else {
							System.out.println("아직 밤이 되지 않았습니다.");
						}
					}
					else {
						System.out.println("잘못된 명령어입니다.");
						if(job.equals("Police"))
							policeLine.println("ignore");
					}
					//policeLine.println("ignore");
				}
				else {
					//inputLine은 각 Client에서 보낸 메시지
					System.out.println("[" + playerName + "]: " + inputLine);
					if(job.equals("Police"))
						policeLine.println("ignore");
				}
				
//				else
//					out.println("ignore");
			}
			
			System.out.println(Thread.currentThread().getName() + "이(가) 게임을 종료했습니다.");
			
			//대기중에서 나가는 경우
			//currentPlayerNum--;
			//System.out.println("Waiting for more players...[" + totalNumOfPlayers + "/8] players waiting");
		}catch(IOException ex) {
			ex.printStackTrace();
		}
	}
}