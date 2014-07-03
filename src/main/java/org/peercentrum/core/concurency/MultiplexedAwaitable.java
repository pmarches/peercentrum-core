package org.peercentrum.core.concurency;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class MultiplexedAwaitable<T> {
	LinkedBlockingQueue<Awaitable<T>> readyAwaitables = new LinkedBlockingQueue<>();
	
	public void add(Awaitable<T> newFuture){
		newFuture.setAwaiter(this);
	}
	
	public Awaitable<T> waitForAnyOne() throws InterruptedException{
		return readyAwaitables.take();
	}

	public List<Awaitable<T>> waitForAll(long timeout, TimeUnit unit) throws InterruptedException {
		LinkedList<Awaitable<T>> doneAwaitables = new LinkedList<Awaitable<T>>();
		while(true){
			Awaitable<T> ready = readyAwaitables.poll(timeout, unit);
			if(ready==null){
				break;
			}
			doneAwaitables.add(ready);
		};
		return doneAwaitables;
	}

	public List<Awaitable<T>> waitForAll() throws InterruptedException{
		LinkedList<Awaitable<T>> doneAwaitables = new LinkedList<Awaitable<T>>();
		while(true){
			Awaitable<T> ready = readyAwaitables.poll();
			if(ready==null){
				break;
			}
			doneAwaitables.add(ready);
		};
		return doneAwaitables;
	}

	public boolean hasCompleted(){
		return false;
	}
	
	public void notifyResultIsSet(Awaitable<T> futureThatIsNowSet){
		readyAwaitables.add(futureThatIsNowSet);
	}
	
}
