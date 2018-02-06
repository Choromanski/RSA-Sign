import java.util.Random;
import java.math.BigInteger;

public class LargeInteger {

	private final byte[] ZERO = {(byte) 0};
	
	private final byte[] ONE = {(byte) 1};

	private final byte[] TWO = {(byte) 2};

	private byte[] val;

	public LargeInteger(byte[] b) {
		val = b;
	}

	public LargeInteger(int n, Random rnd) {
		val = BigInteger.probablePrime(n, rnd).toByteArray();
	}

	public byte[] getVal() {
		return val;
	}

	public int length() {
		return val.length;
	}

	public void extend(byte extension) {
		byte[] newv = new byte[val.length + 1];
		newv[0] = extension;
		for (int i = 0; i < val.length; i++) {
			newv[i + 1] = val[i];
		}
		val = newv;
	}

	public boolean isNegative() {
		return (val[0] < 0);
	}

	public LargeInteger add(LargeInteger other) {
		byte[] a, b;
		if (val.length < other.length()) {
			a = other.getVal();
			b = val;
		}
		else {
			a = val;
			b = other.getVal();
		}

		if (b.length < a.length) {
			int diff = a.length - b.length;

			byte pad = (byte) 0;
			if (b[0] < 0) {
				pad = (byte) 0xFF;
			}

			byte[] newb = new byte[a.length];
			for (int i = 0; i < diff; i++) {
				newb[i] = pad;
			}

			for (int i = 0; i < b.length; i++) {
				newb[i + diff] = b[i];
			}

			b = newb;
		}

		int carry = 0;
		byte[] res = new byte[a.length];
		for (int i = a.length - 1; i >= 0; i--) {
			carry = ((int) a[i] & 0xFF) + ((int) b[i] & 0xFF) + carry;

			res[i] = (byte) (carry & 0xFF);

			carry = carry >>> 8;
		}

		LargeInteger res_li = new LargeInteger(res);
	
		if (!this.isNegative() && !other.isNegative()) {
			if (res_li.isNegative()) {
				res_li.extend((byte) carry);
			}
		}
		else if (this.isNegative() && other.isNegative()) {
			if (!res_li.isNegative()) {
				res_li.extend((byte) 0xFF);
			}
		}

		return res_li;
	}

	public LargeInteger negate() {
		byte[] neg = new byte[val.length];
		int offset = 0;

		if (val[0] == (byte) 0x80) {
			boolean needs_ex = true;
			for (int i = 1; i < val.length; i++) {
				if (val[i] != (byte) 0) {
					needs_ex = false;
					break;
				}
			}
			if (needs_ex) {
				neg = new byte[val.length + 1];
				neg[0] = (byte) 0;
				offset = 1;
			}
		}

		for (int i  = 0; i < val.length; i++) {
			neg[i + offset] = (byte) ~val[i];
		}

		LargeInteger neg_li = new LargeInteger(neg);
	
		return neg_li.add(new LargeInteger(ONE));
	}

	public LargeInteger subtract(LargeInteger other) {
		return this.add(other.negate());
	}

	public LargeInteger multiply(LargeInteger other) {
		LargeInteger sum, multiplicant = this, shift  = other;
		if(this.isNegative()){
			multiplicant = this.negate();
		}
		if(other.isNegative()){
			shift = other.negate();
		}
		sum = new LargeInteger(new byte[multiplicant.length() + shift.length()]);
		for(int i = multiplicant.length()-1; i >= 0; i--){
			int checkVal = 1;
			for(int j = 8; j > 0; j--){
				if((multiplicant.getVal()[i] & checkVal) == checkVal){
					sum = sum.add(shift);
				}
				shift = shift.shiftLeft();
				checkVal = checkVal << 1;
			}
		}
		if(this.isNegative() == other.isNegative()){
			if(sum.isNegative()){
				return shrink(sum.negate());
			}else{
				return shrink(sum);
			}
		}else{
			if(sum.isNegative()){
				return shrink(sum);
			}else{
				return shrink(sum.negate());
			}
		}
	}

	public LargeInteger[] XGCD(LargeInteger other) {
		if(other.equals(new LargeInteger(ZERO))){
			return new LargeInteger[]{this, new LargeInteger(ONE), new LargeInteger(ZERO)};
		}else{
			LargeInteger[] result = other.XGCD(this.modulus(other));
			return new LargeInteger[]{result[0], shrink(result[2]), shrink(result[1].subtract(this.division(other).multiply(result[2])))};
		}
	 }

