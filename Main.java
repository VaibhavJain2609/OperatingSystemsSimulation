/*
 Vaibhav Jain
 Fall 2023
 CSC 139-05
 Language: Java
 Tested on Mac OSX running Java 20
 */


import java.io.*;
import java.util.*;

public class Main {
    static int schedulingOrder;
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        Scanner scanner = null;
        PrintWriter files = null;
        Process processes[];
        Process running = null;

        try {
            scanner = new Scanner(new File("input16.txt"));
        } catch (FileNotFoundException e) {
            System.out.println("Input file not found");
            return;
        }

        try {
            files = new PrintWriter("output16.txt", "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            System.out.println("Error creating output file");
            return;
        }

        int timeQuantum = 0;
        String scheduler = "";

        if (scanner.hasNext()) {
            scheduler = scanner.next();
            timeQuantum = (scheduler.equals("RR") && scanner.hasNextInt()) ? scanner.nextInt() : 0;
        }

        if (scanner.hasNextInt()) {
            int numberOfProcesses = scanner.nextInt();
            processes = new Process[numberOfProcesses];

            for (int i = 0; i < numberOfProcesses; i++) {
                if (scanner.hasNextInt()) {
                    int pid = scanner.nextInt();
                    int arrivalTime = scanner.nextInt();
                    int burstTime = scanner.nextInt();
                    int priority = scanner.nextInt();
                    processes[i] = new Process(pid, arrivalTime, burstTime, priority);
                } else {
                    return;
                }
            }
        } else {
            return;
        }

        switch(scheduler){
            case "RR":
                files.println("RR" + " " + timeQuantum);
                schedulingOrder = 1;
                Arrays.sort(processes);
                roundRobin(files, processes, timeQuantum, running);
                break;
            case "SJF":
                files.println("SJF");
                schedulingOrder = 2;
                Arrays.sort(processes);
                shortestJobFirst(files, processes,running);
                break;
            case "PR_noPREMP":
                files.println("PR_noPREMP");
                schedulingOrder = 1;
                Arrays.sort(processes);
                priorityNoPremp(files, processes, running);
                break;
            case "PR_withPREMP":
                files.println("PR_withPREMP");
                schedulingOrder=1;
                Arrays.sort(processes);
                priorityWithPreEmp(files, processes, running);
                break;
            default:
                return;
        }
        scanner.close();
        files.close();

    }

    private static void roundRobin(PrintWriter files, Process[] processes, int timeQuantum, Process running) {
        Queue<Process> readyQueue = new LinkedList<>();
        int last=-1;
        int totalTime=0;
        int processesCompleted=0;
        while(processesCompleted<processes.length){
            for(Process x: processes){
                if(last < x.getArrivalTime() && x.getArrivalTime() <= totalTime){
                    readyQueue.add(x);
                }
            }

            if(running!=null && running.getCompleted()!=0)
            {
                readyQueue.add(running);
            }
            running=readyQueue.remove();
            last=totalTime;
            files.println(totalTime+" "+running.getPid());
            totalTime+=running.execute(timeQuantum,totalTime);
            if(running.getCompleted()==0) {
                processesCompleted++;
                int totalTurnAroundTime = totalTime - running.getArrivalTime();
                int waitingTime = totalTurnAroundTime - running.getBurstTime();
                running.setWaitingTime(waitingTime);
            }

        }
        average_waiting_time(processes, files);
    }

    private static void shortestJobFirst(PrintWriter files, Process[] processes, Process running ) {
        List<Process> readyQueue = new ArrayList<>();
        int processDone = 0, time = 0;

        while (processDone < processes.length) {
            // Add processes to the ready queue based on arrival time
            for (Process process : processes) {
                if (process.getArrivalTime() <= time && process.getCompleted() > 0 && !readyQueue.contains(process)) {
                    readyQueue.add(process);
                }
            }

            // Sort the ready queue based on burst time
            Collections.sort(readyQueue);

            // If there's no running process and the ready queue is not empty, start a new process
            if (running == null && !readyQueue.isEmpty()) {
                running = readyQueue.remove(0);
                files.println(time + " " + running.getPid());
            }

            // Execute the running process for a time unit
            if (running != null) {
                int burstTimeBefore = running.getCompleted();
                time += running.execute(0, time);
                int burstTimeAfter = running.getCompleted();

                // Update process information after execution
                if (burstTimeBefore > 0 && burstTimeAfter == 0) {
                    processDone++;
                    running.setWaitingTime(time - running.getBurstTime() - running.getArrivalTime());
                }

                // Add the process back to the ready queue if it still has remaining burst time
                if (burstTimeAfter > 0 && !readyQueue.contains(running)) {
                    readyQueue.add(running);
                }

                running = null;
            } else {
                // Increment time if no process is currently running
                time++;
            }
        }

        average_waiting_time(processes, files);
    }

