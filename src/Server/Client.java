package Server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class Client {

	AsynchronousSocketChannel socketChannel;
	String nickName;
	String phoneNumber;
	String gender;
	int age;
	double latitude;
	double longitude;
	
	boolean chatRoomSelected = false;
	CompletionHandler<Integer, ByteBuffer> headerCompleted;
	CompletionHandler<Integer, PacketUtil> bodyCompleted;
	ArrayList<String> groupIDs;
	ArrayList<String> chatIDs;
	
	//write ���̸� list�� ������ ���Ҵٰ� write�� ������ �����ְ� �� ��
	boolean isWriting = false;
	Queue<byte[]> dataTmpList;
	
	private PacketUtil packetUtil;

	public Client(AsynchronousSocketChannel socketChannel) throws IOException {
		this.socketChannel = socketChannel;
		
		headerCompleted = new CompletionHandler<Integer, ByteBuffer>() {

			@Override
			public void failed(Throwable exc, ByteBuffer attachment) {
				// TODO Auto-generated method stub
				exc.printStackTrace();
				attachment.clear();
				stopClient();
			}

			@Override
			public void completed(Integer result, ByteBuffer attachment) {
				// TODO Auto-generated method stub
				if (!attachment.hasRemaining()) {
					byte[] header = attachment.array();
					try {
						String protocol = new String(Arrays.copyOfRange(header, 0, 3), "utf-8");
						int length = PacketUtil.byteArrayToInt(Arrays.copyOfRange(header, 3, 7));
						attachment.clear();
						packetUtil = new PacketUtil(protocol, length);
						packetUtil.setClient(Client.this);
						socketChannel.read(packetUtil.byteBuffer, packetUtil, bodyCompleted);
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} 
				else {
					socketChannel.read(attachment, attachment, this);
				}
			}

		}; // �ϴ� ����� �д´�.

		bodyCompleted = new CompletionHandler<Integer, PacketUtil>() {

			@Override
			public void failed(Throwable exc, PacketUtil attachment) {
				// TODO Auto-generated method stub
				exc.printStackTrace();
				attachment.byteBuffer.clear();
				stopClient();
			}

			@Override
			public void completed(Integer result, PacketUtil attachment) {
				// TODO Auto-generated method stub
				if (!attachment.byteBuffer.hasRemaining()) {
					try {
						String protocol = attachment.protocol;
						if (protocol.equals("100")) {
							byte[] packet = attachment.convertToSendPacket();
							sendToOther(attachment.receiverID, packet);
						} 
						else if(protocol.equals("101")){
							byte[] packet = attachment.convertToSendPacket();
							if(packet != null){						
								sendToAll(packet);
							}
						}
						else if(protocol.equals("102")){
							byte[] packet = attachment.convertToSendPacket();
							if(packet != null){						
								sendToOther(attachment.receiverID, packet);
							}
						}
					} 
					catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					finally {
						attachment.clear();
					}
					receive();
				} 
				else {
					socketChannel.read(attachment.byteBuffer, attachment, this);
				}
			}

		}; // ����� ���� �� �ٵ� �д´�

		dataTmpList = new LinkedList<>();
		receive();
	}

	public synchronized void putInClientMap(String id) {
		this.phoneNumber = id;
		if (Server.clients.containsKey(id)) {
			Server.clients.remove(id);
			Server.clients.put(id, Client.this);
		} else {
			Server.clients.put(id, Client.this);
		}
	}
	
	public synchronized void putInMatchingList(){
		if(this.phoneNumber != null){
			if(!Server.matchingList.contains(this.phoneNumber)){
				Server.matchingList.add(this.phoneNumber);
			}
		}
	}

	public void receive() {
		try {
			System.out.println("���ú�");
			ByteBuffer headerByteBuffer = ByteBuffer.allocate(7);
			socketChannel.read(headerByteBuffer, headerByteBuffer, headerCompleted);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("receive error!");
		}
	}

	//��ü���� ��Ŷ ���� ��
	public synchronized void sendToAll(byte[] data){
		if(data == null){
			System.out.println("Packet is Null - SendToOther");
			return;
		}
		for (String phoneNumber : Server.clients.keySet()) {
			if(!phoneNumber.equals(this.phoneNumber)){
				Server.clients.get(phoneNumber).send(data);
			}			
		}
	}
	
	//�ٸ� ����鿡�� ��Ŷ ���� ��
	public synchronized void sendToOther(String id, byte[] data) {
		if(data == null){
			System.out.println("Packet is Null - SendToOther");
			return;
		}
		if (!id.equals("") && id != null) {
			if(Server.clients.containsKey(id)){
				Server.clients.get(id).send(data);
			}
		}
	}

	public void loadData(){
		if(!dataTmpList.isEmpty()){
			send(dataTmpList.poll());
		}
	}
	
	//�ڱ� �ڽſ��� ��Ŷ ���� ��
	public void send(byte[] data) {
		if(data == null){
			System.out.println("Packet is Null");
			return;
		}
		
		synchronized(dataTmpList){
			if(!dataTmpList.isEmpty() || isWriting){
				dataTmpList.offer(data);
				return;
			}
			isWriting = true;
		}
		
		ByteBuffer byteBuffer = ByteBuffer.wrap(data);
		socketChannel.write(byteBuffer, null, new CompletionHandler<Integer, Void>() {

			@Override
			public void completed(Integer result, Void attachment) {
				// TODO Auto-generated method stub
				if(byteBuffer.hasRemaining()){
					socketChannel.write(byteBuffer, null, this);
				}
				else{
					byteBuffer.clear();
				}
				synchronized (dataTmpList) {
					isWriting = false;
					loadData();
				}
			}

			@Override
			public void failed(Throwable exc, Void attachment) {
				// TODO Auto-generated method stub
				String message = "[Ŭ���̾�Ʈ ��� �ȵ�]";
				System.out.println(message);
				stopClient();
			}
		});
	}

	//���� ��Ŷ ���� ��
	public void sendError(String protocol){
		//������ ������ �� ���� ��Ŷ�� �����ش�. 
		//���� ��Ŷ�� ������ ������ �߻������� Ŭ���̾�Ʈ��	���� ��������
		Packet packet = new Packet("300");
		packet.addData(protocol);
		try {
			send(packet.toByteArray());
			if(protocol.equals("111")){
				socketChannel.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private synchronized void removeFromMaps() {
		if(phoneNumber == null){
			return;
		}
		if(Server.clients.containsKey(phoneNumber)){
			Server.clients.remove(phoneNumber);
		}
	}
	
	private void stopClient(){
		removeFromMaps();
		try {
			socketChannel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
