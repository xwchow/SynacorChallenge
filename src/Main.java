import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        registers = new int[NUMREGS];
        memory = new int[MAX_MEMSIZE];
        stack = new Stack<>();
        buffer = new LinkedList<>();
        in = new Scanner(System.in);

        // execute program
        int ins = readBinary();
        while (ins >= 0 && ins < memsize) {
            Opcode opcode = Opcode.values()[getV(memory[ins])];
            ins = execute(opcode, ins);
        }
    }

    public static final int MAGIC = 25734;
    public static final boolean bypassConfirmation = true;

    public static int readBinary() throws Exception {
        String pathToFiles = "output/synacor/";
        // load state
        Scanner fin = new Scanner(new File(pathToFiles + "state.txt"));
        int ins = fin.nextInt();

        // load registers
        for (int i = 0; i < NUMREGS; i++) registers[i] = fin.nextInt();
        registers[NUMREGS - 1] = MAGIC;

        // fill stack
        while (fin.hasNextInt()) stack.add(fin.nextInt());

        // load binary
        Path path = Paths.get(pathToFiles + "memory.bin");
        byte[] binBytes = Files.readAllBytes(path);
        memsize = binBytes.length / 2;
        for (int i = 0; i < memsize; i++) {
            memory[i] = (ubyte(binBytes[2*i + 1]) << 8) + ubyte(binBytes[2*i]);
        }
        if (bypassConfirmation) {
            memory[5489] = Opcode.SET.ordinal();
            memory[5490] = ULIMIT;
            memory[5491] = 6;

            memory[5492] = Opcode.SET.ordinal();
            memory[5493] = ULIMIT + 1;
            memory[5494] = 1;
        }

        return ins;
    }

    public static int ubyte(byte x) {
        if (x < 0) return 256 + x;
        return x;
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

    public static int getV(int x) {
        assert x >= 0 && x < ULIMIT + NUMREGS : "invalid argument value";
        if (x < ULIMIT) return x; // literal
        return registers[x - ULIMIT]; // register
    }

    public static void memoryDump(int ins) throws Exception {
        PrintWriter writer = new PrintWriter("output/state.txt", "UTF-8");
        writer.println(ins);
        for (int i = 0; i < NUMREGS; i++) {
            writer.print(registers[i] + " ");
        }
        writer.println();
        ArrayList<Integer> stackContents = new ArrayList<>();
        while (!stack.isEmpty()) stackContents.add(stack.pop());
        Collections.reverse(stackContents);
        for (int x : stackContents) {
            writer.print(x + " ");
            stack.add(x);
        }
        writer.println();
        writer.close();

        byte[] byteArray = new byte[2 * memsize];
        for (int i = 0; i < memsize; i++) {
            int x = memory[i];
            byteArray[2*i + 1] = (byte)(x >> 8);
            byteArray[2*i] = (byte)(x & ((1 << 8) - 1));
        }
        Path path = Paths.get("output/memory.bin");
        java.nio.file.Files.write(path, byteArray);
    }

    public static int execute (Opcode opcode, int ins) throws Exception {
        Args args = getArgs(opcode.numArgs, ins);
        int next = ins + 1 + opcode.numArgs;

        switch (opcode) {
            case HALT:
                System.exit(0);
                break;

            case IN:
                while (buffer.isEmpty()) {
                    String line = in.nextLine() + "\n";
                    for (char c : line.toCharArray()) {
                        if (c == '!') {
                            System.err.println("Performing memory dump...");
                            memoryDump(ins);
                        }
                        buffer.add(c);
                    }
                }

                registers[args.a - ULIMIT] = (int)buffer.poll();
                break;

            case OUT:
                System.out.print((char)getV(args.a));
                break;

            case NOOP:
                break;

            case SET:
                registers[args.a - ULIMIT] = getV(args.b);
                break;

            // Arithmetic
            case EQ:
                registers[args.a - ULIMIT] = getV(args.b) == getV(args.c) ? 1 : 0;
                break;

            case GT:
                registers[args.a - ULIMIT] = getV(args.b) > getV(args.c) ? 1 : 0;
                break;

            case ADD:
                registers[args.a - ULIMIT] = (getV(args.b) + getV(args.c)) % ULIMIT;
                break;

            case MULT:
                registers[args.a - ULIMIT] = (getV(args.b) * getV(args.c)) % ULIMIT;
                break;

            case MOD:
                registers[args.a - ULIMIT] = getV(args.b) % getV(args.c);
                break;

            // Logical
            case AND:
                registers[args.a - ULIMIT] = getV(args.b) & getV(args.c);
                break;

            case OR:
                registers[args.a - ULIMIT] = getV(args.b) | getV(args.c);
                break;

            case NOT:
                registers[args.a - ULIMIT] = getV(args.b) ^ (ULIMIT - 1);
                break;

            // Jumps
            case JMP:
                return getV(args.a);

            case JT:
                if (getV(args.a) != 0)
                    return getV(args.b);
                break;

            case JF:
                if (getV(args.a) == 0)
                    return getV(args.b);
                break;

            // Stack
            case PUSH:
                stack.push(getV(args.a));
                break;

            case POP:
                registers[args.a - ULIMIT] = stack.pop();
                break;

            case RET:
                if (stack.empty()) execute(Opcode.HALT, -1);
                return stack.pop();

            case CALL:
                stack.push(next);
                return getV(args.a);

            // Memory
            case RMEM:
                registers[args.a - ULIMIT] = getV(memory[getV(args.b)]);
                break;

            case WMEM:
                memory[getV(args.a)] = getV(args.b);
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
        HALT(0),  SET(2), PUSH(1),  POP(1),   EQ(3),
          GT(3),  JMP(1),   JT(2),   JF(2),  ADD(3),
        MULT(3),  MOD(3),  AND(3),   OR(3),  NOT(2),
        RMEM(2), WMEM(2), CALL(1),  RET(0),  OUT(1),
          IN(1), NOOP(0);

        public int numArgs;

        private Opcode(int numArgs) {
            this.numArgs = numArgs;
        }
    }
}

