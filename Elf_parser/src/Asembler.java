import java.io.PrintStream;
import java.util.Map;

public class Asembler {

	String asem;
	String operation;
	int r1;
	int r2;
	int r3;
	int id;
	int imm;
	int size;
	String label;
	// переменные, которые хранят значения команд

	String[] registers = {"zero", "ra", "sp", "gp", "tp", "t0", "t1", "t2", "s0", "s1", "a0",
							"a1", "a2", "a3", "a4", "a5", "a6", "a7", "s2", "s3", "s4", "s5", "s6",
								"s7", "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"}; // 5 битные регистры

	String[] registers16 = {"s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5"}; // 3 битные регистры
	private long parse(int ind, int size, int[] arr) { // реализует раскодировку little endian
		long t = 1;
		long address = 0;
		for (int i = ind; i < ind + size; t *= 256, i++) {
			address = address + t * arr[i];
		}
		return address;
	}

	public Asembler(int ind, int[] arr, int size) { // создание класса
		long val = parse(ind, size, arr);
		this.size = size;
		StringBuilder sb = new StringBuilder();

		while (val > 0) {
			sb.append(val % 2);
			val /= 2;
		}
		while (sb.length() < (size == 4 ? 32 : 16)) {
			sb.append(0);
		}
		sb.reverse();
		asem = sb.toString();
	}

	private int parseInt(String s) { // переводит двоичную строчку в число
		int value = 0;
		int t = 1;
		for (int i = s.length() - 1; i >= 0; i--, t *= 2) {
			if (s.charAt(i) == '1') {
				value = value + t;
			}
		}
		return value;
	}

	private String parseLabel(int val) { // переводит число в метку нужного формата
		StringBuilder sb = new StringBuilder();
		//System.out.println(size);

		while (val > 0) {
			if (val % 16 == 10) {
				sb.append('a');
			} else if (val % 16 == 11) {
				sb.append('b');
			} else if (val % 16 == 12) {
				sb.append('c');
			} else if (val % 16 == 13) {
				sb.append('d');
			} else if (val % 16 == 14) {
				sb.append('e');
			} else if (val % 16 == 15) {
				sb.append('f');
			} else {
				sb.append(val % 16);
			}
			val /= 16;
		}
		while (sb.length() < 5) {
			sb.append(0);
		}
		return sb.reverse().toString();
	}

