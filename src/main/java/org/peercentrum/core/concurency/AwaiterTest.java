package org.peercentrum.core.concurency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class AwaiterTest {
	
	@Test
	public void testObserver(){
		class MyObservable extends Observable{
			public MyObservable() {
				setChanged();
			}
		};
		final Observable myobservable = new MyObservable();
		final Thread originalThread = Thread.currentThread();
		Observer myObserver = new Observer() {
			@Override
			public void update(Observable arg0, Object arg1) {
				assertSame(originalThread, Thread.currentThread());
			}
		};
		myobservable.addObserver(myObserver);
		myobservable.notifyObservers(new Object());
	}
	
	@Test
	public void testFutureAwaiter() throws InterruptedException, ExecutionException {
		final Random rnd = new Random();
		MultiplexedAwaitable<Integer> multiAwaitable = new MultiplexedAwaitable<>();
		final Awaitable<Integer> neverCompletingAwaitable = new Awaitable<>(multiAwaitable);
		final Awaitable<Integer> earlyCompletingAwaitable = new Awaitable<>(multiAwaitable);
		final Awaitable<Integer> standAloneEarlyCompletingAwaitable = new Awaitable<>();
		Thread earlySetter=new Thread(){
			@Override
			public void run() {
				earlyCompletingAwaitable.set(22);
				standAloneEarlyCompletingAwaitable.set(42);
			}
		};
		earlySetter.start();
		assertEquals(42, standAloneEarlyCompletingAwaitable.get().intValue());
		
		for(int i=0; i<1000; i++){
			final Awaitable<Integer> awaitable = new Awaitable<>();
			multiAwaitable.add(awaitable);
			new Thread(){
				@Override
				public void run() {
					try {
						Thread.sleep(rnd.nextInt(100));
						awaitable.set(rnd.nextInt());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}.start();
		}

		assertSame(earlyCompletingAwaitable, multiAwaitable.waitForAnyOne());
		assertEquals(22, earlyCompletingAwaitable.get().intValue());

		Awaitable<Integer> secondOneDone = multiAwaitable.waitForAnyOne();
		assertNotNull(secondOneDone);
		assertNotNull(secondOneDone.get());
		assertTrue(secondOneDone.isDone());
		
		for(Awaitable<Integer> currentDone : multiAwaitable.waitForAll(100, TimeUnit.MILLISECONDS)){
			assertNotNull(currentDone);
			assertNotNull(currentDone.get());
			assertTrue(currentDone.isDone());
		}
		assertFalse(neverCompletingAwaitable.isDone());
	}

}
