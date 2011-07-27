package org.distropia.server;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileItemFactory;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.distropia.client.Utils;
import org.distropia.server.database.UserCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class WebHelper extends HttpServlet {
	static transient Logger logger = LoggerFactory.getLogger(WebHelper.class);
	
	public SessionCache getSessionCache(){
		return Backend.getSessionCache();
	}
	
	
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		logger.info("get: " + req.getQueryString());
		
		String sessionId = req.getParameter("sessionId");
		if (!Utils.isNullOrEmpty( sessionId)){
			Session session = getSessionCache().getSessionForSessionId(sessionId);
			if (session.getUserProfile() != null){
				String picture = req.getParameter("picture");
				if ("tmp".equals(picture)){
					String fileName = req.getParameter("filename");
					if (fileName.contains("..")) return;
					System.out.println("fnis:"+fileName);
					File picFile = new File( Backend.getWorkDir() + "tmpfiles" + File.separator + fileName);
					
					OutputStream out = resp.getOutputStream();					
					FileInputStream fi = new FileInputStream( picFile);
					resp.setContentType("image");
					IOUtils.copy(fi, out);
					fi.close();
					out.close();
					
				}
				else if ("user".equals(picture)){
					try
					{
						byte[] rawPicture = null;
						String fromUniqueUserId = req.getParameter("uniqueUserId");
						
						if (session.getUserProfile().getUniqueUserID().equals( fromUniqueUserId)){
							if (Boolean.parseBoolean( req.getParameter("bigPicture"))){
								logger.info("sending big user picture");
								rawPicture = session.getUserProfile().getUserCredentials().getPicture();
							}
							else{
								logger.info("sending small user picture");
								rawPicture = session.getUserProfile().getUserCredentials().getSmallPicture();
							}
						}
						OutputStream out = resp.getOutputStream();
						if (rawPicture == null){
							FileInputStream fi = new FileInputStream( Backend.getWebContentFolder().getAbsolutePath() + File.separator + "images" + File.separator + "replacement_user_image.png");
							resp.setContentType("image");
							IOUtils.copy(fi, out);
							fi.close();
						}
						else{
							out.write( rawPicture);
						}
						out.close();
					}
					catch (Exception e) {
						logger.error("error sending user picture", e);
					}
				}
				
			}
		}
		else super.doGet(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		logger.info("post: " + req.getQueryString());
		
		String sessionId = req.getParameter("sessionId");
		if (!Utils.isNullOrEmpty( sessionId)){
			Session session = getSessionCache().getSessionForSessionId(sessionId);
			if (session.getUserProfile() != null){
				String uploadType = req.getParameter("upload");
				logger.info("got uploadrequest, type: " + uploadType);
				String response = "success";
				String callbackName = "";
				try
				{
					// Create a factory for disk-based file items
					FileItemFactory factory = new DiskFileItemFactory();
					
					// Create a new file upload handler
					ServletFileUpload upload = new ServletFileUpload( factory);
					upload.setSizeMax( 1024*1024);
					upload.setFileSizeMax(1024*1024*500);
					// Parse the request
					List<FileItem> /* FileItem */ items = upload.parseRequest(req);
					
					Iterator<FileItem> iter = items.iterator();
					while (iter.hasNext()) {
					    FileItem item = iter.next();

					    if (item.isFormField()) {
					        logger.info("formfield " + item.getFieldName());
					        if ("callbackName".equals( item.getFieldName())) callbackName = item.getString();
					    } else {
					    	logger.info("filefield " + item.getFieldName());
					    	if ("userPicture".equals(uploadType)){
								logger.info("Setting new user picture for " + session.getUserProfile());
								
								
								UserCredentials userCredentials = session.getUserProfile().getUserCredentials();
								// creating the small image
								ByteArrayOutputStream bo = new ByteArrayOutputStream();
								scalePictureToMax( new ByteArrayInputStream( item.get()), bo, 50, 50);
								userCredentials.setSmallPicture( bo.toByteArray());
								// creating big picture
								bo = new ByteArrayOutputStream();
								scalePictureToMax( new ByteArrayInputStream( item.get()), bo, 500, 500);
								userCredentials.setPicture( bo.toByteArray());
								
								session.getUserProfile().setUserCredentials(userCredentials);
					    	}
					    }
					}
					
					
				}
				catch (Exception e) {
					logger.error("error sending user picture", e);
					response = "Error, for details see the server log files.";
				}
				
				logger.info("Callback name: " + callbackName);
				resp.getWriter().print( "<script type=\"text/javascript\">window.top." + callbackName + "('" + response + "');</script>");
				
			}
		}
		else super.doPost(req, resp);
	}
	
	static public void scalePictureToMax( InputStream in, OutputStream out, int maxWidth, int maxHeight) throws Exception{
		BufferedImage bsrc = ImageIO.read( in);
		
		int height = 0;
		int width = 0;
		
		double aspectRatio = ((double) bsrc.getHeight())
				/ ((double) bsrc.getWidth());
		
		double maxAR = ((double) maxHeight) / ((double) maxWidth);
		if (aspectRatio > maxAR) {
			height = maxHeight;
			width = (int) Math.round(maxHeight / aspectRatio);
		} else {
			width = maxWidth;
			height = (int) Math.round(maxWidth * aspectRatio);
		}
		
		BufferedImage bdest = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = bdest.createGraphics();
		AffineTransform at = AffineTransform.getScaleInstance((double)width/bsrc.getWidth(), (double)height/bsrc.getHeight());
		g.drawRenderedImage(bsrc,at);
		ImageIO.write(bdest,"PNG", out);		
	}
}
