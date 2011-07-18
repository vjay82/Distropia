package org.distropia.server.platformspecific;

import java.io.File;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlatformSpecific {
	
	protected static Logger logger = LoggerFactory.getLogger(PlatformSpecific.class);
	private String hostNameCache = null;
	protected String applicationName;
	
	/** Enthält den Pfad zum Profilordner des Benutzers + "\" */
    private static String userHome = System.getProperty("user.home") + File.separator; //$NON-NLS-1$
    
    
    
	public PlatformSpecific(String applicationName) {
		super();
		this.applicationName = applicationName;
	}

	public static PlatformSpecific createPlatformSpecific( String applicationName)
	{
		String osName = System.getProperty("os.name");
		
		boolean isWindows = (osName != null) && (osName.toLowerCase().contains("windows"));
		boolean isMAC = (osName != null) && (osName.toLowerCase().contains("mac os"));
		
		if (isWindows) return new PlatformWindows( applicationName);
		if (isMAC) return new PlatformMAC( applicationName);
		return new PlatformLinux( applicationName); // this is most generic
	}	
	
	public static String getUserHome() {
		return userHome;
	}

	public String getMyApplicationSupportFolder() {
		return null;
	}

	public String getHostName()
	{
		if (null == hostNameCache)
		{
			
			// env abfragen
			try {
				hostNameCache = System.getenv("HOSTNAME"); //$NON-NLS-1$				
			} catch (Exception e) {
			}
			
			// alternative
			if (hostNameCache == null)
			{
				ArrayList<String> commands = new ArrayList<String>();
				
				commands.add("hostname"); // Linux //$NON-NLS-1$
				
				// Wir versuchen die Liste durch, bis wir ein Programm finden, welches existiert.
				for (int wdh = 0; wdh < commands.size(); wdh++)
				{
					try {
				        
				        String command = commands.get(wdh);
				        Process child = Runtime.getRuntime().exec(command);		    
				        child.waitFor();
				        byte[] data = new byte[child.getInputStream().available()];
				        child.getInputStream().read( data);
				        String s1 = new String(data);
				        if (s1.length()>0)
				        {
				        	hostNameCache = s1;
				        	break;
				        }
				        
				    } catch (Exception e) {
				    }
				}
				
			}
			
			
			if ((hostNameCache == null) || ("".equals(hostNameCache))) hostNameCache = "unknown"; //$NON-NLS-1$ //$NON-NLS-2$
			
			if (hostNameCache.contains(".")) // we have to much
			{
				hostNameCache = hostNameCache.substring(0, hostNameCache.indexOf("."));
			}
			
			if (hostNameCache.length() > 20) hostNameCache = hostNameCache.substring(0, 19);
			hostNameCache = hostNameCache.trim();
		}
		return hostNameCache;
	}
	
	public String getDomainName()
	{
		return "";
	}
	
	public String getUserName()
	{
		return "";
	}
	
}
