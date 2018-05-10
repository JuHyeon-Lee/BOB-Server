package Server;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PacketUtil {

	String protocol;
	ByteBuffer byteBuffer;
	int length;
	Client client;
	String receiverID; // 한명한테 패킷 보낼때 아이디
	
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
		case "100": { // 연결시 클라이언트 정보 등록
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
		case "101" : { // 밥먹자 버튼 눌렀을 때 전체에 쏴줘야 하는 패킷으로 바꿈
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
		case "102" : { // 클라이언트에서 마커를 눌렀을 때 보내주는 패킷
			byte[] body = byteBuffer.array();
			String bodyStr = new String(body, "utf-8");
			System.out.println("102");
			System.out.println(bodyStr);
			synchronized(Server.matchingList){
				System.out.println("싱크");
				if(Server.matchingList.contains(bodyStr)){
					//매칭 성공 시
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
					//매칭 실패 시
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