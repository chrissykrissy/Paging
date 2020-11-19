import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.stream.IntStream;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;

public class lab4 {
    public static int nextWordRef (double A, double B, double C, int S, int last, StringTokenizer str){
        //as stated in the lab pdf

        double y = Integer.parseInt(str.nextToken()) / (Integer.MAX_VALUE + 1d);
        if (y < A){
            last = (last + 1) % S;
        }else if (y < A + B){
            last = (last - 5 + S) % S;
        } else if(y < A + B + C) {
            last = (last + 4) % S;
        } else {
            last = Integer.parseInt(str.nextToken()) % S;
        }
        return last;
    }
    public static void main(String[] args) throws IOException {
        int M = Integer.parseInt(args[0]);
        int P = Integer.parseInt(args[1]);
        int S = Integer.parseInt(args[2]);
        int J = Integer.parseInt(args[3]);
        int N = Integer.parseInt(args[4]);
        String R = args[5];

        System.out.println("The machine size is " + M+".");
        System.out.println("The page size is " + P+".");
        System.out.println("The process size is " + S+".");
        System.out.println("The job mix number is " + J+".");
        System.out.println("The number of references per process is " + N+".");
        System.out.println("The replacement algorithm is " + R+".\n");

        StringTokenizer tk = new StringTokenizer(new String(readAllBytes(get("random-numbers"))));

        ArrayList<double[]> prob = new ArrayList<>();
        int numPro = 0;
        switch(J) {
            case 1:
                numPro = 1;
                prob.add(new double[]{1, 0, 0});
                break;
            case 2:
                numPro = 4;
                for (int i = 0; i < numPro; i++) {
                    prob.add(new double[]{1, 0, 0});
                }
                break;
            case 3:
                numPro = 4;
                for (int i = 0; i < numPro; i++) {
                    prob.add(new double[]{0, 0, 0});
                }
                break;
            case 4:
                numPro = 4;
                prob.add(new double[]{0.75, 0.25, 0});
                prob.add(new double[]{0.75, 0, 0.25});
                prob.add(new double[]{0.75, 0.125, 0.125});
                prob.add(new double[]{0.5, 0.125, 0.125});
                break;
        }

        int[] wdArr = new int[numPro];
        int[] faults = new int[numPro];
        int[] runTime = new int[M/P];
        int[] eviction = new int[numPro];
        double[] residency = new double[numPro];
        int[] lines = new int[numPro];
        Arrays.fill(lines, N);

        //getting word
        for (int i = 0; i < numPro; i++) {
            wdArr[i] = (111 * (i + 1)) % S;
        }

        int firstFreeFrame = -1;
        int matchedFrame = -1;
        int replace = -1;

        int curr = 0;

        Integer[][] table = new Integer[M/P][4];
        for(Integer[] r : table){
            Arrays.fill(r,-1);
        }

        int i = 1;
        while (i < N*numPro+1) {
            matchedFrame = -1;
            {
                int j = 0;
                while (j < table.length) {
                    if (table[j][0] == curr) {
                        if (table[j][1] == wdArr[curr] / P) {
                            matchedFrame = j;
                            break;
                        }
                    }
                    j++;
                }
            }

            if (matchedFrame != -1){
                table[matchedFrame][2] = i;
            }else{
                faults[curr]++;
                firstFreeFrame = IntStream.range(0, table.length).filter(k -> table[k][1] == -1).findFirst().orElse(-1);

                switch (firstFreeFrame) {
                    case -1:
                        switch (R) {
                            case "lru":
                                replace = 0;
                                for (int k = 1; k < table.length; k++) {
                                    if (table[replace][2] > table[k][2]) {
                                        replace = k;
                                    }
                                }
                                break;
                            case "random":
                                replace = (Integer.parseInt(tk.nextToken()) + 1) % table.length;
                                break;
                            case "fifo":
                                int tempTime = Integer.MAX_VALUE;
                                for (int j = 0; j < table.length; j++) {
                                    if (table[j][3] < tempTime) {
                                        tempTime = table[j][3];
                                        replace = j;
                                    }
                                }
                                break;
                        }

                        residency[table[replace][0]] += i - runTime[replace];
                        eviction[table[replace][0]]++;

                        table[replace][0] = curr;
                        table[replace][1] = wdArr[curr] / P;
                        table[replace][2] = i;
                        table[replace][3] = i;
                        runTime[replace] = i;
                        break;
                    default:
                        table[firstFreeFrame][0] = curr;
                        table[firstFreeFrame][1] = wdArr[curr] / P;
                        table[firstFreeFrame][2] = i;
                        runTime[firstFreeFrame] = i;
                        break;
                }
            }
            wdArr[curr] = nextWordRef(prob.get(curr)[0],prob.get(curr)[1],prob.get(curr)[2],S, wdArr[curr],tk);
            lines[curr]--;

            if (lines[curr] != 0) {
                if (i % 3 == 0 && lines[curr] > N % 3 - 1){
                    curr = (curr + 1) % numPro;
                }
            } else {
                curr++;
            }
            i++;
        }

        for (int j = 0; j < faults.length; j++) {
            System.out.println("Process " + (j+1) + " had " + faults[j] + " faults and average residency is " + ((eviction[j] == 0) ? "undefined." : (residency[j]/eviction[j]) + "."));
        }

        int totalFaults = 0;
        double totalEvic = 0;
        double totalRes = 0;
        for (int j = 0; j < numPro; j++) {
            totalFaults += faults[j];
            totalEvic += eviction[j];
            totalRes += residency[j];
        }

        System.out.println("\nThe total number of faults is "+totalFaults+" and the overall average residency is "+((totalEvic == 0)? "undefined." : (totalRes/totalEvic) + "."));
    }
}