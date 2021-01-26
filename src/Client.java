
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {
	//�� �������� ���ӿ� �����ϱ� �ռ��� ������ ������ ��´�
	//�� �濡 �� 8���� ������ �ϸ�, �� �� 2���� ���ǾƷ� ����ȴ�. ���ǾƷ� �����ϴ� �ӹ��� ������ �Ѵ�.
	//������ ù�� �㿡�� �� ���� ä�ù濡 �����ϰ� �ڽ��� ������ �����ȴ�
	private static String job;//�� �÷��̾�� �־����� ����
	public static int playerNumber;//�� �÷��̾�Դ� 1~8 ������ ������ �־�����
	public static boolean isAlive = true;
	
	public static void main(String[] args) {
	
		try {
			InetAddress localAddress = InetAddress.getLocalHost();//���� ip�ּ�

			try (Socket cSocket = new Socket(localAddress, 10000);//Socket cSocket = new Socket("127.0.0.1", 9000);
					PrintWriter out = new PrintWriter(cSocket.getOutputStream(), true);
					BufferedReader br = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
					Socket pSocket = new Socket(localAddress, 10001);
					BufferedReader policeLine = new BufferedReader(new InputStreamReader(pSocket.getInputStream()));
							) {
				System.out.println("������ ����Ǿ����ϴ�.");
				Scanner scv = new Scanner(System.in);
				tutorial();
				//receive playerNumber generated from Server
				playerNumber = Integer.parseInt(br.readLine());
				System.out.println("����� "+ playerNumber + "�� �÷��̾��Դϴ�.\n");
				job = br.readLine();
				System.out.println("����� " + job + "(��)�� �����Ǿ����ϴ�.");
				
				
				if(job.equals("Mafia")) {
					System.out.println("����� ��ǥ�� �ù������� �÷��̾���� ��� �����ϴ� ���Դϴ�.");
					System.out.println(br.readLine());
				}
				else if(job.equals("Doctor")) {
					System.out.println("����� ��ǥ�� ���Ǿ��� ������ �����Ͽ� ���ǾƵ��� ���Ŵ���� �츮�� ���Դϴ�.");
				}
				else if(job.equals("Police")) {
					System.out.println("����� ��ǥ�� ��� ���ǾƵ��� ������ ���� ���Դϴ�.");
				}
				else {
					System.out.println("����� ��ǥ�� ��� ���ǾƵ��� ó����Ű�� ���Դϴ�.");
				}
				
				
				while (isAlive) {
					System.out.print("�޼��� �Է� : ");
					String inputLine = scv.nextLine();
					String strFromServer = null;
					if ("!quit".equalsIgnoreCase(inputLine)) {
						break;
					}
					out.println(inputLine); // ������ Ű���� �Է� ��Ʈ���� ����
						
//					String status = br.readLine();
//					if(status != null) {
//						if(status.equals("dead")) {
//							System.out.println("����� �׾����ϴ�.");
//							isAlive = false;
//						}
//						else if(status.equals("ignore"))
//							continue;
//					}
					
					if(job.equals("Police") && (strFromServer = policeLine.readLine()) != null) {//������ ������ ����� �����κ��� ���޹޴´�
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
		System.out.print( "\n*****���Ǿ� ���ӿ� �� ���� ȯ���Ѵ�.*****\n"
				+ "�� ������ �� �������� �÷����ϴ� �ؽ�Ʈ��� ��ġ�����̴�.\n"
				+ "�� �� ���� �� ���� ���ǾƷ� ����ȴ�.\n"
				+ "������ �ο����� �ڵ����� �ù��� �Ǹ�, �� �� �� ���� ����, �׸��� �� �ٸ� �� ���� �ǻ�� ����ȴ�.\n"
				+ "���Ǿ� ���� ���� �㸶�� ������ ����� ����.\n"
				+ "������ ���� �� �ڽ��� ������ �ο��� �� �� ���� �����Ͽ� �ſ��� ������ �� ������,\n"
				+ "�ǻ�� ���� �㸶�� �ڽ� Ȥ�� ���Ǿ����� �����ϱ�� �� �ο��� ġ���� �� �ִ�.\n"
				+ "�ù����� �ο����� ������ �ſ��� ������, ���ǾƵ��� ������ ���縦 �� �� �ִ�.\n"
				+ "�� �÷��̾�� ���� �� ��ǥ�� ���� ó���� �ο��� �����Ѵ�.\n"
				+ "���� ���� ǥ�� ���� �ο��� �� �� ó���ȴ�.\n"
				+ "��� ���ǾƵ��� ����� ��� �ù����� �¸��ϰ�,\n"
				+ "���Ǿ� ���� �ù��� ���� ���� �Ǹ� ���Ǿ����� �¸��� �ȴ�.\n"
				+ "\n+++++���Ǿư��� Ʃ�丮��+++++\n"
				+ "�� ������ ��� ���� �ؽ�Ʈ�� �����մϴ�.\n"
				+ "��ȭ�� ������ �����մϴ�.\n"
				+ "��ǥ�ð��� �Ǹ� !vote ��ɾ ���� ��ǥ�� ���ֽð�,\n"
				+ "���� �Ǿ� �������� �����ϰų�, ġ���ϰų�, Ȥ�� �ϻ��� �õ��Ϸ���\n"
				+ "!point��ɾ ����Ͽ� ������ ���ּ���.\n"
				+ "(ex. [!vote 3] -> 3������ 1ǥ�� �����ϴ�.)\n"
				+ "(ex2. [!point 7] -> 7������ �ڽ��� �ɷ��� ����մϴ�.)\n"
				+ "������ �����ϰ� �ʹٸ� !quit�� ���ּ���.\n"
				+ "������ ���ӵ��� �����°��� �ﰡ���ּ���.\n\n");
	}
}
