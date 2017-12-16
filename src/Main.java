import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    public static final int MAGIC = 25734;
    public static boolean bypassConfirmation = false;

    public static void main(String[] args) throws Exception {
        int ins = loadState("original");
        while (ins >= 0 && ins < memsize) {
            Opcode opcode = Opcode.values()[get(memory[ins])];
            ins = execute(opcode, ins);
        }
    }

    public static void initialise() {
        registers = new int[NUMREGS];
        memory = new int[MAX_MEMSIZE];
        stack = new Stack<>();
        buffer = new LinkedList<>();
        in = new Scanner(System.in);
    }

    public static int loadState(String line) throws IOException {
        initialise();

        String dir = "output/";
        if (!line.isEmpty()) dir += line + "/";

        // load binary
        Path path = Paths.get(dir + "memory.bin");
        byte[] bin = Files.readAllBytes(path);
        memsize = bin.length / 2;
        for (int i = 0; i < memsize; i++) {
            memory[i] = (ubyte(bin[2 * i + 1]) << 8) + ubyte(bin[2 * i]);
        }

        int ins = 0;
        File file = new File(dir + "state.txt");
        if (file.exists()) {
            Scanner fin = new Scanner(file);
            ins = fin.nextInt();

            for (int i = 0; i < NUMREGS; i++) registers[i] = fin.nextInt();
            while (fin.hasNextInt()) stack.add(fin.nextInt());

            if (bypassConfirmation) {
                // set reg[0] to 6
                memory[5489] = Opcode.SET.ordinal();
                memory[5490] = ULIMIT;
                memory[5491] = 6;

                // set reg[1] to 1
                memory[5492] = Opcode.SET.ordinal();
                memory[5493] = ULIMIT + 1;
                memory[5494] = 1;

                registers[NUMREGS - 1] = MAGIC;
            }
        }

        return ins;
    }

    public static void saveState(int ins, String line) throws IOException {
        String dir = "output/";
        if (!line.isEmpty()) dir += line + "/";

        boolean successful = new File(dir).mkdir();
        PrintWriter writer = new PrintWriter(dir + "state.txt", "UTF-8");

        writer.println(ins);
        for (int i = 0; i < NUMREGS; i++) {
            writer.println(registers[i]);
        }

        ArrayList<Integer> currentStack = new ArrayList<>();
        while (!stack.isEmpty()) currentStack.add(stack.pop());
        Collections.reverse(currentStack);
        for (int x : currentStack) {
            writer.println(x);
            stack.add(x);
        }
        writer.close();

        byte[] byteArray = new byte[2 * memsize];
        for (int i = 0; i < memsize; i++) {
            int x = memory[i];
            byteArray[2 * i + 1] = (byte) (x >> 8);
            byteArray[2 * i] = (byte) (x & ((1 << 8) - 1));
        }

        Path path = Paths.get(dir + "memory.bin");
        java.nio.file.Files.write(path, byteArray);
    }

    public static int ubyte(byte x) {
        return x >= 0 ? x : 256 + x;
    }

    public static class Args {
        int a, b, c;
    }

    public static Args getArgs(int numArgs, int ins) {
        Args args = new Args();
        if (numArgs >= 1) args.a = memory[ins + 1];
        if (numArgs >= 2) args.b = memory[ins + 2];
        if (numArgs >= 3) args.c = memory[ins + 3];
        return args;
    }

    public static int get(int x) {
        assert x >= 0 && x < ULIMIT + NUMREGS : "invalid argument value";
        if (x < ULIMIT) return x; // literal
        return registers[x - ULIMIT]; // register
    }

    public static int execute(Opcode opcode, int ins) throws Exception {
        Args args = getArgs(opcode.numArgs, ins);
        int next = ins + 1 + opcode.numArgs;

        switch (opcode) {
            case HALT:
                System.exit(0);
                break;

            case IN:
                while (buffer.isEmpty()) {
                    String line = in.nextLine();
                    // hook in commands
                    if (line.startsWith("save")) {
                        System.err.println("Saving state...");
                        saveState(ins, line.substring(5));
                    } else if (line.startsWith("load")) {
                        System.err.println("Loading state...");
                        return loadState(line.substring(5));
                    } else if (line.startsWith("bypass")) {
                        bypassConfirmation = !bypassConfirmation;
                        System.err.println("Toggle bypass into state: " + bypassConfirmation);
                    } else {
                        for (char c : line.toCharArray()) {
                            buffer.add(c);
                        }
                        buffer.add('\n');
                    }
                }
                registers[args.a - ULIMIT] = (int) buffer.poll();
                break;

            case OUT:
                System.out.print((char) get(args.a));
                break;

            case NOOP:
                break;

            case SET:
                registers[args.a - ULIMIT] = get(args.b);
                break;

            // Arithmetic
            case EQ:
                registers[args.a - ULIMIT] = get(args.b) == get(args.c) ? 1 : 0;
                break;

            case GT:
                registers[args.a - ULIMIT] = get(args.b) > get(args.c) ? 1 : 0;
                break;

            case ADD:
                registers[args.a - ULIMIT] = (get(args.b) + get(args.c)) % ULIMIT;
                break;

            case MULT:
                registers[args.a - ULIMIT] = (get(args.b) * get(args.c)) % ULIMIT;
                break;

            case MOD:
                registers[args.a - ULIMIT] = get(args.b) % get(args.c);
                break;

            // Logical
            case AND:
                registers[args.a - ULIMIT] = get(args.b) & get(args.c);
                break;

            case OR:
                registers[args.a - ULIMIT] = get(args.b) | get(args.c);
                break;

            case NOT:
                registers[args.a - ULIMIT] = get(args.b) ^ (ULIMIT - 1);
                break;

            // Jumps
            case JMP:
                return get(args.a);

            case JT:
                if (get(args.a) != 0)
                    return get(args.b);
                break;

            case JF:
                if (get(args.a) == 0)
                    return get(args.b);
                break;

            // Stack
            case PUSH:
                stack.push(get(args.a));
                break;

            case POP:
                registers[args.a - ULIMIT] = stack.pop();
                break;

            case RET:
                if (stack.empty()) execute(Opcode.HALT, -1);
                return stack.pop();

            case CALL:
                stack.push(next);
                return get(args.a);

            // Memory
            case RMEM:
                registers[args.a - ULIMIT] = memory[get(args.b)];
                break;

            case WMEM:
                memory[get(args.a)] = get(args.b);
                break;

            default:
                System.out.println(memory[ins]);
                System.out.println("DEFAULT REACHED!");
                System.exit(0);
                break;
        }
        return next;
    }

    public static final int NUMREGS = 8;
    public static final int ULIMIT = (1 << 15);
    public static final int MAX_MEMSIZE = (1 << 15);
    public static int[] registers;
    public static int[] memory;
    public static Stack<Integer> stack;
    public static Queue<Character> buffer;
    public static int memsize;
    public static Scanner in;
    public static PrintWriter out;

    public enum Opcode {
        HALT(0), SET(2), PUSH(1), POP(1), EQ(3),
        GT(3), JMP(1), JT(2), JF(2), ADD(3),
        MULT(3), MOD(3), AND(3), OR(3), NOT(2),
        RMEM(2), WMEM(2), CALL(1), RET(0), OUT(1),
        IN(1), NOOP(0);

        public int numArgs;

        private Opcode(int numArgs) {
            this.numArgs = numArgs;
        }
    }
}

