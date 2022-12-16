import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Main {

	public static Map <Integer, String> stringTable = new HashMap<>();
	public static Map <Integer, String> symbTable = new HashMap<>();
	public static Map <Integer, String> labelCollection = new HashMap<>();

	public static int findSection(SectionHeader[] sectionHeaders, String str) { // поиск по таблице заголовков секции, возвращает индекс в таблице по названию
		for (int i = 0; i < sectionHeaders.length; i++) {
			if(sectionHeaders[i].strName.equals(str))
				return i;
		}
		return 0;
	}

	public static Map<Integer, String> makeCollection(int ind, int size, int[] arr) { // создание Map
		StringBuilder sb = new StringBuilder();
		Map <Integer, String> collection = new HashMap<>();
		int start = 0;
		for (int i = ind, j = 0; j < size; j++, i++) {
			if(arr[i] == 0) {
				collection.put(start, sb.toString());
				start = j + 1;
				sb = new StringBuilder();
			} else {
				sb.append((char)arr[i]);
			}
		}

		return collection;
	}

	public static void outSymb(SymbolTable str, int ind, PrintStream out) { // вывод таблицы сивполов
		out.printf("[%4d] 0x%-15x %5d %-8s %-8s %-8s %6s %s",
				ind, str.value, str.size, str.type, str.binding, str.other, str.shndx, (str.strName == null ? "" : str.strName));
		out.println();
	}


	public static String symbTable(int ind, int[] arr) { // получение имени для таблицы символов
		StringBuilder sb = new StringBuilder();

		while (arr[ind] != 0) {
			sb.append((char) arr[ind]);
			ind++;
		}
		return sb.toString();
	}

    public static void main(String[] args) {
        BufferedWriter writer;

        byte[] bit;
        int[] arr;
        try {
			PrintStream out = new PrintStream(args[1]);
            InputStream instr = new FileInputStream(args[0]);

            bit = instr.readAllBytes();
            arr = new int[bit.length];

            for (int i = 0; i < bit.length; i++) {
                arr[i] = (bit[i] < 0 ? (char) (bit[i] + 256) : bit[i]);
            }
			if(arr.length < 5 || arr[0] != 127 || arr[1] != 69 || arr[2] != 76 || arr[3] != 70 || arr[4] != 1) {
				out.println("Unknown type of file");
				return;
			}


				/* Header table */
			FileHeader[] fileHeaders = {
					new FileHeader(28 , 4, arr, "E_phoff"),
					new FileHeader(32,4, arr, "E_shoff"),
					new FileHeader(48,2, arr, "E_shnum"),
					new FileHeader(50,2, arr, "E_shstrndx"),
			}; // получение таблици заголовков в начале
			SectionHeader[] sectionHeaders = new SectionHeader[fileHeaders[2].address];

			for (int i = fileHeaders[1].address, j = 0; j < fileHeaders[2].address; j++, i += 40) {
				sectionHeaders[j] = new SectionHeader(i, arr);
			} // получение таблицы секций

			int indStrT = fileHeaders[3].address;
			int start = 0;
			stringTable = makeCollection(sectionHeaders[indStrT].offset, sectionHeaders[indStrT].size, arr);
			// получение таблицы названий таблицы секций

			for (SectionHeader sectionHeader : sectionHeaders) {
				sectionHeader.giveStrName(stringTable.get(sectionHeader.intName));
			}
			// раздача имен строкам таблице
			int indStrTable = findSection(sectionHeaders, ".strtab"); // поиск индекса таблицы имен symtab

			symbTable = makeCollection(sectionHeaders[indStrTable].offset, sectionHeaders[indStrTable].size, arr);

			int indSymbTable = findSection(sectionHeaders, ".symtab"); // поиск индекса таблицы symtab
			SymbolTable[] symbolTables = new SymbolTable[sectionHeaders[indSymbTable].size / 16];

			for (int i = sectionHeaders[indSymbTable].offset, j = 0; j * 16 < sectionHeaders[indSymbTable].size; j++, i += 16) {
				symbolTables[j] = new SymbolTable(i, arr);
				symbolTables[j].givaName(symbTable(symbolTables[j].intName + sectionHeaders[indStrTable].offset, arr));

				if (symbolTables[j].strName != null && symbolTables[j].type.equals("FUNC")) {
					labelCollection.put(symbolTables[j].value, symbolTables[j].strName);
				}
			} // создание всей таблички symtab
			int indtext = findSection(sectionHeaders, ".text"); // поиск индекса text
			int asemSize = 0;
			Asembler[] asembler = new Asembler[sectionHeaders[indtext].size / 2];
			for (int i = sectionHeaders[indtext].offset, j = 0; i < sectionHeaders[indtext].size + sectionHeaders[indtext].offset; j++, i += (arr[i] % 4 == 3 ? 4 : 2)) {
				asembler[j] = new Asembler(i, arr, (arr[i] % 4 == 3 ? 4 : 2));
				asemSize++;
			} // создание команд

			int label = 0;
			for (int i = 0, j = sectionHeaders[indtext].adderess; i < asemSize; j += asembler[i].size, i++) {
				label += asembler[i].parse(j, label, labelCollection, false, out);
			} // дизассемблер

			for (int i = 0; i < asemSize; i++) {
				Asembler value = asembler[i];
				value.label = (labelCollection.containsKey(value.id) ? labelCollection.get(value.id) + ":" : "");
			} // присвоение ссылок
			out.println(".text");
			for (int i = 0; i < asemSize; i++) {
				Asembler value = asembler[i];
				if (value == null) {
					break;
				}
				value.parse(value.id, 0, labelCollection, true, out);
			} // вывод команд

			out.println();
			out.println(".symtab");
			out.printf("%s %-15s %7s %-8s %-8s %-8s %6s %s",
					"Symbol", "Value", "Size", "Type", "Bind", "Vis", "Index", "Name");
			out.println();
			for (int i = 0; i < symbolTables.length; i++) {
				outSymb(symbolTables[i], i, out);
			} // вывод таблицы символов
			out.close();
            instr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

