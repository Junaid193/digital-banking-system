package com.banking.transactionservice.service;

import com.banking.transactionservice.entity.Transaction;
import com.banking.transactionservice.entity.TransactionStatus;
import com.banking.transactionservice.repository.TransactionRespository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final TransactionRespository transactionRespository;
    private final RedisTemplate<String, String> redisTemplate;
    private final TransactionService transactionService;
    private static final long OTP_EXPIRY_MINUTE = 5;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TRANSACTION_OTP_GENERATED_TOPIC = "transaction.otp.generated";
    //consume verifiaction.required
    //generate otp to verify

    @KafkaListener(topics = "verification.required")
     public void consumeVerificationRequired(
             @Payload Map<String, Object> payload){

         try{
             String transactionId = (String) payload.get("transactionId");
             String accountNumber = (String) payload.get("accountNumber");
             String reason = (String) payload.get("reason");

             log.info("verification required - transaction : {} reason : {}", transactionId, reason);
             Transaction transaction = transactionRespository.findById(transactionId)
                     .orElseThrow(() -> new RuntimeException(
                             "Transaction not found "+ transactionId
                     ));

             if(transaction.getStatus()!= TransactionStatus.PROCESSING){
                 log.warn("transaction {}  not PROCESSING - skipping ",  transactionId);
                 return;
             }

             //generate 6 digit otp
             String otp = String.format("%06d", (int) (Math.random()*90000)+100000);

             // storing otp in redis - expires in 5 minute
             String otpKey = "verification:otp"+ transactionId;
             redisTemplate.opsForValue().set(otpKey,otp,OTP_EXPIRY_MINUTE, TimeUnit.MINUTES);

             //updating the status
             transaction.setStatus(TransactionStatus.PENDING_VERIFICATION);
             transactionRespository.save(transaction);

             log.info("OTP generated for transaction : {} expires in {} min ",
                     transactionId,OTP_EXPIRY_MINUTE);

             //Notify user
             Map<String,Object> otpEvent = new HashMap<>();
             otpEvent.put("transactionId",transactionId);
             otpEvent.put("accountNumber",accountNumber);
             otpEvent.put("reason",reason);
             otpEvent.put("otp",otp);
             otpEvent.put("amount",payload.get("amount"));

             kafkaTemplate.send(TRANSACTION_OTP_GENERATED_TOPIC, transactionId,otpEvent);
         }
         catch(Exception e){
             log.error("Error handling verification required {} : ", e.getMessage());
         }
     }

     @KafkaListener(topics = "fraud.check.clean")
     public void consumeFraudCheckCleanResult(
             @Payload Map<String, Object> payload){
        try{
             String transactionId = (String) payload.get("transactionId");
             transactionService.processCleanResult(transactionId);
        }catch(Exception e){
            log.error("Error processing fraud check result : {}", e.getMessage());
        }
     }















}
