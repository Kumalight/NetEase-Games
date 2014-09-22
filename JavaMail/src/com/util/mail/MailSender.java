package com.util.mail;

import java.util.Date; 
import java.util.Properties;
import javax.mail.Address; 
import javax.mail.Authenticator;
import javax.mail.BodyPart; 
import javax.mail.Message; 
import javax.mail.MessagingException; 
import javax.mail.Multipart; 
import javax.mail.PasswordAuthentication;
import javax.mail.Session; 
import javax.mail.Transport; 
import javax.mail.internet.InternetAddress; 
import javax.mail.internet.MimeBodyPart; 
import javax.mail.internet.MimeMessage; 
import javax.mail.internet.MimeMultipart; 


/** 
* 简单邮件（不带附件的邮件）发送器 
*/ 
public class MailSender  { 
	
	/** 
	  * 以HTML格式发送邮件 
	  * @param mailInfo 待发送的邮件信息
	  */ 
	
	private static boolean sendHtmlMail(MailSenderInfo mailInfo){ 
		  // 判断是否需要身份认证 
		  MyAuthenticator authenticator = null;
		  Properties pro = mailInfo.getProperties();
		  //如果需要身份认证，则创建一个密码验证器  
		  if (mailInfo.isValidate()) { 
			authenticator = new MyAuthenticator(mailInfo.getUserName(), mailInfo.getPassword());
		  } 
		  // 根据邮件会话属性和密码验证器构造一个发送邮件的session 
		  Session sendMailSession = Session.getDefaultInstance(pro,authenticator); 
		  try { 
		  // 根据session创建一个邮件消息 
		  Message mailMessage = new MimeMessage(sendMailSession); 
		  // 创建邮件发送者地址 
		  Address from = new InternetAddress(mailInfo.getFromAddress()); 
		  // 设置邮件消息的发送者 
		  mailMessage.setFrom(from);
		  
		  
		  // 创建邮件的接收者地址，抄送地址，密送地址，并设置到邮件消息中 
		  //设置发送
		  String[] toAddress = mailInfo.getToAddress();
		  if( toAddress != null && toAddress.length != 0 )
		  { 
			  String toList = getMailList(toAddress); 
			  InternetAddress[] iaToList = new InternetAddress().parse(toList); 
			  mailMessage.setRecipients(Message.RecipientType.TO, iaToList); // 收件人 
		  } 
		  
			  //抄送设置
		   String[] ccAddress = mailInfo.getccAddress();
		   if( ccAddress != null && ccAddress.length != 0 )
		   {
			   	 String toListcc = getMailList(ccAddress);
				 InternetAddress[] iaToListcc = new InternetAddress().parse(toListcc); 
				 mailMessage.setRecipients(Message.RecipientType.CC, iaToListcc); // 抄送人 
		   }
			   
			  //密送设置
		    String[] bccAddress = mailInfo.getbccAddress();
		    if( bccAddress != null && bccAddress.length != 0 )
		    {
		    	 String toListbcc = getMailList(bccAddress);
				 InternetAddress[] iaToListbcc = new InternetAddress().parse(toListbcc); 
				 mailMessage.setRecipients(Message.RecipientType.BCC, iaToListbcc); // 密送人 
		    }
			  
		  // 设置邮件消息的主题 
		  mailMessage.setSubject(mailInfo.getSubject()); 
		  // 设置邮件消息发送的时间 
		  mailMessage.setSentDate(new Date()); 
		  // MiniMultipart类是一个容器类，包含MimeBodyPart类型的对象 
		  Multipart mainPart = new MimeMultipart();
		  // 创建一个包含HTML内容的MimeBodyPart 
		  BodyPart html = new MimeBodyPart(); 
		  // 设置HTML内容 
		  html.setContent(mailInfo.getContent(), "text/html;charset=utf-8"); 
		  mainPart.addBodyPart(html); 
		  // 将MiniMultipart对象设置为邮件内容 
		  mailMessage.setContent(mainPart); 
		  // 发送邮件 
		  Transport.send(mailMessage); 
		  System.out.println("邮件发送成功！");
		  return true; 
		  } catch (MessagingException ex) {
			  ex.printStackTrace();
			  System.err.println("邮件发送失败！");
		  }
		  return false;
		}
	private static String getMailList(String[] mailArray) { 

		StringBuffer toList = new StringBuffer();
		int length = mailArray.length;
		
		if (mailArray != null && length < 2) { 
			toList.append(mailArray[0]);
		} else {
			for (int i = 0; i < length; i++) { 
			
				toList.append(mailArray[i]); 
				if (i != (length - 1)) { 
				toList.append(",");
				}//end of if 
			}//end of for
		} 
			
	return toList.toString(); 

	} 
	
	public static void sendMail( String[] toaddress, String[] ccaddress, String[] bccaddress,
			String subject, String content)
	{
		  MailSenderInfo mailInfo = new MailSenderInfo();
		  mailInfo.setMailServerHost("corp.netease.com");

		  mailInfo.setMailServerPort("25");
		  mailInfo.setValidate(true); 

		  mailInfo.setUserName("username");
		  
		  mailInfo.setPassword("password");

		  mailInfo.setFromAddress("fromaddress");

		  mailInfo.setToAddress(toaddress);

		  mailInfo.setccAddress(ccaddress);

		  mailInfo.setbccAddress(bccaddress);
		  mailInfo.setSubject(subject);
		  mailInfo.setContent(content);
	      
		  //这个类主要来发送邮件
		  MailSender sms = new MailSender();
	      
	      sms.sendHtmlMail(mailInfo);//发送html格式
	}
	public static void main(String[] args){    
	 
		System.out.println(new String[]{}.length);
		MailSender.sendMail( new String[]{"dujintao999@sina.com","dujintao999@163.com"}, 
    		  new String[]{},
    		  new String[]{}, "subject", "content");
         
	}

} 

