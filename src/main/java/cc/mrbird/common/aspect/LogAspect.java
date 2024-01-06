package cc.mrbird.common.aspect;

import java.lang.reflect.Method;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import cc.mrbird.common.annotation.Log;
import cc.mrbird.common.util.AddressUtils;
import cc.mrbird.common.util.HttpContextUtils;
import cc.mrbird.common.util.IPUtils;
import cc.mrbird.system.domain.SysLog;
import cc.mrbird.system.domain.User;
import cc.mrbird.system.service.LogService;

@Aspect
@Component
public class LogAspect {

	private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);

	@Autowired
	private LogService logService;

	@Autowired
	private ObjectMapper mapper;

	@Pointcut("@annotation(cc.mrbird.common.annotation.Log)")
	public void pointcut() {
	}

	@Around("pointcut()")
	public Object around(ProceedingJoinPoint point) throws Throwable {
		long beginTime = System.currentTimeMillis();
		Object result = null;
		try {
			result = point.proceed();
		} catch (Throwable e) {
			logger.error("方法执行异常: ", e);
			throw e; // 抛出异常，以便上层处理
		} finally {
			long time = System.currentTimeMillis() - beginTime;
			saveLog(point, time);
		}
		return result;
	}

	private void saveLog(ProceedingJoinPoint joinPoint, long time) {
		try {
			User user = (User) SecurityUtils.getSubject().getPrincipal();
			if (user == null) {
				return; // 用户未登录，不记录日志
			}

			MethodSignature signature = (MethodSignature) joinPoint.getSignature();
			Method method = signature.getMethod();
			SysLog log = new SysLog();
			Log logAnnotation = method.getAnnotation(Log.class);
			if (logAnnotation != null) {
				log.setOperation(logAnnotation.value());
			}
			String className = joinPoint.getTarget().getClass().getName();
			String methodName = signature.getName();
			log.setMethod(className + "." + methodName + "()");
			Object[] args = joinPoint.getArgs();
			LocalVariableTableParameterNameDiscoverer paramDiscoverer = new LocalVariableTableParameterNameDiscoverer();
			String[] paramNames = paramDiscoverer.getParameterNames(method);
			if (args != null && paramNames != null) {
				StringBuilder params = new StringBuilder();
				for (int i = 0; i < args.length; i++) {
					params.append("  ").append(paramNames[i]).append(": ").append(args[i]);
				}
				log.setParams(params.toString());
			}
			HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
			log.setIp(IPUtils.getIpAddr(request));
			log.setUsername(user.getUsername());
			log.setTime(time);
			log.setCreateTime(new Date());
			log.setLocation(AddressUtils.getRealAddressByIP(log.getIp(), mapper));
			this.logService.save(log);
		} catch (Exception e) {
			logger.error("保存日志异常: ", e);
		}
	}
}
