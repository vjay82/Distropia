package org.distropia.server.platformspecific;

import java.io.File;
import java.util.ArrayList;

public class PlatformLinux extends PlatformSpecific {
	
	public PlatformLinux(String applicationName) {
		super(applicationName);
	}
	
	@Override
	public String getMyApplicationSupportFolder() {
		return "/usr/share/" + applicationName + File.separator;
	}

	protected String domainNameCache = null;	
	public String getDomainName()
	{
		if (null == domainNameCache)
		{
			domainNameCache = ""; // no domain
			
			ArrayList<String> commands = new ArrayList<String>();
			
			commands.add("dnsdomainname"); // Linux //$NON-NLS-1$
			commands.add("domainname"); // Linux //$NON-NLS-1$
			commands.add("hostname --domain"); // Linux //$NON-NLS-1$
			
			// Wir versuchen die Liste durch, bis wir ein Programm finden, welches existiert.
			for (int wdh = 0; wdh < commands.size(); wdh++)
			{
				try {
			        
			        String command = commands.get(wdh);
			        Process child = Runtime.getRuntime().exec(command);		    
			        child.waitFor();
			        byte[] data = new byte[child.getInputStream().available()];
			        child.getInputStream().read( data);
			        child.getErrorStream().close();
					child.getInputStream().close();
					child.getOutputStream().close();
			        String s1 = new String(data);
			        if (s1.length()>0)
			        {
			        	domainNameCache = s1;
			        	break;
			        }
			        
			    } catch (Exception e) {
			    }
			}
			if (domainNameCache != null) 
			{
				domainNameCache = domainNameCache.replace("\n", "").trim();
			}
		}
		return domainNameCache;
	}
	
	protected String userNameCache = null;
	public String getUserName()
	{
		if (null == userNameCache)
		{
			userNameCache = ""; // no username
			
			ArrayList<String> commands = new ArrayList<String>();
			
			commands.add("whoami"); // Linux //$NON-NLS-1$
			
			// Wir versuchen die Liste durch, bis wir ein Programm finden, welches existiert.
			for (int wdh = 0; wdh < commands.size(); wdh++)
			{
				try {
			        String command = commands.get(wdh);
			        Process child = Runtime.getRuntime().exec(command);		    
			        child.waitFor();
			        byte[] data = new byte[child.getInputStream().available()];
			        child.getInputStream().read( data);
			        child.getErrorStream().close();
					child.getInputStream().close();
					child.getOutputStream().close();
			        String s1 = new String(data);
			        if (s1.length()>0)
			        {
			        	userNameCache = s1;
			        	break;
			        }
			        
			    } catch (Exception e) {
			    }
			}
			
			if ((userNameCache == null) || ("".equals(userNameCache))) userNameCache = "unknown";
			userNameCache = userNameCache.trim();
			
		}
		
		return userNameCache;
	}
	
}
