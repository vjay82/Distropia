package org.distropia.client;

public class Utils {
	public static boolean equalsWithNull(Object a, Object b){
    	return ((a == b) || ((a != null) && a.equals(b)));
    }
	public static boolean isNullOrEmpty( String s){
		if ((s == null) || ("".equals(s))) return true;
		return false;
	}
	public static boolean isNullOrEmpty( Object s){
		if (s == null) return true;
		return isNullOrEmpty( s.toString());
	}
	public static String boolToLanguage( boolean b){
		if ( b) return "ja";
		else return "nein";
	}
	public static void whoCalledMe(){
		try {
			throw new Exception("detect me");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
