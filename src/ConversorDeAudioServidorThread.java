import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * 
 * @author Jose Riaño
 *
 */
public class ConversorDeAudioServidorThread extends Thread
{
	//---------------------------------
	// Constantes
	//---------------------------------
	
	private static final String CARGAR="cargar";
	private static final String PEDIR="pedir";
	private static final String COVERTIDO = "(conv)";
	private static final String SALUDO = "Hola!";
	private static final int PUERTO_INCIAL= 4000;
	private static final String DIRECCION_MULTICAST_INICIAL = "230.0.0.";
	
	
	//---------------------------------
	// Atributos
	//---------------------------------
	
	private Socket socket;
	private static int idArchivo = 0;
	private static String rutaArchivos;
	private static DatagramSocket multicast;
	
	
	//---------------------------------
	// Constructores
	//---------------------------------
	
	public ConversorDeAudioServidorThread(SSLSocket socketP, DatagramSocket multicastP)
	{
		super("ConversorDeAudioServidorThread");
		multicast =multicastP;
		socket = socketP;
		
	}
	
	//---------------------------------
	// Métodos
	//---------------------------------

	public void run()
	{
		try(	
				InputStream in =socket.getInputStream();	
				BufferedReader inStr =new BufferedReader(new InputStreamReader(in));
				PrintWriter outStr = new PrintWriter(socket.getOutputStream(),true);
					
			)
		{
			String outputLine;	
			String inputLine;
			
			//Saluda al cliente
			outputLine = SALUDO; 
			outStr.println(outputLine);
			
			// Lee la intención del cliente
			inputLine= inStr.readLine();
			
			//El cliente quiere subir un archivo
			if(inputLine.equalsIgnoreCase(CARGAR))
			{
				//Lee el nombre del archivo
				inputLine= inStr.readLine();
				String archivo = inputLine;
				
				// Envía un número al cliente para identificar su archivo
				outputLine = ""+ asignarId();
				String id = outputLine;
				outStr.println(outputLine);
				
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(id+archivo));
			
				
				// Recibe el archivo del cliente
				byte[] buff = new byte[1024];
				int contador;
				
				System.out.println("Recibiendo archivo ["+archivo+"] ...");
				while((contador = in.read(buff)) >= 0)
				{
					bos.write(buff,0,contador);
				}
				
				System.out.println("Archivo recibido exitosamente ["+archivo+"]");
				bos.close();
				socket.close();
				
				
			}
			// El cliente quiere recibir el archivo procesado
			else if (inputLine.equalsIgnoreCase(PEDIR))
			{
				//Lee el nombre del archivo(que incluye el id único)
				inputLine= inStr.readLine();	
				String archivo = inputLine;
				int idArchivo = Integer.parseInt(""+archivo.charAt(0));
				
				// Convierte el archivo 
				convertirArchivo(archivo,COVERTIDO+archivo);

				
				
				BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
				BufferedInputStream inArch = new BufferedInputStream(new FileInputStream(COVERTIDO+archivo));
				//BufferedInputStream inArch = new BufferedInputStream(new FileInputStream(archivo));
				int contador;
				byte[]buff = new byte[1024];
				
				while((contador = inArch.read(buff)) >= 0)
				{
					bos.write(buff, 0,contador);
				}
				System.out.println("Archivo enviado al cliente ["+archivo+"]");
				
				bos.close();
				inArch.close();
				socket.close();
				
				//Borra el archivo original
				File borr = new File(archivo);
				System.out.println(borr.delete());
				
				/* Crea un streaming con el nuevo archivo
				* El puerto y la dirección usados son una constante más el valor del id del archivo. 
				*/
				int puertoStreaming = PUERTO_INCIAL+idArchivo;
				String direccionStreaming = DIRECCION_MULTICAST_INICIAL+idArchivo; 
				StreamArchivo(direccionStreaming,puertoStreaming, COVERTIDO+archivo);
				//StreamArchivo(direccionStreaming,puertoStreaming, idArchivo+"D.txt");
			}
			else
			{
				System.out.println("Ocurrió un error de protocolo: se obtuvo ["+inputLine+"] cundo se esperaba "+CARGAR+" o "+ PEDIR );
			}					
		}
		catch (IOException e) 
		{

			e.printStackTrace();
		} 
	}
	
	/**
	 * Crea el canal por el que se va hacer el streaming del video.
	 * @param puerto puerto que va a usar el streaming.
	 * @param nombreArchivo el nombre del archivo que se va a transmitir.
	 */
	private void StreamArchivo(String host, int puerto, String nombreArchivo)
	{
		
		MulticastServidorThread streaming = new MulticastServidorThread(host, puerto, nombreArchivo, multicast);
		streaming.start();	
	}

	/**
	 * El método que hace la conversión de los archivos.
	 */
	private static void convertirArchivo(String inputPath, String outputPath)
	{
		AudioFileFormat inFileFormat;
		File inFile;
		File outFile;
		
		System.out.println("Comprimiendo archivo...");
		
		//Se define el nuevo formato que va a tener el archivo de salida: 8 kHz , 8 bit, stereo
		AudioFormat outDataFormat = new AudioFormat((float) 10000.0,(int) 16,(int) 2, true, false);
		try
		{
			inFile = new File(inputPath);
			outFile = new File(outputPath);
		}
		catch(NullPointerException e)
		{
			System.out.println("Error: Uno de los parámetros es null.");
			return;
		}
		
		try
		{
			//Obtener el tipo del archivo de entrada
			inFileFormat = AudioSystem.getAudioFileFormat(inFile);
			

			AudioInputStream inFileAIS = AudioSystem.getAudioInputStream(inFile);
				
			//Se crea un stream con el formato de baja resolución(si es posible)
			AudioInputStream lowResAIS;				
			if(AudioSystem.isConversionSupported(outDataFormat, inFileAIS.getFormat()))
			{
				//System.out.println("Es posible realizar la compresión");
				lowResAIS = AudioSystem.getAudioInputStream(outDataFormat, inFileAIS);
			}
			else
			{
				lowResAIS = inFileAIS;
				System.out.println("Advertencia: No es posible realizar la compresión. Se intentará hacer solo el cambio de formato.");
			}
			
			
			//Si es posible escribir un archivo de formato N usando el archivo de entrada
			if(AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE, inFileAIS))
			{
				
				//AudioSystem.write(inFileAIS, AudioFileFormat.Type.AIFF, outFile);
				AudioSystem.write(lowResAIS, AudioFileFormat.Type.WAVE, outFile);
				System.out.println("Creación exitosa de un archivo WAVE comprimido (8KHz, 8bit).");
				inFileAIS.close();
				return;
			}
			else
			{
				System.out.println("Advertencia: La conversión a formato AIFF no está soportada por AudioSystem,");
			}

		}
		catch(UnsupportedAudioFileException e)
		{
			System.out.println("Error: El archivo de entrada no tiene un formato soportado");
			
			return;
		}
		catch(IOException e)
		{
			System.out.println("Error: No fue posible leer el archivo de entrada");
			e.printStackTrace();
			return;
		}		
	}
	
	/**
	 * Método que asigna un id único a cada archivo que recibe el servidor.
	 * @return
	 */
	private static synchronized int asignarId()
	{
		idArchivo++;
		return idArchivo;
	}
	
}
