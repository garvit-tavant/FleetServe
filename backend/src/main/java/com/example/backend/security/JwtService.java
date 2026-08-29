package com.example.backend.security;

import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;


@Component
public class JwtService {

    private final SecretKey signingKey; // secret key
    private final long expirationMs; // expiration time

  //  private static String secret = Jwts.SIG.HS256.key()
    private static long expirationms = 3600000;

    // ==========================
   
    //    TO DO HERE  
 


    public JwtService(@Value("${jwt.secret}") String secret , @Value("${jwt.expiration-ms}") long expirationMs){  
   //  public JwtService(){
      //  this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret.getBytes()));
      this.signingKey = Jwts.SIG.HS256.key().build();  
      this.expirationMs = expirationms;
    
      //System.out.println(secret + "   "+expirationMs);

    }
    
    // secret need to be done correctly implemented !!!!

     // ========================== 

    public String generateToken(String username , Set<String> roles){
        Date now = new Date();
        Date expiry = new Date(now.getTime()+expirationMs);

        return Jwts.builder().subject(username)
                           .claim("roles",roles)
                           .issuedAt(now)
                           .expiration(expiry)
                           .signWith(signingKey).compact();                 
    }

    public boolean isTokenValid (String token){
      
        try{
            parseClaims(token);
            return true;
        }
        catch(ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | IllegalArgumentException e){
            System.err.println("[ERROR] : "+e.getMessage());    
            return false;
            }
        catch(Exception e){
            System.err.println("JWT ERROR WHICH NOT RECORDED HAPPEND !!");
            return false;
        }
    
        }

    
    public String extractUsername (String token){
            return parseClaims(token).getSubject();
        }

    public List<String> extractRoles(String token){
        
         Collection<?> roles=null;

        try{
        roles = parseClaims(token).get("roles",Collection.class);
       
        }
        catch(Exception e){
            System.out.println("[ERROR] : "+ e.getMessage());
        }
        
         return roles == null ? List.of() : roles.stream().map(String::valueOf).collect(Collectors.toList());

    }

    private Claims parseClaims(String token){
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();        
    }






}
