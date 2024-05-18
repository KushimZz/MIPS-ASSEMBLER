import java.io.*;
import java.util.*;

public class MIPSAssembler {

    //file read method
    public static List<String> readFile(String filename) throws IOException {
        List<String> lines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = reader.readLine()) != null) { //if line is not null it keeps reading
            lines.add(line.trim());
        }
        reader.close();
        return lines;
    }

    public static Map<String, Integer> parseLabels(List<String> lines) {
        Map<String, Integer> labels = new HashMap<>();
        int address = 0x00400000; //first adress
        for (String line : lines) {
            if (line.endsWith(":")) {
                labels.put(line.substring(0, line.length() - 1), address);
            } else if (!line.startsWith(".") && !line.isEmpty()) {
                address += 4;
            }
        }
        return labels;
    }

    public static String translateInstruction(String instruction, Map<String, Integer> labels, int currentAddress) {
        String[] parts = instruction.split("[ ,\t]+");
        String opcode = parts[0];

        switch (opcode) {
            case "add":
                return formatRType(0x20, register(parts[1]), register(parts[2]), register(parts[3]));
            case "sub":
                return formatRType(0x22, register(parts[1]), register(parts[2]), register(parts[3]));
            case "and":
                return formatRType(0x24, register(parts[1]), register(parts[2]), register(parts[3]));
            case "or":
                return formatRType(0x25, register(parts[1]), register(parts[2]), register(parts[3]));
            case "sllv":
                return formatRType(0x04, register(parts[1]), register(parts[3]), register(parts[2]));
            case "srlv":
                return formatRType(0x06, register(parts[1]), register(parts[3]), register(parts[2]));
            case "sll":
                return formatShiftType(0x00, register(parts[1]), register(parts[2]), immediate(parts[3]));
            case "srl":
                return formatShiftType(0x02, register(parts[1]), register(parts[2]), immediate(parts[3]));
            case "addi":
                return formatIType(0x20000000, register(parts[2]), register(parts[1]), immediate(parts[3]));
            case "andi":
                return formatIType(0x30000000, register(parts[2]), register(parts[1]), immediate(parts[3]));
            case "lw":
                return formatMemoryType(0x8C000000, register(parts[1]), parts[2]);
            case "sw":
                return formatMemoryType(0xAC000000, register(parts[1]), parts[2]);
            case "beq":
                return formatBranchType(0x10000000, register(parts[1]), register(parts[2]), labels.get(parts[3]), currentAddress);
            case "bne":
                return formatBranchType(0x14000000, register(parts[1]), register(parts[2]), labels.get(parts[3]), currentAddress);
            case "blez":
                return formatBranchType(0x18000000, register(parts[1]), 0, labels.get(parts[2]), currentAddress);
            case "bgtz":
                return formatBranchType(0x1C000000, register(parts[1]), 0, labels.get(parts[2]), currentAddress);
            case "j":
                return String.format("0x%08x", 0x08000000 | ((labels.get(parts[1]) >> 2) & 0x3FFFFFF));
            default:
                throw new IllegalArgumentException("This instruction is not supported: " + instruction);
        }
    }

    private static String formatRType(int funct, int rd, int rs, int rt) {
        return String.format("0x%08x", funct | (rd << 11) | (rs << 21) | (rt << 16));
    }

    private static String formatShiftType(int funct, int rd, int rt, int shamt) {
        return String.format("0x%08x", funct | (rd << 11) | (rt << 16) | (shamt << 6));
    }

    private static String formatIType(int opcode, int rs, int rt, int immediate) {
        return String.format("0x%08x", opcode | (rt << 16) | (rs << 21) | (immediate & 0xFFFF));
    }

    private static String formatMemoryType(int opcode, int rt, String offsetAndBase) {
        String[] parts = offsetAndBase.split("[()]");
        int offset = Integer.parseInt(parts[0]);
        int base = register(parts[1].replace("$", ""));
        return String.format("0x%08x", opcode | (rt << 16) | (base << 21) | (offset & 0xFFFF));
    }

    private static String formatBranchType(int opcode, int rs, int rt, int labelAddress, int currentAddress) {
        int offset = (labelAddress - currentAddress - 4) >> 2;
        return String.format("0x%08x", opcode | (rs << 21) | (rt << 16) | (offset & 0xFFFF));
    }

    private static int register(String reg) {
        return Integer.parseInt(reg.replace("$", ""));
    }

    private static int immediate(String imm) {
        return Integer.parseInt(imm);
    }

    public static void writeOutputFile(String filename, List<String> machineCode) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename.replace(".asm", ".obj")));
        int address = 0x00400000;
        writer.write("Adress     Code\n");
        for (String code : machineCode) {
            writer.write(String.format("0x%08x %s\n", address, code));
            address += 4;
        }
        writer.close();
    }

    public static void main(String[] args) throws IOException {
        String inputFilename = "C:\\Users\\yasin\\Desktop\\Intellij\\MIPSAssembler\\src\\mipsmars.asm"; // Change as needed
        List<String> lines = readFile(inputFilename);
        Map<String, Integer> labels = parseLabels(lines);
        List<String> machineCode = new ArrayList<>();

        lines.removeIf(line -> line.startsWith("#") || line.isEmpty()); // Remove comments and empty lines

        int address = 0x00400000;
        for (String line : lines) {
            if (!line.endsWith(":") && !line.startsWith(".")) {
                String code = translateInstruction(line, labels, address);
                machineCode.add(code);
                address += 4;
            }
        }
        writeOutputFile(inputFilename, machineCode);
    }
}
