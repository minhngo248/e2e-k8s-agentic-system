package fr.minhnn.touristapi.aspects;

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
    @Around("execution(* fr.minhnn.touristapi.infra..*(..)) || execution(* fr.minhnn.touristapi.destination..*(..))")
    public Object log(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.debug("Executing {}() with args: {}", methodName, Arrays.toString(args));

        try {
            Object result = joinPoint.proceed();
            log.info("Successfully executed {}()", methodName);
            return result;
        } catch (Exception e) {
            log.error("Error executing {}(): {}", methodName, e.getMessage(), e);
            throw e;
        }
    }
}
