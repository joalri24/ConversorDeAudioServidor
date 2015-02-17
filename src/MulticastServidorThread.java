import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * Thread encargado de hacer streaming de un archivo de audio.
 * @author Jose Riaño
 *
 */
public class MulticastServidorThread extends Thread
{
	
	//-----------------------------------------------
	// Constantes
	//-----------------------------------------------
	private static final String FIN_TRANSMISION = "fin transmision";
	private static final int PUERTO_SERVIDOR = 2425;
	
	//-----------------------------------------------
	// Atributos
	//-----------------------------------------------
	private int numPuertoClientes; 
	private String direccionGrupo;
	private DatagramSocket socket;
	private BufferedInputStream inArch;
	private boolean ejecutar;
	private DatagramPacket packet;
	private String nombreArchivo;
	
	
	//-----------------------------------------------
	// Constructores
	//-----------------------------------------------
	
	public MulticastServidorThread(String host, int puerto, String nombreArchivoP, DatagramSocket socketP)
	{
		try 
		{
			numPuertoClientes = puerto;
			socket = socketP;
			direccionGrupo = host;
			nombreArchivo = nombreArchivoP;
			inArch = new BufferedInputStream(new FileInputStream((nombreArchivoP)));
			ejecutar = true;
			packet = null;
		}  
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
	}
	
	
	//-----------------------------------------------
	// Métodos
	//-----------------------------------------------
	
	/**
	 * Inicia la ejecución del thread
	 */
	public void run()
	{
		System.out.println("Grupo multicast:  dirección :"+ direccionGrupo+" puerto clientes: "+ numPuertoClientes);
		System.out.println("Transmitiendo datos ...");
		try 
		{
			transmitirDatos();
		} 
		catch (IOException e) 
		{

			e.printStackTrace();
		} catch (InterruptedException e) 
		{

			e.printStackTrace();
		}	
		
	}

	
	/**
	 * @throws IOException 
	 * @throws UnknownHostException 
	 * @throws InterruptedException 
	 * 
	 */
	private void transmitirDatos () throws UnknownHostException, IOException, InterruptedException
	{
		
		InetAddress group = InetAddress.getByName(direccionGrupo);
		
		int contador;
		byte[] buff = new byte[2224];
		while((contador = inArch.read(buff, 0,buff.length)) >= 0)
		{

			
			
			packet = new DatagramPacket(buff, buff.length, group, numPuertoClientes);
			socket.send(packet);

		}
		
		for(int i = 0; i<5; i++)
		{
			buff=FIN_TRANSMISION.getBytes();
			packet = new DatagramPacket(buff, buff.length, group, numPuertoClientes);
			socket.send(packet);
		}

		
	}
}
