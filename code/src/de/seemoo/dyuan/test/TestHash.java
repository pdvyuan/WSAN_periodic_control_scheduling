package de.seemoo.dyuan.test;

public class TestHash {

	private static long hash(int key)
	{
	  key = ~key + (key << 15); // key = (key << 15) - key - 1;
	  key = key ^ (key >>> 12);
	  key = key + (key << 2);
	  key = key ^ (key >>> 4);
	  key = key * 2057; // key = (key + (key << 3)) + (key << 11);
	  key = key ^ (key >>> 16);
	  return (key & 0x00000000ffffffffL);
	}
	
	public static void main(String[] args) {
		for (int i=0; i<100; i++) {
			long v = hash(i);
			System.out.println("hash "+i + " "+v);
			int m = (int)(v % 100);
			System.out.println("mod = " + m);
		}
	
	}
}
