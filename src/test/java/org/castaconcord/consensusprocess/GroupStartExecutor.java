package org.castaconcord.consensusprocess;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GroupStartExecutor {
	LinkedList<Runnable> runnables = new LinkedList<Runnable>();
	private ExecutorService threadPool;
	
	public GroupStartExecutor(int nbThread){
		threadPool = Executors.newFixedThreadPool(nbThread);
	}
	
	public void queue(Collection<Runnable> newRunnables){
		runnables.addAll(newRunnables);
	}

	public void queue(Runnable runnable){
		runnables.add(runnable);
	}

	public void start(){
		for(Runnable currentRunnable : runnables){
			threadPool.execute(currentRunnable);
		}
	}

	public void shutdown() {
		threadPool.shutdown();
	}
}
