package fr.minhnn.weatheragent.aspects;

import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Log4j2
public class LoggingAspect {
    
    @Around("execution(* fr.minhnn.weatheragent..*(..))")
    public Object logAgent(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        log.info("Executing {}.{}() with args: {}", className, methodName, Arrays.toString(args));
        
        try {
            Object result = joinPoint.proceed();
            log.info("Successfully executed {}.{}()", className, methodName);
            return result;
        } catch (Exception e) {
            log.error("Error executing {}.{}(): {}", className, methodName, e.getMessage(), e);
            throw e;
        }
    }
}
