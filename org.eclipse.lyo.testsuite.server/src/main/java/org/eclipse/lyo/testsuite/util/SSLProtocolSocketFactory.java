/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *
 *    Matt Dawson - initial version
 *    Dong Bing Li
 *    Michael Fiedler
 *******************************************************************************/
package org.eclipse.lyo.testsuite.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;



public class SSLProtocolSocketFactory implements SecureProtocolSocketFactory {

	public static final ProtocolSocketFactory INSTANCE = new SSLProtocolSocketFactory();

	private SSLContext ctx = null ;

    private SSLProtocolSocketFactory() {
        super();
    }


    private class AcceptAllTrustManager implements X509TrustManager {

            public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                            throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                            throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
            }

    }



    public Socket createSocket(String host,int port,InetAddress cHost, int cPort) throws IOException, UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(host,port,cHost,cPort
        );
    }

    public Socket createSocket(String host,int port,InetAddress lAddress,int lPort,HttpConnectionParams parms)
    		throws IOException, UnknownHostException, ConnectTimeoutException {

        return createSocket(host, port, lAddress, lPort);
    }

    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
    	SSLContext ctx = getSSLContext();
    	SSLSocketFactory sf = ctx.getSocketFactory();
    	Socket socket = sf.createSocket(host, port);
        return socket;
    }

    public Socket createSocket(Socket socket,String host,int port,boolean autoclose) throws IOException, UnknownHostException {
    	SSLContext ctx = getSSLContext();
    	SSLSocketFactory sf = ctx.getSocketFactory();
    	Socket newSocket = sf.createSocket(socket, host, port, autoclose);
        return newSocket;
    }

    private SSLContext getSSLContext(){
    	if(ctx == null)
    	{
    		try {
				ctx = SSLContext.getInstance("SSL");
				TrustManager[] tm = new TrustManager[] { new AcceptAllTrustManager() };
	    		ctx.init(null, tm, null);
    		} catch (Exception e)
    		{
    			e.printStackTrace();
    		}
    	}
    	return ctx;
    }


}
