package Server;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PacketUtil {

	String protocol;
	ByteBuffer byteBuffer;
	int length;
	Client client;
	String receiverID; // �Ѹ����� ��Ŷ ������ ���̵�
	
	PacketUtil(String protocol, int length) {
		this.protocol = protocol;
		this.length = length;
		this.byteBuffer = ByteBuffer.allocate(length);
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public byte[] convertToSendPacket() throws IOException {
		switch (protocol) {
		case "100": { // ����� Ŭ���̾�Ʈ ���� ���
			byte[] body = byteBuffer.array();
			String[] bodyStr = (new String(body, "utf-8")).split("\\|", 5);
			System.out.println("100");
			for(String str : bodyStr){
				System.out.println(str);
			}
			client.phoneNumber = bodyStr[0];
			client.nickName = bodyStr[1];
			client.age = Integer.parseInt(bodyStr[2]);
			client.gender = bodyStr[3];
			client.putInClientMap(bodyStr[0]);					
			break;			
		}
		case "101" : { // ����� ��ư ������ �� ��ü�� ����� �ϴ� ��Ŷ���� �ٲ�
			client.putInMatchingList();
			byte[] body = byteBuffer.array();
			String[] bodyStr = (new String(body, "utf-8")).split("\\|", 2);
			System.out.println("101");
			for(String str : bodyStr){
				System.out.println(str);
			}
			client.latitude = Double.parseDouble(bodyStr[0]);
			client.longitude = Double.parseDouble(bodyStr[1]);
			Packet packet = new Packet("200");
			packet.addData(client.phoneNumber, 
					Integer.toString(client.age), 
					client.gender,
					client.nickName,
					bodyStr[0], 
					bodyStr[1]);
			return packet.toByteArray();
		}
		case "102" : { // Ŭ���̾�Ʈ���� ��Ŀ�� ������ �� �����ִ� ��Ŷ
			byte[] body = byteBuffer.array();
			String bodyStr = new String(body, "utf-8");
			System.out.println("102");
			System.out.println(bodyStr);
			synchronized(Server.matchingList){
				System.out.println("��ũ");
				if(Server.matchingList.contains(bodyStr)){
					//��Ī ���� ��
					Server.matchingList.remove(bodyStr);
					Packet myInfo = new Packet("201");
					Packet yourInfo = new Packet("201");
					this.receiverID = bodyStr;
					
					yourInfo.addData(Server.clients.get(bodyStr).phoneNumber, 
							Server.clients.get(bodyStr).nickName,
							Integer.toString(Server.clients.get(bodyStr).age),
							Server.clients.get(bodyStr).gender);
					client.send(yourInfo.toByteArray());
					
					myInfo.addData(client.phoneNumber, 
							client.nickName,
							Integer.toString(client.age),
							client.gender);
					return myInfo.toByteArray();
				}
				else{
					//��Ī ���� ��
					Packet failed = new Packet("202");
					failed.addData("fail");
					return failed.toByteArray();
				}	
			}		
		}		
		default: {
			break;
		}
		}
	
		return null;
	}

	public static int byteArrayToInt(byte[] byteArray) {
		return (byteArray[0] & 0xff) << 24 | (byteArray[1] & 0xff) << 16 | (byteArray[2] & 0xff) << 8
				| (byteArray[3] & 0xff);
	}

	public static byte[] intToByteArray(int a) {
		return ByteBuffer.allocate(4).putInt(a).array();
	}
	
	public void clear(){
		this.protocol = null;
		this.byteBuffer = null;
		this.client = null;	
	}
}