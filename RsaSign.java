import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RsaSign {

    public static void main(String[] args) {
    	LargeInteger hash;
		try {
			Path path = Paths.get(args[1]);
			byte[] data = Files.readAllBytes(path);
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(data);
			hash = new LargeInteger(md.digest());
		}catch(IOException e){
			System.out.println("ERROR: " + args[1] + " not found.");
			return;
		}catch(NoSuchAlgorithmException e){
			System.out.println("ERROR: SHA-256 algorithm not found");
			return;
		}
		if(args[0].equals("s")){
			try {
				FileInputStream privIn = new FileInputStream("privkey.rsa");
				byte[] tempD = new byte[65], tempN = new byte[65];
				privIn.read(tempD, 1, 64);
				privIn.read(tempN, 1, 64);
				LargeInteger d = new LargeInteger(tempD);
				LargeInteger n = new LargeInteger(tempN);
				privIn.close();
				LargeInteger signature = hash.modularExp(d, n);
				FileOutputStream outFile = new FileOutputStream(args[1].concat(".sig"));
				outFile.write(signature.getVal());
				outFile.close();
			}catch(FileNotFoundException e){
				System.out.println("ERROR: privkey.rsa not found.");
			}catch(IOException e){
				System.out.println("IO ERROR FOUND");
			}

		}else if(args[0].equals("v")){
			try {
				Path sigFile = Paths.get(args[1].concat(".sig"));
				byte[] temp = Files.readAllBytes(sigFile);
				LargeInteger signedHash = new LargeInteger(temp);
				FileInputStream pubIn = new FileInputStream("pubkey.rsa");
				byte[] tempE = new byte[65], tempN = new byte[65];
				pubIn.read(tempE, 1, 64);
				pubIn.read(tempN, 1, 64);
				LargeInteger e = new LargeInteger(tempE);
				LargeInteger n = new LargeInteger(tempN);
				pubIn.close();
				signedHash = signedHash.modularExp(e, n);
				if(hash.equals(signedHash)){
					System.out.println("Signature is valid");
				}else{
					System.out.println("WARNING: Signature is not valid!!!");
				}
			}catch(FileNotFoundException e){
				System.out.println("ERROR: " + args[1] + ".sig or pubkey.rsa not found.");
			}catch(IOException e) {
				System.out.println(e);
			}
		}
	}
}
