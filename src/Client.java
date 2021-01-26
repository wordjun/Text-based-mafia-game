
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {
	//각 유저들은 게임에 참가하기 앞서서 간략한 설명을 듣는다
	//한 방에 총 8명이 참가를 하며, 그 중 2명이 마피아로 지목된다. 마피아로 지목하는 임무는 서버가 한다.
	//게임의 첫날 밤에는 각 개인 채팅방에 은밀하게 자신의 직업이 공개된다
	private static String job;//각 플레이어에게 주어지는 직업
	public static int playerNumber;//각 플레이어에게는 1~8 사이의 정수가 주어진다
	public static boolean isAlive = true;
	
	public static void main(String[] args) {
	
		try {
			InetAddress localAddress = InetAddress.getLocalHost();//로컬 ip주소

			try (Socket cSocket = new Socket(localAddress, 10000);//Socket cSocket = new Socket("127.0.0.1", 9000);
					PrintWriter out = new PrintWriter(cSocket.getOutputStream(), true);
					BufferedReader br = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
					Socket pSocket = new Socket(localAddress, 10001);
					BufferedReader policeLine = new BufferedReader(new InputStreamReader(pSocket.getInputStream()));
							) {
				System.out.println("서버에 연결되었습니다.");
				Scanner scv = new Scanner(System.in);
				tutorial();
				//receive playerNumber generated from Server
				playerNumber = Integer.parseInt(br.readLine());
				System.out.println("당신은 "+ playerNumber + "번 플레이어입니다.\n");
				job = br.readLine();
				System.out.println("당신은 " + job + "(으)로 배정되었습니다.");
				
				
				if(job.equals("Mafia")) {
					System.out.println("당신의 목표는 시민진영의 플레이어들을 모두 제거하는 것입니다.");
					System.out.println(br.readLine());
				}
				else if(job.equals("Doctor")) {
					System.out.println("당신의 목표는 마피아의 공격을 예측하여 마피아들의 제거대상을 살리는 것입니다.");
				}
				else if(job.equals("Police")) {
					System.out.println("당신의 목표는 모든 마피아들을 색출해 내는 것입니다.");
				}
				else {
					System.out.println("당신의 목표는 모든 마피아들을 처형시키는 것입니다.");
				}
				
				
				while (isAlive) {
					System.out.print("메세지 입력 : ");
					String inputLine = scv.nextLine();
					String strFromServer = null;
					if ("!quit".equalsIgnoreCase(inputLine)) {
						break;
					}
					out.println(inputLine); // 서버에 키보드 입력 스트링을 전송
						
//					String status = br.readLine();
//					if(status != null) {
//						if(status.equals("dead")) {
//							System.out.println("당신은 죽었습니다.");
//							isAlive = false;
//						}
//						else if(status.equals("ignore"))
//							continue;
//					}
					
					if(job.equals("Police") && (strFromServer = policeLine.readLine()) != null) {//경찰은 조사한 결과를 서버로부터 전달받는다
						if(!strFromServer.equals("ignore"))
							System.out.println(strFromServer);
						else
							continue;
					}
				}
				scv.close();
			}
		} catch (IOException ex) {

		}
	}
	public static void tutorial() {
		System.out.print( "\n*****마피아 게임에 온 것을 환영한다.*****\n"
				+ "이 게임은 총 여덟명이 플레이하는 텍스트기반 정치게임이다.\n"
				+ "이 중 오직 두 명만이 마피아로 지목된다.\n"
				+ "나머지 인원들은 자동으로 시민이 되며, 그 중 한 명은 경찰, 그리고 또 다른 한 명은 의사로 지목된다.\n"
				+ "마피아 팀은 매일 밤마다 제거할 대상을 고른다.\n"
				+ "경찰은 매일 밤 자신을 제외한 인원들 중 한 명을 조사하여 신원을 밝혀낼 수 있으며,\n"
				+ "의사는 매일 밤마다 자신 혹은 마피아팀이 제거하기로 한 인원을 치료할 수 있다.\n"
				+ "시민팀의 인원들은 서로의 신원을 모르지만, 마피아들은 서로의 존재를 알 수 있다.\n"
				+ "각 플레이어는 매일 밤 투표를 통해 처형할 인원을 선택한다.\n"
				+ "가장 많은 표를 받은 인원은 그 날 처형된다.\n"
				+ "모든 마피아들이 사망할 경우 시민팀이 승리하고,\n"
				+ "마피아 수가 시민의 수와 같게 되면 마피아팀의 승리가 된다.\n"
				+ "\n+++++마피아게임 튜토리얼+++++\n"
				+ "본 게임은 모든 것을 텍스트로 진행합니다.\n"
				+ "대화는 낮에만 가능합니다.\n"
				+ "투표시간이 되면 !vote 명령어를 통해 투표를 해주시고,\n"
				+ "밤이 되어 누군가를 조사하거나, 치료하거나, 혹은 암살을 시도하려면\n"
				+ "!point명령어를 사용하여 지목을 해주세요.\n"
				+ "(ex. [!vote 3] -> 3번에게 1표를 던집니다.)\n"
				+ "(ex2. [!point 7] -> 7번에게 자신의 능력을 사용합니다.)\n"
				+ "게임을 종료하고 싶다면 !quit을 쳐주세요.\n"
				+ "하지만 게임도중 나가는것은 삼가해주세요.\n\n");
	}
}
