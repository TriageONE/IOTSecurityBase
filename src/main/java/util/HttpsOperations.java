package util;

import javax.net.ssl.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;

/**
 * <b>CLASS HTTPS</b>
 * <p><b>DESCRIPTION: </b>provides static methods for creating custom HTTPS post and get requests</p>
 * @author Aron
 * @version 1.0
 * @apiNote When creating headers, avoid having identical key with different values, as this will corrupt header assignment
 */
public class HttpsOperations {

    public HttpsOperations(){}

    /**
     * <b>RECORD REQUEST</b>
     * <p><b>DESCRIPTION: </b>Provides an object class that allows for separate storage of body and status with respective getters, but not setters.
     * @param status the status code generated
     * @param body the string that is given back from the responder
     */
    public record Request(int status, String body) {
        public int getStatus() {
            return status;
        }
        public String getBody() {
            return body;
        }
    }

    /**
     * <b>HttpsOperations POST method formulator</b>
     * <p><b>DESCRIPTION: </b>Formulates a simple POST request with HTTPS to a local or non-local URL</p>
     * @param httpsURL The URL to contact for the GET request. full format would include HttpsOperations://<domain name, or IP address>:<port>
     * @param headers An ordered list of headers to apply to the packet being sent
     * @param body The string that should be sent to the receiver
     * @return Request object with the corresponding response code as an integer, and body as a string
     */
    public Request post(String httpsURL, String body, HashMap<String, String> headers, boolean fastClose) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        //Convert to URL
        URL myurl = new URL(httpsURL);
        //Open a connection

        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {

                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs,
                                           String authType) {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs,
                                           String authType) {

            }
        } };

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("TLS");

        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        CustomSSLSocketFactory factory = new CustomSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultSSLSocketFactory(factory);
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);


        HttpsURLConnection con = (HttpsURLConnection)myurl.openConnection();

        //Set the type of request to POST
        con.setRequestMethod("POST");
        con.setDoOutput(true);

        //Iteratively sets headers
        for (String key : headers.keySet()) con.setRequestProperty(key, headers.get(key));

        //Prepare the output stream and send the remote server the data
        DataOutputStream output = new DataOutputStream(con.getOutputStream());
        output.writeBytes(body);
        output.close();

        //Prepare to capture the input to us, from the remote connection
        DataInputStream input = new DataInputStream( con.getInputStream() );
        StringBuilder stringOut = new StringBuilder();
        //Build the final response
        for( int c = input.read(); c != -1; c = input.read() )
            stringOut.append((char)c);
        input.close();

        if (factory.isOpen && fastClose) {
            System.out.println("Socket found to be OPEN, attempting close operation to free ephemeral port bind...");
            factory.getSocket().close();
            factory.isOpen = false;
        }
        return new Request(con.getResponseCode(), stringOut.toString());
    }


    /**
     * <b>HttpsOperations GET method formulator</b>
     *
     * <p><b>DESCRIPTION: </b>Formulates a simple GET request with HTTPS to a local or non-local URL</p>
     *
     * @param httpsURL The URL to contact for the GET request. full format would include HttpsOperations://<domain name, or IP address>:<port>
     * @param headers An ordered list of headers to apply to the packet being sent
     * @return Request object with the corresponding response code as an integer, and body as a string
     */
    public Request get(String httpsURL, HashMap<String, String> headers, boolean fastClose) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        //Convert to URL
        URL myurl = new URL(httpsURL);
        //Open a connection
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {

                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs,
                                           String authType) {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs,
                                           String authType) {

            }
        } };

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());

        CustomSSLSocketFactory factory = new CustomSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultSSLSocketFactory(factory);
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        HttpsURLConnection con = (HttpsURLConnection)myurl.openConnection();

        //Set the type of request to GET
        con.setRequestMethod("GET");

        //Iteratively sets headers
        for (String key : headers.keySet()) con.setRequestProperty(key, headers.get(key));

        //Prepare to capture the input to us, from the remote connection
        DataInputStream input = new DataInputStream(con.getInputStream());
        StringBuilder stringOut = new StringBuilder();

        for(int c = input.read(); c != -1; c = input.read()) stringOut.append((char)c);
        input.close();

        if (factory.isOpen && fastClose) {
            System.out.println("Socket found to be OPEN, attempting close operation to free ephemeral port bind...");
            factory.getSocket().close();
            factory.isOpen = false;
        }

        return new Request(con.getResponseCode(), stringOut.toString());
    }

    class CustomSSLSocketFactory extends SSLSocketFactory {
        SSLSocketFactory factory = null;
        Socket socket = null;
        boolean isOpen = false;

        Socket getSocket() { return socket; }

        CustomSSLSocketFactory(SSLSocketFactory factory) {
            this.factory = factory;
        }

        @Override
        public Socket createSocket(Socket s, String host, int port,
                                   boolean autoClose) throws IOException {
            Socket skt = factory.createSocket(s, host, port, autoClose);
            skt.setReuseAddress(true);
            return customizeSSLSocket(skt);
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return factory.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return factory.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            Socket skt = factory.createSocket(host, port);
            skt.setReuseAddress(true);
            return customizeSSLSocket(skt);
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            Socket skt = factory.createSocket(host, port);
            skt.setReuseAddress(true);
            return customizeSSLSocket(skt);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost,
                                   int localPort) throws IOException {
            Socket skt = factory.createSocket(host, port, localHost, localPort);
            skt.setReuseAddress(true);
            return customizeSSLSocket(skt);
        }

        @Override
        public Socket createSocket(InetAddress address, int port,
                                   InetAddress localAddress, int localPort) throws IOException {
            Socket skt = factory.createSocket(address, port, localAddress, localPort);
            skt.setReuseAddress(true);
            return customizeSSLSocket(skt);
        }

        private Socket customizeSSLSocket(Socket skt) {
            ((SSLSocket) skt).addHandshakeCompletedListener(
                    event -> {
                        socket = skt;
                        isOpen = true;
                    }
            );
            return skt;
        }
    }
}
