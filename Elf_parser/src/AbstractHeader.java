public class AbstractHeader {

	public int parse(int ind, int size, int[] arr) { // реализует раскодировку little endian
		int t = 1;
		int address = 0;
		for (int i = ind; i < ind + size; t *= 256, i++) {
			address = address + t * arr[i];
		}
		return address;
	}
}

