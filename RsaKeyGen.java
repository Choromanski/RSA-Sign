import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.io.FileOutputStream;

public class RsaKeyGen {

    public static void main(String[] args) {

    	LargeInteger p, q , n, phiN, e, d;
    	final LargeInteger ONE = new LargeInteger(new byte[]{0x01});
    	Random rand =  new Random();

    	do{
    		p = new LargeInteger(256, rand);
    		q = new LargeInteger(256, rand);
    		while(p.equals(q) && !p.isNegative() && !q.isNegative()){
    			q = new LargeInteger(256, rand);
    			p = new LargeInteger(256, rand);
			}
			n = p.multiply(q);
    		phiN = p.subtract(ONE).multiply(q.subtract(ONE));
    		e = new LargeInteger(512, rand);
    		while(!phiN.XGCD(e)[0].equals(ONE) && !e.isNegative()){
    			e = new LargeInteger(512, rand);
    		}
    		d = e.modularExp(ONE.negate(), phiN);
    	}while(phiN.subtract(e).isNegative());

		try{
			FileOutputStream pubOut = new FileOutputStream("pubkey.rsa");
			pubOut.write(e.store().getVal());
			pubOut.write(n.store().getVal());
			pubOut.close();
		}catch(FileNotFoundException err){
			System.out.println("ERROR: pubkey.rsa not found");
		}catch(IOException err){
			System.out.println("ERROR: IO error writing to pubkey.rsa");
		}

		try{
			FileOutputStream privOut = new FileOutputStream("privkey.rsa");
			privOut.write(d.store().getVal());
			privOut.write(n.store().getVal());
			privOut.close();
		}catch(FileNotFoundException err){
			System.out.println("ERROR: privkey.rsa not found");
		}catch(IOException err){
			System.out.println("ERROR: IO error writing to privkey.rsa");
		}
    }
}
