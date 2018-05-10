package Server;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class Server {
	
	AsynchronousChannelGroup asynchronousChannelGroup;
	AsynchronousServerSocketChannel serverSocketChannel;
	
	static List<Client> tempClients;
	
	static ConcurrentHashMap<String, Client> clients;
	
	static List<String> matchingList;
	
	public static void main(String[] args) {
		Server server = new Server();
		server.startServer();
	}
	
	public void startServer() {
		try {
			asynchronousChannelGroup = AsynchronousChannelGroup
					.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), Executors.defaultThreadFactory());
			serverSocketChannel = AsynchronousServerSocketChannel.open(asynchronousChannelGroup);
			serverSocketChannel.bind(new InetSocketAddress(9123));
			System.out.println("서버 시작");
		} catch (Exception e) {
			e.printStackTrace();
			if (serverSocketChannel.isOpen()) {
				stopServer();
			}
			return;
		}
		
		serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {

			@Override
			public void completed(AsynchronousSocketChannel socketChannel, Void attachment) {
				// TODO Auto-generated method stub
				try {
					String message = "[서버: " + socketChannel.getRemoteAddress() + ": "
							+ Thread.currentThread().getName() + "]";
					System.out.println(message);
					synchronized (tempClients) {
						tempClients.add(new Client(socketChannel));
					}					
				} catch (IOException e) {
					e.printStackTrace();
				}
				serverSocketChannel.accept(null, this);
			}

			@Override
			public void failed(Throwable exc, Void attachment) {
				// TODO Auto-generated method stub
				if (serverSocketChannel.isOpen()) {
					stopServer();
				}
				exc.printStackTrace();
			}

		});		
		
		tempClients = Collections.synchronizedList(new ArrayList<>());
		matchingList = Collections.synchronizedList(new ArrayList<>());
		clients = new ConcurrentHashMap<>();
	
	}
	
	public void stopServer() {
		try {
			if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
				serverSocketChannel.close();
			}
			if (asynchronousChannelGroup != null && !asynchronousChannelGroup.isShutdown()) {
				asynchronousChannelGroup.shutdown();
			}
			
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
