/*  Name:   
     Course: CNT 4714 Spring 2023 
     Assignment title: Project 2 – Synchronized, Cooperating Threads Under Locking 
     Due Date: February 12, 2023 
*/ 
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.Random;

public class Project2 {
    private int balance;
    private int transactionNum;
    private Random rand;
    private ReentrantLock accessLock;
    private Condition canWithdraw;
    private Condition canDeposit;
    private ExecutorService threadExecuter;

    public Project2 () {
        balance = 0;
        transactionNum = 0;
        rand = new Random();
        accessLock = new ReentrantLock();
        canWithdraw = accessLock.newCondition();
        canDeposit = accessLock.newCondition();

        threadExecuter = Executors.newFixedThreadPool(16);

        try {
            File myObj = new File("transactions.txt");
            if (myObj.delete()) {
                myObj.createNewFile();
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    

        for (int i = 0; i < 5; i++) {
            threadExecuter.execute(new Deposit("DT" + String.valueOf(i)));
        }
        for (int j = 0; j < 10; j++) {
            threadExecuter.execute(new Withdraw("WT" + String.valueOf(j)));
        }       
        threadExecuter.execute(new Auditor()); 
        threadExecuter.shutdown();
    }

    public class Deposit extends Thread {
        Random thisRand;
        String name;
        public Deposit(String name) {
            this.name = name;
            thisRand = Project2.this.rand;
        }
        public void run() {
            try {
                Thread.sleep(thisRand.nextInt(100));
            }
            catch (InterruptedException e) {  
            }
            int depositNum = 0;
            String output = "";
            while (true) {
                Project2.this.accessLock.lock();
                depositNum = thisRand.nextInt(500) + 1;
                Project2.this.balance += depositNum;
                output += "Agent " + this.name + " deposits $" + String.valueOf(depositNum);
                Project2.this.transactionNum += 1;

                output += "\t\t\t\t\t(+) $" + Project2.this.balance;
                output += "\t\t" + String.valueOf(Project2.this.transactionNum);
                Project2.this.canWithdraw.signalAll();
                System.out.println(output);

                if (depositNum > 350) {
                    System.out.println(" * * * Flagged Transaction - Depositor Agent " + this.name + " Made a deposit in the excess of $350.00 USD - See Flagged Transaction Log");
                    try {
                        Date dNow = new Date();
                        SimpleDateFormat f = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a zzz");
                        FileWriter myWriter = new FileWriter("transactions.txt", true);
                        myWriter.write("Depositor Agent " + this.name + " issued deposit of $" + String.valueOf(depositNum) + " at: " + f.format(dNow) + "   Transaction Number: " + Project2.this.transactionNum + "\n");
                        myWriter.close();
                    } catch (IOException e) {
                        System.out.println("An error occurred.");
                        e.printStackTrace();
                    }
                }
                Project2.this.accessLock.unlock();
                try {
                    Thread.sleep(thisRand.nextInt(1500));
                }
                catch (InterruptedException e) {  
                }
                output = "";
            }
        }
    }

    public class Withdraw extends Thread {
        Random thisRand;
        String name;
        public Withdraw(String name) {
            this.name = name;
            thisRand = Project2.this.rand;
        }
        public void run() {
            try {
                Thread.sleep(thisRand.nextInt(100));
            }
            catch (InterruptedException e) {  
            }
            int withdrawNum = 0;
            String output = "";
            while (true) {
                Project2.this.accessLock.lock();
                withdrawNum = thisRand.nextInt(99) + 1;
                output += "\t\t\tAgent " + this.name + " withdraws $" + String.valueOf(withdrawNum);
                if (Project2.this.balance >= withdrawNum) {
                    Project2.this.balance -= withdrawNum;
                    Project2.this.transactionNum += 1;
                    output += "\t\t(-) $" + String.valueOf(Project2.this.balance);
                    if (Project2.this.balance < 100)
                        output += "\t";
                    output += "\t\t" + String.valueOf(Project2.this.transactionNum);

                    if (withdrawNum > 75){
                        System.out.println(" * * * Flagged Transaction - Withdrawal Agent " + this.name + " Made a withdrawal in the excess of $75.00 USD - See Flagged Transaction Log");
                        try {
                            Date dNow = new Date();
                            SimpleDateFormat f = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a zzz");
                            FileWriter myWriter = new FileWriter("transactions.txt", true);
                            myWriter.write("\tWithdrawal Agent " + this.name + " issued withdrawal of $" + String.valueOf(withdrawNum) + " at: " + f.format(dNow) + "   Transaction Number: " + Project2.this.transactionNum + "\n");
                            myWriter.close();
                        } catch (IOException e) {
                            System.out.println("An error occurred.");
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    output += "\t\tINSUFFICIENT FUNDS!";
                    try {
                        Project2.this.canWithdraw.await();
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                System.out.println(output);
                Project2.this.accessLock.unlock();
                try {
                    Thread.sleep(thisRand.nextInt(500));
                }
                catch (InterruptedException e) {  
                }
                output = "";
            }
        }
    }

    public class Auditor extends Thread {
        Random thisRand;
        int lastTransaction;
        public Auditor() {
            this.thisRand = Project2.this.rand;
            lastTransaction = 0;
        }
        public void run() {
            try {
                Thread.sleep(thisRand.nextInt(100));
            }
            catch (InterruptedException e) {  
            }
            int currentTransaction;
            int balance;
            while (true) {
                try {
                    Thread.sleep(thisRand.nextInt(3000));
                }
                catch (InterruptedException e) {  
                }
                currentTransaction = Project2.this.transactionNum;
                balance = Project2.this.balance;
                System.out.println("*".repeat(100));
                System.out.println("\t\tAUDITOR FINDS BALANCE TO BE: $" + balance + "\tNumber of Transactions since last audit: " + (currentTransaction - lastTransaction));
                System.out.println("*".repeat(100));
                lastTransaction = currentTransaction;
            }
        }
    }
    public static void main (String [] args)
    {
        System.out.println("Deposit Agents\t\tWithdrawal Agents\t\tBalance\t\tTransaction Number");
        System.out.println("--------------\t\t-----------------\t\t-------\t\t------------------");
        Project2 bank = new Project2();
    }
}