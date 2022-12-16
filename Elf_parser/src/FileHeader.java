public class FileHeader extends AbstractHeader{

	public String name;
	public int address;

	FileHeader (int ind, int size, int[] arr, String name) { // создание класса
		this.name = name;
		this.address = parse(ind, size, arr);
	}
}
