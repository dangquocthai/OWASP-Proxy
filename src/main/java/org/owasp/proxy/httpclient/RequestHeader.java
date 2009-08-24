package org.owasp.proxy.httpclient;

import java.net.InetSocketAddress;

public interface RequestHeader extends MessageHeader {

	InetSocketAddress getTarget();

	boolean isSsl();

	String getMethod() throws MessageFormatException;

	String getResource() throws MessageFormatException;

	String getVersion() throws MessageFormatException;

	long getTime();

}
