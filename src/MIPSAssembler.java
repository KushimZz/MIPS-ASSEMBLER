import java.io.*;
import java.util.*;

public class MIPSAssembler {

    // Method to read a file line by line and return a list of lines
    public static List<String> readFile(String filename) throws IOException {
        List<String> lines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        // Read each line from the file until EOF (end of file)
        while ((line = reader.readLine()) != null) {
            // Add the trimmed line (without leading/trailing spaces) to the list
            lines.add(line.trim());
        }
        reader.close(); // Close the file reader
        return lines; // Return the list of lines
    }

    // Method to separate labels and their addresses from the list of lines
    public static Map<String, Integer> labelSeparation(List<String> lines) {
        Map<String, Integer> labels = new HashMap<>();
        int address = 0x00400000; // Initial address (starting address of the code section)
        for (String line : lines) {
            // Check if the line ends with a colon
            if (line.endsWith(":")) {
                // Add the label (without the colon) and its address to the map
                labels.put(line.substring(0, line.length() - 1), address);
            } else if (!line.isEmpty()) {
                // Increment the address by 4 for each non-empty line
                address += 4;
            }
        }
        return labels; // Return the map of labels and their addresses
    }

    // 0x%08x in String.format
    // 0x Adds "0x" at the beginning
    // %0 Pads with leading zeros
    // 8  Minimum width is 8 characters
    // x  Formats as a hexadecimal number

    // Method to translate an instruction line into machine code
    public static String instruction(String instruction, Map<String, Integer> labels, int currentAddress) {
        // Split the instruction line into parts using spaces, commas, and tabs as delimiters
        String[] parts = instruction.split("[ ,\t]+");
        String opcode = parts[0]; // The first part is the opcode
        // R type opcode always 0
        // Determine the type of instruction and format it accordingly
        return switch (opcode) {
            case "add" -> formatRType(0x20, register(parts[1]), register(parts[2]), register(parts[3]));
            case "sub" -> formatRType(0x22, register(parts[1]), register(parts[2]), register(parts[3]));
            case "and" -> formatRType(0x24, register(parts[1]), register(parts[2]), register(parts[3]));
            case "or" -> formatRType(0x25, register(parts[1]), register(parts[2]), register(parts[3]));
            case "sllv" -> formatRType(0x04, register(parts[1]), register(parts[3]), register(parts[2]));
            case "srlv" -> formatRType(0x06, register(parts[1]), register(parts[3]), register(parts[2]));
            case "sll" -> formatShiftType(0x00, register(parts[1]), register(parts[2]), immediate(parts[3]));
            case "srl" -> formatShiftType(0x02, register(parts[1]), register(parts[2]), immediate(parts[3]));
            case "addi" -> formatIType(0x20000000, register(parts[2]), register(parts[1]), immediate(parts[3]));
            case "andi" -> formatIType(0x30000000, register(parts[2]), register(parts[1]), immediate(parts[3]));
            case "lw" -> formatMemoryType(0x8C000000, register(parts[1]), parts[2]);
            case "sw" -> formatMemoryType(0xAC000000, register(parts[1]), parts[2]);
            case "beq" -> formatBranchType(0x10000000, register(parts[1]), register(parts[2]), labels.get(parts[3]), currentAddress);
            case "bne" -> formatBranchType(0x14000000, register(parts[1]), register(parts[2]), labels.get(parts[3]), currentAddress);
            case "blez" -> formatBranchType(0x18000000, register(parts[1]), 0, labels.get(parts[2]), currentAddress);
            case "bgtz" -> formatBranchType(0x1C000000, register(parts[1]), 0, labels.get(parts[2]), currentAddress);
            case "j" -> String.format("0x%08x", 0x08000000 /*converts opcode to hexa (normaly its 0x08)*/ + ((labels.get(parts[1]) >> 2) & 0x3FFFFFF));
            default -> throw new IllegalArgumentException("This instruction is not supported: " + instruction);
        };
    }

    // Method to format R-type instructions
    private static String formatRType(int funct, int rd, int rs, int rt) {
        // Format the instruction using the function code and registers
        return String.format("0x%08x",(rs * (1 << 21)) + (rt * (1 << 16)+ (rd * (1 << 11))+ funct));
    }

    // Method to format shift-type instructions
    private static String formatShiftType(int funct, int rd, int rt, int shamt) {
        // Format the instruction using the function code, registers, and shift amount
        return String.format("0x%08x",(rt * (1 << 16)) + (rd * (1 << 11)) + (shamt * (1 << 6) + funct));
    }

    // Method to format I-type instructions
    private static String formatIType(int opcode, int rs, int rt, int immediate) {
        // Format the instruction using the opcode, registers, and immediate value
        return String.format("0x%08x", opcode + (rs * (1 << 21)) + (rt * (1 << 16)) + (immediate & 0xFFFF));
    }

    // Method to format memory access instructions (load/store)
    private static String formatMemoryType(int opcode, int rt, String offsetAndBase) {
        // Split the offset and base register from the instruction part
        String[] parts = offsetAndBase.split("[()]");
        int offset = Integer.parseInt(parts[0]);
        int base = register(parts[1].replace("$", ""));
        // Format the instruction using the opcode, register, base, and offset
        return String.format("0x%08x", opcode +  (base * (1 << 21)) + (rt * (1 << 16)) + (offset & 0xFFFF));
    }

    // Method to format branch instructions
    private static String formatBranchType(int opcode, int rs, int rt, int labelAddress, int currentAddress) {
        // Calculate the offset for the branch instruction
        int offset = (labelAddress - currentAddress - 4) / 4;
        // Format the instruction using the opcode, registers, and offset
        return String.format("0x%08x", opcode + (rs * (1 << 21)) + (rt * (1 << 16)) + (offset & 0xFFFF));
    }

    // Method to convert an immediate value from string to integer
    private static int immediate(String imm) {
        return Integer.parseInt(imm); // Parse the string as an integer
    }

    // Method to convert a register name to its numerical value
    private static int register(String reg) {
        return Integer.parseInt(reg.replace("$", "")); // Remove the '$' and parse as integer
    }

    // Method to write the output machine code to a file
    public static void writeOutputFile(String filename, List<String> machineCode) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename.replace(".asm", ".obj")));
        adressInHexa(machineCode, writer);
        writer.close(); // Close the file writer
    }

    private static void adressInHexa(List<String> machineCode, BufferedWriter writer) throws IOException {
        int address = 0x00400000; // Starting address
        writer.write("Address     Code\n"); // Header for the output file
        for (String code : machineCode) {
            // Write each machine code line with its address
            writer.write(String.format("0x%08x %s\n", address, code));
            address += 4; // Increment the address by 4 for each instruction
        }
    }

    // Main method to run the assembler
    public static void main(String[] args) throws IOException {
        String inputFilename = "mipsmars.asm"; // Input file path
        List<String> lines = readFile(inputFilename); // Read the input file
        Map<String, Integer> labels = labelSeparation(lines); // Parse the labels and their addresses
        List<String> machineCode = new ArrayList<>();

        // Remove comments and empty lines from the list of lines
        lines.removeIf(line -> line.startsWith("#") || line.isEmpty());

        int address = 0x00400000; // Starting address
        for (String line : lines) {
            // If the line is not a label or directive, translate it
            if (!line.endsWith(":") && !line.startsWith(".")) {
                String code = instruction(line, labels, address); // Translate the instruction
                machineCode.add(code); // Add the machine code to the list
                address += 4; // Increment the address by 4 for each instruction
            }
        }
        writeOutputFile(inputFilename, machineCode); // Write the output file
    }
}
