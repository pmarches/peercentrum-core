package org.castaconcord.core.concurency;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Awaitable<T> {
	T result;
	Throwable exception;
	MultiplexedAwaitable<T> awaiter;

	public Awaitable() {
	}
	
	public Awaitable(MultiplexedAwaitable<T> awaiter) {
		setAwaiter(awaiter);
	}
	
	synchronized public T get() throws InterruptedException, ExecutionException {
		if(isDone()==false){
			wait();
		}
		return result;
	}

	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if(isDone()==false){
			unit.timedWait(this, timeout);
		}
		return result;
	}

	public boolean isDone() {
		return result!=null || exception!=null;
	}

	synchronized public void set(T result) {
		this.result=result;
		if(this.awaiter!=null){
			this.awaiter.notifyResultIsSet(this);
		}
		notifyAll();
	}

	protected void setAwaiter(MultiplexedAwaitable<T> awaiter){
		this.awaiter=awaiter;
	}
}
