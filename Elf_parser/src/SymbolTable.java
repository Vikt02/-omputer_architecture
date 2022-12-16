import java.util.Map;

public class SymbolTable extends AbstractHeader {

	int intName;
	String strName;
	int value;
	int size;
	String binding;
	String type;
	String other;
	String shndx;

	final Map<Integer, String> bind = Map.of(
			0, "LOCAL",
			1, "GLOBAL",
			2, "WEAK",
			13, "LOPROC",
			15, "HIPROC"
	); // поле Bind

	final Map<Integer, String> ty = Map.of(
			0, "NOTYPE",
			1, "OBJECT",
			2, "FUNC",
			3, "SECTION",
			4, "FILE",
			13, "LOPROC",
			15, "HIPROC"
	); // поле type

	final Map<Integer, String> sh = Map.of(
			0, "UNDEF",
			65280, "LOPROC",
			65311, "HIPROC",
			65521, "ABS",
			65522, "COMMON",
			65535, "HIRESERVE"
	); // поле Index

	public SymbolTable(int ind, int[] arr) { // создание класса
		intName = parse(ind, 4, arr);
		value = parse(ind + 4, 4, arr);
		size = parse(ind + 8, 4, arr);
		int info = parse(ind + 12, 1, arr);
		binding = bind.get(info / 16);
		type = ty.get(info % 16);
		other = (parse(ind + 13, 1, arr) == 0 ? "DEFAULT" : "HIDDEN");
		shndx = (sh.get(parse(ind + 14, 2, arr)) == null ? Integer.toString(parse(ind + 14, 2, arr)) : sh.get(parse(ind + 14, 2, arr)));
	}

	public void givaName(String name) { // присвоение имени
		strName = name;
	}
}

