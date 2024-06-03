import java.io.*;
import java.util.*;

public class MIPSAssembler {
    public static List<String> readFile(String filename) throws IOException {
        List<String> lines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line.trim());
        }
        reader.close();
        return lines;
    }

    public static Map<String, Integer> labelSeparation(List<String> lines) {
        Map<String, Integer> labels = new HashMap<>();
        int address = 0x00400000;
        for (String line : lines) {
            if (line.endsWith(":")) {
                labels.put(line.substring(0, line.length() - 1), address);
            } else if (!line.isEmpty()) {
                address += 4;
            }
        }
        return labels;
    }
    public static String instruction(String instruction, Map<String, Integer> labels, int currentAddress) {
        String[] parts = instruction.split("[ ,\t]+");
        String first = parts[0];

        return switch (first) {
            case "add" -> formatRType(0x20, register(parts[1]), register(parts[2]), register(parts[3]));
            case "addu" -> formatRType(0x21, register(parts[1]), register(parts[2]), register(parts[3]));
            case "sub" -> formatRType(0x22, register(parts[1]), register(parts[2]), register(parts[3]));
            case "and" -> formatRType(0x24, register(parts[1]), register(parts[2]), register(parts[3]));
            case "or" -> formatRType(0x25, register(parts[1]), register(parts[2]), register(parts[3]));
            case "sllv" -> formatRType(0x04, register(parts[1]), register(parts[3]), register(parts[2]));
            case "srlv" -> formatRType(0x06, register(parts[1]), register(parts[3]), register(parts[2]));
            case "sll" -> formatShiftType(0x00, register(parts[1]), register(parts[2]), immediate(parts[3]));
            case "srl" -> formatShiftType(0x02, register(parts[1]), register(parts[2]), immediate(parts[3]));
            case "addi" -> formatIType(0x20000000, register(parts[2]), register(parts[1]), immediate(parts[3]));
            case "addiu" -> formatIType(0x24000000, register(parts[2]), register(parts[1]), immediate(parts[3]));
            case "andi" -> formatIType(0x30000000, register(parts[2]), register(parts[1]), immediate(parts[3]));
            case "lw" -> formatMemoryType(0x8C000000, register(parts[1]), parts[2]);
            case "sw" -> formatMemoryType(0xAC000000, register(parts[1]), parts[2]);
            case "beq" -> formatBranchType(0x10000000, register(parts[1]), register(parts[2]), labels.get(parts[3]), currentAddress);
            case "bne" -> formatBranchType(0x14000000, register(parts[1]), register(parts[2]), labels.get(parts[3]), currentAddress);
            case "blez" -> formatBranchType(0x18000000, register(parts[1]), 0, labels.get(parts[2]), currentAddress);
            case "bgtz" -> formatBranchType(0x1C000000, register(parts[1]), 0, labels.get(parts[2]), currentAddress);
            case "j" -> String.format("0x%08x", 0x08000000 + ((labels.get(parts[1]) >> 2) & 0x3FFFFFF));
            default -> throw new IllegalArgumentException("This instruction is not supported: " + instruction);
        };
    }

    private static String formatRType(int funct, int rd, int rs, int rt) {
        return String.format("0x%08x",(rs * (1 << 21)) + (rt * (1 << 16)+ (rd * (1 << 11))+ funct));
    }

    private static String formatShiftType(int funct, int rd, int rt, int shamt) {
        return String.format("0x%08x",(rt * (1 << 16)) + (rd * (1 << 11)) + (shamt * (1 << 6) + funct));
    }

    private static String formatIType(int opcode, int rs, int rt, int immediate) {
        return String.format("0x%08x", opcode + (rs * (1 << 21)) + (rt * (1 << 16)) + (immediate & 0xFFFF));
    }

    private static String formatMemoryType(int opcode, int rt, String offsetAndBase) {
        String[] parts = offsetAndBase.split("[()]");
        int offset = Integer.parseInt(parts[0]);
        int base = register(parts[1].replace("$", ""));
        return String.format("0x%08x", opcode +  (base * (1 << 21)) + (rt * (1 << 16)) + (offset & 0xFFFF));
    }

    private static String formatBranchType(int opcode, int rs, int rt, int labelAddress, int currentAddress) {
        int offset = (labelAddress - currentAddress - 4) / 4;
        return String.format("0x%08x", opcode + (rs * (1 << 21)) + (rt * (1 << 16)) + (offset & 0xFFFF));
    }

    private static String formatBranchWithCodeType(int opcode, int rs, int rt, int labelAddress, int currentAddress) {
        int offset = (labelAddress - currentAddress - 4) / 4;
        return String.format("0x%08x", opcode + (rs * (1 << 21)) + (rt * (1 << 16)) + (offset & 0xFFFF));
    }

    private static int immediate(String imm) {
        return Integer.parseInt(imm);
    }

    private static int register(String reg) {
        return Integer.parseInt(reg.replace("$", ""));
    }

    public static void writeOutputFile(String filename, List<String> machineCode) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename.replace(".asm", ".obj")));
        writeHexCode(machineCode, writer);
        writer.close();
    }

    private static void writeHexCode(List<String> machineCode, BufferedWriter writer) throws IOException {
        int address = 0x00400000;
        writer.write("Address     Code\n");
        for (String code : machineCode) {
            writer.write(String.format("0x%08x %s\n", address, code));
            address += 4;
        }
    }

    private static void translateInstructions(List<String> lines, Map<String, Integer> labels, List<String> machineCode) {
        int address = 0x00400000;
        for (String line : lines) {
            if (!line.endsWith(":") && !line.startsWith(".")) {
                String code = instruction(line, labels, address);
                machineCode.add(code);
                address += 4;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String inputFilename = "mipsmars.asm";
        List<String> lines = readFile(inputFilename);
        Map<String, Integer> labels = labelSeparation(lines);
        List<String> machineCode = new ArrayList<>();

        lines.removeIf(line -> line.startsWith("#") || line.isEmpty());

        translateInstructions(lines, labels, machineCode);
        writeOutputFile(inputFilename, machineCode);
    }

}