	public int parse(int ind, int labelNum, Map <Integer, String> labelCollection, boolean log, PrintStream out) { // дизассемблер
		id = ind;
		int labelFlag = 0;
		if(size == 4) { // 32 битные команды
			String opcode = asem.substring(25);
			switch (opcode) {
				case "0110111" -> {
					operation = "lui";
					r3 = parseInt(asem.substring(20, 25));
					imm = parseInt(asem.substring(0, 20));
					if (log) {
						out.printf("%08x %10s %s %s, %s",
								id, label, operation, registers[r3], imm);
						out.println();
					}
				}
				case "0010111" -> {
					operation = "auipc";
					r3 = parseInt(asem.substring(20, 25));
					imm = parseInt(asem.substring(0, 20));
					if (log) {
						out.printf("%08x %10s %s %s, %s",
								id, label, operation, registers[r3], imm);
						out.println();
					}
				}
				case "1101111" -> {
					operation = "jal";
					r3 = parseInt(asem.substring(20, 25));
					imm = parseInt(asem.substring(12, 20) + asem.charAt(11) + asem.substring(1, 11)) - parseInt(String.valueOf(asem.charAt(0))) * 524288;//31 21..12 22 30..23

					if (!labelCollection.containsKey(imm * 2 + id)) {
						labelFlag = 1;
						labelCollection.put(imm * 2 + id, "LOC_" + parseLabel(labelNum));
					}
					if (log) {
						out.printf("%08x %10s %s %s, %s",
								id, label, operation, registers[r3], labelCollection.get(imm * 2 + id));
						out.println();
					}
				}
				case "1100111" -> {
					operation = "jalr";
					r3 = parseInt(asem.substring(20, 25));
					r1 = parseInt(asem.substring(12, 17));
					imm = parseInt(asem.substring(1, 12)) - parseInt(asem.substring(0, 1)) * 2048;
					if (log) {
						out.printf("%08x %10s %s %s, %s(%s)",
								id, label, operation, registers[r3], imm, registers[r1]);
						out.println();
					}
				}
				case "1100011" -> {
					switch (asem.substring(17, 20)) {
						case "000" -> {
							operation = "beq";
						}
						case "001" -> {
							operation = "bne";
						}
						case "100" -> {
							operation = "blt";
						}
						case "101" -> {
							operation = "bge";
						}
						case "110" -> {
							operation = "bltu";
						}
						case "111" -> {
							operation = "bgeu";
						}
					}
					imm = parseInt(asem.charAt(24) + asem.substring(1, 7) + asem.substring(20, 24)) - parseInt(String.valueOf(asem.charAt(0))) * 2048;
					r2 = parseInt(asem.substring(7, 12));
					r1 = parseInt(asem.substring(12, 17));
					if (!labelCollection.containsKey(imm * 2 + id)) {
						labelFlag = 1;
						labelCollection.put(imm * 2 + id, "LOC_" + parseLabel(labelNum));
					}
					if (log) {
						out.printf("%08x %10s %s %s, %s, %s",
								id, label, operation, registers[r1], registers[r2], labelCollection.get(imm * 2 + id));
						out.println();
					}
				}
				case "0000011" -> {
					switch (asem.substring(17, 20)) {
						case "000" -> {
							operation = "lb";
						}
						case "001" -> {
							operation = "lh";
						}
						case "010" -> {
							operation = "lw";
						}
						case "100" -> {
							operation = "lbu";
						}
						case "101" -> {
							operation = "lhu";
						}
					}
					r3 = parseInt(asem.substring(20, 25));
					r1 = parseInt(asem.substring(12, 17));
					imm = parseInt(asem.substring(1, 12)) - parseInt(asem.substring(0, 1)) * 2048;
					if (log) {
						out.printf("%08x %10s %s %s, %s(%s)",
								id, label, operation, registers[r3], imm, registers[r1]);
						out.println();
					}
				}
				case "0100011" -> {
					switch (asem.substring(17, 20)) {
						case "000" -> {
							operation = "sb";
						}
						case "001" -> {
							operation = "sh";
						}
						case "010" -> {
							operation = "sw";
						}
					}
					imm = parseInt(asem.substring(1, 7) + asem.substring(20, 25)) - parseInt(asem.substring(0, 1)) * 2048;
					r1 = parseInt(asem.substring(12, 17));
					r2 = parseInt(asem.substring(7, 12));
					if (log) {
						out.printf("%08x %10s %s %s, %s(%s)",
								id, label, operation, registers[r2], imm, registers[r1]);
						out.println();
					}
				}
				case "0010011" -> {
					switch (asem.substring(17, 20)) {
						case "000" -> {
							operation = "addi";
						}
						case "010" -> {
							operation = "slti";
						}
						case "011" -> {
							operation = "sltiu";
						}
						case "100" -> {
							operation = "xori";
						}
						case "110" -> {
							operation = "ori";
						}
						case "111" -> {
							operation = "andi";
						}
						case "001" -> {
							operation = "slli";
						}
						case "101" -> {
							if (asem.charAt(1) == '1') {
								operation = "srai";
							} else {
								operation = "srli";
							}
						}
					}
					r3 = parseInt(asem.substring(20, 25));
					r1 = parseInt(asem.substring(12, 17));
					if (operation.equals("slii") || operation.equals("srli") || operation.equals("srai")) {
						imm = parseInt(asem.substring(7, 12));
					} else {
						imm = parseInt(asem.substring(1, 12)) - parseInt(asem.substring(0, 1)) * 2048;
					}
					if (log) {
						out.printf("%08x %10s %s %s, %s, %s",
								id, label, operation, registers[r3], registers[r1], imm);
						out.println();
					}
				}
				case "0110011" -> {
					if (asem.charAt(6) == '1') {
						switch (asem.substring(17, 20)) {
							case "000" -> {
								operation = "mul";
							}
							case "001" -> {
								operation = "mulh";
							}
							case "010" -> {
								operation = "mulhsu";
							}
							case "011" -> {
								operation = "mulhu";
							}
							case "100" -> {
								operation = "div";
							}
							case "101" -> {
								operation = "divu";
							}
							case "110" -> {
								operation = "rem";
							}
							case "111" -> {
								operation = "remu";
							}
						}
					} else {
						switch (asem.substring(17, 20)) {
							case "000" -> {
								if (asem.charAt(1) == '1') {
									operation = "sub";
								} else {
									operation = "add";
								}
							}
							case "101" -> {
								if (asem.charAt(1) == '1') {
									operation = "srl";
								} else {
									operation = "sra";
								}
							}
							case "001" -> {
								operation = "sll";
							}
							case "010" -> {
								operation = "slt";
							}
							case "011" -> {
								operation = "sltu";
							}
							case "100" -> {
								operation = "xor";
							}
							case "110" -> {
								operation = "or";
							}
							case "111" -> {
								operation = "and";
							}
						}
					}
					r3 = parseInt(asem.substring(20, 25));
					r1 = parseInt(asem.substring(12, 17));
					r2 = parseInt(asem.substring(7, 12));
					if (log) {
						out.printf("%08x %10s %s %s, %s, %s",
								id, label, operation, registers[r3], registers[r1], registers[r2]);
						out.println();
					}
				}
				case "1110011" -> {
					if (asem.substring(17, 20).equals("000")) {
						if (asem.charAt(11) == '0') {
							operation = "ecall";
							if (log) {
								out.printf("%08x %10s %s",
										id, label, operation);
								out.println();
							}
							//System.out.println("ecall");
						} else {
							operation = "ebreak";
							if (log) {
								out.printf("%08x %10s %s",
										id, label, operation);
								out.println();
							}
						}
					} else {
						switch (asem.substring(17, 20)) {
							case "001" -> {
								operation = "csrrw";
								r1 = parseInt(asem.substring(20, 25));
								r2 = parseInt(asem.substring(12, 17));
								imm = parseInt(asem.substring(0, 12));
								if (log) {
									out.printf("%08x %10s %s %s, %s, %s",
											id, label, operation, registers[r1], registers[r2], imm);
									out.println();
								}
							}
							case "010" -> {
								operation = "csrrs";
								r1 = parseInt(asem.substring(20, 25));
								r2 = parseInt(asem.substring(12, 17));
								imm = parseInt(asem.substring(0, 12));
								if (log) {
									out.printf("%08x %10s %s %s, %s, %s",
											id, label, operation, registers[r1], imm, registers[r2]);
									out.println();
								}
							}
							case "011" -> {
								operation = "csrrc";
								r1 = parseInt(asem.substring(20, 25));
								r2 = parseInt(asem.substring(12, 17));
								imm = parseInt(asem.substring(0, 12));
								if (log) {
									out.printf("%08x %10s %s %s, %s, %s",
											id, label, operation, registers[r2], imm, registers[r1]);
									out.println();
								}
							}
							case "101" -> {
								operation = "csrrwi";
								r1 = parseInt(asem.substring(20, 25));
								r2 = parseInt(asem.substring(12, 17));
								imm = parseInt(asem.substring(0, 12));
								if (log) {
									out.printf("%08x %10s %s %s, %s, %s",
											id, label, operation, registers[r2], imm, r1);
									out.println();
								}
							}
							case "110" -> {
								operation = "csrrsi";
								r1 = parseInt(asem.substring(20, 25));
								r2 = parseInt(asem.substring(12, 17));
								imm = parseInt(asem.substring(0, 12));
								if (log) {
									out.printf("%08x %10s %s %s, %s, %s",
											id, label, operation, registers[r2], imm, r1);
									out.println();
								}
							}
							case "111" -> {
								operation = "csrrci";
								r1 = parseInt(asem.substring(20, 25));
								r2 = parseInt(asem.substring(12, 17));
								imm = parseInt(asem.substring(0, 12));
								if (log) {
									out.printf("%08x %10s %s %s, %s, %s",
											id, label, operation, registers[r2], imm, r1);
									out.println();
								}
							}

						}
					}
				}
				default -> {
					if (log) {
						out.printf("%08x %10s %s",
								id, label, "unknown_command");
						out.println();
					}
				}
			}
		} else { // 16-ти битные команды
			switch (asem.substring(14, 16)) {
				case "00" -> {
					int t = 0;
					switch (asem.substring(0, 3)) {
						case "000" -> {
							operation = "c.addi4spn";
							imm = parseInt( asem.substring(5, 9) + asem.substring(3, 5) + asem.charAt(10) + asem.charAt(9));
							r1 = parseInt(asem.substring(11, 14));
							if (log) {
								if (r1 == 0) {
									out.printf("%08x %10s %s",
											id, label, "llegal instruction");
									out.println();
								} else {
									out.printf("%08x %10s %s %s, sp, %s",
											id, label, operation, registers16[r1], imm * 4);
									out.println();
								}
							}
							return labelFlag;
						}
						case "001" -> {
							operation = "c.fld";
							t = 8;
							imm = parseInt(asem.substring(9, 11) + asem.substring(3, 6));
						}
						case "010" -> {
							operation = "c.lw";
							t = 4;
							imm = parseInt(asem.charAt(10) + asem.substring(3, 6) + asem.charAt(9));
						}
						case "011" -> {
							operation = "c.flw";
							t = 4;
							imm = parseInt(asem.charAt(10) + asem.substring(3, 6) + asem.charAt(9));
						}
						case "101" -> {
							operation = "c.fsd";
							t = 8;
							imm = parseInt(asem.substring(9, 11) + asem.substring(3, 6));
						}
						case "110" -> {
							operation = "c.sw";
							t = 4;
							imm = parseInt(asem.charAt(10) + asem.substring(3, 6) + asem.charAt(9));
						}
						case "111" -> {
							operation = "c.fsw";
							t = 4;
							imm = parseInt(asem.charAt(10) + asem.substring(3, 6) + asem.charAt(9));
						}
					}
					r1 = parseInt(asem.substring(11, 14));
					r2 = parseInt(asem.substring(6, 9));

					if (log) {
						out.printf("%08x %10s %s %s, %s(%s)",
								id, label, operation, registers16[r1], imm * t, registers16[r2]);
						out.println();
					}
				}
				case "01" -> {
					switch (asem.substring(0, 3)) {
						case "000" -> {
							r1 = parseInt(asem.substring(4, 9));
							if (r1 == 0) {
								operation = "c.nop";
							} else {
								operation = "c.addi";
							}
							imm = parseInt( asem.substring(9, 14)) - parseInt(String.valueOf(asem.charAt(3))) * 32;
							if (log) {
								if (r1 == 0) {
									out.printf("%08x %10s %s",
											id, label, operation);
									out.println();
								} else {
									out.printf("%08x %10s %s %s, %s",
											id, label, operation, registers[r1], imm);
									out.println();
								}
							}
						}
						case "001" -> {

							imm = parseInt(asem.charAt(7) + asem.substring(5, 7) + asem.charAt(9)
							+ asem.charAt(8) + asem.charAt(13) + asem.charAt(4) + asem.substring(10, 13)) - parseInt(String.valueOf(asem.charAt(3))) * 1024;
							operation = "c.jal";
							if (!labelCollection.containsKey(imm * 2 + id)) {
								labelFlag = 1;
								labelCollection.put(imm * 2 + id, "LOC_" + parseLabel(labelNum));
							}
							if (log) {
								out.printf("%08x %10s %s %s",
										id, label, operation, labelCollection.get(imm * 2 + id));
								out.println();
							}
						}
						case "010" -> {
							operation = "c.li";
							r1 = parseInt(asem.substring(4, 9));
							imm = parseInt(asem.substring(9, 14)) - parseInt(String.valueOf(asem.charAt(3))) * 32;
							if (log) {
								out.printf("%08x %10s %s %s, %s",
										id, label, operation, registers[r1], imm);
								out.println();
							}
						}
						case "011" -> {
							r1 = parseInt(asem.substring(4, 9));
							if (r1 == 2) {
								operation = "c.addi16sp";
								imm = parseInt(asem.substring(11, 13) + asem.charAt(10)
										+ asem.charAt(13) + asem.charAt(9)) * 16 - parseInt(String.valueOf(asem.charAt(3))) * 512;
							} else {
								operation = "c.lui";
								imm = parseInt(asem.substring(9, 14)) * 4096 - parseInt(String.valueOf(asem.charAt(3))) * 131072;
							}
							if (log) {
								out.printf("%08x %10s %s %s, %s",
										id, label, operation, registers[r1], imm);
								out.println();
							}
						}
						case "100" -> {
							switch (asem.substring(4, 6)) {
								case "00" -> {
									operation = "c.srli";
								}
								case "01" -> {
									operation = "c.srai";
								}
								case "10" -> {
									operation = "c.andi";
								}
								case "11" -> {
									switch (asem.substring(9, 11)){
										case "00" -> {
											operation = "c.sub"; //c.subw
										}
										case "01" -> {
											operation = "c.xor"; // c.addw
										}
										case "10" -> {
											operation = "c.or";
										}
										case "11" -> {
											operation = "c.and";
										}
									}
									r2 = parseInt(asem.substring(11, 14));
									r1 = parseInt(asem.substring(6, 9));
									if (log) {
										out.printf("%08x %10s %s %s, %s",
												id, label, operation, registers16[r1], registers16[r2]);
										out.println();
									}
									return labelFlag;
								}
							}
							r1 = parseInt(asem.substring(6, 9));
							imm = parseInt(asem.charAt(3) + asem.substring(9, 14));
							if (log) {
								out.printf("%08x %10s %s %s, %s",
										id, label, operation, registers16[r1], imm);
								out.println();
							}
						}
						case "101" -> {
							operation = "c.j";
							imm = parseInt(asem.charAt(7) + asem.substring(5, 7) + asem.charAt(9)
									+ asem.charAt(8) + asem.charAt(13) + asem.charAt(4) + asem.substring(10, 13)) - parseInt(String.valueOf(asem.charAt(3))) * 1024;
							if (!labelCollection.containsKey(imm * 2 + id)) {
								labelFlag = 1;
								labelCollection.put(imm * 2 + id, "LOC_" + parseLabel(labelNum));
							}
							if (log) {
								out.printf("%08x %10s %s %s",
										id, label, operation, labelCollection.get(imm * 2 + id));
								out.println();
							}
						}
						case "110" -> {
							operation = "c.beqz";
							r1 = parseInt(asem.substring(6, 9));
							imm = parseInt(asem.substring(9, 11) + asem.charAt(13) + asem.substring(4, 6) + asem.substring(11, 13)) - parseInt(String.valueOf(asem.charAt(3))) * 128;
							if (!labelCollection.containsKey(imm * 2 + id)) {
								labelFlag = 1;
								labelCollection.put(imm * 2 + id, "LOC_" + parseLabel(labelNum));
							}
							if (log) {
								out.printf("%08x %10s %s %s, %s",
										id, label, operation, registers16[r1], labelCollection.get(imm * 2 + id));
								out.println();
							}
						}
						case "111" -> {
							operation = "c.bnez"; // c.slli64
							r1 = parseInt(asem.substring(6, 9));
							imm = parseInt(asem.substring(9, 11) + asem.charAt(13) + asem.substring(4, 6) + asem.substring(11, 13)) - parseInt(String.valueOf(asem.charAt(3))) * 128;
							if (!labelCollection.containsKey(imm * 2 + id)) {
								labelFlag = 1;
								labelCollection.put(imm * 2 + id, "LOC_" + parseLabel(labelNum));
							}
							if (log) {
								out.printf("%08x %10s %s %s, %s",
										id, label, operation, registers16[r1], labelCollection.get(imm * 2 + id));
								out.println();
							}
						}
					}
				}
				case "10" -> {
					switch (asem.substring(0, 3)) {
						case "000" -> {
							operation = "c.slli";
							r1 = parseInt(asem.substring(4, 9));
							imm = parseInt(asem.charAt(3) + asem.substring(9, 14));
							if (log) {
								out.printf("%08x %10s %s %s, %s",
										id, label, operation, registers[r1], imm);
								out.println();
							}
						}
						case "001" -> {
							operation = "c.fldsp";
							r1 = parseInt(asem.substring(4, 9));
							imm = parseInt(asem.substring(11, 14) + asem.charAt(3) + asem.substring(9, 11));
							if (log) {
								out.printf("%08x %10s %s %s, %s(sp)",
										id, label, operation, registers[r1], imm * 8);
								out.println();
							}
						}
						case "010" -> {
							operation = "c.lwsp";
							r1 = parseInt(asem.substring(4, 9));
							imm = parseInt(asem.substring(12, 14) + asem.charAt(3) + asem.substring(9, 12));
							if (log) {
								out.printf("%08x %10s %s %s, %s(sp)",
										id, label, operation, registers[r1], imm * 4);
								out.println();
							}
						}
						case "011" -> {
							operation = "c.flwsp";
							r1 = parseInt(asem.substring(4, 9));
							imm = parseInt(asem.substring(11, 14) + asem.charAt(3) + asem.substring(9, 11));
							if (log) {
								out.printf("%08x %10s %s %s, %s(sp)",
										id, label, operation, registers[r1], imm * 4);
								out.println();
							}
						}
						case "100" -> {
							switch (parseInt(String.valueOf(asem.charAt(3)))) {
								case 0 -> {
									r1 = parseInt(asem.substring(4, 9));
									r2 = parseInt(asem.substring(9, 14));
									if (r2 != 0) {
										operation = "c.mv";
										if (log) {
											out.printf("%08x %10s %s %s, %s",
													id, label, operation, registers[r1], registers[r2]);
											out.println();
										}
									} else {
										operation = "c.jr";
										if (log) {
											out.printf("%08x %10s %s %s",
													id, label, operation, registers[r1]);
											out.println();
										}
									}
								}
								case 1 -> {
									r1 = parseInt(asem.substring(4, 9));
									r2 = parseInt(asem.substring(9, 14));
									if (r2 != 0 && r1 != 0) {
										operation = "c.add";
										if (log) {
											out.printf("%08x %10s %s %s, %s",
													id, label, operation, registers[r1], registers[r2]);
											out.println();
										}
									} else if (r1 != 0) {
										operation = "c.jalr";
										if (log) {
											out.printf("%08x %10s %s %s",
													id, label, operation, registers[r1]);
											out.println();
										}
									} else {
										operation = "c.ebreak";
										if (log) {
											out.printf("%08x %10s %s",
													id, label, operation);
											out.println();
										}
									}
								}
							}
						}
						case "101" -> {
							operation = "c.fsdsp";
							r1 = parseInt(asem.substring(9, 14));
							imm = parseInt(asem.substring(6, 9) + asem.substring(3, 6));
							if (log) {
								out.printf("%08x %10s %s %s, %s(sp)",
										id, label, operation, registers[r1], imm * 8);
								out.println();
							}
						}
						case "110" -> {
							operation = "c.swsp";
							r1 = parseInt(asem.substring(9, 14));
							imm = parseInt(asem.substring(7, 9) + asem.substring(3, 7));
							if (log) {
								out.printf("%08x %10s %s %s, %s(sp)",
										id, label, operation, registers[r1], imm * 4);
								out.println();
							}
						}
						case "111" -> {
							operation = "c.fswsp";
							r1 = parseInt(asem.substring(9, 14));
							imm = parseInt(asem.substring(7, 9) + asem.substring(3, 7));
							if (log) {
								out.printf("%08x %10s %s %s, %s(sp)",
										id, label, operation, registers[r1], imm * 4);
								out.println();
							}
						}
					}
				}
				default -> {
					if (log) {
						out.printf("%08x %10s %s",
								id, label, "unknown_command");
						out.println();
					}
				}
			}
		}
		return labelFlag;
	}
}
