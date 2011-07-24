package org.distropia.server.platformspecific;

import java.io.File;


public class PlatformWindows extends PlatformSpecific{
	
	public PlatformWindows(String applicationName) {
		super(applicationName);
	}
	
	@Override
	public String getMyApplicationSupportFolder() {
		// it is somewhere in c:/Programme/common shared files, it even changes in every language, crazy os!
		// i don't know how to get this without a system dll, so i put it to c:/ hey they are familiar feeling the pain, aren't they?
		return "c" + File.pathSeparator + File.separator + applicationName + File.separator; // TODO: fix this
	}

	protected String domainNameCache = null;
	public String getDomainName()
	{
		if (null == domainNameCache)
		{
			domainNameCache = ""; // no domain
			
			try {
				String s1 = System.getenv("USERDOMAIN"); //$NON-NLS-1$
				if (s1 != null) domainNameCache = s1.trim();
			} catch (Exception e) {
				e.printStackTrace();
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
			
			try {
				String s1 = System.getenv("USERNAME"); //$NON-NLS-1$
				if (s1 != null) userNameCache = s1;
			} catch (Exception e) {
				e.printStackTrace();
				logger.error( "error getting username", e);
			}
			
			if ((userNameCache == null) || ("".equals(userNameCache))) userNameCache = "unknown";
			userNameCache = userNameCache.trim();
		}
		
		return userNameCache;
	}
	
}
