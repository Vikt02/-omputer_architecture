public class SectionHeader extends AbstractHeader{
	int intName;
	int offset;
	int size;
	int adderess;
	String strName;

	SectionHeader (int ind, int[] arr) { // создание класса
		intName = parse(ind, 4, arr);
		offset = parse(ind + 16, 4, arr);
		size = parse(ind + 20, 4, arr);
		adderess = parse(ind + 12, 4, arr);
	}

	public void giveStrName(String str) { // присвоение имени
		strName = str;
	}
}