	public LargeInteger modularExp(LargeInteger y, LargeInteger n) {
		if(y.equals(new LargeInteger(ONE).negate())){
			LargeInteger[] GCD = n.XGCD(this);
			if(GCD[2].isNegative()){
				return n.add(GCD[2]);
			}else {
				return GCD[2];
			}
		}else{
			boolean negative = false;
			if(y.isNegative()){
				negative = true;
				y = y.negate();
			}
			LargeInteger result = new LargeInteger(ONE);
			LargeInteger value = this.modulus(n);
			LargeInteger pow = y;
			while (!pow.isNegative() && !pow.equals(new LargeInteger(ZERO))) {
				if (pow.modulus(new LargeInteger(TWO)).equals(new LargeInteger(ONE))) {
					result = (result.multiply(value)).modulus(n);
				}
				pow = pow.shiftRight();
				value = (value.multiply(value)).modulus(n);
			}
			if(negative){
				return result.modularExp(new LargeInteger(ONE).negate(), n);
			}
			return result;
		}
	 }

	public LargeInteger shiftLeft(){
		byte[] result;
		if((val[0] & 0xC0) == 0x40){
			result = new byte[val.length + 1];
			result[0] = (byte)0x00;
		}else if((val[0] & 0xC0) == 0x80) {
			result = new byte[val.length + 1];
			result[0] = (byte)0xFF;
		}else{
			result = new byte[val.length];
		}
		int oldShiftVal = 0, shiftVal;
		for(int i = 1; i <= val.length; i++){
			shiftVal = val[val.length - i] >> 7 & 0x01;
			result[result.length - i] = (byte)((val[val.length - i] << 1) & 0xfe);
			result[result.length - i] |= oldShiftVal;
			oldShiftVal = shiftVal;
		}
		return new LargeInteger(result);
	}

	public LargeInteger shiftRight(){
		byte[] result;
		int i;
		if(val[0] == 0x00 && (val[1] & 0x80) == 0x80){
			result = new byte[val.length - 1];
			i = 1;
		}else{
			result = new byte[val.length];
			i = 0;
		}
		int oldShiftVal, shiftVal;
		if(this.isNegative()){
			oldShiftVal = 1;
		}else{
			oldShiftVal = 0;
		}
		for(int j = 0; j < result.length; j++, i++){
			shiftVal = val[i] & 0x01;
			result[j] = (byte)((val[i] >> 1) & 0x7f);
			result[j] |= oldShiftVal << 7;
			oldShiftVal = shiftVal;
		}
		return shrink(new LargeInteger(result));
	}

	public LargeInteger modulus(LargeInteger other){
		LargeInteger quotient = this.division(other);
		return shrink(this.subtract(other.multiply(quotient)));
	}

	public LargeInteger division(LargeInteger other){
		LargeInteger quotient = new LargeInteger(ZERO), accumulator = this, denominator = other;
		if(this.isNegative()){
			accumulator = this.negate();
		}
		if(other.isNegative()){
			denominator = other.negate();
		}
		int shiftCount = 0;
		while(!accumulator.subtract(denominator).isNegative()){
			denominator = denominator.shiftLeft();
			shiftCount++;
		}
		denominator = denominator.shiftRight();
		for(; shiftCount > 0; shiftCount--){
			quotient = quotient.shiftLeft();
			if(!accumulator.subtract(denominator).isNegative()){
				accumulator = accumulator.subtract(denominator);
				quotient = quotient.add(new LargeInteger(ONE));
			}
			denominator = denominator.shiftRight();
		}
		if(this.isNegative() == other.isNegative()){
			if(quotient.isNegative()){
				return shrink(quotient.negate());
			}else{
				return shrink(quotient);
			}
		}else{
			if(quotient.isNegative()){
				return shrink(quotient);
			}else{
				return shrink(quotient.negate());
			}
		}
	}

	public LargeInteger store(){
		byte[] store = val;
		while(store.length > 64){
			byte[] result = new byte[this.length() - 1];
			System.arraycopy(store, 1, result, 0, result.length);
			store = result;
		}
		while(this.length() < 64) {
			byte[] result = new byte[this.length() + 1];
			if ((store[0] & 0x80) == 0x80){
				result[0] = (byte) 0xFF;
			}else{
				result[0] = (byte)0x00;
			}
			System.arraycopy(store, 0, result, 1, result.length-1);
			store = result;
		}
		return new LargeInteger(store);
	}

	public LargeInteger shrink(LargeInteger other){
		if(other.length() > 1 && (((other.getVal()[0] & 0xff) == 0x00 && ((other.getVal()[1] & 0x80) == 0x00)) || ((other.getVal()[0] & 0xff) == 0xff && (other.getVal()[1] & 0x80) == 0x80))){
			byte[] shrunk = new byte[other.length() - 1];
			System.arraycopy(other.getVal(), 1, shrunk, 0, shrunk.length);
			return shrink(new LargeInteger(shrunk));
		}
		return other;
	}

	public boolean equals(LargeInteger other){
		if(this.length() != other.length()){
			return false;
		}
		for(int i = 0; i < this.length(); i++){
			if(this.getVal()[i] != other.getVal()[i]){
				return false;
			}
		}
		return true;
	}
}
