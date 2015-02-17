
import java.io.IOException;
import java.net.DatagramSocket;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

/**
 * 
 * @author Jose Riaño
 *
 */
public class ConversorDeAudioServidor 
{
	
	//---------------------------------------------
	// Constantes
	//---------------------------------------------
	
	
	//---------------------------------------------
	// Atributos
	//---------------------------------------------
	
	private static int port = 8889;
	private static String ruta_archivos = "./server_data/"; 

	
	
	//---------------------------------------------
	// Métodos
	//---------------------------------------------
	
	// Método Main
	public static void main(String[] args)
	{
		
		//Debe haber exactamente un parámetro: Número de puerto
		if(args.length != 1)
		{
	        System.err.println("Uso: java ConversorDeAudioServidor <número de puerto> <ruta de la carpeta para guardar los archivos (debe terminar en />) >");
	        System.exit(1);
		}
		
		int portNumber = Integer.parseInt(args[0]);
		//String rutaArchivos = args[1];
		boolean listening = true;
		
		
		
		SSLServerSocket serverSocket = null;
		SSLSocket socket = null;
		DatagramSocket multicast = null;
		
		try
		{
			// TCP
			SSLServerSocketFactory sslSrvFact = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			serverSocket =(SSLServerSocket)sslSrvFact.createServerSocket(portNumber);
			serverSocket.setEnabledCipherSuites(serverSocket.getSupportedCipherSuites());
			
			//UDP
			multicast = new DatagramSocket(portNumber);
			
			//Espera conexiones y crea nuevos threads
			while (listening)
			{
				socket = (SSLSocket) serverSocket.accept();
				
				new ConversorDeAudioServidorThread(socket, multicast).start();
				System.out.println("Conexión establecida");
			}
			
		}
		catch(IOException e)
		{

			e.printStackTrace();
		} 
		finally 
		{ 
			 try 
			 {
				 if(serverSocket!=null) serverSocket.close();
				 if(socket!=null) socket.close();
				 if(multicast!= null) multicast.close();
			 } 
			 catch (IOException e) 
			 {
				 e.printStackTrace();
			 }
		 }
	}

}

