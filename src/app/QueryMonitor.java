package app;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import edu.iastate.cs.boa.BoaClient;
import edu.iastate.cs.boa.BoaException;
import edu.iastate.cs.boa.ExecutionStatus;
import edu.iastate.cs.boa.JobHandle;
import edu.iastate.cs.boa.NotLoggedInException;

public class QueryMonitor extends Thread {
	private static String username; 
	private static String password; 
	private static BoaClient client; 
	private static int numRequests = 0; 
	public static void main(String args[]) {
		//getting username and password 
		if(args.length != 2) {
			System.out.println("usage: boaUsername boaPassword");
		}
		else {
			username = args[0];
			password = args[1];
		}
		
		//log in
		client = null; 
		try {
			client = new BoaClient();
			client.login(username, password);
		} catch (BoaException e) {
			System.out.println("Invalid username or password");
			e.printStackTrace();
		}	 
		
		QueryMonitor m = new QueryMonitor();
		m.run();
		
	}

	public void run() {
		//get most recent 10 jobs
		ArrayList<Integer> runningJobIDs = new ArrayList<Integer>();
		runningJobIDs = addRecentJobs(runningJobIDs);
		System.out.println("Monitoring " + runningJobIDs.size() + " projects");
		int numJobs = runningJobIDs.size(); 
		while(true) {
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			ArrayList<Integer> finishedJobs = findFinishedJobs(runningJobIDs);
			if(finishedJobs.size() != 0) {
				String infoMessage = "Jobs ";
				for(Integer f: finishedJobs) {
					infoMessage += f + " ";
				}
				infoMessage += "have finished.";
				
				JFrame dummyFrame = new JFrame(); 
				dummyFrame.setVisible(true);
				dummyFrame.setAlwaysOnTop(true);
				JOptionPane.showMessageDialog(dummyFrame, infoMessage, "QueryMonitor", JOptionPane.INFORMATION_MESSAGE);
				dummyFrame.dispose(); 
			}
			
			runningJobIDs = addRecentJobs(runningJobIDs);
			//System.out.println("NumRequests: " + numRequests);
			if(numJobs != runningJobIDs.size()) {
				numJobs = runningJobIDs.size(); 
				System.out.println("Now monitoring " + numJobs + " jobs");
			}
		}
		
	}
		
	private static ArrayList<Integer> addRecentJobs(ArrayList<Integer> runningJobIDs) {
		try {
			numRequests++; 
			List<JobHandle> recentJobs = client.getJobList();
			for(JobHandle j: recentJobs) {
				numRequests++; 
				if(j.getExecutionStatus() == ExecutionStatus.RUNNING
						&& !runningJobIDs.contains(new Integer(j.getId()))) {
					runningJobIDs.add(new Integer(j.getId()));
				}
			}
			
		} catch (NotLoggedInException e) {
			e.printStackTrace();
		} catch (BoaException e) {
			e.printStackTrace();
		}
		return runningJobIDs; 
	}
	
	private static boolean checkStillRunning(Integer i) {
		JobHandle job = null;
		try {
			job = client.getJob(i.intValue());
			numRequests++; 
		} catch (NotLoggedInException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BoaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		numRequests++; 
		if(job.getExecutionStatus() != ExecutionStatus.RUNNING) {
			return false; 
		}
		return true; 
	}
	
	private static ArrayList<Integer> findFinishedJobs(ArrayList<Integer> runningJobIDs) {
		ArrayList<Integer> finishedJobs = new ArrayList<Integer>();
		for(Integer j: runningJobIDs) {
			if(!checkStillRunning(j)) {
				finishedJobs.add(j);
			}
		}
		for(Integer finished: finishedJobs) {
			runningJobIDs.remove(finished);
		}
		return finishedJobs; 
	}
}