    private static void priorityNoPremp(PrintWriter files, Process[] processes, Process running){
        List<Process> readyQueue = new ArrayList<Process>();
        int processCompleted = 0;
        int totalTime = 0;

        while (processCompleted < processes.length) {
            for (Process process : processes) {
                if (totalTime >= process.getArrivalTime() && process.getCompleted() > 0
                && !readyQueue.contains(process)) {
                    readyQueue.add(process);
                }
            }
            if(running== null && !readyQueue.isEmpty()){
                schedulingOrder = 3;
                Collections.sort(readyQueue);
                running = readyQueue.remove(0);
                files.println(totalTime + " " + running.getPid());
            }

            if (running != null) {
                int burstTimeBefore = running.getCompleted();
                totalTime += running.execute(0, totalTime);
                int burstTimeAfter = running.getCompleted();

                if (burstTimeBefore > 0 && burstTimeAfter == 0) {
                    processCompleted += 1;
                    running.setWaitingTime(totalTime - running.getBurstTime() - running.getArrivalTime());
                }

                if (burstTimeAfter > 0 && !readyQueue.contains(running)) {
                    readyQueue.add(running);
                }

                running = null;
            } else {
                totalTime++;
            }
        }
        average_waiting_time(processes, files);
    }
    private static void priorityWithPreEmp(PrintWriter files, Process[] processes, Process running) {
        Vector<Process> readyQueue = new Vector<>();
        int processDone = 0;
        int time = 0;
        int last = -1;

        while(processDone<processes.length){
            for(Process x: processes){
                if(last<x.getArrivalTime() && x.getArrivalTime() <= time){
                    readyQueue.add(x);
                }
            }
            if(running!=null && running.getCompleted()!=0)
            {
                readyQueue.add(running);
            }
            schedulingOrder = 3;
            Collections.sort(readyQueue);
            running = readyQueue.remove(0);
            int timeTaken=time+running.getCompleted();
            int interruptTime=0;
            for(int i=0;i<processes.length;i++)
            {
                if(last<processes[i].getArrivalTime() && processes[i].getArrivalTime()<timeTaken &&
                        processes[i].getPriority()<running.getPriority())
                {
                    interruptTime=processes[i].getArrivalTime()-time;
                    break;
                }
            }
            last=time;
            files.println(time+" "+running.getPid());
            time+=running.execute(interruptTime,time);
            if(running.getCompleted()==0) {
                processDone+=1;
                running.setWaitingTime(time-running.getBurstTime()-running.getArrivalTime());
            }
        }

        average_waiting_time(processes, files);
    }
    private static void average_waiting_time(Process[] processes, PrintWriter files) {
        int totalWaitingTime = 0;
        for(Process x: processes){
            totalWaitingTime += x.getWaitingTime();
            x.reset();
        }
        files.println("AVG Waiting Time: "+ (double) totalWaitingTime / (double) processes.length);
    }
    private static class Process implements Comparable {
        private int pid, arrivalTime, burstTime, priority, completed, waitingTime ;

        public Process(int pid, int arrivalTime, int burstTime, int priority) {
            this.pid = pid;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.priority = priority;
            this.completed=burstTime;
            this.waitingTime=0;
        }
        public void reset()
        {
            waitingTime=0;
            completed=burstTime;
        }
        public int execute(int timeQuantum, int time)
        {
            int time2=0;
            if (timeQuantum > 0) {
                time2 = Math.min(timeQuantum, completed);
                completed -= time2;
            } else {
                time2 = completed;
                completed = 0;
            }

            return time2;
        }
        @Override
        public int compareTo(Object o) {
            int otherArrivalTime = ((Process) o).getArrivalTime();
            int otherBurstTime = ((Process) o).getBurstTime();
            int otherPriority = ((Process) o).getPriority();
            int compareBurstTime = Integer.compare(this.getBurstTime(), otherBurstTime);
            int compareArrivalTime = Integer.compare(this.getArrivalTime(), otherArrivalTime);
            int comparePriority = Integer.compare(this.getPriority(), otherPriority);
            switch (schedulingOrder) {
                case 1:
                    return compareArrivalTime;
                case 2:
                    return compareBurstTime;
                case 3:
                    return comparePriority;
                default:
                    return 0;
            }

        }
        public int getPid() {
            return pid;
        }
        public int getArrivalTime() {
            return arrivalTime;
        }

        public int getBurstTime() {
            return burstTime;
        }


        public int getPriority() {
            return priority;
        }

        public int getCompleted() {
            return completed;
        }
        public int getWaitingTime() {
            return waitingTime;
        }
        public void setWaitingTime(int waitingTime) {
            this.waitingTime = waitingTime;
        }
    }

}
