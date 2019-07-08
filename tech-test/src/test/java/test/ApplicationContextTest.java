package test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @Description:
 * @author: zhangyafeng1
 * @Date 2019-07-08 14:55:49
 */
public class ApplicationContextTest {
	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
		context.getBean("userBean");
	}
}
