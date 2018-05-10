package Server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Packet {

	String protocol;
	String body = "";

	public Packet(String protocol) {
		this.protocol = protocol;
	}

	public void addData(String... data) {
		this.body = "";
		if (data.length >= 1) {
			for (int i = 0; i < data.length - 1; i++) {
				this.body += (data[i] + "|");
			}
			this.body += data[data.length - 1];
		}
	}

	public byte[] toByteArray() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] protocolToByte = protocol.getBytes("UTF-8");
		byte[] bodyToByte = this.body.substring(0, body.length()).getBytes("UTF-8");
		baos.write(protocolToByte);
		baos.write(PacketUtil.intToByteArray(bodyToByte.length));
		baos.write(bodyToByte);
		return baos.toByteArray();
	}
}
