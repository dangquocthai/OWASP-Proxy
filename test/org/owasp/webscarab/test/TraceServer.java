package org.owasp.webscarab.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.owasp.webscarab.io.CopyInputStream;
import org.owasp.webscarab.model.Request;
import org.owasp.webscarab.model.Response;

public class TraceServer implements Runnable {
	
	private Thread thread;
	
	private ServerSocket socket;
	
	public TraceServer(int port) throws IOException {
		try {
			InetAddress address = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
			socket = new ServerSocket(port, 20, address);
			socket.setReuseAddress(true);
			socket.setSoTimeout(500);
		} catch (UnknownHostException uhe) {
			// should never happen
		}
	}
	
	public void run() {
		try {
			synchronized(this) {
				if (thread == null) {
					thread = Thread.currentThread();
				} else {
					throw new RuntimeException("Multiple threads! " + thread + " and " + Thread.currentThread());
				}
			}
	        while (!Thread.interrupted()) {
	            try {
	                CH ch = new CH(socket.accept());
	                Thread thread = new Thread(ch);
	                thread.setDaemon(true);
	                thread.start();
	            } catch (SocketTimeoutException ste) {
	            }
	        }
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		try {
			if (socket != null && !socket.isClosed())
				socket.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		synchronized(this) {
			thread = null;
			notifyAll();
		}
	}

	public void stop() {
		if (thread != null && thread.isAlive()) {
			thread.interrupt();
			synchronized(this) {
				while (!isStopped()) {
					try {
						wait();
					} catch (InterruptedException ie) {}
				}
			}
		}
	}
	
	public synchronized boolean isStopped() {
		return thread == null || !thread.isAlive();
	}
	
	private static class CH implements Runnable {
		
		private Socket socket;
		
		public CH(Socket socket) {
			this.socket = socket;
		}
		
		public void run() {
			try {
				ByteArrayOutputStream copy = new ByteArrayOutputStream();
				CopyInputStream in = new CopyInputStream(socket.getInputStream(), copy);
				OutputStream out = socket.getOutputStream();
				
				boolean close = true;
				do {
                	copy.reset();
            		Request request = null;
            		// read the whole header. Each line gets written into the copy defined
            		// above
            		while (!"".equals(in.readLine()))
            			;
            		
            		{
	            		byte[] headerBytes = copy.toByteArray();
	            		
	                    // empty request line, connection closed?
	            		if (headerBytes == null || headerBytes.length == 0)
	            			return;
	            		
	            		request = new Request();
	                    request.setHeader(headerBytes);
            		}
            		
                    // Get the request content (if any) from the stream,
                    if (Request.flushContent(request, in, null))
                    	request.setMessage(copy.toByteArray());
	            		
                    Response response = new Response();
                    response.setStartLine("HTTP/1.0 200 Ok");
                    response.setContent(request.getMessage());
                    out.write(response.getMessage());
                    out.flush();
                    String connection = request.getHeader("Connection");
	                if ("Keep-Alive".equalsIgnoreCase(connection)) {
	                    close = false;
	                } else {
	                    close = true;
	                }
				} while (!close);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (!socket.isClosed())
						socket.close();
				} catch (IOException ioe2) {}
			}
		}
	}
	
}
