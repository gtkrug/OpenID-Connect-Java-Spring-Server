package org.mitre.jwt.signer.service.impl;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.mitre.jwt.model.Jwt;
import org.mitre.jwt.model.JwtHeader;
import org.mitre.jwt.signer.JwtSigner;
import org.mitre.jwt.signer.impl.HmacSigner;
import org.mitre.jwt.signer.impl.PlaintextSigner;
import org.mitre.jwt.signer.impl.RsaSigner;
import org.mitre.key.fetch.KeyFetcher;
import org.mitre.util.Utility;


public class DynamicJwtSigningAndValidationService extends AbstractJwtSigningAndValidationService{
	
	private String x509SigningUrl;
	
	private String jwkSigningUrl;
	
	private String clientSecret;
	
	private Key signingKey;
	
	private Map<String, PublicKey> map;
	
	private PublicKey publicKey;
	
	private Map<String, ? extends JwtSigner> signers;
	
	public DynamicJwtSigningAndValidationService(String x509SigningUrl, String jwkSigningUrl, String clientSecret) {
		setX509SigningUrl(x509SigningUrl);
		setJwkSigningUrl(jwkSigningUrl);
		setClientSecret(clientSecret);
	}
	
	public Key getSigningKey() {
		if(signingKey == null){
			if(x509SigningUrl != null){
				File file = new File(x509SigningUrl);
				URL url;
				try {
					url = file.toURI().toURL();
					signingKey = KeyFetcher.retrieveX509Key();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (CertificateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if (jwkSigningUrl != null){
				File file = new File(jwkSigningUrl);
				URL url;
				try {
					url = file.toURI().toURL();
					signingKey = KeyFetcher.retrieveJwkKey();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidKeySpecException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return signingKey;
	}

	public String getSigningX509Url() {
		return x509SigningUrl;
	}

	public void setX509SigningUrl(String x509SigningUrl) {
		this.x509SigningUrl = x509SigningUrl;
	}

	public String getSigningJwkUrl() {
		return jwkSigningUrl;
	}

	public void setJwkSigningUrl(String jwkSigningUrl) {
		this.jwkSigningUrl = jwkSigningUrl;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	@Override
	public Map<String, PublicKey> getAllPublicKeys() {
		if(publicKey != null){
			//check to make sure key isn't null, return map
			map.put(((RSAPublicKey) publicKey).getModulus()
					.toString(16).toUpperCase()
					+ ((RSAPublicKey) publicKey).getPublicExponent()
						.toString(16).toUpperCase(), publicKey);
		}
		return map;
	}

	@Override
	public void signJwt(Jwt jwt) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, ? extends JwtSigner> getSigners() {
		// TODO Auto-generated method stub
		signers = new HashMap<String, JwtSigner>();
		return signers;
	}
	
	@Override
	public boolean validateSignature(String jwtString) {
		
		try {
			JwtSigner signer = getSigner(jwtString);
			return signer.verify(jwtString);
		}
		catch(Exception e) {
			return false;
		}
		
	}
	
	public JwtSigner getSigner(String str) {
		JwtHeader header = Jwt.parse(str).getHeader();
		String alg = header.getAlgorithm();
		JwtSigner signer = null;
		
		if(alg.equals("HS256") || alg.equals("HS384") || alg.equals("HS512")){
			signer = new HmacSigner(alg, clientSecret); // TODO: huh? no, we're not signing with the client secret
		} else if (alg.equals("RS256") || alg.equals("RS384") || alg.equals("RS512")){
			
			PublicKey rsaSigningKey = null;
            try {
	            rsaSigningKey = (PublicKey) getSigningKey();
            } catch (Exception e) {
	            // FIXME this function call should not throw Exception
	            e.printStackTrace();
	            return null;
            }
			signer = new RsaSigner(alg, rsaSigningKey, null);
		} else if (alg.equals("none")){
			signer = new PlaintextSigner();
		} else {
			throw new IllegalArgumentException("Not an existing algorithm type");
		}
		
		return signer;
	}

	@Override
	public boolean validateIssuedAt(Jwt jwt) {
		Date issuedAt = jwt.getClaims().getIssuedAt();
		
		if (issuedAt != null)
			return new Date().before(issuedAt);
		else
			return false;
	}

	@Override
	public boolean validateNonce(Jwt jwt, String nonce) {
		if(jwt.getClaims().getNonce().equals(nonce)){
			return true;
		}
		else{
			return false;
		}
	}
}